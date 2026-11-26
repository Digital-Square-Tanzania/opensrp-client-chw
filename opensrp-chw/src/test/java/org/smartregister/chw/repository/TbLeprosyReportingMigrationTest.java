package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
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
import org.smartregister.repository.AllSharedPreferences;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Executes version 56 and the actual TB/Leprosy indicator SQL against SQLite. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class TbLeprosyReportingMigrationTest {
    private static final String REPORT_DATE = "2026-10-01";
    private static final String OCTOBER_TIMESTAMP =
            "CAST(strftime('%s', '2026-10-10 12:00:00') AS INTEGER) * 1000";
    private SQLiteDatabase db;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;
    private ReportingLibrary reporting;
    private final Map<String, String> preferences = new HashMap<>();
    private final Map<String, JSONObject> schemas = new HashMap<>();
    private Map<String, String> queries;

    @Before
    public void setUp() throws Exception {
        JSONArray bindings = new JSONObject(asset("ec_client_fields.json")).getJSONArray("bindobjects");
        for (int i = 0; i < bindings.length(); i++) {
            JSONObject table = bindings.getJSONObject(i);
            schemas.put(table.getString("name"), table);
        }
        queries = configQueries(TbLeprosyReportingMigration.REPORT_CONFIG);
        db = SQLiteDatabase.create(null);
        createProjection(TbLeprosyReportingMigration.SCREENING_TABLE, true);
        createProjection(TbLeprosyReportingMigration.RESULTS_TABLE, true);
        createProjection("ec_family_member", false);
        createProjection("ec_tbleprosy_contact_visit", false);
        createProjection("ec_tbleprosy_followup_visit", false);
        // Supports the separately merged version-55 migration in the combined app QA build.
        createProjection("ec_kvp_prep_followup", false);
        db.execSQL("CREATE TABLE task (\"for\" TEXT, focus TEXT)");
        db.execSQL("CREATE TABLE event (eventId TEXT PRIMARY KEY, json TEXT, eventType TEXT, "
                + "formSubmissionId TEXT, baseEntityId TEXT, eventDate TEXT, updatedAt TEXT)");
        db.execSQL("CREATE TABLE indicators (indicator_code TEXT, description TEXT)");
        db.execSQL("CREATE TABLE indicator_queries (indicator_code TEXT, indicator_query TEXT, grouping TEXT DEFAULT 'TBLeprosy Household')");
        db.execSQL("CREATE TABLE indicator_daily_tally (indicator_code TEXT, day TEXT, indicator_value REAL, indicator_grouping TEXT, indicator_is_value_set INTEGER DEFAULT 0, indicator_value_set TEXT)");
        cipher = mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(cipher.rawQuery(anyString(), nullable(String[].class)))
                .thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
        doAnswer(call -> {
            db.execSQL(call.getArgument(0));
            return null;
        }).when(cipher).execSQL(anyString());
        doAnswer(call -> {
            db.execSQL(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(cipher).execSQL(anyString(), any(Object[].class));
        preferences.put(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE, "2026-10-24 12:00:00");
        AllSharedPreferences shared = mock(AllSharedPreferences.class);
        doAnswer(call -> {
            preferences.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(shared).savePreference(anyString(), anyString());
        org.smartregister.Context context = mock(org.smartregister.Context.class);
        when(context.allSharedPreferences()).thenReturn(shared);
        reporting = mock(ReportingLibrary.class);
        when(reporting.getContext()).thenReturn(context);
        doAnswer(call -> {
            for (Map.Entry<String, String> indicator : configQueries(call.getArgument(0)).entrySet()) {
                db.execSQL("INSERT INTO indicators VALUES (?, ?)",
                        new Object[] {indicator.getKey(), "fixture-description"});
                db.execSQL("INSERT INTO indicator_queries (indicator_code,indicator_query) VALUES (?, ?)",
                        new Object[] {indicator.getKey(), indicator.getValue()});
            }
            return null;
        }).when(reporting).readConfigFile(anyString(), eq(cipher));
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
    }

    @Test
    public void upgradesBoth54And55UsingNewVersion56AndMappedFields() throws Exception {
        screening("client");
        results("submission", "client");
        event("screen", TbLeprosyReportingMigration.SCREENING_EVENT, "screen-submission", "client",
                observations("screening_location", "pharmacy", "hiv_tested", "yes"));
        event("result", TbLeprosyReportingMigration.RESULTS_EVENT, "submission", "client",
                observations("hiv_tested", "yes_hiv_tested", "tpt_initiation_date", "03-08-2026"));
        try (MockedStatic<ReportingLibrary> library = mockStatic(ReportingLibrary.class)) {
            library.when(ReportingLibrary::getInstance).thenReturn(reporting);
            ChwRepositoryFlv.onUpgrade(RuntimeEnvironment.getApplication(), cipher, 54, 56);
            ChwRepositoryFlv.onUpgrade(RuntimeEnvironment.getApplication(), cipher, 55, 56);
        }
        assertTrue(BuildConfig.DATABASE_VERSION >= 56);
        assertEquals("pharmacy", value("ec_tbleprosy_screening", "screening_location", "client"));
        assertEquals("yes_hiv_tested", value("ec_tbleprosy_observation_results", "hiv_tested", "submission"));
        assertEquals("03-08-2026", value("ec_tbleprosy_observation_results", "tpt_initiation_date", "submission"));
        assertMapping("ec_tbleprosy_screening", "screening_location");
        assertMapping("ec_tbleprosy_screening", "hiv_tested");
        assertMapping("ec_tbleprosy_observation_results", "hiv_tested");
        assertMapping("ec_tbleprosy_observation_results", "tpt_initiation_date");
    }

    @Test
    public void existingAnswersAndHistoricalDatesArePreservedOnReplay() throws Exception {
        migrate();
        screening("client");
        results("submission", "client");
        db.execSQL("UPDATE ec_tbleprosy_screening SET screening_location='market', hiv_tested='no'");
        db.execSQL("UPDATE ec_tbleprosy_observation_results SET hiv_tested='no_hiv_not_tested', "
                + "tpt_initiation_date='29-02-2024', tb_treatment_start_date='01-02-2024'");
        event("result", TbLeprosyReportingMigration.RESULTS_EVENT, "submission", "client",
                observations("hiv_tested", "yes_hiv_tested", "tpt_initiation_date", "03-08-2026"));
        migrate();
        migrate();
        assertEquals("market", value("ec_tbleprosy_screening", "screening_location", "client"));
        assertEquals("no", value("ec_tbleprosy_screening", "hiv_tested", "client"));
        assertEquals("no_hiv_not_tested", value("ec_tbleprosy_observation_results", "hiv_tested", "submission"));
        assertEquals("29-02-2024", value("ec_tbleprosy_observation_results", "tpt_initiation_date", "submission"));
        assertEquals("01-02-2024", value("ec_tbleprosy_observation_results", "tb_treatment_start_date", "submission"));
        assertEquals(272, count("indicator_queries"));
    }

    @Test
    public void latestCapturedAnswersAreSelectedIndependentlyForEachField() throws Exception {
        screening("client");
        event("older", TbLeprosyReportingMigration.SCREENING_EVENT, "screen", "client",
                observations("screening_location", "pharmacy", "hiv_tested", "no"));
        db.execSQL("UPDATE event SET updatedAt='2026-08-01 12:00:00' WHERE eventId='older'");
        event("newer", TbLeprosyReportingMigration.RESULTS_EVENT, "result", "client",
                observations("hiv_tested", "yes_hiv_tested"));
        migrate();
        assertEquals("pharmacy", value("ec_tbleprosy_screening", "screening_location", "client"));
        assertEquals("yes_hiv_tested", value("ec_tbleprosy_screening", "hiv_tested", "client"));
    }

    @Test
    public void lateSyncedOlderScreeningCannotOverrideMoreRecentClinicalAnswers() throws Exception {
        screening("client");
        event("newer-visit", TbLeprosyReportingMigration.SCREENING_EVENT, "screen-new", "client",
                observations("screening_location", "pharmacy", "hiv_tested", "yes"));
        db.execSQL("UPDATE event SET eventDate='2026-10-10', updatedAt='2026-10-11 12:00:00' WHERE eventId='newer-visit'");
        event("older-late-sync", TbLeprosyReportingMigration.SCREENING_EVENT, "screen-old", "client",
                observations("screening_location", "market", "hiv_tested", "no"));
        db.execSQL("UPDATE event SET eventDate='2026-08-01', updatedAt='2026-10-24 12:00:00' WHERE eventId='older-late-sync'");
        migrate();
        assertEquals("pharmacy", value("ec_tbleprosy_screening", "screening_location", "client"));
        assertEquals("yes", value("ec_tbleprosy_screening", "hiv_tested", "client"));
    }

    @Test
    public void differentClientsSubmissionsAndEventTypesCannotSupplyAnswers() throws Exception {
        screening("client-a");
        results("submission-a", "client-a");
        event("other-client", TbLeprosyReportingMigration.RESULTS_EVENT, "submission-a", "client-b",
                observations("tpt_initiation_date", "01-10-2026"));
        event("other-submission", TbLeprosyReportingMigration.RESULTS_EVENT, "submission-b", "client-a",
                observations("tpt_initiation_date", "01-10-2026"));
        event("other-module", "Other module visit", "submission-a", "client-a",
                observations("tpt_initiation_date", "01-10-2026", "hiv_tested", "yes"));
        event("bad-payload", TbLeprosyReportingMigration.RESULTS_EVENT, "submission-a", "client-a",
                observations("tpt_initiation_date", "01-10-2026"));
        JSONObject mismatch = payload(TbLeprosyReportingMigration.RESULTS_EVENT, "wrong", "client-a",
                observations("tpt_initiation_date", "01-10-2026"));
        db.execSQL("UPDATE event SET json=? WHERE eventId='bad-payload'", new Object[] {mismatch.toString()});
        migrate();
        assertNull(value("ec_tbleprosy_observation_results", "tpt_initiation_date", "submission-a"));
        assertNull(value("ec_tbleprosy_screening", "hiv_tested", "client-a"));
        assertEquals(1, count("ec_tbleprosy_observation_results"));
    }

    @Test
    public void corruptVoidedAndOrphanEventsDoNotInventProjections() throws Exception {
        screening("client");
        results("submission", "client");
        event("voided", TbLeprosyReportingMigration.RESULTS_EVENT, "submission", "client",
                observations("hiv_tested", "yes_hiv_tested"));
        JSONObject voided = payload(TbLeprosyReportingMigration.RESULTS_EVENT, "submission", "client",
                observations("hiv_tested", "yes_hiv_tested")).put("voided", true);
        db.execSQL("UPDATE event SET json=? WHERE eventId='voided'", new Object[] {voided.toString()});
        event("corrupt", TbLeprosyReportingMigration.SCREENING_EVENT, "screen", "client", new JSONArray());
        db.execSQL("UPDATE event SET json='{broken' WHERE eventId='corrupt'");
        event("orphan", TbLeprosyReportingMigration.RESULTS_EVENT, "orphan", "missing-client",
                observations("hiv_tested", "yes_hiv_tested", "tpt_initiation_date", "01-10-2026"));
        migrate();
        assertNull(value("ec_tbleprosy_screening", "hiv_tested", "client"));
        assertNull(value("ec_tbleprosy_observation_results", "hiv_tested", "submission"));
        assertEquals(1, count("ec_tbleprosy_observation_results"));
    }

    @Test
    public void optionalTptDateMustBeStrictCapturedCalendarDate() throws Exception {
        String[] dates = {"", "   ", "31-02-2026", "1-02-2026", "2026-02-01", "01-02-2026extra", "29-02-2025", "29-02-2024"};
        for (int i = 0; i < dates.length; i++) {
            results("submission-" + i, "client");
            event("event-" + i, TbLeprosyReportingMigration.RESULTS_EVENT, "submission-" + i, "client",
                    observations("tpt_initiation_date", dates[i]));
        }
        migrate();
        assertEquals("1", scalar("SELECT COUNT(*) FROM ec_tbleprosy_observation_results WHERE tpt_initiation_date IS NOT NULL"));
        assertEquals("29-02-2024", value("ec_tbleprosy_observation_results", "tpt_initiation_date", "submission-7"));
    }

    @Test
    public void refreshPreservesOtherModulesAndNonreplayableDaysAndClearsCheckpoint() throws Exception {
        db.execSQL("INSERT INTO indicators VALUES ('tbleprosy_old','old'), ('kvp-1','keep')");
        db.execSQL("INSERT INTO indicator_queries (indicator_code,indicator_query) VALUES ('tbleprosy_old','broken'), ('kvp-1','SELECT 2')");
        tally("tbleprosy_old", "2026-10-01", 8);
        tally("tbleprosy_old", "2026-09-01", 7);
        tally("tbleprosy_old", "2026-08-01", 6);
        tally("tbleprosy_old", "2026-07-01", 5);
        tally("tbleprosy_old", "2026-06-01", 4);
        tally("kvp-1", "2026-10-01", 3);
        event("valid", TbLeprosyReportingMigration.SCREENING_EVENT, "screen", "client", new JSONArray());
        event("report", "TB tallies Report", "report", "client", new JSONArray());
        db.execSQL("UPDATE event SET eventDate='2026-08-01' WHERE eventId='report'");
        event("missing-update", TbLeprosyReportingMigration.SCREENING_EVENT, "old", "client", new JSONArray());
        db.execSQL("UPDATE event SET eventDate='2026-07-01', updatedAt=NULL WHERE eventId='missing-update'");
        event("bad-update", TbLeprosyReportingMigration.SCREENING_EVENT, "old-2", "client", new JSONArray());
        db.execSQL("UPDATE event SET eventDate='2026-06-01', updatedAt='broken' WHERE eventId='bad-update'");
        migrate();
        migrate();
        assertEquals(273, count("indicator_queries"));
        assertEquals("SELECT 2", scalar("SELECT indicator_query FROM indicator_queries WHERE indicator_code='kvp-1'"));
        assertEquals(5, count("indicator_daily_tally"));
        assertEquals("0", scalar("SELECT COUNT(*) FROM indicator_daily_tally WHERE indicator_code='tbleprosy_old' AND day='2026-10-01'"));
        assertEquals(3, Integer.parseInt(scalar("SELECT CAST(indicator_value AS INTEGER) FROM indicator_daily_tally WHERE indicator_code='kvp-1'")));
        assertEquals("", preferences.get(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE));
    }

    @Test
    public void missingRetainedEventsLeaveHistoricalTalliesIntact() {
        db.execSQL("DROP TABLE event");
        tally("tbleprosy_old", "2025-01-01", 4);
        migrate();
        assertEquals(1, count("indicator_daily_tally"));
        assertEquals("", preferences.get(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE));
    }

    @Test
    public void all272QueriesExecuteAgainstTheActualMappedSchema() {
        migrate();
        assertEquals(272, queries.size());
        for (String key : queries.keySet()) {
            try (Cursor cursor = db.rawQuery(query(key, REPORT_DATE), null)) {
                assertTrue(key, cursor.moveToFirst());
                assertEquals(key, 1, cursor.getColumnCount());
            }
        }
    }

    @Test
    public void observationResultsJoinByClientAndCountRepeatedResultsOnce() {
        migrate();
        family("client", 12);
        results("submission-a", "client");
        results("submission-b", "client");
        assertEquals("1", scalar(query("tbleprosy_comm_col5_ke_6_14", REPORT_DATE)));
        assertEquals("0", scalar(query("tbleprosy_comm_col5_ke_6_14", "2026-09-01")));
    }

    @Test
    public void combinedInvestigationTypesCountBothExactTokens() {
        migrate();
        String[] types = {"tb", "leprosy", "tb,leprosy", "not_tb", "not_leprosy"};
        for (int i = 0; i < types.length; i++) {
            String client = "client-" + i;
            family(client, 12);
            results("submission-" + i, client);
            db.execSQL("UPDATE ec_tbleprosy_observation_results SET investigation_type=? WHERE entity_id=?",
                    new Object[] {types[i], client});
        }
        assertEquals("2", scalar(query("tbleprosy_comm_col6_ke_6_14", REPORT_DATE)));
        assertEquals("2", scalar(query("tbleprosy_comm_col7_ke_6_14", REPORT_DATE)));
    }

    @Test
    public void followUpVisitsJoinByClientAndCountRepeatedVisitsOnce() {
        migrate();
        family("client", 12);
        db.execSQL("INSERT INTO ec_tbleprosy_followup_visit (id,base_entity_id,entity_id,returned_to_treatment,last_interacted_with) "
                + "VALUES ('visit-a','visit-a','client','yes'," + OCTOBER_TIMESTAMP + "), "
                + "('visit-b','visit-b','client','yes'," + OCTOBER_TIMESTAMP + ")");
        assertEquals("1", scalar(query("tbleprosy_treat_followed_ke_0_14", REPORT_DATE)));
    }

    @Test
    public void negativeOrUnansweredHivTestsAreExcludedWhileBothPositiveCodesCount() {
        migrate();
        String[] screeningAnswers = {"yes", "no", "no", null, "", "   "};
        String[] resultAnswers = {"no_hiv_not_tested", "yes_hiv_tested", "no_hiv_not_tested", null, "", "   "};
        for (int i = 0; i < screeningAnswers.length; i++) {
            String client = "client-" + i;
            family(client, 12);
            screening(client);
            results("submission-" + i, client);
            db.execSQL("UPDATE ec_tbleprosy_screening SET hiv_tested=? WHERE base_entity_id=?", new Object[] {screeningAnswers[i], client});
            db.execSQL("UPDATE ec_tbleprosy_observation_results SET hiv_tested=? WHERE entity_id=?", new Object[] {resultAnswers[i], client});
        }
        assertEquals("2", scalar(query("tbleprosy_comm_col9_ke_6_14", REPORT_DATE)));
    }

    @Test
    public void dualPresumptiveStatusCountsWithoutMatchingUnrelatedTokens() {
        migrate();
        String[] statuses = {"tb_presumptive", "leprosy_presumptive", "tb_presumptive,leprosy_presumptive", "not_tb_presumptive", "negative"};
        for (int i = 0; i < statuses.length; i++) {
            String client = "client-" + i;
            family(client, 12);
            screening(client);
            db.execSQL("UPDATE ec_tbleprosy_screening SET screening_location='pharmacy', screening_status=? WHERE base_entity_id=?",
                    new Object[] {statuses[i], client});
        }
        assertEquals("3", scalar(query("tbleprosy_area_col2_ke_duka", REPORT_DATE)));
    }

    @Test
    public void householdContactJoinsDoNotMultiplyClientsWithRepeatedResults() {
        migrate();
        family("child", 4);
        screening("child");
        db.execSQL("UPDATE ec_tbleprosy_screening SET screening_status='tb_presumptive,leprosy_presumptive'");
        results("submission-a", "child");
        results("submission-b", "child");
        db.execSQL("UPDATE ec_tbleprosy_observation_results SET tb_sample_test_results='pulmonary_tb'");
        db.execSQL("INSERT INTO ec_tbleprosy_contact_visit (id,base_entity_id,relationship_to_index_client,last_interacted_with) "
                + "VALUES ('child','child','same_household'," + OCTOBER_TIMESTAMP + ")");
        assertEquals("1", scalar(query("tbleprosy_house_col7_ke_0_5", REPORT_DATE)));
    }

    @Test
    public void tptUsesCapturedInitiationMonthAndRejectsUnknownOrInvalidDates() throws Exception {
        family("child", 4);
        String[] dates = {"03-08-2026", "03-08-2026", "", "   ", "31-02-2026", "2026-08-03"};
        for (int i = 0; i < dates.length; i++) {
            String submission = "submission-" + i;
            results(submission, "child");
            event("event-" + i, TbLeprosyReportingMigration.RESULTS_EVENT, submission, "child",
                    observations("tpt_initiation_date", dates[i]));
        }
        migrate();
        assertEquals("03-08-2026", value("ec_tbleprosy_observation_results", "tpt_initiation_date", "submission-0"));
        assertEquals("1", scalar(query("tbleprosy_house_col9_ke_0_5", "2026-08-01")));
        assertEquals("0", scalar(query("tbleprosy_house_col9_ke_0_5", REPORT_DATE)));
        assertEquals("0", scalar("SELECT COUNT(*) FROM event WHERE date(eventDate) LIKE '2026-08-%'"));
        assertEquals("1", scalar("SELECT CAST(indicator_value AS INTEGER) FROM indicator_daily_tally "
                + "WHERE indicator_code='tbleprosy_house_col9_ke_0_5' AND day='2026-08-03'"));
        assertEquals("0", scalar("SELECT COUNT(*) FROM indicator_daily_tally "
                + "WHERE indicator_code='tbleprosy_house_col9_ke_0_5' AND day LIKE '2026-10-%'"));
        // Imported projections can predate strict capture validation; SQL must still reject them.
        db.execSQL("UPDATE ec_tbleprosy_observation_results SET tpt_initiation_date='31-02-2026'");
        assertEquals("0", scalar(query("tbleprosy_house_col9_ke_0_5", "2026-02-01")));
    }

    @Test
    public void tptRecoveryCorrectsLaterStaleTalliesWithoutChangingDaysOrOtherHistory() {
        migrate();
        family("child", 4);
        results("submission", "child");
        db.execSQL("UPDATE ec_tbleprosy_observation_results SET tpt_initiation_date='03-08-2026'");
        String code = "tbleprosy_house_col9_ke_0_5";
        tally(code, "2026-08-01", 20);
        tally(code, "2026-08-31", 9);
        tally(code, "2026-09-01", 4);
        tally("tbleprosy_comm_col5_ke_6_14", "2026-08-31", 7);
        migrate();
        int tallyCount = count("indicator_daily_tally");
        assertEquals("2", scalar("SELECT COUNT(*) FROM indicator_daily_tally WHERE indicator_code='" + code
                + "' AND day LIKE '2026-08-%' AND indicator_value=1"));
        assertEquals("1", scalar("SELECT CAST(indicator_value AS INTEGER) FROM indicator_daily_tally "
                + "WHERE indicator_code='" + code + "' AND day='2026-08-31'"));
        assertEquals("4", scalar("SELECT CAST(indicator_value AS INTEGER) FROM indicator_daily_tally "
                + "WHERE indicator_code='" + code + "' AND day='2026-09-01'"));
        assertEquals("7", scalar("SELECT CAST(indicator_value AS INTEGER) FROM indicator_daily_tally "
                + "WHERE indicator_code='tbleprosy_comm_col5_ke_6_14' AND day='2026-08-31'"));
        migrate();
        assertEquals(tallyCount, count("indicator_daily_tally"));
    }

    private void migrate() {
        TbLeprosyReportingMigration.upgrade(cipher, reporting);
    }

    private void screening(String client) {
        db.execSQL("INSERT INTO ec_tbleprosy_screening (id,base_entity_id,last_interacted_with) VALUES (?, ?, "
                + OCTOBER_TIMESTAMP + ")", new Object[] {client, client});
    }

    private void results(String submission, String client) {
        db.execSQL("INSERT INTO ec_tbleprosy_observation_results (id,base_entity_id,entity_id,last_interacted_with) VALUES (?, ?, ?, "
                + OCTOBER_TIMESTAMP + ")", new Object[] {submission, submission, client});
    }

    private void family(String client, int years) {
        db.execSQL("INSERT INTO ec_family_member (id,base_entity_id,gender,dob) VALUES (?, ?, 'Female', date('now', ?))",
                new Object[] {client, client, "-" + years + " years"});
    }

    private JSONArray observations(String... fields) throws Exception {
        JSONArray observations = new JSONArray();
        for (int i = 0; i < fields.length; i += 2) {
            observations.put(new JSONObject().put("fieldCode", fields[i])
                    .put("values", new JSONArray().put(fields[i + 1])));
        }
        return observations;
    }

    private JSONObject payload(String type, String submission, String client, JSONArray observations) throws Exception {
        return new JSONObject().put("eventType", type).put("formSubmissionId", submission)
                .put("baseEntityId", client).put("obs", observations);
    }

    private void event(String id, String type, String submission, String client, JSONArray observations) throws Exception {
        db.execSQL("INSERT INTO event VALUES (?, ?, ?, ?, ?, '2026-10-01', '2026-10-02 01:00:00')",
                new Object[] {id, payload(type, submission, client, observations).toString(), type, submission, client});
    }

    private void tally(String code, String day, int count) {
        db.execSQL("INSERT INTO indicator_daily_tally (indicator_code,day,indicator_value) VALUES (?, ?, ?)", new Object[] {code, day, count});
    }

    private String query(String key, String reportDate) {
        return queries.get(key).replace("%s", reportDate).replace("%p", "fixture-provider");
    }

    private String value(String table, String field, String id) {
        try (Cursor cursor = db.rawQuery("SELECT " + field + " FROM " + table + " WHERE base_entity_id=?", new String[] {id})) {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
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

    private void assertMapping(String table, String field) throws Exception {
        JSONArray columns = schemas.get(table).getJSONArray("columns");
        int matches = 0;
        for (int i = 0; i < columns.length(); i++) {
            JSONObject column = columns.getJSONObject(i);
            if (field.equals(column.getString("column_name"))) {
                matches++;
                assertEquals("Event", column.getString("type"));
                assertEquals("obs.fieldCode", column.getJSONObject("json_mapping").getString("field"));
                assertEquals(field, column.getJSONObject("json_mapping").getString("concept"));
            }
        }
        assertEquals(1, matches);
    }

    private void createProjection(String table, boolean omitNewFields) throws Exception {
        List<String> columns = new ArrayList<>();
        columns.add("id TEXT PRIMARY KEY");
        JSONArray fields = schemas.get(table).getJSONArray("columns");
        for (int i = 0; i < fields.length(); i++) {
            String field = fields.getJSONObject(i).getString("column_name");
            boolean newField = "screening_location".equals(field) || "hiv_tested".equals(field) || "tpt_initiation_date".equals(field);
            if (!"id".equals(field) && !(omitNewFields && newField)) columns.add("\"" + field + "\" TEXT");
        }
        db.execSQL("CREATE TABLE " + table + " (" + String.join(", ", columns) + ")");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> configQueries(String path) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets().open(path)) {
            Map<String, Object> config = new Yaml().load(input);
            for (Map<String, Object> indicator : (List<Map<String, Object>>) config.get("indicators")) {
                result.put((String) indicator.get("key"), (String) indicator.get("indicatorQuery"));
            }
        }
        return result;
    }

    private String asset(String path) throws Exception {
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets().open(path);
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            return scanner.next();
        }
    }
}
