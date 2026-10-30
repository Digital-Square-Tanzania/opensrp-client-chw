package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.app.Application;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.tag.FormTag;

import java.util.HashMap;
import java.util.Map;

/** Legacy EC results must survive restoration into the new returned-results gate. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildEidReturnedResultsTest {
    private static final String[] LOCALES = {"json.form", "json.form-sw"};

    @Test
    public void legacyPositiveAndNegativeResultsRestoreTheYesGateInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            for (String result : new String[] {"positive", "negative"}) {
                JSONObject form = prepare(locale, result, null);
                assertEquals("yes", value(form, LAST_TEST_RESULTS_RETURNED));
                assertEquals(result, value(form, LAST_TEST_RESULT));
                assertBoundIdentity(form);
            }
        }
    }

    @Test
    public void legacyWaitingRestoresTheNoGateInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            JSONObject form = prepare(locale, "waiting", null);
            assertEquals("no", value(form, LAST_TEST_RESULTS_RETURNED));
            assertEquals("waiting", value(form, LAST_TEST_RESULT));
            assertBoundIdentity(form);
        }
    }

    @Test
    public void missingAndUnknownResultsDoNotInventAnAnswerInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            for (String result : new String[] {null, "", "unknown"}) {
                JSONObject form = prepare(locale, result, null);
                assertEquals("", value(form, LAST_TEST_RESULTS_RETURNED));
                assertBoundIdentity(form);
            }
        }
    }

    @Test
    public void explicitlyRestoredGateTakesPrecedenceOverLegacyResultInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            JSONObject notReturned = prepare(locale, "positive", "no");
            assertEquals("no", value(notReturned, LAST_TEST_RESULTS_RETURNED));
            assertEquals("waiting", value(notReturned, LAST_TEST_RESULT));
            JSONObject returned = prepare(locale, "waiting", "yes");
            assertEquals("yes", value(returned, LAST_TEST_RESULTS_RETURNED));
            assertEquals("", value(returned, LAST_TEST_RESULT));
            assertBoundIdentity(notReturned);
            assertBoundIdentity(returned);
        }
    }

    @Test
    public void savingNoReplacesStaleClinicalResultWithLegacyWaitingInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            for (String result : new String[] {"positive", "negative", ""}) {
                JSONObject form = form(locale);
                set(form, LAST_TEST, "dbs_6");
                set(form, LAST_TEST_RESULTS_RETURNED, "no");
                set(form, LAST_TEST_RESULT, result);
                form.put(ENTITY_ID, "mother");
                MotherMentorChildEidFormUtils.bindIdentity(form, mother(), child());
                assertEquals("no", value(form, LAST_TEST_RESULTS_RETURNED));
                assertEquals("waiting", value(form, LAST_TEST_RESULT));
                assertBoundIdentity(form);
            }
        }
    }

    @Test
    public void savingNoTestClearsGateAndResultWithoutChangingIdentityInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            for (String gate : new String[] {"yes", "no", ""}) {
                JSONObject form = form(locale);
                set(form, LAST_TEST, "no_test_has_been_done_yet");
                set(form, LAST_TEST_RESULTS_RETURNED, gate);
                set(form, LAST_TEST_RESULT, "positive");
                MotherMentorChildEidFormUtils.bindIdentity(form, mother(), child());
                JSONObject serialized = new JSONObject(form.toString());
                assertEquals("", value(serialized, LAST_TEST_RESULTS_RETURNED));
                assertEquals("", value(serialized, LAST_TEST_RESULT));
                assertBoundIdentity(serialized);
            }
        }
    }

    @Test
    public void savingYesPreservesOnlyPositiveOrNegativeInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            for (String result : new String[] {"positive", "negative", "waiting", "unknown", ""}) {
                JSONObject form = form(locale);
                set(form, LAST_TEST, "dbs_9");
                set(form, LAST_TEST_RESULTS_RETURNED, "yes");
                set(form, LAST_TEST_RESULT, result);
                MotherMentorChildEidFormUtils.bindIdentity(form, mother(), child());
                assertEquals("yes", value(form, LAST_TEST_RESULTS_RETURNED));
                assertEquals("positive".equals(result) || "negative".equals(result) ? result : "",
                        value(form, LAST_TEST_RESULT));
                assertBoundIdentity(form);
            }
        }
    }

    @Test
    public void actualEnrollmentEventRetainsWaitingAndReturnedGateCodesInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            for (String gate : new String[] {"yes", "no"}) {
                for (String answer : new String[] {"positive", "negative"}) {
                    JSONObject form = form(locale);
                    set(form, LAST_TEST, "dbs_6");
                    set(form, LAST_TEST_RESULTS_RETURNED, gate);
                    set(form, LAST_TEST_RESULT, answer);
                    MotherMentorChildEidFormUtils.bindIdentity(form, mother(), child());
                    assertEquals("native_radio", field(form, LAST_TEST_RESULT).getString("type"));
                    MotherMentorChildEidFormUtils.prepareForEvent(form);
                    String prepared = form.toString();
                    MotherMentorChildEidFormUtils.prepareForEvent(form);
                    assertEquals(prepared, form.toString());
                    Event event = event(form);
                    assertEquals("child", event.getBaseEntityId());
                    assertObservation(event, LAST_TEST_RESULTS_RETURNED, gate);
                    assertObservation(event, LAST_TEST_RESULT, "no".equals(gate) ? "waiting" : answer);
                }
            }
        }
    }

    @Test
    public void noTestEnrollmentEventDoesNotRetainStaleGateOrResultInBothLanguages() throws Exception {
        for (String locale : LOCALES) {
            JSONObject form = form(locale);
            set(form, LAST_TEST, "no_test_has_been_done_yet");
            set(form, LAST_TEST_RESULTS_RETURNED, "yes");
            set(form, LAST_TEST_RESULT, "positive");
            MotherMentorChildEidFormUtils.bindIdentity(form, mother(), child());
            MotherMentorChildEidFormUtils.prepareForEvent(form);
            Event event = event(form);
            assertObservation(event, LAST_TEST, "no_test_has_been_done_yet");
            for (Obs observation : event.getObs()) {
                if (LAST_TEST_RESULTS_RETURNED.equals(observation.getFormSubmissionField())
                        || LAST_TEST_RESULT.equals(observation.getFormSubmissionField())) {
                    for (Object answer : observation.getValues()) {
                        assertTrue("No test must not retain an answer", answer == null || answer.toString().isEmpty());
                    }
                }
            }
        }
    }

    private Event event(JSONObject form) throws Exception {
        JSONArray allFields = new JSONArray();
        for (int step = 1; step <= form.getInt("count"); step++) {
            JSONArray fields = form.getJSONObject("step" + step).getJSONArray("fields");
            for (int index = 0; index < fields.length(); index++) allFields.put(fields.getJSONObject(index));
        }
        FormTag tag = new FormTag();
        tag.providerId = "synthetic-provider";
        tag.appVersion = 68;
        tag.databaseVersion = 59;
        Event event = org.smartregister.util.JsonFormUtils.createEvent(allFields,
                form.getJSONObject("metadata"), tag, form.getString(ENTITY_ID),
                MotherMentorChildEidFormUtils.EVENT,
                org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID);
        assertNotNull(event);
        return event;
    }

    private void assertObservation(Event event, String key, String answer) {
        for (Obs observation : event.getObs()) {
            if (key.equals(observation.getFormSubmissionField())) {
                assertEquals(key, observation.getFieldCode());
                assertTrue(key + " must retain " + answer, observation.getValues().contains(answer));
                return;
            }
        }
        throw new AssertionError("Missing enrollment observation: " + key);
    }

    private JSONObject prepare(String locale, String result, String gate) throws Exception {
        Map<String, String> previous = new HashMap<>();
        previous.put(LAST_TEST, "dbs_6");
        if (result != null) previous.put(LAST_TEST_RESULT, result);
        if (gate != null) previous.put(LAST_TEST_RESULTS_RETURNED, gate);
        return MotherMentorChildEidFormUtils.prepare(form(locale), mother(), child(), previous);
    }

    private JSONObject form(String locale) throws Exception {
        return MotherMentorTestAssets.form(locale, MotherMentorChildEidFormUtils.FORM + ".json");
    }

    private Map<String, String> mother() {
        Map<String, String> mother = new HashMap<>();
        mother.put(BASE_ENTITY_ID, "mother");
        mother.put(FIRST_NAME, "Neema");
        mother.put(CTC_NUMBER, "12-34-5678-123456");
        return mother;
    }

    private Map<String, String> child() {
        Map<String, String> child = new HashMap<>();
        child.put(BASE_ENTITY_ID, "child");
        child.put(RELATIONAL_ID, "child-family");
        child.put(FIRST_NAME, "Amina");
        child.put(GENDER, "Female");
        child.put(DOB, "2026-06-01T00:00:00.000+03:00");
        return child;
    }

    private void assertBoundIdentity(JSONObject form) throws Exception {
        assertEquals("child", form.getString(ENTITY_ID));
        assertEquals(MotherMentorChildEidFormUtils.EVENT, form.getString("encounter_type"));
        assertEquals("mother", value(form, MOTHER_BASE_ENTITY_ID));
        assertEquals("child-family", value(form, RELATIONAL_ID));
        assertEquals("12-34-5678-123456", value(form, MOTHER_CTC_NUMBER));
    }

    private String value(JSONObject form, String key) throws Exception {
        return field(form, key).optString("value");
    }

    private void set(JSONObject form, String key, String answer) throws Exception {
        field(form, key).put("value", answer);
    }

    private JSONObject field(JSONObject form, String key) throws Exception {
        for (int step = 1; step <= form.getInt("count"); step++) {
            JSONArray fields = form.getJSONObject("step" + step).getJSONArray("fields");
            for (int index = 0; index < fields.length(); index++) {
                JSONObject field = fields.getJSONObject(index);
                if (key.equals(field.optString("key"))) return field;
            }
        }
        throw new AssertionError("Missing child EID field: " + key);
    }
}
