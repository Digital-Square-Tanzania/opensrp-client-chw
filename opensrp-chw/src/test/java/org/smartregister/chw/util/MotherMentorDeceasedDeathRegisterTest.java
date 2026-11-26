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
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import org.smartregister.AllConstants;
import org.smartregister.CoreLibrary;
import org.smartregister.SyncConfiguration;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.repository.ScheduleRepository;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.dao.PersonDao;
import org.smartregister.chw.hps.dao.HpsDao;
import org.smartregister.chw.hps.domain.HpsDeathRegisterModel;
import org.smartregister.chw.model.FamilyProfileMemberModel;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.presenter.FamilyProfileMemberPresenter;
import org.smartregister.chw.sync.ChwClientProcessor;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
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
    public void finalizedMwenziDeathUsesTheOrdinaryMemberAndIcchwDeathProjection() throws Exception {
        assertStandardDeathProjection("json.form", "tracing");
    }

    @Test
    public void finalizedPostAcceptanceDeathUsesTheOrdinaryMemberAndIcchwDeathProjection() throws Exception {
        assertStandardDeathProjection("json.form", "attendance");
    }

    @Test
    public void swahiliMotherMentorDeathsAppearInIcchwDeathRegisterWithoutDuplicates() throws Exception {
        assertStandardDeathProjection("json.form-sw", null);
    }

    private void assertStandardDeathProjection(String formDirectory, String partnerPhase) throws Exception {
        boolean projectPartnerVisit = partnerPhase != null;
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
        when(cipher.delete(anyString(), anyString(), any(String[].class)))
                .thenAnswer(call -> data.delete(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
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
        for (String table : new String[] {CoreConstants.TABLE_NAME.FAMILY_MEMBER,
                CoreConstants.TABLE_NAME.CHILD}) {
            data.execSQL("CREATE TABLE " + table + " (base_entity_id TEXT, is_closed INTEGER,"
                    + " date_removed TEXT, dod TEXT)");
            data.execSQL("CREATE TABLE " + CommonFtsObject.searchTableName(table) + " ("
                    + CommonFtsObject.idColumn + " TEXT, is_closed INTEGER, date_removed TEXT)");
        }
        data.execSQL("ALTER TABLE ec_family_member ADD COLUMN dob TEXT");
        data.execSQL("ALTER TABLE ec_family_member ADD COLUMN first_name TEXT");
        data.execSQL("ALTER TABLE ec_family_member ADD COLUMN middle_name TEXT");
        data.execSQL("ALTER TABLE ec_family_member ADD COLUMN last_name TEXT");
        data.execSQL("ALTER TABLE ec_family_member ADD COLUMN gender TEXT");
        data.execSQL("ALTER TABLE ec_family_member ADD COLUMN relational_id TEXT DEFAULT 'household'");
        data.execSQL("CREATE TABLE ec_family (base_entity_id TEXT, family_head TEXT)");
        data.execSQL("INSERT INTO ec_family VALUES ('household', 'living-mother')");
        data.execSQL("INSERT INTO ec_family_member (base_entity_id, is_closed) VALUES"
                + " ('living-mother', 0), ('living-relative', 0)");
        data.execSQL("CREATE TABLE visits (base_entity_id TEXT, visit_type TEXT, visit_json TEXT)");
        data.execSQL(ScheduleRepository.CREATE_TABLE_SQL);

        CoreLibrary core = mock(CoreLibrary.class);
        when(core.context()).thenReturn(context);
        when(core.getEcClientFieldsFile()).thenReturn("ec_client_fields.json");
        when(core.getSyncConfiguration()).thenReturn(mock(SyncConfiguration.class));
        when(core.isPeerToPeerProcessing()).thenReturn(true);
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
        doAnswer(call -> projection.processEvent(call.getArgument(0), call.getArgument(1), classification))
                .when(appProcessor).processEvent(any(Event.class), any(Client.class), eq(classification));
        CoreChwApplication application = mock(CoreChwApplication.class);
        when(application.getRepository()).thenReturn(repository);
        ScheduleRepository schedules = new ScheduleRepository() {
            @Override
            public net.zetetic.database.sqlcipher.SQLiteDatabase getWritableDatabase() {
                return cipher;
            }
        };
        when(application.getScheduleRepository()).thenReturn(schedules);
        when(application.getAllCommonsRepository(CoreConstants.TABLE_NAME.FAMILY_MEMBER))
                .thenReturn(mock(AllCommonsRepository.class));
        when(application.getAllCommonsRepository(CoreConstants.TABLE_NAME.CHILD))
                .thenReturn(mock(AllCommonsRepository.class));

        try (MockedStatic<CoreLibrary> coreStatic = mockStatic(CoreLibrary.class);
                MockedStatic<FamilyLibrary> familyStatic = mockStatic(FamilyLibrary.class);
                MockedStatic<MotherMentorLibrary> motherMentorStatic = mockStatic(MotherMentorLibrary.class);
                MockedStatic<CoreChwApplication> appStatic = mockStatic(CoreChwApplication.class);
                MockedStatic<org.smartregister.Context> contextStatic = mockStatic(org.smartregister.Context.class);
                MockedStatic<PersonDao> personStatic = mockStatic(PersonDao.class);
                MockedStatic<Utils> utilities = mockStatic(Utils.class);
                MockedConstruction<FormUtils> formLoader = mockConstruction(FormUtils.class,
                        (mock, construction) -> when(mock.getFormJsonFromRepositoryOrAssets(
                                any(android.content.Context.class), anyString()))
                                .thenAnswer(call -> new JSONObject(asset(formDirectory
                                        + "/" + call.getArgument(1) + ".json"))))) {
            coreStatic.when(CoreLibrary::getInstance).thenReturn(core);
            familyStatic.when(FamilyLibrary::getInstance).thenReturn(family);
            motherMentorStatic.when(MotherMentorLibrary::getInstance).thenReturn(motherMentor);
            appStatic.when(CoreChwApplication::getInstance).thenReturn(application);
            contextStatic.when(org.smartregister.Context::getInstance).thenReturn(context);

            Answer<String> projectRemoval = call -> {
                JSONObject form = call.getArgument(1);
                Triple<Pair<Date, String>, String,
                        List<org.smartregister.clientandeventmodel.Event>> result =
                        CoreJsonFormUtils.processRemoveMemberEvent(null, preferences, form, "test-provider");
                assertNotNull("The standard form must create a removal event", result);
                String expectedEventType = form.getString("entity_id").endsWith("-3")
                        ? CoreConstants.EventType.REMOVE_CHILD : CoreConstants.EventType.REMOVE_MEMBER;
                assertEquals(expectedEventType, result.getLeft().second);
                assertEquals(1, result.getRight().size());
                Event event = org.smartregister.util.JsonFormUtils.gson.fromJson(
                        CoreJsonFormUtils.gson.toJson(result.getRight().get(0)), Event.class);
                assertEquals(form.getString("entity_id"), event.getBaseEntityId());
                // Replay through the real classification and upsert to check for duplicate entries.
                for (int replay = 0; replay < 2; replay++) {
                    if (call.getArguments().length == 4) {
                        appProcessor.processEvents(classification, null, null,
                                new EventClient(event, new Client(event.getBaseEntityId())), event, event.getEventType());
                        continue;
                    }
                    assertEquals(Boolean.TRUE, projection.processEvent(
                            event, new Client(event.getBaseEntityId()), classification));
                    if (CoreConstants.EventType.REMOVE_CHILD.equals(event.getEventType())) {
                        ReflectionHelpers.callInstanceMethod(appProcessor, "processRemoveChild",
                                ClassParameter.from(String.class, event.getBaseEntityId()),
                                ClassParameter.from(Event.class, event));
                    }
                    ReflectionHelpers.callInstanceMethod(appProcessor, "processRemoveMember",
                            ClassParameter.from(String.class, event.getBaseEntityId()),
                            ClassParameter.from(Event.class, event));
                }
                return expectedEventType;
            };
            utilities.when(() -> Utils.removeUser(isNull(), any(JSONObject.class), eq("test-provider")))
                    .thenAnswer(projectRemoval);
            utilities.when(() -> Utils.removeUser(isNull(), any(JSONObject.class), eq("test-provider"), anyString()))
                    .thenAnswer(projectRemoval);

            for (int role = 0; role < MOTHER_MENTOR_TABLES.length; role++) {
                String clientId = "mother-mentor-death-role-" + role;
                data.execSQL("INSERT INTO " + MOTHER_MENTOR_TABLES[role] + " VALUES (?, 0)",
                        new Object[] {clientId});
                String[] client = CLIENTS[role];
                data.execSQL("INSERT INTO ec_family_member"
                                + " (base_entity_id, is_closed, first_name, middle_name, last_name, gender, dob)"
                                + " VALUES (?, 0, ?, ?, ?, ?, ?)",
                        new Object[] {clientId, client[0], client[1], client[2], client[3], client[4]});
                data.execSQL("INSERT INTO " + CommonFtsObject.searchTableName(
                        CoreConstants.TABLE_NAME.FAMILY_MEMBER) + " VALUES (?, 0, NULL)",
                        new Object[] {clientId});
                if (role == 3) {
                    data.execSQL("INSERT INTO " + CoreConstants.TABLE_NAME.CHILD
                            + " VALUES (?, 0, NULL, NULL)", new Object[] {clientId});
                    data.execSQL("INSERT INTO " + CommonFtsObject.searchTableName(
                            CoreConstants.TABLE_NAME.CHILD) + " VALUES (?, 0, NULL)",
                            new Object[] {clientId});
                }
                personStatic.when(() -> PersonDao.getDob(clientId)).thenReturn(client[4]);
                if (role == 2 && projectPartnerVisit) {
                    data.execSQL("INSERT INTO schedule_service (base_entity_id) VALUES (?), ('living-relative')",
                            new String[] {clientId});
                    Event prior = MotherMentorDeathPropagationTest.partnerVisit(clientId, "living-mother",
                            "other", "2026-10-27");
                    Event visit = MotherMentorDeathPropagationTest.partnerVisit(clientId, "living-mother",
                            "deceased", "2026-10-28", partnerPhase);
                    for (Event saved : new Event[] {prior, visit}) {
                        data.execSQL("INSERT INTO visits VALUES (?, ?, ?)", new Object[] {
                                saved.getBaseEntityId(), saved.getEventType(),
                                org.smartregister.util.JsonFormUtils.gson.toJson(saved)});
                    }
                    assertTrue(isInHouseholdMembers(clientId));
                    MotherMentorDeathPropagation.process(RuntimeEnvironment.application, prior);
                    MotherMentorDeathPropagation.process(RuntimeEnvironment.application, visit);
                    MotherMentorDeathPropagation.process(RuntimeEnvironment.application, visit);
                    assertFalse(isInHouseholdMembers(clientId));
                    assertTrue(isInHouseholdMembers("living-mother"));
                    assertTrue(isInHouseholdMembers("living-relative"));
                    try (Cursor pending = data.rawQuery("SELECT base_entity_id FROM schedule_service", null)) {
                        assertEquals(1, pending.getCount());
                        assertTrue(pending.moveToFirst());
                        assertEquals("living-relative", pending.getString(0));
                    }
                    try (Cursor history = data.rawQuery("SELECT visit_json FROM visits WHERE "
                            + "base_entity_id = ? AND visit_type = ? ORDER BY rowid", new String[] {
                                    clientId, MotherMentorFollowUpType.PARTNER.getEventType()})) {
                        assertEquals(2, history.getCount());
                        assertTrue(history.moveToNext());
                        assertEquals(org.smartregister.util.JsonFormUtils.gson.toJson(prior), history.getString(0));
                        assertTrue(history.moveToNext());
                        assertEquals(org.smartregister.util.JsonFormUtils.gson.toJson(visit), history.getString(0));
                    }
                } else if (role == 3) {
                    MarkClientAsDeceasedHelper.markChildAsDeceased(RuntimeEnvironment.application,
                            clientId, client[0], client[1], client[2], client[3], "2026-10-28");
                } else {
                    MarkClientAsDeceasedHelper.markAsDeceased(RuntimeEnvironment.application,
                            clientId, client[0], client[1], client[2], client[3], "2026-10-28");
                }

                try (Cursor cursor = data.rawQuery("SELECT is_closed FROM "
                        + MOTHER_MENTOR_TABLES[role] + " WHERE base_entity_id = ?",
                        new String[] {clientId})) {
                    assertTrue(cursor.moveToFirst());
                    assertEquals("The deceased client's MM enrollment must close", 1, cursor.getInt(0));
                }
                assertSharedClientClosed(CoreConstants.TABLE_NAME.FAMILY_MEMBER, clientId);
                if (role == 3) assertSharedClientClosed(CoreConstants.TABLE_NAME.CHILD, clientId);
            }
            try (Cursor living = data.rawQuery("SELECT COUNT(*) FROM ec_family_member WHERE "
                    + "base_entity_id IN ('living-mother', 'living-relative') AND is_closed = 0"
                    + " AND date_removed IS NULL AND dod IS NULL", null)) {
                assertTrue(living.moveToFirst());
                assertEquals(2, living.getInt(0));
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
                    eq("test-provider")), times(MOTHER_MENTOR_TABLES.length - (projectPartnerVisit ? 1 : 0)));
            if (projectPartnerVisit) {
                utilities.verify(() -> Utils.removeUser(isNull(), any(JSONObject.class),
                        eq("test-provider"), anyString()));
            }
        }
    }

    private boolean isInHouseholdMembers(String baseEntityId) {
        FamilyProfileMemberModel model = mock(FamilyProfileMemberModel.class);
        FamilyProfileMemberPresenter presenter = new FamilyProfileMemberPresenter(
                null, model, null, "household", "living-mother", null);
        try (Cursor row = data.rawQuery("SELECT ec_family_member.base_entity_id FROM ec_family_member"
                + " LEFT JOIN ec_child USING (base_entity_id) WHERE " + presenter.getMainCondition()
                + " AND ec_family_member.base_entity_id = ?", new String[] {baseEntityId})) {
            return row.moveToFirst();
        }
    }

    private void assertSharedClientClosed(String table, String clientId) {
        try (Cursor cursor = data.rawQuery("SELECT is_closed, dod, date_removed FROM " + table
                + " WHERE base_entity_id = ?", new String[] {clientId})) {
            assertTrue(cursor.moveToFirst());
            assertEquals(table + " must close for the deceased client", 1, cursor.getInt(0));
            assertEquals("2026-10-28", cursor.getString(1));
            assertFalse("The active child register filters date_removed", cursor.isNull(2));
        }
        try (Cursor cursor = data.rawQuery("SELECT is_closed FROM "
                + CommonFtsObject.searchTableName(table) + " WHERE " + CommonFtsObject.idColumn
                + " = ?", new String[] {clientId})) {
            assertTrue(cursor.moveToFirst());
            assertEquals("The search row must also close", 1, cursor.getInt(0));
        }
    }

    private static String asset(String path) throws Exception {
        try (Scanner scanner = new Scanner(RuntimeEnvironment.application.getAssets().open(path), "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
