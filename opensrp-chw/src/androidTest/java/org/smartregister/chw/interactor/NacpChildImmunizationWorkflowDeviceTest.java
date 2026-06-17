package org.smartregister.chw.interactor;

import android.content.Context;
import android.database.Cursor;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.VisitVaccineUtil;
import org.smartregister.chw.sync.ChwClientProcessor;
import org.smartregister.domain.Alert;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.jsonmapping.Vaccine;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.service.intent.VaccineIntentService;
import org.smartregister.util.JsonFormUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
public class NacpChildImmunizationWorkflowDeviceTest {

    private static final String BASE_ENTITY_ID = "device-test-child-immunization";
    private static final String PROGRAM_CLIENT_ID = "device-test-program-client";
    private static final String PROVIDER_ID = "markchw";
    private static final String LOCATION_ID = "fbdd93f1-2045-4744-ae38-133f78a049c0";
    private static final String TEAM = "Kia - 102557-6";
    private static final String TEAM_ID = "8b0ad916-115b-410c-9dd2-0b9fc00ca84f";
    private static final Pattern DATE_ONLY = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final LocalDate birthDate = new LocalDate(2026, 4, 23);

    private Context context;
    private DefaultChildHomeVisitInteractorFlv interactor;
    private VaccineRepository vaccineRepository;
    private ChwClientProcessor processor;
    private List<VaccineGroup> vaccineGroups;
    private List<Vaccine> specialVaccines;
    private List<VaccineRepo.Vaccine> vaccineDefinitions;
    private Map<String, Integer> stageOffsets;
    private Map<String, List<String>> expectedStageVaccines;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        initializeSqlCipherPassword();
        interactor = new ChildHomeVisitInteractorFlv();
        interactor.context = context;
        vaccineRepository = ChwApplication.getInstance().vaccineRepository();
        processor = (ChwClientProcessor) ChwClientProcessor.getInstance(context);
        vaccineGroups = interactor.getVaccineGroups();
        specialVaccines = interactor.getSpecialVaccines();
        vaccineDefinitions = VaccineRepo.getVaccines(CoreConstants.SERVICE_GROUPS.CHILD);
        loadNacpSchedule();
        clearVaccines();
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
        clearVaccines();
    }

    @Test
    public void fullNacpSchedulePersistsCorrectlyAndDoesNotRepeatAtOneYear() throws Exception {
        runStage("Birth");
        runStage("6 Weeks");
        runStage("10 Weeks");
        runStage("14 Weeks");
        runStage("9 Months");

        assertNoPendingVaccines(birthDate.plusYears(1));

        runStage("18 Months");
        assertNoPendingVaccines(stageDate("18 Months").plusDays(1));
    }

    private void runStage(String stageName) throws Exception {
        assertPendingStage(stageName);

        List<String> stageVaccines = expectedStageVaccines.get(stageName);
        recordVaccines(stageDate(stageName), stageVaccines);

        assertStoredVaccines(includeVaccinesThrough(stageName));
    }

    private void assertPendingStage(String stageName) {
        LinkedHashMap<String, List<String>> pendingVaccines = getPendingVaccines(stageDate(stageName));
        Assert.assertEquals("Unexpected number of pending groups at " + stageName, 1, pendingVaccines.size());

        Map.Entry<String, List<String>> entry = pendingVaccines.entrySet().iterator().next();
        Assert.assertEquals(normalize(stageName), normalize(entry.getKey()));
        assertNormalizedListEquals(expectedStageVaccines.get(stageName), entry.getValue());
    }

    private void assertNoPendingVaccines(LocalDate currentDate) {
        LinkedHashMap<String, List<String>> pendingVaccines = getPendingVaccines(currentDate);
        Assert.assertTrue("Expected no pending vaccines at " + currentDate + " but found " + pendingVaccines, pendingVaccines.isEmpty());
    }

    private LinkedHashMap<String, List<String>> getPendingVaccines(LocalDate currentDate) {
        DateTimeUtils.setCurrentMillisFixed(currentDate.toDateTimeAtStartOfDay().plusHours(12).getMillis());

        List<org.smartregister.immunization.domain.Vaccine> recordedVaccines = vaccineRepository.findByEntityId(BASE_ENTITY_ID);
        recordedVaccines = interactor.normalizeIssuedVaccines(recordedVaccines, vaccineDefinitions);

        Map<String, VaccineRepo.Vaccine> vaccinesRepo = new LinkedHashMap<>();
        for (VaccineRepo.Vaccine vaccineDefinition : vaccineDefinitions) {
            addVaccineRepoEntry(vaccinesRepo, vaccineDefinition.display(), vaccineDefinition);
            addVaccineRepoEntry(vaccinesRepo, vaccineDefinition.name(), vaccineDefinition);
        }

        Map<VaccineGroup, List<Pair<VaccineRepo.Vaccine, Alert>>> pendingVaccines = VisitVaccineUtil.generateVisitVaccines(
                BASE_ENTITY_ID,
                vaccinesRepo,
                birthDate.toDateTimeAtStartOfDay(),
                vaccineGroups,
                specialVaccines,
                recordedVaccines,
                null
        );

        pendingVaccines = interactor.removeIssuedVaccinesFromPending(pendingVaccines, recordedVaccines, vaccineDefinitions);

        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<VaccineGroup, List<Pair<VaccineRepo.Vaccine, Alert>>> entry : pendingVaccines.entrySet()) {
            List<String> vaccineNames = new ArrayList<>();
            if (entry.getValue() != null) {
                for (Pair<VaccineRepo.Vaccine, Alert> vaccineAlertPair : entry.getValue()) {
                    if (vaccineAlertPair != null && vaccineAlertPair.first != null) {
                        vaccineNames.add(vaccineAlertPair.first.display());
                    }
                }
            }

            if (!vaccineNames.isEmpty()) {
                result.put(entry.getKey().name, vaccineNames);
            }
        }

        return result;
    }

    private void recordVaccines(LocalDate visitDate, List<String> vaccinesToRecord) throws Exception {
        for (String vaccineDisplayName : vaccinesToRecord) {
            EventClient eventClient = buildVaccinationEvent(vaccineDisplayName, visitDate);
            processor.processEvents(null, null, null, eventClient, eventClient.getEvent(), VaccineIntentService.EVENT_TYPE);
        }
    }

    private EventClient buildVaccinationEvent(String vaccineDisplayName, LocalDate visitDate) throws Exception {
        String vaccineFieldName = toStoredVaccineName(vaccineDisplayName);
        String visitDateString = visitDate.toString("yyyy-MM-dd");
        String visitTimestamp = visitDate.toDateTimeAtStartOfDay().plusHours(12).toString();

        JSONObject eventJson = new JSONObject();
        eventJson.put("baseEntityId", BASE_ENTITY_ID);
        eventJson.put("duration", 0);
        eventJson.put("entityType", "vaccination");
        eventJson.put("eventDate", visitTimestamp);
        eventJson.put("eventId", UUID.randomUUID().toString());
        eventJson.put("eventType", VaccineIntentService.EVENT_TYPE);
        eventJson.put("formSubmissionId", UUID.randomUUID().toString());
        eventJson.put("locationId", LOCATION_ID);
        eventJson.put("providerId", PROVIDER_ID);
        eventJson.put("team", TEAM);
        eventJson.put("teamId", TEAM_ID);
        eventJson.put("dateCreated", visitTimestamp);
        eventJson.put("clientApplicationVersion", 40);
        eventJson.put("clientDatabaseVersion", 37);

        JSONObject details = new JSONObject();
        details.put("program_client_id", PROGRAM_CLIENT_ID);
        eventJson.put("details", details);

        JSONArray observations = new JSONArray();
        observations.put(buildDateObs(vaccineFieldName, visitDateString));
        observations.put(buildDoseObs(vaccineFieldName, doseFor(vaccineDisplayName)));
        eventJson.put("obs", observations);

        Event event = JsonFormUtils.gson.fromJson(eventJson.toString(), Event.class);
        return new EventClient(event);
    }

    private JSONObject buildDateObs(String vaccineFieldName, String visitDate) throws Exception {
        JSONObject observation = new JSONObject();
        observation.put("fieldCode", "1410AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        observation.put("fieldDataType", "date");
        observation.put("fieldType", "concept");
        observation.put("formSubmissionField", vaccineFieldName);
        observation.put("parentCode", parentCodeFor(vaccineFieldName));
        observation.put("saveObsAsArray", false);

        JSONArray values = new JSONArray();
        values.put(visitDate);
        observation.put("values", values);
        observation.put("humanReadableValues", new JSONArray());
        return observation;
    }

    private JSONObject buildDoseObs(String vaccineFieldName, int dose) throws Exception {
        JSONObject observation = new JSONObject();
        observation.put("fieldCode", "1418AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        observation.put("fieldDataType", "calculate");
        observation.put("fieldType", "concept");
        observation.put("formSubmissionField", vaccineFieldName + "_dose");
        observation.put("parentCode", parentCodeFor(vaccineFieldName));
        observation.put("saveObsAsArray", false);

        JSONArray values = new JSONArray();
        values.put(String.valueOf(dose));
        observation.put("values", values);
        observation.put("humanReadableValues", new JSONArray());
        return observation;
    }

    private String parentCodeFor(String vaccineFieldName) {
        String normalizedName = normalize(vaccineFieldName);
        for (VaccineGroup vaccineGroup : vaccineGroups) {
            if (vaccineGroup == null || vaccineGroup.vaccines == null) {
                continue;
            }

            for (Vaccine vaccine : vaccineGroup.vaccines) {
                if (vaccine == null || vaccine.openmrs_date == null || vaccine.openmrs_date.parent_entity == null) {
                    continue;
                }

                if (normalize(vaccine.name).equals(normalizedName)) {
                    return vaccine.openmrs_date.parent_entity;
                }
            }
        }

        throw new AssertionError("Could not find parent OpenMRS entity for " + vaccineFieldName);
    }

    private int doseFor(String vaccineDisplayName) {
        String normalizedName = normalize(vaccineDisplayName);
        if ("bcg".equals(normalizedName) || "ipv".equals(normalizedName)) {
            return 1;
        }

        for (int index = normalizedName.length() - 1; index >= 0; index--) {
            if (!Character.isDigit(normalizedName.charAt(index))) {
                if (index == normalizedName.length() - 1) {
                    return 1;
                }
                return Integer.parseInt(normalizedName.substring(index + 1));
            }
        }

        return Integer.parseInt(normalizedName);
    }

    private void assertStoredVaccines(List<String> expectedDisplayNames) {
        List<String> actualStoredNames = getStoredVaccineNames();
        for (String actualStoredName : actualStoredNames) {
            Assert.assertFalse("Stored vaccine name should not be a date: " + actualStoredName, DATE_ONLY.matcher(actualStoredName).matches());
        }

        List<String> expectedStoredNames = new ArrayList<>();
        for (String expectedDisplayName : expectedDisplayNames) {
            expectedStoredNames.add(toStoredVaccineName(expectedDisplayName));
        }

        Collections.sort(actualStoredNames);
        Collections.sort(expectedStoredNames);
        Assert.assertEquals(expectedStoredNames, actualStoredNames);
    }

    private List<String> getStoredVaccineNames() {
        SQLiteDatabase database = ChwApplication.getInstance().getRepository().getReadableDatabase();
        List<String> names = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT name FROM vaccines WHERE base_entity_id = ? ORDER BY date, name", new String[]{BASE_ENTITY_ID});
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return names;
    }

    private void clearVaccines() {
        SQLiteDatabase database = ChwApplication.getInstance().getRepository().getWritableDatabase();
        database.delete("vaccines", "base_entity_id = ?", new String[]{BASE_ENTITY_ID});
    }

    private void initializeSqlCipherPassword() throws Exception {
        Field passwordField = CoreChwApplication.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(ChwApplication.getInstance(), LOCATION_ID);
    }

    private void addVaccineRepoEntry(Map<String, VaccineRepo.Vaccine> vaccinesRepo, String vaccineName, VaccineRepo.Vaccine vaccineDefinition) {
        String vaccineKey = normalize(vaccineName);
        if (!vaccineKey.isEmpty()) {
            vaccinesRepo.put(vaccineKey, vaccineDefinition);
        }
    }

    private LocalDate stageDate(String stageName) {
        Integer offset = stageOffsets.get(stageName);
        if (offset == null) {
            throw new AssertionError("Unknown stage " + stageName);
        }

        LocalDate dueDate = birthDate.plusDays(offset);
        return offset == 0 ? dueDate : dueDate.plusDays(1);
    }

    private List<String> includeVaccinesThrough(String stageName) {
        List<String> completedVaccines = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : expectedStageVaccines.entrySet()) {
            completedVaccines.addAll(entry.getValue());
            if (entry.getKey().equals(stageName)) {
                break;
            }
        }
        return completedVaccines;
    }

    private void loadNacpSchedule() throws Exception {
        stageOffsets = new LinkedHashMap<>();
        expectedStageVaccines = new LinkedHashMap<>();

        JSONArray vaccineSchedule = new JSONArray(readAsset("vaccines.json"));
        for (int i = 0; i < vaccineSchedule.length(); i++) {
            JSONObject vaccineGroup = vaccineSchedule.getJSONObject(i);
            String stageName = vaccineGroup.getString("name");
            stageOffsets.put(stageName, vaccineGroup.getInt("days_after_birth_due"));

            JSONArray vaccines = vaccineGroup.getJSONArray("vaccines");
            List<String> vaccineNames = new ArrayList<>();
            for (int j = 0; j < vaccines.length(); j++) {
                vaccineNames.add(vaccines.getJSONObject(j).getString("name"));
            }
            expectedStageVaccines.put(stageName, vaccineNames);
        }
    }

    private String readAsset(String assetName) throws Exception {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            inputStream = context.getAssets().open(assetName);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            outputStream.close();
        }
    }

    private void assertNormalizedListEquals(List<String> expectedVaccines, List<String> actualVaccines) {
        List<String> normalizedExpected = normalizeAll(expectedVaccines);
        List<String> normalizedActual = normalizeAll(actualVaccines);
        Collections.sort(normalizedExpected);
        Collections.sort(normalizedActual);
        Assert.assertEquals(normalizedExpected, normalizedActual);
    }

    private List<String> normalizeAll(List<String> vaccineNames) {
        List<String> normalizedNames = new ArrayList<>();
        if (vaccineNames == null) {
            return normalizedNames;
        }

        for (String vaccineName : vaccineNames) {
            normalizedNames.add(normalize(vaccineName));
        }
        return normalizedNames;
    }

    private String toStoredVaccineName(String vaccineDisplayName) {
        return vaccineDisplayName.toLowerCase(Locale.ENGLISH)
                .replace(" ", "_")
                .replace("-", "_");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }
}
