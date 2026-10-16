package org.smartregister.chw.interactor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(application = android.app.Application.class, sdk = 28, manifest = org.robolectric.annotation.Config.NONE)
public class MotherMentorFollowUpFormTest {

    @Test
    public void statusUsesStableValuesInBothLocales() throws Exception {
        String[] expected = {"found", "not_found", "deceased", "transferred_facility", "refused_return", "under_follow_up"};
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject field = field(form(locale, "mothermentor_follow_up_status.json"), "follow_up_status");
            assertEquals("true", field.getJSONObject("v_required").getString("value"));
            JSONArray options = field.getJSONArray("options");
            assertEquals(expected.length, options.length());
            for (int i = 0; i < expected.length; i++) assertEquals(expected[i], options.getJSONObject(i).getString("key"));
        }
    }

    @Test
    public void homeAndFollowUpFormsRemainSeparate() throws Exception {
        assertNotNull(field(form("json.form", "mothermentor_visit_type.json"), "attendance_type"));
        assertNotNull(field(form("json.form", "mothermentor_follow_up_status.json"), "follow_up_status"));

        JSONObject homeComment = field(form("json.form", "mothermentor_visit_comment.json"), "comments");
        JSONObject followUpComment = field(form("json.form", "mothermentor_follow_up_comment.json"), "comments");
        assertTrue(homeComment.has("v_required"));
        assertFalse(followUpComment.has("v_required"));
    }

    @Test
    public void dependentFieldsAreRequiredAndConditional() throws Exception {
        JSONObject reason = form("json.form", "mothermentor_service_interruption_reason.json");
        JSONObject other = field(reason, "service_interruption_reason_other");
        assertNotNull(other.getJSONObject("relevance"));
        assertEquals("true", other.getJSONObject("v_required").getString("value"));
        assertEquals("true", field(reason, "expected_facility_visit_date").getJSONObject("v_required").getString("value"));

        JSONObject returned = form("json.form", "mothermentor_return_to_facility.json");
        JSONObject date = field(returned, "facility_return_date");
        assertNotNull(date.getJSONObject("relevance"));
        assertEquals("true", date.getJSONObject("v_required").getString("value"));
    }

    @Test
    public void commentsAreOptionalAndLimitedToOneHundredCharacters() throws Exception {
        JSONObject comments = field(form("json.form-sw", "mothermentor_follow_up_comment.json"), "comments");
        assertFalse(comments.has("v_required"));
        assertEquals("100", comments.getJSONObject("v_max_length").getString("value"));
        assertTrue(comments.getString("hint").contains("100"));
    }

    private JSONObject form(String directory, String name) throws Exception {
        return org.smartregister.chw.util.MotherMentorTestAssets.form(directory, name);
    }

    private JSONObject field(JSONObject form, String key) throws Exception {
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject candidate = fields.getJSONObject(i);
            if (key.equals(candidate.getString("key"))) return candidate;
        }
        throw new AssertionError("Missing form field: " + key);
    }
}
