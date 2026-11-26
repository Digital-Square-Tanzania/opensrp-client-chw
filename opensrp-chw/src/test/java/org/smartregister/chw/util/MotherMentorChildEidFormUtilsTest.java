package org.smartregister.chw.util;

import android.content.Context;
import android.content.res.Configuration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.util.JsonFormUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildEidFormUtilsTest {
    @Test public void linkingIncludesNineteenMonthAnniversaryAndAllowsNewborns() {
        LocalDate today = new LocalDate(2026, 11, 24);
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChildOn("2026-11-24", today));
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChildOn("2025-04-25T00:00:00.000+03:00", today));
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChildOn("2025-04-24T00:00:00.000+03:00", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2025-04-23", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2024-11-24", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2026-11-25", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn(null, today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2026-02-31", today));
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChildOn("2024-07-31", new LocalDate(2026, 2, 28)));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn("2024-07-31", new LocalDate(2026, 3, 1)));
    }

    @Test public void estimatedBirthdatesCannotEstablishEligibility() {
        String dob = LocalDate.now().minusMonths(6).toString();
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChild(dob, "true"));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChild(dob, "1"));
        assertTrue(MotherMentorChildEidFormUtils.isEligibleChild(dob, "false"));
    }

    @Test public void directEnrollmentAllowsEighteenThroughTwentyThreeMonthsWithExactBirthdate() {
        LocalDate today = new LocalDate(2026, 10, 16);
        String twentyThreeMonths = "2024-10-17T00:00:00.000+03:00";
        assertTrue(MotherMentorChildEidFormUtils.isEligibleForDirectEnrollment(twentyThreeMonths, "false", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleChildOn(twentyThreeMonths, today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleForDirectEnrollment(twentyThreeMonths, "true", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleForDirectEnrollment("2024-10-16", "false", today));
        assertFalse(MotherMentorChildEidFormUtils.isEligibleForDirectEnrollment("2026-10-17", "false", today));
    }

    @Test public void registrationRequiresDobAndRetainsBothSexesInBothLocales() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "all_clients_registration_form.json");
            MotherMentorChildEidFormUtils.configureNewClientForm(form, "Enter the child's date of birth");
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

    @Test
    public void newChildUsesEligibleDeliveryDateAndKeepsAnExistingDob() throws Exception {
        for (String locale : new String[] {"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "all_clients_registration_form.json");
            LocalDate delivery = LocalDate.now().minusMonths(3);
            MotherMentorChildEidFormUtils.configureNewClientForm(form, "Invalid age", delivery);
            assertEquals(delivery.toString("dd-MM-yyyy"), value(form, "dob"));
            assertFalse(field(form, "dob").optBoolean("read_only"));

            String corrected = delivery.minusDays(1).toString("dd-MM-yyyy");
            field(form, "dob").put("value", corrected);
            MotherMentorChildEidFormUtils.configureNewClientForm(form, "Invalid age", delivery);
            assertEquals(corrected, value(form, "dob"));
        }
    }

    @Test
    public void missingAndIneligibleDeliveryDatesLeaveManualEntryAvailable() throws Exception {
        for (LocalDate delivery : new LocalDate[] {null, LocalDate.now().plusDays(1),
                LocalDate.now().minusMonths(19).minusDays(1)}) {
            JSONObject form = MotherMentorTestAssets.form("json.form", "all_clients_registration_form.json");
            MotherMentorChildEidFormUtils.configureNewClientForm(form, "Invalid age", delivery);
            assertEquals("", field(form, "dob").optString("value"));
            assertFalse(field(form, "dob").optBoolean("read_only"));
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
            field(form, DBConstants.KEY.LAST_TEST_RESULTS_RETURNED)
                    .put("value", "no");
            field(form, "last_test_result").put("value", "waiting");
            MotherMentorChildEidFormUtils.bindIdentity(form, mother, child);
            assertEquals("child", form.getString("entity_id"));
            assertEquals("child-family", value(form, "relational_id"));
            assertEquals("waiting", value(form, "last_test_result"));
        }
    }

    @Test
    public void heiEnrollmentEventRestoresStoredSexAfterLocalizedDisplay() throws Exception {
        Map<String, String> mother = person("mother", "family", "Test Mother");
        Map<String, String> child = person("child", "family", "Test Child");
        String storedDob = "2026-08-14T23:30:00-05:00";
        child.put(DBConstants.KEY.DOB, storedDob);
        for (String language : new String[] {"en", "sw"}) {
            String directory = "sw".equals(language) ? "json.form-sw" : "json.form";
            Context application = RuntimeEnvironment.getApplication();
            Configuration configuration = new Configuration(application.getResources().getConfiguration());
            configuration.setLocale(new Locale(language));
            Context context = application.createConfigurationContext(configuration);

            String[] storedValues = {"Female", "Male", "female", " male ", "Me (Mvulana)",
                    "Ke (Msichana)", "unknown", ""};
            String female = "sw".equals(language) ? "Wa kike" : "Female";
            String male = "sw".equals(language) ? "Wa kiume" : "Male";
            String[] captions = {female, male, female, male, male, female, "unknown", ""};
            for (int i = 0; i < storedValues.length; i++) {
                String storedSex = storedValues[i];
                child.put("gender", storedSex);
                JSONObject form = MotherMentorTestAssets.form(directory, MotherMentorChildEidFormUtils.FORM + ".json");
                MotherMentorChildEidFormUtils.bindIdentity(form, mother, child);
                MotherMentorChildEidFormUtils.localizeSexForDisplay(context, form);
                assertEquals(captions[i], value(form, "child_sex"));
                MotherMentorChildEidFormUtils.formatDobForDisplay(form);
                assertEquals("14-08-2026", value(form, DBConstants.KEY.CHILD_DOB));
                if ("unknown".equals(storedSex) || storedSex.isEmpty()) continue;

                JSONObject returned = new JSONObject(form.toString());
                MotherMentorChildEidFormUtils.bindIdentity(returned, mother, child);
                MotherMentorChildEidFormUtils.bindHeiNumber(returned, mother);
                MotherMentorChildEidFormUtils.prepareForEvent(returned);
                FormTag tag = new FormTag();
                tag.providerId = "synthetic-provider";
                Event event = JsonFormUtils.createEvent(JsonFormUtils.fields(returned),
                        returned.getJSONObject("metadata"), tag, returned.getString("entity_id"),
                        returned.getString("encounter_type"), Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID);
                assertNotNull(event);
                assertEquals("child", event.getBaseEntityId());
                Obs sexObservation = event.getObs().stream()
                        .filter(obs -> "child_sex".equals(obs.getFormSubmissionField()))
                        .findFirst().orElseThrow(() -> new AssertionError("Missing child sex observation"));
                assertEquals("child_sex", sexObservation.getFieldCode());
                assertEquals(Collections.singletonList(storedSex), sexObservation.getValues());
                assertEquals(storedSex, child.get("gender"));
                Obs dobObservation = event.getObs().stream()
                        .filter(obs -> "child_dob".equals(obs.getFormSubmissionField()))
                        .findFirst().orElseThrow(() -> new AssertionError("Missing child DOB observation"));
                assertEquals(Collections.singletonList("2026-08-14"), dobObservation.getValues());
                assertEquals(storedDob, child.get(DBConstants.KEY.DOB));
            }
        }
    }

    @Test
    public void newChildLocationUsesMappedMotherValuesInBothLocales() throws Exception {
        Map<String, String> location = new HashMap<>();
        location.put("village_town", "Mama Street");
        location.put("landmark", "Mama landmark");
        location.put("gps", "-6.81 39.28 12.0 8.0");
        location.put("nearest_facility", "[\"Ward A\",\"Hamlet B\"]");
        for (String locale : new String[] {"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "all_clients_registration_form.json");
            JSONObject hamlet = field(form, "nearest_facility");
            hamlet.put("tree", new JSONArray("[{\"key\":\"Ward A\",\"nodes\":[{\"key\":\"Hamlet B\"}]}]"));
            MotherMentorChildEidFormUtils.prefillRegistrationLocation(form, location);
            assertEquals("Mama Street", value(form, "fam_village"));
            assertEquals("Mama landmark", value(form, "landmark"));
            assertEquals("-6.81 39.28 12.0 8.0", value(form, "gps"));
            assertEquals(location.get("nearest_facility"), value(form, "nearest_facility"));
            assertFalse(hamlet.optBoolean("read_only"));
        }
    }

    @Test
    public void locationKeepsChildValuesAndIgnoresInvalidMotherDetails() throws Exception {
        JSONObject form = MotherMentorTestAssets.form("json.form", "all_clients_registration_form.json");
        field(form, "fam_village").put("value", "Child Street");
        Map<String, String> location = new HashMap<>();
        location.put("village_town", "Mama Street");
        location.put("gps", "-6.81 39.28 12.0 8.0 9.0");
        location.put("nearest_facility", "[\"Unknown Hamlet\"]");
        MotherMentorChildEidFormUtils.prefillRegistrationLocation(form, location);
        assertEquals("Child Street", value(form, "fam_village"));
        assertEquals("", field(form, "landmark").optString("value"));
        assertEquals("", field(form, "gps").optString("value"));
        assertEquals("", field(form, "nearest_facility").optString("value"));
    }

    @Test(expected = IllegalArgumentException.class) public void motherCannotBeHerOwnChild() throws Exception {
        Map<String, String> mother = person("mother", "family", "Neema");
        MotherMentorChildEidFormUtils.bindIdentity(MotherMentorTestAssets.form("json.form", "mothermentor_enroll_child_eid.json"), mother, mother);
    }

    @Test
    public void newChildCaregiverUsesMothersNameRelationshipAndPhoneInBothLocales() throws Exception {
        Map<String, String> mother = person("mother", "family", "Neema");
        mother.put(DBConstants.KEY.PHONE_NUMBER, "0712345678");
        for (String locale : new String[] {"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "all_clients_registration_form.json");
            MotherMentorChildEidFormUtils.prefillRegistrationCaregiver(form, mother);
            assertEquals("Yes", value(form, "has_primary_caregiver"));
            assertEquals("Neema Mtemi", value(form, "primary_caregiver_name"));
            assertEquals("Mother", value(form, "caregiver_relationship_base"));
            assertEquals("0712345678", value(form, "other_phone_number"));
            assertFalse(field(form, "primary_caregiver_name").optBoolean("read_only"));
        }
    }

    @Test
    public void caregiverPrefillKeepsAnotherCaregiverAndSkipsInvalidPhone() throws Exception {
        Map<String, String> mother = person("mother", "family", "Neema");
        mother.put(DBConstants.KEY.PHONE_NUMBER, "invalid");
        JSONObject form = MotherMentorTestAssets.form("json.form", "all_clients_registration_form.json");
        MotherMentorChildEidFormUtils.prefillRegistrationCaregiver(form, mother);
        assertEquals("", field(form, "other_phone_number").optString("value"));

        field(form, "primary_caregiver_name").put("value", "Another Caregiver");
        field(form, "caregiver_relationship_base").put("value", "Guardian");
        MotherMentorChildEidFormUtils.prefillRegistrationCaregiver(form, mother);
        assertEquals("Another Caregiver", value(form, "primary_caregiver_name"));
        assertEquals("Guardian", value(form, "caregiver_relationship_base"));
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
