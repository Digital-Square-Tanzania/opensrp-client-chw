package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.vijay.jsonwizard.utils.FormUtils;

import android.util.Pair;

import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.AllConstants;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.dao.ChwHpsDao;
import org.smartregister.chw.dao.PersonDao;
import org.smartregister.chw.tbleprosy.dao.TbLeprosyDao;
import org.smartregister.chw.tbleprosy.domain.MemberObject;
import org.smartregister.chw.util.Utils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.repository.AllSharedPreferences;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TbLeprosyDeathRegistrationTest extends BaseUnitTest {

    @Test
    public void deceasedFollowUpCreatesCompleteEnglishDeathRegistration() throws Exception {
        assertCompleteDeathRegistration("json.form");
    }

    @Test
    public void deceasedFollowUpCreatesCompleteSwahiliDeathRegistration() throws Exception {
        assertCompleteDeathRegistration("json.form-sw");
    }

    @Test
    public void repairedDeathRegistrationPreservesPreviouslyRecordedDate() throws Exception {
        List<JSONObject> submitted = captureDeathRegistrations("json.form", "2026-06-10", false, false);
        assertEquals(1, submitted.size());
        assertEquals("10-06-2026", value(submitted.get(0), "date_died"));
        assertEquals("10-06-2026", value(submitted.get(0), "dod"));
    }

    @Test
    public void completeDeathRegistrationIsNotSubmittedAgain() throws Exception {
        assertTrue(captureDeathRegistrations("json.form", "2026-06-10", true, false).isEmpty());
    }

    @Test
    public void postSaveButtonRefreshRegistersDeceasedClientOnlyOnce() throws Exception {
        List<JSONObject> submitted = captureDeathRegistrations("json.form-sw", null, false, true);
        assertEquals(1, submitted.size());
        assertEquals(value(submitted.get(0), "date_died"), value(submitted.get(0), "dod"));
    }

    private void assertCompleteDeathRegistration(String locale) throws Exception {
        List<JSONObject> registrations = captureDeathRegistrations(locale, null, false, false);
        assertEquals("Reopening a completed death registration must not emit another event", 1,
                registrations.size());
        JSONObject submitted = registrations.get(0);
        assertNotNull("The deceased follow-up must submit a removal event", submitted);
        assertEquals("base-id", submitted.getString("entity_id"));
        assertEquals("Remove Family Member", submitted.getString("encounter_type"));
        assertFalse("The death date must be populated", value(submitted, "date_died").isEmpty());
        assertEquals("The death register reads dod, not date_died",
                value(submitted, "date_died"), value(submitted, "dod"));
        assertEquals("14-02-1985", value(submitted, "dob"));
        assertEquals("Asha", value(submitted, "first_name"));
        assertEquals("Juma", value(submitted, "middle_name"));
        assertEquals("Moshi", value(submitted, "last_name"));
        assertEquals("Female", value(submitted, "sex"));
        assertEquals("Death", value(submitted, "remove_reason"));
    }

    private List<JSONObject> captureDeathRegistrations(String locale, String recordedDate,
                                                     boolean alreadyComplete, boolean refreshButtons)
            throws Exception {
        JSONObject form = new JSONObject(new String(Files.readAllBytes(Paths.get(
                "src/nacp/assets", locale, "family_details_remove_member.json")),
                StandardCharsets.UTF_8));
        TbLeprosyProfileActivity activity = mock(TbLeprosyProfileActivity.class, CALLS_REAL_METHODS);
        MemberObject member = mock(MemberObject.class);
        when(member.getBaseEntityId()).thenReturn("base-id");
        when(member.getAge()).thenReturn(41);
        when(member.getFirstName()).thenReturn("Asha");
        when(member.getMiddleName()).thenReturn("Juma");
        when(member.getLastName()).thenReturn("Moshi");
        when(member.getGender()).thenReturn("Female");
        ReflectionHelpers.setField(activity, "memberObject", member);

        org.smartregister.Context context = mock(org.smartregister.Context.class);
        AllSharedPreferences preferences = mock(AllSharedPreferences.class);
        when(context.allSharedPreferences()).thenReturn(preferences);
        when(preferences.getPreference(AllConstants.CURRENT_LOCATION_ID)).thenReturn("location-id");
        when(preferences.fetchRegisteredANM()).thenReturn("test-provider");
        FamilyLibrary family = mock(FamilyLibrary.class);
        List<JSONObject> submitted = new ArrayList<>();
        AtomicBoolean complete = new AtomicBoolean(alreadyComplete);

        try (MockedStatic<org.smartregister.Context> contexts = mockStatic(org.smartregister.Context.class);
             MockedStatic<org.smartregister.family.util.Utils> familyUtils =
                     mockStatic(org.smartregister.family.util.Utils.class);
             MockedStatic<FamilyLibrary> families = mockStatic(FamilyLibrary.class);
             MockedStatic<TbLeprosyDao> tbDao = mockStatic(TbLeprosyDao.class);
             MockedStatic<PersonDao> personDao = mockStatic(PersonDao.class);
             MockedStatic<ChwHpsDao> hpsDao = mockStatic(ChwHpsDao.class);
             MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedConstruction<FormUtils> forms = mockConstruction(FormUtils.class,
                     (loader, construction) -> when(loader.getFormJsonFromRepositoryOrAssets(
                             eq(activity), eq(CoreConstants.JSON_FORM.FAMILY_DETAILS_REMOVE_MEMBER)))
                             .thenReturn(form))) {
            contexts.when(org.smartregister.Context::getInstance).thenReturn(context);
            familyUtils.when(org.smartregister.family.util.Utils::context).thenReturn(context);
            families.when(FamilyLibrary::getInstance).thenReturn(family);
            tbDao.when(() -> TbLeprosyDao.isClientDeceased("base-id")).thenReturn(true);
            personDao.when(() -> PersonDao.getDob("base-id")).thenReturn("1985-02-14");
            hpsDao.when(() -> ChwHpsDao.hasCompleteDeathRegisterRecord("base-id"))
                    .thenAnswer(invocation -> complete.get());
            hpsDao.when(() -> ChwHpsDao.getRecordedDateOfDeath("base-id")).thenReturn(recordedDate);
            utils.when(() -> Utils.removeUser(isNull(), any(JSONObject.class), anyString()))
                    .thenAnswer(invocation -> {
                        JSONObject payload = invocation.getArgument(1);
                        assertSerializedDeathEvent(payload, preferences);
                        submitted.add(payload);
                        complete.set(true);
                        return CoreConstants.EventType.REMOVE_MEMBER;
                    });

            for (int i = 0; i < 2; i++) {
                if (refreshButtons) {
                    activity.setupButtons();
                } else {
                    activity.applyTbLeprosyDeceasedHandling();
                }
            }
        }
        return submitted;
    }

    private void assertSerializedDeathEvent(JSONObject form, AllSharedPreferences preferences)
            throws Exception {
        Triple<Pair<Date, String>, String, List<Event>> result =
                CoreJsonFormUtils.processRemoveMemberEvent(null, preferences, form, "test-provider");
        assertNotNull("The real removal processor must accept the completed form", result);
        assertEquals(1, result.getRight().size());
        Event event = result.getRight().get(0);
        assertEquals("base-id", event.getBaseEntityId());
        assertEquals(CoreConstants.EventType.REMOVE_MEMBER, event.getEventType());
        assertEquals(value(form, "date_died"), observationValue(event,
                field(form, "date_died").getString("openmrs_entity_id")));
        assertEquals(value(form, "date_died"), observationValue(event, "dod"));
        assertEquals("14-02-1985", observationValue(event, "dob"));
        assertEquals("Asha", observationValue(event, "first_name"));
        assertEquals("Juma", observationValue(event, "middle_name"));
        assertEquals("Moshi", observationValue(event, "last_name"));
        assertEquals("Female", observationValue(event, "sex"));
    }

    private String observationValue(Event event, String concept) {
        for (Obs observation : event.getObs()) {
            if (concept.equals(observation.getFieldCode())) {
                return String.valueOf(observation.getValue());
            }
        }
        throw new AssertionError("Missing death registration observation: " + concept);
    }

    private String value(JSONObject form, String key) throws Exception {
        return field(form, key).optString("value");
    }

    private JSONObject field(JSONObject form, String key) throws Exception {
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        throw new AssertionError("Missing death registration field: " + key);
    }
}
