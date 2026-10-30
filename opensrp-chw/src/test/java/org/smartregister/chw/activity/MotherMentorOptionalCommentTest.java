package org.smartregister.chw.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import java.util.LinkedHashMap;
import java.util.Map;

public class MotherMentorOptionalCommentTest extends BaseUnitTest {
    @Test
    public void newAndRepeatServicesCanCompleteWithoutComment() throws Exception {
        for (String attendance : new String[] {"new", "repeat"}) {
            Map<String, String> values = completeService(attendance);
            assertTrue(isComplete(values, false));
            values.put(DBConstants.KEY.COMMENTS, "");
            assertTrue(isComplete(values, false));
            values.put(DBConstants.KEY.COMMENTS, "Additional follow-up information");
            assertTrue(isComplete(values, false));
        }
    }

    @Test
    public void otherRequiredSectionsStillBlockNewAndRepeatCompletion() throws Exception {
        String[] requiredFields = {
            DBConstants.KEY.ATTENDANCE_TYPE,
            DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP,
            DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP,
            DBConstants.KEY.EDUCATION_PROVIDED,
            DBConstants.KEY.REFERRAL_GIVEN,
            DBConstants.KEY.NEXT_APPOINTMENT_DATE,
            DBConstants.KEY.VISIT_PREGNANCY_STATUS,
            DBConstants.KEY.NEW_PMTCT_ART_INITIATION
        };
        for (String attendance : new String[] {"new", "repeat"}) {
            for (String required : requiredFields) {
                Map<String, String> values = completeService(attendance);
                values.put(DBConstants.KEY.COMMENTS, "Existing feedback");
                values.remove(required);
                assertFalse(attendance + ": " + required, isComplete(values, false));
            }
        }
        Map<String, String> missingStatus = completeService("repeat");
        missingStatus.remove(DBConstants.KEY.FOLLOW_UP_STATUS);
        assertFalse(isComplete(missingStatus, false));
    }

    @Test
    public void nonContinuingRepeatNeedsStatusButNoCommentOrHiddenSections() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(DBConstants.KEY.ATTENDANCE_TYPE, "repeat");
        values.put(DBConstants.KEY.FOLLOW_UP_STATUS, "lost_to_follow_up");
        assertTrue(isComplete(values, false));
        values.remove(DBConstants.KEY.FOLLOW_UP_STATUS);
        assertFalse(isComplete(values, false));
    }

    @Test
    public void partnerFollowUpKeepsItsOwnCompletionSchema() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(DBConstants.KEY.FOLLOW_UP_STATUS, "found");
        assertFalse(isComplete(values, true));
        values.put(DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY, "no");
        assertTrue(isComplete(values, true));
        values.put(DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY, "yes");
        assertFalse(isComplete(values, true));
        values.put(DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE, "25-10-2026");
        assertTrue(isComplete(values, true));
    }

    private Map<String, String> completeService(String attendance) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(DBConstants.KEY.ATTENDANCE_TYPE, attendance);
        if ("repeat".equals(attendance)) {
            values.put(DBConstants.KEY.FOLLOW_UP_STATUS, "continuing_services");
        }
        values.put(DBConstants.KEY.MOTHERMENTOR_REPORTING_VERSION, "2");
        values.put(DBConstants.KEY.PRIMARY_MOTHERMENTOR_CLIENT, "true");
        values.put(DBConstants.KEY.VISIT_PREGNANCY_STATUS, "pregnant");
        values.put(DBConstants.KEY.NEW_PMTCT_ART_INITIATION, "no");
        values.put(DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP, "no");
        values.put(DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP, "no");
        values.put(DBConstants.KEY.EDUCATION_PROVIDED, "no");
        values.put(DBConstants.KEY.REFERRAL_GIVEN, "no");
        values.put(DBConstants.KEY.NEXT_APPOINTMENT_DATE, "2026-11-01");
        return values;
    }

    private boolean isComplete(Map<String, String> values, boolean partnerFollowUp) throws Exception {
        Intent intent = new Intent();
        if (partnerFollowUp) {
            intent.putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true);
            intent.putExtra(
                    MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                    MotherMentorFollowUpType.PARTNER.getValue());
        }
        MotherMentorProfileActivity activity =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, intent).get();
        JSONArray observations = new JSONArray();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            observations.put(
                    new JSONObject()
                            .put("fieldCode", entry.getKey())
                            .put("values", new JSONArray().put(entry.getValue())));
        }
        Visit visit = new Visit();
        if (partnerFollowUp) visit.setVisitType(MotherMentorFollowUpType.PARTNER.getEventType());
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return ReflectionHelpers.callInstanceMethod(
                activity,
                "hasCompletedMotherMentorServiceSections",
                ReflectionHelpers.ClassParameter.from(Visit.class, visit));
    }
}
