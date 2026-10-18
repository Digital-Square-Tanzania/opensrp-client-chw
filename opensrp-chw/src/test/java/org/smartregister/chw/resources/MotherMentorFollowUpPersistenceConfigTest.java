package org.smartregister.chw.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.util.MotherMentorFollowUpType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

public class MotherMentorFollowUpPersistenceConfigTest {
    private static final String[] FOLLOW_UP_COLUMNS = {
        "follow_up_category",
        "follow_up_status",
        "service_interruption_reason",
        "service_interruption_reason_other",
        "expected_facility_visit_date",
        "returned_to_facility",
        "facility_return_date",
        "partner_non_attendance_reason",
        "partner_non_attendance_reason_other",
        "partner_expected_facility_visit_date",
        "partner_arrived_at_facility",
        "partner_facility_arrival_date",
        "child_eid_test_type",
        "child_eid_non_attendance_reason",
        "child_eid_non_attendance_reason_other",
        "child_eid_expected_facility_visit_date",
        "child_eid_arrived_at_facility",
        "child_eid_facility_arrival_date",
        "comments"
    };

    @Test
    public void allCategoriesUseDifferentStableEventTypes() {
        assertFalse(
                MotherMentorFollowUpType.MOTHER_IIT
                        .getEventType()
                        .equals(MotherMentorFollowUpType.PARTNER.getEventType()));
        assertEquals(
                MotherMentorFollowUpType.PARTNER,
                MotherMentorFollowUpType.fromEnrollmentTable("ec_mothermentor_enroll_partner"));
        assertFalse(
                MotherMentorFollowUpType.MOTHER_IIT
                        .getEventType()
                        .equals(MotherMentorFollowUpType.CHILD_EID.getEventType()));
        assertFalse(
                MotherMentorFollowUpType.PARTNER
                        .getEventType()
                        .equals(MotherMentorFollowUpType.CHILD_EID.getEventType()));
        assertEquals(
                MotherMentorFollowUpType.CHILD_EID,
                MotherMentorFollowUpType.fromEnrollmentTable("ec_mothermentor_enroll_child_eid"));
    }

    @Test
    public void partnerEventIsClassifiedAndAllAnswersAreMappedAndMigrated() throws Exception {
        JSONObject classification = json("src/nacp/assets/ec_client_classification.json");
        assertTrue(
                classification
                        .toString()
                        .contains(MotherMentorFollowUpType.PARTNER.getEventType()));
        assertTrue(
                classification
                        .toString()
                        .contains(MotherMentorFollowUpType.CHILD_EID.getEventType()));

        JSONObject fields = json("src/nacp/assets/ec_client_fields.json");
        JSONArray columns =
                findTable(
                                fields.getJSONArray("bindobjects"),
                                MotherMentorFollowUpType.FOLLOW_UP_TABLE)
                        .getJSONArray("columns");
        String repository =
                text("src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java");
        for (String column : FOLLOW_UP_COLUMNS) {
            assertEquals("Mapped column must occur once: " + column, 1, count(columns, column));
            String constantName = column.toUpperCase(Locale.ROOT);
            assertEquals(column, DBConstants.KEY.class.getField(constantName).get(null));
            assertTrue(
                    "Missing migration for " + column,
                    repository.contains("DBConstants.KEY." + constantName));
        }
        assertTrue(repository.contains("case 52:"));
        assertTrue(org.smartregister.chw.BuildConfig.DATABASE_VERSION >= 52);
    }

    private JSONObject findTable(JSONArray tables, String name) throws Exception {
        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.getJSONObject(i);
            if (name.equals(table.optString("name"))) return table;
        }
        throw new AssertionError("Missing table: " + name);
    }

    private int count(JSONArray columns, String name) throws Exception {
        int count = 0;
        for (int i = 0; i < columns.length(); i++) {
            if (name.equals(columns.getJSONObject(i).optString("column_name"))) count++;
        }
        return count;
    }

    private JSONObject json(String path) throws Exception {
        return new JSONObject(text(path));
    }

    private String text(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
