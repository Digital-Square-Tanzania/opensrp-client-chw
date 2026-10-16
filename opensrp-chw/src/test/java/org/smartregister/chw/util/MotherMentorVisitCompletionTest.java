package org.smartregister.chw.util;

import android.app.Application;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorVisitCompletionTest {
    @Test public void householdAttendanceValidatesAgainstCalculatedCounsellingTotal() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "mother_mentor_household_members_who_received_counselling.json");
            JSONObject attendance = null;
            JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                if ("household_members_found".equals(field.optString("key"))) attendance = field;
                if ("counselled_total".equals(field.optString("key"))) field.put("value", "3");
            }
            assertNotNull(attendance);
            assertFalse(attendance.optBoolean("read_only"));
            JSONObject config = attendance.getJSONObject("v_relative_min");
            com.vijay.jsonwizard.fragments.JsonFormFragment fragment =
                    org.mockito.Mockito.mock(com.vijay.jsonwizard.fragments.JsonFormFragment.class);
            org.mockito.Mockito.when(fragment.getCurrentJsonState()).thenReturn(form.toString());
            com.vijay.jsonwizard.validators.edittext.RelativeNumericValidator validator =
                    new com.vijay.jsonwizard.validators.edittext.RelativeNumericValidator(
                            config.getString("err"), fragment, config.getString("value"),
                            Integer.MIN_VALUE, "step1", false);
            assertFalse(validator.isValid("2", false));
            assertFalse(validator.isValid("0", false));
            assertTrue(validator.isValid("3", false));
            assertTrue(validator.isValid("5", false));
        }
    }
    @Test public void legacyLostVisitCanFinalizeWithoutHiddenSections() throws Exception {
        assertTrue(MotherMentorVisitCompletion.isComplete(obs("attendance_type", "repeat", "follow_up_status", "lost_to_follow_up", "comments", "Tracing")));
        assertFalse(MotherMentorVisitCompletion.isComplete(obs("attendance_type", "repeat", "comments", "Tracing")));
    }
    @Test public void fullVisitsStillRequireAllVisibleSections() throws Exception {
        assertFalse(MotherMentorVisitCompletion.isComplete(obs("attendance_type", "new", "comments", "Visit")));
        assertFalse(MotherMentorVisitCompletion.isComplete(obs("attendance_type", "repeat", "follow_up_status", "continuing_services", "comments", "Visit")));
        assertTrue(MotherMentorVisitCompletion.isComplete(obs("attendance_type", "new", "comments", "Visit", "has_been_linked_to_psychosocial_support_group", "no", "has_been_linked_to_iga_group", "no", "education_provided", "no", "referral_given", "no", "next_appointment_date", "2026-11-01")));
    }
    @Test public void newTracingRequiresReasonOutcomeAndConfirmedReturnDate() {
        Map<String,String> values = new HashMap<>();
        values.put("attendance_type", "repeat"); values.put("follow_up_status", "lost_to_follow_up");
        values.put("mothermentor_reporting_version", "1"); values.put("iit_enrolled", "true");
        values.put("iit_tracing_visit", "yes");
        assertFalse(MotherMentorVisitCompletion.isVisitTypeComplete(values));
        values.put("iit_tracing_outcome", "found"); values.put("iit_interruption_reason", "travel"); values.put("returned_to_art", "yes");
        assertFalse(MotherMentorVisitCompletion.isVisitTypeComplete(values));
        values.put("art_return_date", "15-10-2026");
        assertTrue(MotherMentorVisitCompletion.isVisitTypeComplete(values));
    }
    @Test public void newPrimaryVisitRequiresPregnancyAndArtAssessment() {
        Map<String,String> values = new HashMap<>();
        values.put("attendance_type", "new"); values.put("mothermentor_reporting_version", "1"); values.put("primary_mothermentor_client", "true");
        assertFalse(MotherMentorVisitCompletion.isVisitTypeComplete(values));
        values.put("visit_pregnancy_status", "pregnant"); values.put("new_pmtct_art_initiation", "no");
        assertTrue(MotherMentorVisitCompletion.isVisitTypeComplete(values));
        values.put("primary_mothermentor_client", "false"); values.remove("visit_pregnancy_status");
        assertTrue(MotherMentorVisitCompletion.isVisitTypeComplete(values));
    }
    private JSONArray obs(String... values) throws Exception {
        JSONArray array = new JSONArray();
        for (int i=0; i<values.length; i+=2) array.put(new JSONObject().put("fieldCode",values[i]).put("values",new JSONArray().put(values[i+1])));
        return array;
    }
}
