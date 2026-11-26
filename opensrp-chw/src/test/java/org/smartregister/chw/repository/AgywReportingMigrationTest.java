package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.BuildConfig;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.reporting.dao.ReportIndicatorDaoImpl;
import org.smartregister.reporting.domain.CompositeIndicatorTally;
import org.smartregister.reporting.domain.IndicatorQuery;
import org.smartregister.reporting.repository.DailyIndicatorCountRepository;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/** Executes the real AGYW queries and migration with SQLite; no clinical hooks are replaced. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class AgywReportingMigrationTest {
    private static final String MONTH = "2026-10-01";
    private static final String PRIMARY_TOPICS = "gender_based_violence,prep_education,"
            + "education_hiv_testing,sti_hiv_aids,good_relationships";
    private static final String SECONDARY_TOPICS = "gender_based_violence,education_hiv_testing,"
            + "sti_hiv_aids,reproductive_health,good_relationships";
    private static final String ECONOMIC_TOPICS = "starting_a_business,financial_edu_and_investments";
    private static final String[] PROJECTIONS = {"ec_agyw_register", "ec_family_member", "ec_referral",
            "ec_agyw_bio_medical_services", "ec_agyw_behavioral_services", "ec_agyw_structural_services"};

    private SQLiteDatabase db;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;
    private ReportingLibrary reporting;
    private final Map<String, String> preferences = new HashMap<>();
    private final Map<String, String> queries = new LinkedHashMap<>();
    private final Map<String, JSONObject> schemas = new HashMap<>();

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        JSONArray bindings = new JSONObject(asset("ec_client_fields.json")).getJSONArray("bindobjects");
        for (int i = 0; i < bindings.length(); i++) {
            JSONObject table = bindings.getJSONObject(i);
            schemas.put(table.getString("name"), table);
        }
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets()
                .open(AgywReportingMigration.REPORT_CONFIG)) {
            Map<String, Object> config = new Yaml().load(input);
            for (Map<String, Object> row : (List<Map<String, Object>>) config.get("indicators")) {
                queries.put((String) row.get("key"), (String) row.get("indicatorQuery"));
            }
        }
        db = SQLiteDatabase.create(null);
        for (String table : PROJECTIONS) createProjection(table);
        db.execSQL("CREATE TABLE event (eventType TEXT, eventDate TEXT, updatedAt TEXT)");
        db.execSQL("CREATE TABLE indicators (indicator_code TEXT, description TEXT)");
        db.execSQL("CREATE TABLE indicator_queries (indicator_code TEXT, indicator_query TEXT)");
        db.execSQL("CREATE TABLE indicator_daily_tally (indicator_code TEXT, day TEXT, indicator_value REAL)");
        cipher = mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(cipher.rawQuery(anyString(), nullable(String[].class)))
                .thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
        doAnswer(call -> {
            db.execSQL(call.getArgument(0));
            return null;
        }).when(cipher).execSQL(anyString());

        preferences.put(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE, "2026-10-24 12:00:00");
        preferences.put("APP_VERSION_CODE", "old-version");
        AllSharedPreferences shared = mock(AllSharedPreferences.class);
        doAnswer(call -> {
            preferences.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(shared).savePreference(anyString(), anyString());
        when(shared.fetchRegisteredANM()).thenReturn("");
        org.smartregister.Context context = mock(org.smartregister.Context.class);
        when(context.allSharedPreferences()).thenReturn(shared);
        reporting = mock(ReportingLibrary.class);
        when(reporting.getContext()).thenReturn(context);
        loadConfigNormally();
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
    }

    @Test
    public void version57RefreshesOnlyAgywDefinitionsAndCanBeRepeated() {
        oldDefinitions();
        try (MockedStatic<ReportingLibrary> library = mockStatic(ReportingLibrary.class)) {
            library.when(ReportingLibrary::getInstance).thenReturn(reporting);
            ChwRepositoryFlv.onUpgrade(RuntimeEnvironment.getApplication(), cipher, 56, 57);
        }
        assertTrue(BuildConfig.DATABASE_VERSION >= 57);
        assertEquals(39, queries.size());
        assertEquals(40, count("indicator_queries"));
        assertEquals(40, count("indicators"));
        assertEquals("SELECT 4", scalar("SELECT indicator_query FROM indicator_queries WHERE indicator_code='other-report'"));
        assertEquals(query("agyw-7-10-14", "%s"), scalar("SELECT indicator_query FROM indicator_queries WHERE indicator_code='agyw-7-10-14'"));
        migrate();
        assertEquals(40, count("indicator_queries"));
        assertEquals(40, count("indicators"));
    }

    @Test
    public void refreshRemovesOnlyReplayableAgywTalliesAndResetsSharedCheckpoint() {
        oldDefinitions();
        tally("agyw-7-10-14", "2026-10-01", 9);
        tally("agyw-7-10-14", "2026-09-01", 4);
        tally("agyw-7-10-14", "2026-08-01", 5);
        tally("agyw-7-10-14", "2026-07-01", 6);
        tally("agyw-7-10-14", "2026-06-01", 7);
        tally("other-report", "2026-10-01", 3);
        event("AGYW Behavioral Services", "2026-10-01", "2026-10-02 01:00:00");
        event("AGYW tallies Report", "2026-08-01", "2026-08-02 01:00:00");
        event("AGYW Behavioral Services", "2026-07-01", null);
        event("AGYW Behavioral Services", "2026-06-01", "malformed");

        migrate();

        assertEquals("0", scalar("SELECT COUNT(*) FROM indicator_daily_tally WHERE indicator_code='agyw-7-10-14' AND day='2026-10-01'"));
        assertEquals(5, count("indicator_daily_tally"));
        assertEquals("3", scalar("SELECT CAST(indicator_value AS INTEGER) FROM indicator_daily_tally WHERE indicator_code='other-report'"));
        assertEquals("", preferences.get(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE));
        assertEquals(String.valueOf(BuildConfig.VERSION_CODE), preferences.get("APP_VERSION_CODE"));
        migrate();
        assertEquals(5, count("indicator_daily_tally"));
    }

    @Test
    public void absentRetainedEventsNeverEraseHistoricalTallies() {
        db.execSQL("DROP TABLE event");
        tally("agyw-7-10-14", "2025-01-01", 4);

        migrate();

        assertEquals(1, count("indicator_daily_tally"));
        assertEquals("4", scalar("SELECT CAST(indicator_value AS INTEGER) FROM indicator_daily_tally"));
        assertEquals("", preferences.get(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE));
    }

    @Test
    public void failedConfigLoadRollsBackAndRetryLoadsCorrectDefinitions() {
        oldDefinitions();
        tally("agyw-7-10-14", "2026-10-01", 8);
        IllegalStateException failure = new IllegalStateException("asset unavailable");
        doThrow(failure).when(reporting).readConfigFile(AgywReportingMigration.REPORT_CONFIG, cipher);
        try {
            migrate();
            fail("The database upgrade must fail so SQLiteOpenHelper can retry");
        } catch (IllegalStateException actual) {
            assertSame(failure, actual);
        }
        assertOldStatePreserved();
        loadConfigNormally();
        migrate();
        assertEquals(40, count("indicator_queries"));
        assertEquals("", preferences.get(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE));
    }

    @Test
    public void silentlyEmptyConfigCannotAdvanceTheCheckpointAndCanBeRetried() {
        oldDefinitions();
        tally("agyw-7-10-14", "2026-10-01", 8);
        doAnswer(call -> null).when(reporting).readConfigFile(AgywReportingMigration.REPORT_CONFIG, cipher);
        try {
            migrate();
            fail("An empty configuration must not silently complete the upgrade");
        } catch (IllegalStateException actual) {
            assertEquals("AGYW reporting definitions were not reloaded", actual.getMessage());
        }
        assertOldStatePreserved();
        loadConfigNormally();
        migrate();
        assertEquals(40, count("indicator_queries"));
    }

    @Test
    public void allThirtyNineConfiguredQueriesExecuteAgainstMappedSchema() {
        assertEquals(39, queries.size());
        for (Map.Entry<String, String> row : queries.entrySet()) {
            try (Cursor cursor = db.rawQuery(query(row.getKey(), MONTH), null)) {
                assertTrue(row.getKey(), cursor.moveToFirst());
                assertEquals(row.getKey(), 1, cursor.getColumnCount());
                assertEquals(row.getKey(), 0, cursor.getInt(0));
            }
        }
    }

    @Test
    public void distinctPackageQueriesSaveEveryPositiveReportCellThroughTheTallyDao() {
        seedPackage(12, "dreams", PRIMARY_TOPICS, "");
        Set<String> expandedQueries = new LinkedHashSet<>();
        Map<String, IndicatorQuery> packageQueries = new LinkedHashMap<>();
        for (int indicator = 7; indicator <= 10; indicator++) {
            for (String ages : new String[] {"10-14", "15-19", "20-24"}) {
                String key = "agyw-" + indicator + "-" + ages;
                assertTrue(key + " must not share the worker's SQL deduplication key",
                        expandedQueries.add(query(key, MONTH)));
                IndicatorQuery definition = new IndicatorQuery();
                definition.setIndicatorCode(key);
                definition.setQuery(queries.get(key));
                packageQueries.put(key, definition);
            }
        }
        assertEquals(12, expandedQueries.size());
        DailyIndicatorCountRepository tallies = mock(DailyIndicatorCountRepository.class);
        Map<String, Integer> saved = new LinkedHashMap<>();
        doAnswer(call -> {
            CompositeIndicatorTally tally = call.getArgument(0);
            saved.put(tally.getIndicatorCode(), tally.getCount());
            return null;
        }).when(tallies).add(any(CompositeIndicatorTally.class));
        try (MockedStatic<ReportingLibrary> library = mockStatic(ReportingLibrary.class);
                MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            library.when(ReportingLibrary::getInstance).thenReturn(reporting);
            utils.when(() -> Utils.getBooleanProperty(anyString())).thenReturn(false);
            new TallyDao(tallies).save(packageQueries, cipher);
        }
        assertEquals(4, saved.size());
        for (int indicator = 7; indicator <= 10; indicator++) {
            assertEquals(Integer.valueOf(1), saved.get("agyw-" + indicator + "-10-14"));
        }
    }

    @Test
    public void primaryPackageCountsOnlyItsAgeMonthGenderAndCompleteServices() {
        seedPackage(12, "dreams", PRIMARY_TOPICS, "");
        assertPackageCounts("10-14", MONTH, 1);
        assertPackageCounts("10-14", "2026-09-01", 0);
        assertPackageCounts("15-19", MONTH, 0);
        assertPackageCounts("20-24", MONTH, 0);
        db.execSQL("UPDATE ec_family_member SET gender='Male'");
        assertPackageCounts("10-14", MONTH, 0);
    }

    @Test
    public void primaryPackageKeepsExistingProgramSpecificPredicates() {
        seedPackage(12, "other-program", PRIMARY_TOPICS, "");
        assertEquals("1", scalar(query("agyw-7-10-14", MONTH)));
        assertEquals("0", scalar(query("agyw-8-10-14", MONTH)));
        assertEquals("0", scalar(query("agyw-9-10-14", MONTH)));
        assertEquals("0", scalar(query("agyw-10-10-14", MONTH)));
    }

    @Test
    public void missingPrimaryReferralBehavioralOrStructuralComponentIsExcluded() {
        seedPackage(12, "dreams", PRIMARY_TOPICS, "");
        db.execSQL("UPDATE ec_referral SET chw_referral_service='Other service'");
        assertPackageCounts("10-14", MONTH, 0);
        db.execSQL("UPDATE ec_referral SET chw_referral_service='Conventional HIV Test'");
        db.execSQL("UPDATE ec_agyw_behavioral_services SET sbcc_intervention_provided='gender_based_violence'");
        assertPackageCounts("10-14", MONTH, 0);
        db.execSQL("UPDATE ec_agyw_behavioral_services SET sbcc_intervention_provided=?", new Object[] {PRIMARY_TOPICS});
        db.execSQL("UPDATE ec_agyw_structural_services SET given_capital='no'");
        assertPackageCounts("10-14", MONTH, 0);
    }

    @Test
    public void secondaryPackageRequiresItsExistingTopicsAndEconomicServices() {
        seedPackage(17, "other-program", SECONDARY_TOPICS, ECONOMIC_TOPICS);
        assertPackageCounts("15-19", MONTH, 1);
        assertPackageCounts("10-14", MONTH, 0);
        db.execSQL("UPDATE ec_referral SET chw_referral_service='Other service'");
        assertPackageCounts("15-19", MONTH, 1);
        db.execSQL("UPDATE ec_agyw_structural_services SET economic_empowerment_education='starting_a_business'");
        assertPackageCounts("15-19", MONTH, 0);
        db.execSQL("UPDATE ec_agyw_structural_services SET economic_empowerment_education=?", new Object[] {ECONOMIC_TOPICS});
        db.execSQL("UPDATE ec_agyw_behavioral_services SET sbcc_intervention_provided=?", new Object[] {PRIMARY_TOPICS});
        assertPackageCounts("15-19", MONTH, 0);
    }

    @Test
    public void olderAgeCohortAndDistinctClientCountingRemainUnchanged() {
        seedPackage(22, "dreams", SECONDARY_TOPICS, ECONOMIC_TOPICS);
        db.execSQL("INSERT INTO ec_agyw_behavioral_services (id,base_entity_id,entity_id,sbcc_intervention_provided,last_interacted_with) "
                + "SELECT 'duplicate','duplicate',entity_id,sbcc_intervention_provided,last_interacted_with FROM ec_agyw_behavioral_services");
        assertPackageCounts("20-24", MONTH, 1);
        assertPackageCounts("15-19", MONTH, 0);
        db.execSQL("UPDATE ec_family_member SET dob=date('now','-25 years')");
        assertPackageCounts("20-24", MONTH, 0);
    }

    @Test
    public void existingFourteenthBirthdaySwitchToSecondaryPackageIsPreserved() {
        seedPackage(14, "dreams", PRIMARY_TOPICS, "");
        assertPackageCounts("10-14", MONTH, 0);
        db.execSQL("UPDATE ec_agyw_behavioral_services SET sbcc_intervention_provided=?", new Object[] {SECONDARY_TOPICS});
        db.execSQL("UPDATE ec_agyw_structural_services SET economic_empowerment_education=?", new Object[] {ECONOMIC_TOPICS});
        assertPackageCounts("10-14", MONTH, 1);
    }

    private void migrate() {
        // SQLiteOpenHelper wraps onUpgrade in a transaction; mirror that contract for direct helper calls.
        db.beginTransaction();
        try {
            AgywReportingMigration.upgrade(cipher, reporting);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void loadConfigNormally() {
        doAnswer(call -> {
            for (Map.Entry<String, String> row : queries.entrySet()) {
                db.execSQL("INSERT INTO indicators VALUES (?, ?)", new Object[] {row.getKey(), "AGYW"});
                db.execSQL("INSERT INTO indicator_queries VALUES (?, ?)", new Object[] {row.getKey(), row.getValue()});
            }
            return null;
        }).when(reporting).readConfigFile(eq(AgywReportingMigration.REPORT_CONFIG), eq(cipher));
    }

    private void assertOldStatePreserved() {
        assertEquals("broken", scalar("SELECT indicator_query FROM indicator_queries WHERE indicator_code='agyw-old'"));
        assertEquals("SELECT 4", scalar("SELECT indicator_query FROM indicator_queries WHERE indicator_code='other-report'"));
        assertEquals(2, count("indicators"));
        assertEquals(1, count("indicator_daily_tally"));
        assertEquals("2026-10-24 12:00:00", preferences.get(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE));
        assertEquals("old-version", preferences.get("APP_VERSION_CODE"));
    }

    private void oldDefinitions() {
        db.execSQL("INSERT INTO indicators VALUES ('agyw-old','old'),('other-report','keep')");
        db.execSQL("INSERT INTO indicator_queries VALUES ('agyw-old','broken'),('other-report','SELECT 4')");
    }

    private void assertPackageCounts(String ages, String month, int expected) {
        for (int indicator = 7; indicator <= 10; indicator++) {
            String key = "agyw-" + indicator + "-" + ages;
            assertEquals(key, String.valueOf(expected), scalar(query(key, month)));
        }
    }

    private void seedPackage(int age, String program, String topics, String economicTopics) {
        db.execSQL("INSERT INTO ec_family_member (id,base_entity_id,gender,dob) VALUES ('client','client','Female',date('now',?))",
                new Object[] {"-" + age + " years"});
        db.execSQL("INSERT INTO ec_agyw_register (id,base_entity_id,program_name,enrollment_date) VALUES ('client','client',?,'01-10-2026')",
                new Object[] {program});
        db.execSQL("INSERT INTO ec_referral (id,base_entity_id,entity_id,chw_referral_service) VALUES ('referral','referral','client','Conventional HIV Test')");
        db.execSQL("INSERT INTO ec_agyw_bio_medical_services (id,base_entity_id,entity_id) VALUES ('bio','bio','client')");
        db.execSQL("INSERT INTO ec_agyw_behavioral_services (id,base_entity_id,entity_id,sbcc_intervention_provided,last_interacted_with) "
                + "VALUES ('behavior','behavior','client',?,strftime('%s','2026-10-10 12:00:00')*1000)", new Object[] {topics});
        db.execSQL("INSERT INTO ec_agyw_structural_services (id,base_entity_id,entity_id,education_materials,given_capital,nurturing_and_upb_edu,economic_empowerment_education) "
                + "VALUES ('structural','structural','client','yes','yes','yes',?)", new Object[] {economicTopics});
    }

    private void event(String type, String day, String updated) {
        db.execSQL("INSERT INTO event VALUES (?, ?, ?)", new Object[] {type, day, updated});
    }

    private void tally(String code, String day, int value) {
        db.execSQL("INSERT INTO indicator_daily_tally VALUES (?, ?, ?)", new Object[] {code, day, value});
    }

    private String query(String key, String month) {
        return queries.get(key).replace("%s", month).replace("%p", "fixture-provider");
    }

    private String scalar(String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private int count(String table) {
        return Integer.parseInt(scalar("SELECT COUNT(*) FROM " + table));
    }

    private static class TallyDao extends ReportIndicatorDaoImpl {
        TallyDao(DailyIndicatorCountRepository tallies) {
            super(null, tallies, null);
        }

        void save(Map<String, IndicatorQuery> definitions,
                net.zetetic.database.sqlcipher.SQLiteDatabase database) {
            saveTallies(definitions, new AbstractMap.SimpleImmutableEntry<>(MONTH, new Date()),
                    database, new LinkedHashSet<>());
        }
    }

    private void createProjection(String table) throws Exception {
        Set<String> definitions = new LinkedHashSet<>();
        definitions.add("id TEXT PRIMARY KEY");
        JSONArray columns = schemas.get(table).getJSONArray("columns");
        for (int i = 0; i < columns.length(); i++) {
            String column = columns.getJSONObject(i).getString("column_name");
            if (!"id".equals(column)) definitions.add("\"" + column + "\" TEXT");
        }
        db.execSQL("CREATE TABLE " + table + " (" + String.join(", ", definitions) + ")");
    }

    private String asset(String name) throws Exception {
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets().open(name);
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            return scanner.next();
        }
    }
}
