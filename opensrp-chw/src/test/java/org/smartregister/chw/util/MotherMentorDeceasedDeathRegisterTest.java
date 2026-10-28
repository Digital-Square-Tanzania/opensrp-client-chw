package org.smartregister.chw.util;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import org.smartregister.AllConstants;
import org.smartregister.CoreLibrary;
import org.smartregister.SyncConfiguration;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.dao.PersonDao;
import org.smartregister.chw.hps.dao.HpsDao;
import org.smartregister.chw.hps.domain.HpsDeathRegisterModel;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.sync.ChwClientProcessor;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.domain.jsonmapping.ClientField;
import org.smartregister.domain.jsonmapping.Table;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.Repository;
import org.smartregister.sync.ClientProcessorForJava;

import timber.log.Timber;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/** Uses production form/event creation, NACP classification, projection and iCCHW DAO queries. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorDeceasedDeathRegisterTest {
    private static final String DEATH_TABLE = "ec_hps_death_register";
    private static final String[] MOTHER_MENTOR_TABLES = {
        Constants.TABLES.MOTHERMENTOR_ENROLLMENT,
        Constants.TABLES.MOTHERMENTOR_ENROLL_IIT,
        Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER,
        Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID
    };
    private static final String[][] CLIENTS = {
        {"Asha", "Juma", "Moshi", "Female", "1985-02-14", "14-02-1985"},
        {"Rehema", "Salim", "Moshi", "Female", "1990-06-20", "20-06-1990"},
        {"Juma", "Salim", "Moshi", "Male", "1984-08-08", "08-08-1984"},
        {"Amina", "Juma", "Moshi", "Female", "2026-04-28", "28-04-2026"}
    };
    private SQLiteDatabase data;
    private Repository previousDaoRepository;
    private final Timber.Tree failures = new Timber.DebugTree() {
        @Override
        protected void log(int priority, String tag, String message, Throwable error) {
            if (error != null) error.printStackTrace();
        }
    };

    @Before
    public void setUp() {
        Timber.plant(failures);
        data = SQLiteDatabase.create(null);
        previousDaoRepository = ReflectionHelpers.getStaticField(AbstractDao.class, "repository");
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(AbstractDao.class, "repository", previousDaoRepository);
        data.close();
        Timber.uproot(failures);
    }

    @Test
    public void englishMotherMentorDeathsAppearInIcchwDeathRegisterWithoutDuplicates() throws Exception {
        assertStandardDeathProjection("json.form");
    }

    @Test
    public void swahiliMotherMentorDeathsAppearInIcchwDeathRegisterWithoutDuplicates() throws Exception {
        assertStandardDeathProjection("json.form-sw");
    }

    private void assertStandardDeathProjection(String formDirectory) throws Exception {
        org.smartregister.Context context = mock(org.smartregister.Context.class, RETURNS_DEEP_STUBS);
        AllSharedPreferences preferences = mock(AllSharedPreferences.class);
        when(context.allSharedPreferences()).thenReturn(preferences);
        when(context.customHumanReadableConceptResponse()).thenReturn(Collections.emptyMap());
        when(preferences.fetchRegisteredANM()).thenReturn("test-provider");
        when(preferences.getPreference(AllConstants.CURRENT_LOCATION_ID)).thenReturn("test-location");
        when(preferences.fetchUserLocalityId("test-provider")).thenReturn("test-location");

        Repository repository = mock(Repository.class);
        net.zetetic.database.sqlcipher.SQLiteDatabase cipher =
                mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(repository.getReadableDatabase()).thenReturn(cipher);
        when(repository.getWritableDatabase()).thenReturn(cipher);
        when(cipher.rawQuery(anyString(), any(String[].class)))
                .thenAnswer(call -> data.rawQuery(call.getArgument(0), call.getArgument(1)));
        doAnswer(call -> {
            data.execSQL(call.getArgument(0));
            return null;
        }).when(cipher).execSQL(anyString());
        when(cipher.insertWithOnConflict(anyString(), nullable(String.class),
                any(ContentValues.class), anyInt()))
                .thenAnswer(call -> data.insertWithOnConflict(call.getArgument(0),
                        call.getArgument(1), call.getArgument(2), call.getArgument(3)));
        when(cipher.update(anyString(), any(ContentValues.class), anyString(), any(String[].class)))
                .thenAnswer(call -> data.update(call.getArgument(0), call.getArgument(1),
                        call.getArgument(2), call.getArgument(3)));
        ReflectionHelpers.setStaticField(AbstractDao.class, "repository", repository);

        ClientField mappings = org.smartregister.util.JsonFormUtils.gson.fromJson(
                asset("ec_client_fields.json"), ClientField.class);
        Table deathMapping = null;
        for (Table table : mappings.bindobjects) {
            if (DEATH_TABLE.equals(table.name)) deathMapping = table;
        }
        assertNotNull("The NACP iCCHW death-table mapping is required", deathMapping);
        String[] columns = new String[deathMapping.columns.size()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = deathMapping.columns.get(i).column_name;
        }
        CommonRepository deathRepository = new CommonRepository(DEATH_TABLE, columns);
        deathRepository.updateMasterRepository(repository);
        deathRepository.onCreate(cipher);
        when(context.commonrepository(DEATH_TABLE)).thenReturn(deathRepository);
        for (String table : MOTHER_MENTOR_TABLES) {
            data.execSQL("CREATE TABLE " + table + " (base_entity_id TEXT, is_closed INTEGER)");
        }

        CoreLibrary core = mock(CoreLibrary.class);
        when(core.context()).thenReturn(context);
        when(core.getEcClientFieldsFile()).thenReturn("ec_client_fields.json");
        when(core.getSyncConfiguration()).thenReturn(mock(SyncConfiguration.class));
        FamilyLibrary family = mock(FamilyLibrary.class);
        when(family.context()).thenReturn(context);
        MotherMentorLibrary motherMentor = mock(MotherMentorLibrary.class);
        when(motherMentor.getRepository()).thenReturn(repository);
        ClientClassification classification = org.smartregister.util.JsonFormUtils.gson.fromJson(
                asset("ec_client_classification.json"), ClientClassification.class);
        ClientProcessorForJava projection = new ClientProcessorForJava(RuntimeEnvironment.application) {
            @Override
            public void completeProcessing(Event event) {
                // Sync-queue bookkeeping is outside this projection test. Its repository
                // references optional FHIR classes absent from the app unit-test runtime.
            }
        };
        ChwClientProcessor appProcessor = mock(ChwClientProcessor.class, CALLS_REAL_METHODS);
        CoreChwApplication application = mock(CoreChwApplication.class);

        try (MockedStatic<CoreLibrary> coreStatic = mockStatic(CoreLibrary.class);
                MockedStatic<FamilyLibrary> familyStatic = mockStatic(FamilyLibrary.class);
                MockedStatic<MotherMentorLibrary> motherMentorStatic = mockStatic(MotherMentorLibrary.class);
                MockedStatic<CoreChwApplication> appStatic = mockStatic(CoreChwApplication.class);
                MockedStatic<org.smartregister.Context> contextStatic = mockStatic(org.smartregister.Context.class);
                MockedStatic<PersonDao> personStatic = mockStatic(PersonDao.class);
                MockedStatic<Utils> utilities = mockStatic(Utils.class);
                MockedConstruction<FormUtils> formLoader = mockConstruction(FormUtils.class,
                        (mock, construction) -> when(mock.getFormJsonFromRepositoryOrAssets(
                                any(android.content.Context.class),
                                eq(CoreConstants.JSON_FORM.FAMILY_DETAILS_REMOVE_MEMBER)))
                                .thenAnswer(call -> new JSONObject(asset(formDirectory
                                        + "/family_details_remove_member.json"))))) {
            coreStatic.when(CoreLibrary::getInstance).thenReturn(core);
            familyStatic.when(FamilyLibrary::getInstance).thenReturn(family);
            motherMentorStatic.when(MotherMentorLibrary::getInstance).thenReturn(motherMentor);
            appStatic.when(CoreChwApplication::getInstance).thenReturn(application);
            contextStatic.when(org.smartregister.Context::getInstance).thenReturn(context);

            utilities.when(() -> Utils.removeUser(isNull(), any(JSONObject.class), eq("test-provider")))
                    .thenAnswer(call -> {
                        JSONObject form = call.getArgument(1);
                        Triple<Pair<Date, String>, String,
                                List<org.smartregister.clientandeventmodel.Event>> result =
                                CoreJsonFormUtils.processRemoveMemberEvent(
                                        null, preferences, form, "test-provider");
                        assertNotNull("The standard form must create a removal event", result);
                        assertEquals(CoreConstants.EventType.REMOVE_MEMBER, result.getLeft().second);
                        assertEquals(1, result.getRight().size());
                        Event event = org.smartregister.util.JsonFormUtils.gson.fromJson(
                                CoreJsonFormUtils.gson.toJson(result.getRight().get(0)), Event.class);
                        assertEquals(form.getString("entity_id"), event.getBaseEntityId());
                        // Replay the same canonical event through the real classification and
                        // CommonRepository upsert; this must retain one death entry per client.
                        for (int replay = 0; replay < 2; replay++) {
                            assertEquals(Boolean.TRUE, projection.processEvent(
                                    event, new Client(event.getBaseEntityId()), classification));
                            ReflectionHelpers.callInstanceMethod(appProcessor, "processRemoveMember",
                                    ClassParameter.from(String.class, event.getBaseEntityId()),
                                    ClassParameter.from(Event.class, event));
                        }
                        return CoreConstants.EventType.REMOVE_MEMBER;
                    });

            for (int role = 0; role < MOTHER_MENTOR_TABLES.length; role++) {
                String clientId = "mother-mentor-death-role-" + role;
                data.execSQL("INSERT INTO " + MOTHER_MENTOR_TABLES[role] + " VALUES (?, 0)",
                        new Object[] {clientId});
                String[] client = CLIENTS[role];
                personStatic.when(() -> PersonDao.getDob(clientId)).thenReturn(client[4]);
                MarkClientAsDeceasedHelper.markAsDeceased(RuntimeEnvironment.application,
                        clientId, client[0], client[1], client[2], client[3], "2026-10-28");

                try (Cursor cursor = data.rawQuery("SELECT is_closed FROM "
                        + MOTHER_MENTOR_TABLES[role] + " WHERE base_entity_id = ?",
                        new String[] {clientId})) {
                    assertTrue(cursor.moveToFirst());
                    assertEquals("The deceased client's MM enrollment must close", 1, cursor.getInt(0));
                }
            }

            List<HpsDeathRegisterModel> records = HpsDao.getHpsDeathRegisterRecords();
            assertNotNull(records);
            assertEquals("Replaying must not duplicate death entries", MOTHER_MENTOR_TABLES.length,
                    records.size());
            for (HpsDeathRegisterModel record : records) {
                assertTrue(record.getDeathId().startsWith("mother-mentor-death-role-"));
                String[] client = CLIENTS[Integer.parseInt(record.getDeathId()
                        .substring(record.getDeathId().lastIndexOf('-') + 1))];
                assertEquals(client[0], record.getFirstName());
                assertEquals(client[1], record.getMiddleName());
                assertEquals(client[2], record.getLastName());
                assertEquals(client[3], record.getSex());
                assertEquals(client[5], record.getDob());
                assertEquals("28-10-2026", record.getDod());
            }
            try (Cursor cursor = data.rawQuery("SELECT COUNT(*) FROM " + DEATH_TABLE
                    + " WHERE is_closed = 0 AND base_entity_id = id", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals(MOTHER_MENTOR_TABLES.length, cursor.getInt(0));
            }
            utilities.verify(() -> Utils.removeUser(isNull(), any(JSONObject.class),
                    eq("test-provider")), times(MOTHER_MENTOR_TABLES.length));
        }
    }

    private static String asset(String path) throws Exception {
        try (Scanner scanner = new Scanner(RuntimeEnvironment.application.getAssets().open(path), "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
