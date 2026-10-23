package org.smartregister.chw.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildEidFormUtilsTest {
    @Test public void eligibilityExcludesEighteenthMonthBirthdayAndAllowsNewborns() {
        LocalDate today = new LocalDate(2026, 10, 16);
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChildOn("2026-10-16", today));
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChildOn("2025-04-17T00:00:00.000+03:00", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2025-04-16", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2026-10-17", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn(null, today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2026-02-31", today));
        // August 31 reaches its 18-month anniversary on February 28.
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2024-08-31", new LocalDate(2026, 2, 28)));
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChildOn("2024-08-31", new LocalDate(2026, 2, 27)));
    }

    @Test public void estimatedBirthdatesCannotEstablishEligibility() {
        String dob = LocalDate.now().minusMonths(6).toString();
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChild(dob, "true"));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChild(dob, "1"));
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChild(dob, "false"));
    }

    @Test public void registrationRequiresDobAndRetainsBothSexesInBothLocales() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "all_clients_registration_form.json");
            MotherMentorChildEidFormUtils.configureNewClientForm(form, "Child must be younger than 18 months");
            assertEquals("today", field(form, "dob").getString("max_date"));
            String minimum = field(form, "dob").getString("min_date");
            LocalDate earliest = LocalDate.parse(minimum, org.joda.time.format.DateTimeFormat.forPattern("dd-MM-yyyy"));
            assertTrue(MotherMentorChildEidFormUtils.isEligibleChild(earliest.toString()));
            assertFalse(MotherMentorChildEidFormUtils.isEligibleChild(earliest.minusDays(1).toString()));
            assertFalse(field(form, "dob").has("relevance"));
            assertTrue(field(form, "dob").getJSONObject("v_required").getBoolean("value"));
            assertEquals("hidden", field(form, "dob_unknown").getString("type"));
            assertEquals("false", field(form, "dob_unknown").getString("value"));
            assertEquals("hidden", field(form, "age").getString("type"));
            assertEquals(2, field(form, "sex").getJSONArray("keys").length());
        }
    }

    @Test public void bothLibraryFormsBindChildAndMotherWithoutOverwritingClinicalAnswers() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "mothermentor_enroll_child_eid.json");
            Map<String, String> mother = person("mother", "mother-family", "Neema");
            Map<String, String> child = person("child", "child-family", "Amina");
            child.put("gender", "Female"); child.put("dob", "2026-06-01T00:00:00.000+03:00");
            Map<String, String> previous = new HashMap<>();
            previous.put("hei_registered", "yes"); previous.put("hei_number", "12-34-5678-123456");
            previous.put("breastfeeding_status", "breast_feeding"); previous.put("last_test", "dbs_6");
            previous.put("last_test_result", "negative");
            MotherMentorChildEidFormUtils.prepare(form, mother, child, previous);
            assertEquals("child", form.getString("entity_id"));
            assertEquals("Mother Mentor Enroll Child Eid", form.getString("encounter_type"));
            assertEquals("child-family", value(form, "relational_id"));
            assertEquals("mother", value(form, "mother_base_entity_id"));
            assertEquals("Neema Mtemi", value(form, "mother_name"));
            assertEquals("Amina Mtemi", value(form, "child_name"));
            assertEquals("Female", value(form, "child_sex"));
            assertEquals("2026-06-01", value(form, "child_dob"));
            for (Map.Entry<String, String> answer : previous.entrySet()) assertEquals(answer.getValue(), value(form, answer.getKey()));
            assertTrue(field(form, "mother_name").getBoolean("read_only"));
            assertTrue(field(form, "child_name").getBoolean("read_only"));
            form.put("entity_id", "mother");
            field(form, "relational_id").put("value", "mother-family");
            field(form, "last_test_result").put("value", "waiting");
            MotherMentorChildEidFormUtils.bindIdentity(form, mother, child);
            assertEquals("child", form.getString("entity_id"));
            assertEquals("child-family", value(form, "relational_id"));
            assertEquals("waiting", value(form, "last_test_result"));
        }
    }

    @Test(expected = IllegalArgumentException.class) public void motherCannotBeHerOwnChild() throws Exception {
        Map<String, String> mother = person("mother", "family", "Neema");
        MotherMentorChildEidFormUtils.bindIdentity(MotherMentorTestAssets.form("json.form", "mothermentor_enroll_child_eid.json"), mother, mother);
    }

    private Map<String, String> person(String id, String family, String name) {
        Map<String, String> result = new HashMap<>();
        result.put("base_entity_id", id); result.put("relational_id", family);
        result.put("first_name", name); result.put("last_name", "Mtemi");
        return result;
    }

    private String value(JSONObject form, String key) throws Exception { return field(form, key).getString("value"); }
    private JSONObject field(JSONObject form, String key) throws Exception {
        for (int i = 1; i <= form.getInt("count"); i++) {
            JSONArray fields = form.getJSONObject("step" + i).getJSONArray("fields");
            for (int j = 0; j < fields.length(); j++) if (key.equals(fields.getJSONObject(j).optString("key"))) return fields.getJSONObject(j);
        }
        throw new AssertionError("Missing field " + key);
    }
}
