package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.JsonFormUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/** Checks the assets shipped in the installed APK, including transitive library assets. */
@RunWith(AndroidJUnit4.class)
public class MotherMentorFormAssetsInstrumentedTest {
    private static final String[] FORMS = {
        "mama_kinara_household_enrolment",
        "mother_mentor_household_members_who_received_counselling",
        "mother_mentor_household_visit_topics_taught",
        "mothermentor_child_eid_arrival",
        "mothermentor_child_eid_follow_up_comment",
        "mothermentor_child_eid_follow_up_status",
        "mothermentor_child_eid_non_attendance_reason",
        "mothermentor_child_eid_test_type",
        "mothermentor_enroll_child_eid",
        "mothermentor_enroll_iit",
        "mothermentor_enroll_partner",
        "mothermentor_enrollment",
        "mothermentor_follow_up_comment",
        "mothermentor_follow_up_status",
        "mothermentor_household_visit_comment",
        "mothermentor_household_visit_purpose",
        "mothermentor_mobilization_session",
        "mothermentor_next_visit_date",
        "mothermentor_partner_arrival",
        "mothermentor_partner_follow_up_comment",
        "mothermentor_partner_follow_up_status",
        "mothermentor_partner_non_attendance_reason",
        "mothermentor_return_to_facility",
        "mothermentor_service_interruption_reason",
        "mothermentor_visit_comment",
        "mothermentor_visit_education_provided",
        "mothermentor_visit_group_linkage",
        "mothermentor_visit_referal",
        "mothermentor_visit_type"
    };

    private final AssetManager assets =
            InstrumentationRegistry.getInstrumentation().getTargetContext().getAssets();

    @Test
    public void everyWorkflowFormAndReferencedRuleIsPackagedInBothLanguages() throws Exception {
        for (String form : FORMS) {
            for (String directory : new String[] {"json.form", "json.form-sw"}) {
                JSONObject json = new JSONObject(read(directory + "/" + form + ".json"));
                int steps = json.getInt("count");
                assertTrue(form, steps > 0);
                for (int step = 1; step <= steps; step++) {
                    assertTrue(form, json.getJSONObject("step" + step).has("fields"));
                }
                checkRules(json);
            }
        }
    }

    @Test
    public void translationsRetainTheSameStoredFieldIdentifiers() throws Exception {
        for (String form : FORMS) {
            assertEquals(
                    form,
                    fields(new JSONObject(read("json.form/" + form + ".json"))),
                    fields(new JSONObject(read("json.form-sw/" + form + ".json"))));
        }
    }

    @Test
    public void selectedClinicalAnswersKeepTheirCodesWhenSerialized() throws Exception {
        for (String directory : new String[] {"json.form", "json.form-sw"}) {
            assertStoredAnswer(
                    directory, "mothermentor_enroll_child_eid", DBConstants.KEY.LAST_TEST, "dbs_6");
            assertStoredAnswer(
                    directory, "mothermentor_enroll_child_eid", DBConstants.KEY.LAST_TEST, "dbs_9");
            assertStoredAnswer(
                    directory, "mothermentor_enrollment", DBConstants.KEY.ART_NO_REASON, "other");
            assertStoredAnswer(
                    directory,
                    "mothermentor_enrollment",
                    DBConstants.KEY.OTHER_REASONS_ART_NO_REASON,
                    "Transport unavailable",
                    DBConstants.KEY.ART_NO_REASON_OTHER);
            assertStoredAnswer(
                    directory,
                    "mother_mentor_household_members_who_received_counselling",
                    DBConstants.KEY.PARTICIPANTS,
                    "other");
        }
    }

    private void assertStoredAnswer(String directory, String name, String key, String answer)
            throws Exception {
        assertStoredAnswer(directory, name, key, answer, key);
    }

    private void assertStoredAnswer(
            String directory, String name, String key, String answer, String fieldCode)
            throws Exception {
        JSONObject form = new JSONObject(read(directory + "/" + name + ".json"));
        form.put("entity_id", "form-coding-test");
        for (int step = 1; step <= form.getInt("count"); step++) {
            JSONArray fields = form.getJSONObject("step" + step).getJSONArray("fields");
            for (int index = 0; index < fields.length(); index++) {
                JSONObject field = fields.getJSONObject(index);
                if (key.equals(field.optString("key"))) {
                    field.put("value", answer);
                    if ("check_box".equals(field.optString("type"))) {
                        JSONArray options = field.getJSONArray("options");
                        for (int option = 0; option < options.length(); option++) {
                            JSONObject item = options.getJSONObject(option);
                            item.put("value", String.valueOf(answer.equals(item.getString("key"))));
                        }
                    }
                }
            }
        }
        Event event =
                JsonFormUtils.processJsonForm(
                        MotherMentorLibrary.getInstance().context().allSharedPreferences(),
                        form.toString(),
                        "form_coding_test");
        assertTrue(name, event != null);
        for (Obs observation : event.getObs()) {
            if (key.equals(observation.getFormSubmissionField())) {
                assertEquals(name + ": mapped field code", fieldCode, observation.getFieldCode());
                assertTrue(
                        name + ": " + key + " must retain " + answer,
                        observation.getValues().contains(answer));
                return;
            }
        }
        throw new AssertionError("Missing serialized answer: " + name + ": " + key);
    }

    private Set<String> fields(JSONObject form) throws Exception {
        Set<String> fields = new HashSet<>();
        for (int step = 1; step <= form.getInt("count"); step++) {
            JSONArray values = form.getJSONObject("step" + step).getJSONArray("fields");
            for (int i = 0; i < values.length(); i++) {
                JSONObject value = values.getJSONObject(i);
                fields.add(
                        step
                                + ":"
                                + value.getString("key")
                                + ":"
                                + value.optString("openmrs_entity_id"));
                JSONArray options = value.optJSONArray("options");
                Set<String> optionCodes = new HashSet<>();
                for (int option = 0; options != null && option < options.length(); option++) {
                    JSONObject item = options.getJSONObject(option);
                    String code = item.optString("openmrs_entity_id");
                    if (!code.isEmpty()) {
                        assertTrue(
                                value.getString("key") + ": duplicate option code " + code,
                                optionCodes.add(code));
                        fields.add(
                                step
                                        + ":"
                                        + value.getString("key")
                                        + ":option:"
                                        + item.getString("key")
                                        + ":"
                                        + code);
                    }
                }
            }
        }
        return fields;
    }

    private void checkRules(Object value) throws Exception {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if ("rules-file".equals(key))
                    assertTrue(read("rule/" + object.getString(key)).length() > 0);
                else checkRules(object.get(key));
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) checkRules(array.get(i));
        }
    }

    private String read(String path) throws Exception {
        try (InputStream input = assets.open(path);
                Scanner scanner = new Scanner(input, "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
