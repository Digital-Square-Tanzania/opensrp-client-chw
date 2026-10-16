package org.smartregister.chw.interactor;

import android.database.Cursor;

import com.vijay.jsonwizard.utils.FormUtils;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.util.MotherMentorFollowUpType;

import java.util.Arrays;

import timber.log.Timber;

/** Child EID-specific follow-up workflow and approved age-based test recommendation. */
public class ChildEidFollowUpVisitInteractor extends BaseMotherMentorFollowUpVisitInteractor {
    static final String TEST_6_WEEKS = "six_weeks";
    static final String TEST_9_MONTHS = "nine_months";
    static final String TEST_POST_WEANING = "three_months_after_weaning";
    static final String TEST_18_MONTHS = "eighteen_months";

    public ChildEidFollowUpVisitInteractor(String eventType) {
        super(new FollowUpFormDefinition(eventType, MotherMentorFollowUpType.CHILD_EID.getValue(),
                R.string.mothermentor_follow_up_status_section,
                R.string.mothermentor_child_eid_non_attendance_reason_section,
                R.string.mothermentor_child_eid_arrival_section,
                R.string.mothermentor_comments_section,
                "mothermentor_child_eid_follow_up_status",
                "mothermentor_child_eid_non_attendance_reason",
                "mothermentor_child_eid_arrival",
                "mothermentor_child_eid_follow_up_comment",
                "child_eid_non_attendance_reason",
                "child_eid_non_attendance_reason_other",
                "child_eid_expected_facility_visit_date",
                "child_eid_arrived_at_facility",
                "child_eid_facility_arrival_date",
                "mothermentor_child_eid_reason_")
                .withTestType(R.string.mothermentor_child_eid_test_type_section,
                        "mothermentor_child_eid_test_type", "child_eid_test_type",
                        "mothermentor_child_eid_test_"));
    }

    @Override
    protected String preprocessTestTypeForm(String source) {
        if (StringUtils.isBlank(source) || memberObject == null) return null;
        try {
            String recommendation = recommendTest(loadEnrollmentValue(memberObject.getBaseEntityId(), "m.dob"),
                    loadEnrollmentValue(memberObject.getBaseEntityId(), "e.weaning_date"),
                    loadEnrollmentValue(memberObject.getBaseEntityId(), "e.last_test"), DateTime.now());
            if (recommendation == null) return source;
            JSONObject form = new JSONObject(source);
            JSONObject testField = FormUtils.getFieldFromForm(form, "child_eid_test_type");
            JSONObject noteField = FormUtils.getFieldFromForm(form, "child_eid_test_recommendation");
            if (testField != null && StringUtils.isBlank(testField.optString("value"))) {
                testField.put("value", recommendation);
            }
            if (noteField != null) {
                int labelId = context.getResources().getIdentifier(
                        "mothermentor_child_eid_test_" + recommendation, "string", context.getPackageName());
                String label = labelId == 0 ? recommendation : context.getString(labelId);
                int messageId = TEST_POST_WEANING.equals(recommendation)
                        ? R.string.mothermentor_child_eid_weaning_recommendation
                        : R.string.mothermentor_child_eid_recommendation;
                noteField.put("text", context.getString(messageId, label));
            }
            return form.toString();
        } catch (Exception e) {
            Timber.e(e, "Unable to recommend a Child EID follow-up test");
            return source;
        }
    }

    static String recommendTest(String dob, String weaningDate, String lastTest, DateTime now) {
        DateTime birth = parseDate(dob);
        if (birth == null || birth.isAfter(now)) return null;
        int months = Months.monthsBetween(birth.withTimeAtStartOfDay(), now.withTimeAtStartOfDay()).getMonths();
        int weeks = Weeks.weeksBetween(birth.withTimeAtStartOfDay(), now.withTimeAtStartOfDay()).getWeeks();
        if (months >= 18) return TEST_18_MONTHS;
        DateTime weaning = parseDate(weaningDate);
        if (weaning != null && !weaning.isAfter(now) && !weaning.plusMonths(3).isAfter(now)) return TEST_POST_WEANING;
        if (months >= 9) return "dbs_9".equals(lastTest) ? null : TEST_9_MONTHS;
        if (weeks >= 4) return "dbs_6".equals(lastTest) || "dbs_9".equals(lastTest) ? null : TEST_6_WEEKS;
        return null;
    }

    private String loadEnrollmentValue(String baseEntityId, String column) {
        if (StringUtils.isBlank(baseEntityId)) return null;
        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
            String selected = "e.weaning_date".equals(column) ? "e.weaning_date"
                    : "e.last_test".equals(column) ? "e.last_test" : "m.dob";
            try (Cursor cursor = db.rawQuery("SELECT " + selected + " FROM ec_mothermentor_enroll_child_eid e " +
                    "INNER JOIN ec_family_member m ON m.base_entity_id = e.base_entity_id " +
                    "WHERE e.base_entity_id = ? AND e.is_closed = 0 ORDER BY e.last_interacted_with DESC LIMIT 1",
                    new String[]{baseEntityId})) {
                return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getString(0) : null;
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to load Child EID enrollment data");
            return null;
        }
    }

    private static DateTime parseDate(String value) {
        if (StringUtils.isBlank(value)) return null;
        for (String pattern : Arrays.asList("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy")) {
            try { return org.joda.time.format.DateTimeFormat.forPattern(pattern).parseDateTime(value); }
            catch (IllegalArgumentException ignored) { }
        }
        try { return new DateTime(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
