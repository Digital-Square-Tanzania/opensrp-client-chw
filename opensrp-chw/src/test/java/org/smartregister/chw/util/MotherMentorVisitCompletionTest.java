package org.smartregister.chw.util;

import android.app.Application;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.actionhelper.MotherMentorHouseholdCounsellingActionHelper;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorVisitCompletionTest {
    @Test public void householdRoleCountsDetermineTheReadOnlyCounsellingTotal() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale,
                    "mother_mentor_household_members_who_received_counselling.json");
            Map<String, JSONObject> fields = new HashMap<>();
            JSONArray definitions = form.getJSONObject("step1").getJSONArray("fields");
            for (int index = 0; index < definitions.length(); index++) {
                JSONObject field = definitions.getJSONObject(index);
                fields.put(field.getString("key"), field);
            }
            assertFalse(fields.containsKey("household_members_found"));
            assertFalse(fields.containsKey("counselled_male"));
            assertFalse(fields.containsKey("counselled_female"));
            assertTrue(fields.get("counselled_total").getBoolean("read_only"));
            JSONArray options = fields.get("participants").getJSONArray("options");
            for (int index = 0; index < options.length(); index++) {
                JSONObject option = options.getJSONObject(index);
                String key = option.getString("key");
                option.put("value", "mother".equals(key) || "partner".equals(key) || "child".equals(key));
            }
            MotherMentorHouseholdCounsellingActionHelper helper =
                    new MotherMentorHouseholdCounsellingActionHelper();
            helper.onPayloadReceived(form.toString());
            assertEquals(BaseMotherMentorVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());
            fields.get("counselled_child_count").put("value", "2");
            helper.onPayloadReceived(form.toString());
            assertEquals(BaseMotherMentorVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
            JSONObject saved = new JSONObject(helper.postProcess(form.toString()));
            assertEquals("4", com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(
                    saved, "counselled_total").getString("value"));
        }
    }
    @Test public void legacyHouseholdAttendanceStillValidatesAgainstItsCalculatedTotal() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "mother_mentor_household_members_who_received_counselling_legacy.json");
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
    @Test
    public void notFoundVisitCanFinalizeWithItsReasonAndNoServiceSections() throws Exception {
        assertTrue(MotherMentorVisitCompletion.isComplete(obs(
                "attendance_type", "repeat", "follow_up_status", "not_found",
                "mama_not_found_reason", "moved_area")));
        assertFalse(MotherMentorVisitCompletion.isComplete(obs(
                "attendance_type", "repeat", "follow_up_status", "not_found")));
    }
    private JSONArray obs(String... values) throws Exception {
        JSONArray array = new JSONArray();
        for (int i=0; i<values.length; i+=2) array.put(new JSONObject().put("fieldCode",values[i]).put("values",new JSONArray().put(values[i+1])));
        return array;
    }
}
