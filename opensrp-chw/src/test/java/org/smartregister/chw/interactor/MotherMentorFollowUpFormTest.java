package org.smartregister.chw.interactor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void partnerUsesIndependentClinicalFormsAndStableValues() throws Exception {
        JSONObject status = field(form("json.form-sw", "mothermentor_partner_follow_up_status.json"), "follow_up_status");
        assertEquals("found", status.getJSONArray("options").getJSONObject(0).getString("key"));

        JSONObject reasonForm = form("json.form-sw", "mothermentor_partner_non_attendance_reason.json");
        JSONObject reason = field(reasonForm, "partner_non_attendance_reason");
        String[] expectedReasons = {"travel_or_moved", "transport_cost", "stigma", "other"};
        assertEquals(expectedReasons.length, reason.getJSONArray("options").length());
        for (int i = 0; i < expectedReasons.length; i++) {
            assertEquals(expectedReasons[i], reason.getJSONArray("options").getJSONObject(i).getString("key"));
        }
        assertFalse(hasOption(reason, "medicine_side_effects"));
        assertFalse(hasOption(reason, "forgot"));
        assertFalse(hasOption(reason, "facility_congestion"));
        assertNotNull(field(reasonForm, "partner_non_attendance_reason_other").getJSONObject("relevance"));
        assertEquals("date_picker", field(reasonForm, "partner_expected_facility_visit_date").getString("type"));

        JSONObject arrivalForm = form("json.form-sw", "mothermentor_partner_arrival.json");
        assertNotNull(field(arrivalForm, "partner_arrived_at_facility"));
        JSONObject arrivalDate = field(arrivalForm, "partner_facility_arrival_date");
        assertEquals("date_picker", arrivalDate.getString("type"));
        assertNotNull(arrivalDate.getJSONObject("relevance"));
        assertFalse(arrivalForm.toString().contains("Mama amerudi kituoni"));

        JSONObject comments = field(form("json.form-sw", "mothermentor_partner_follow_up_comment.json"), "comments");
        assertFalse(comments.has("v_required"));
        assertEquals("100", comments.getJSONObject("v_max_length").getString("value"));
    }

    @Test
    public void motherAndPartnerFieldsRemainSeparate() throws Exception {
        JSONObject motherReason = form("json.form", "mothermentor_service_interruption_reason.json");
        JSONObject partnerReason = form("json.form", "mothermentor_partner_non_attendance_reason.json");
        assertNotNull(field(motherReason, "service_interruption_reason"));
        assertNotNull(field(partnerReason, "partner_non_attendance_reason"));
        assertFalse(motherReason.toString().contains("partner_non_attendance_reason"));
        assertFalse(partnerReason.toString().contains("service_interruption_reason"));
    }

    private boolean hasOption(JSONObject field, String key) throws Exception {
        JSONArray options = field.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            if (key.equals(options.getJSONObject(i).getString("key"))) return true;
        }
        return false;
    }

    private JSONObject form(String directory, String name) throws Exception {
        Path path = Paths.get("src", "nacp", "assets", directory, name);
        return new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
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
