package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
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
import android.content.ContentValues;
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

/** Executes the version-55 migration and every KVP indicator against real SQLite. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class KvpReportingMigrationTest {
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
                .open(KvpReportingMigration.REPORT_CONFIG)) {
            Map<String, Object> config = new Yaml().load(input);
            for (Map<String, Object> indicator : (List<Map<String, Object>>) config.get("indicators")) {
                queries.put((String) indicator.get("key"), (String) indicator.get("indicatorQuery"));
            }
        }
        db = SQLiteDatabase.create(null);
        createProjection(KvpReportingMigration.FOLLOW_UP_TABLE, true);
        createProjection("ec_kvp_prep_register", false);
        createProjection("ec_family_member", false);
        createProjection("ec_hivst_results", false);
        db.execSQL("CREATE TABLE task (\"for\" TEXT, focus TEXT, authored_on INTEGER)");
        db.execSQL("CREATE TABLE event (eventId TEXT PRIMARY KEY, json TEXT, eventType TEXT, "
                + "formSubmissionId TEXT, baseEntityId TEXT, eventDate TEXT, updatedAt TEXT)");
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
            for (Map.Entry<String, String> indicator : queries.entrySet()) {
                db.execSQL("INSERT INTO indicators VALUES (?, ?)",
                        new Object[] {indicator.getKey(), "KVP"});
                db.execSQL("INSERT INTO indicator_queries VALUES (?, ?)",
                        new Object[] {indicator.getKey(), indicator.getValue()});
            }
            return null;
        }).when(reporting).readConfigFile(eq(KvpReportingMigration.REPORT_CONFIG), eq(cipher));
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
    }

    @Test
    public void version54UpgradeAddsMappedDateAndRecoversOnlyMatchingVisit() throws Exception {
        projection("visit-a", "client-a", null);
        projection("visit-b", "client-a", null);
        event("event-a", "visit-a", "client-a", "03-08-2026");
        event("event-b", "visit-b", "client-a", "15-09-2026");
        try (MockedStatic<ReportingLibrary> library = mockStatic(ReportingLibrary.class)) {
            library.when(ReportingLibrary::getInstance).thenReturn(reporting);
            ChwRepositoryFlv.onUpgrade(RuntimeEnvironment.getApplication(), cipher, 54, 55);
        }
        assertEquals("03-08-2026", date("visit-a"));
        assertEquals("15-09-2026", date("visit-b"));
        assertTrue(BuildConfig.DATABASE_VERSION >= 55);
        JSONArray columns = schemas.get(KvpReportingMigration.FOLLOW_UP_TABLE).getJSONArray("columns");
        int matches = 0;
        for (int i = 0; i < columns.length(); i++) {
            JSONObject column = columns.getJSONObject(i);
            if (KvpReportingMigration.VISIT_DATE.equals(column.getString("column_name"))) {
                matches++;
                assertEquals("Event", column.getString("type"));
                assertEquals("obs.fieldCode", column.getJSONObject("json_mapping").getString("field"));
                assertEquals("kvp_visit_date", column.getJSONObject("json_mapping").getString("concept"));
            }
        }
        assertEquals(1, matches);
    }

    @Test
    public void existingDatesAndOtherAnswersSurviveReplay() throws Exception {
        db.execSQL("ALTER TABLE ec_kvp_prep_followup ADD COLUMN kvp_visit_date TEXT");
        projection("visit", "client", "01-07-2026");
        db.execSQL("UPDATE ec_kvp_prep_followup SET visit_type='new_visit', "
                + "number_of_male_condoms_issued='12' WHERE base_entity_id='visit'");
        event("event", "visit", "client", "20-10-2026");
        migrate();
        migrate();
        assertEquals("01-07-2026", date("visit"));
        assertEquals("new_visit", scalar("SELECT visit_type FROM ec_kvp_prep_followup"));
        assertEquals("12", scalar("SELECT number_of_male_condoms_issued FROM ec_kvp_prep_followup"));
        assertEquals(queries.size(), count("indicator_queries"));
        assertEquals(queries.size(), count("indicators"));
    }

    @Test
    public void malformedMissingAndImpossibleDatesRemainUnknown() throws Exception {
        String[] invalid = {"31-02-2026", "1-02-2026", "2026-02-01", "01-02-2026extra", "", "29-02-2025"};
        for (int i = 0; i < invalid.length; i++) {
            projection("visit-" + i, "client", null);
            event("event-" + i, "visit-" + i, "client", invalid[i]);
        }
        projection("malformed", "client", null);
        insertEvent("malformed-event", "malformed", "client", "{broken-json", "2026-10-01", "2026-10-02 01:00:00");
        projection("no-observation", "client", null);
        JSONObject noDate = payload("no-observation", "client", "01-10-2026");
        noDate.put("obs", new JSONArray());
        insertEvent("missing-observation", "no-observation", "client", noDate.toString(), "2026-10-01", "2026-10-02 01:00:00");
        migrate();
        assertEquals("0", scalar("SELECT COUNT(*) FROM ec_kvp_prep_followup WHERE kvp_visit_date IS NOT NULL"));
    }

    @Test
    public void validLeapDayKeepsExactObservationDateRatherThanEventOrProcessingDate() throws Exception {
        projection("leap", "client", null);
        event("event", "leap", "client", "29-02-2024");
        migrate();
        assertEquals("29-02-2024", date("leap"));
    }

    @Test
    public void mismatchedClientSubmissionOrEventTypeNeverCrossLinksDates() throws Exception {
        projection("wrong-client", "client-a", null);
        event("event-a", "wrong-client", "client-b", "01-10-2026");
        projection("wrong-payload", "client-a", null);
        insertEvent("event-b", "wrong-payload", "client-a",
                payload("another-submission", "client-a", "01-10-2026").toString(),
                "2026-10-01", "2026-10-02 01:00:00");
        projection("wrong-type", "client-a", null);
        event("event-c", "wrong-type", "client-a", "01-10-2026");
        db.execSQL("UPDATE event SET eventType='Other visit' WHERE eventId='event-c'");
        event("orphan-event", "missing-projection", "missing-client", "01-10-2026");
        migrate();
        assertEquals("0", scalar("SELECT COUNT(*) FROM ec_kvp_prep_followup WHERE kvp_visit_date IS NOT NULL"));
        assertEquals(3, count("ec_kvp_prep_followup"));
    }

    @Test
    public void latestValidRetainedEventWinsForPreviouslyUnprojectedDate() throws Exception {
        projection("visit", "client", null);
        event("older", "visit", "client", "01-08-2026");
        db.execSQL("UPDATE event SET updatedAt='2026-08-02 01:00:00' WHERE eventId='older'");
        event("newer", "visit", "client", "02-08-2026");
        migrate();
        assertEquals("02-08-2026", date("visit"));
    }

    @Test
    public void refreshPreservesOtherReportsAndUnreplayableKvpDaysAndResetsCheckpoint() throws Exception {
        db.execSQL("INSERT INTO indicators VALUES ('kvp-old', 'old'), ('other-report', 'keep')");
        db.execSQL("INSERT INTO indicator_queries VALUES ('kvp-old', 'broken'), ('other-report', 'SELECT 4')");
        tally("kvp-1", "2026-10-01", 9);
        tally("kvp-1", "2026-09-01", 4);
        tally("kvp-1", "2026-08-01", 5);
        tally("kvp-1", "2026-07-01", 6);
        tally("kvp-1", "2026-06-01", 7);
        tally("other-report", "2026-10-01", 3);
        event("event", "visit", "client", "01-10-2026");
        insertEvent("report-event", "report", "client", "{}", "2026-08-01", "2026-08-02 01:00:00");
        db.execSQL("UPDATE event SET eventType='KVP tallies Report' WHERE eventId='report-event'");
        insertEvent("null-update", "null", "client", "{}", "2026-07-01", null);
        insertEvent("bad-update", "bad", "client", "{}", "2026-06-01", "malformed");
        migrate();
        assertEquals(queries.size() + 1, count("indicator_queries"));
        assertEquals(queries.size() + 1, count("indicators"));
        assertEquals("SELECT 4", scalar("SELECT indicator_query FROM indicator_queries WHERE indicator_code='other-report'"));
        assertEquals("0", scalar("SELECT COUNT(*) FROM indicator_daily_tally WHERE indicator_code='kvp-1' AND day='2026-10-01'"));
        assertEquals(5, count("indicator_daily_tally"));
        assertEquals("3.0", scalar("SELECT indicator_value FROM indicator_daily_tally WHERE indicator_code='other-report'"));
        assertEquals("", preferences.get(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE));
        migrate();
        assertEquals(queries.size() + 1, count("indicator_queries"));
        assertEquals(5, count("indicator_daily_tally"));
    }

    @Test
    public void missingRetainedEventTablePreservesExistingTallies() {
        db.execSQL("DROP TABLE event");
        tally("kvp-1", "2025-01-01", 4);
        migrate();
        assertEquals(1, count("indicator_daily_tally"));
        assertEquals("", preferences.get(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE));
    }

    @Test
    public void everyConfiguredIndicatorExecutesAgainstTheMappedSchema() {
        migrate();
        assertEquals(799, queries.size());
        for (Map.Entry<String, String> indicator : queries.entrySet()) {
            try (Cursor cursor = db.rawQuery(query(indicator.getKey(), "2026-10-01"), null)) {
                assertTrue(indicator.getKey(), cursor.moveToFirst());
                assertEquals(indicator.getKey(), 1, cursor.getColumnCount());
            }
        }
    }

    @Test
    public void countsAndServiceTotalsUseRecoveredVisitMonthAndExcludeOtherMonths() throws Exception {
        projection("visit", "client", null);
        event("event", "visit", "client", "03-08-2026");
        db.execSQL("INSERT INTO ec_family_member (id, base_entity_id, gender, dob) "
                + "VALUES ('client', 'client', 'Male', date('now','-12 years'))");
        db.execSQL("INSERT INTO ec_kvp_prep_register (id, base_entity_id, client_group) "
                + "VALUES ('client', 'client', 'pwid')");
        db.execSQL("UPDATE ec_kvp_prep_followup SET visit_type='new_visit', "
                + "number_of_male_condoms_issued='4', number_of_female_condoms_issued='2'");
        migrate();
        assertEquals("1", scalar(query("kvp-1-10-14-pwid-ME", "2026-08-01")));
        assertEquals("0", scalar(query("kvp-1-10-14-pwid-ME", "2026-10-01")));
        assertEquals("6", scalar(query("kvp-5-10-14-pwid-ME", "2026-08-01")));
        assertEquals("0", scalar(query("kvp-2-10-14-pwid-ME", "2026-08-01")));
    }

    private void migrate() {
        KvpReportingMigration.upgrade(cipher, reporting);
    }

    private void projection(String submission, String entity, String date) {
        ContentValues values = new ContentValues();
        values.put("id", submission);
        values.put("base_entity_id", submission);
        values.put("entity_id", entity);
        if (date != null) values.put("kvp_visit_date", date);
        db.insertOrThrow(KvpReportingMigration.FOLLOW_UP_TABLE, null, values);
    }

    private JSONObject payload(String submission, String entity, String date) throws Exception {
        return new JSONObject().put("eventType", KvpReportingMigration.FOLLOW_UP_EVENT)
                .put("formSubmissionId", submission).put("baseEntityId", entity)
                .put("obs", new JSONArray().put(new JSONObject().put("fieldCode", "kvp_visit_date")
                        .put("values", new JSONArray().put(date))));
    }

    private void event(String eventId, String submission, String entity, String date) throws Exception {
        insertEvent(eventId, submission, entity, payload(submission, entity, date).toString(),
                "2026-10-01", "2026-10-02 01:00:00");
    }

    private void insertEvent(String eventId, String submission, String entity, String payload,
            String eventDate, String updatedAt) {
        db.execSQL("INSERT INTO event VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[] {eventId,
                payload, KvpReportingMigration.FOLLOW_UP_EVENT, submission, entity, eventDate, updatedAt});
    }

    private void tally(String code, String day, int value) {
        db.execSQL("INSERT INTO indicator_daily_tally VALUES (?, ?, ?)", new Object[] {code, day, value});
    }

    private String date(String submission) {
        try (Cursor cursor = db.rawQuery("SELECT kvp_visit_date FROM ec_kvp_prep_followup "
                + "WHERE base_entity_id = ?", new String[] {submission})) {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private String query(String key, String reportDate) {
        return queries.get(key).replace("%s", reportDate).replace("%p", "fixture-provider");
    }

    private String scalar(String query) {
        try (Cursor cursor = db.rawQuery(query, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private int count(String table) {
        return Integer.parseInt(scalar("SELECT COUNT(*) FROM " + table));
    }

    private void createProjection(String table, boolean omitDate) throws Exception {
        List<String> definitions = new ArrayList<>();
        definitions.add("id TEXT PRIMARY KEY");
        JSONArray columns = schemas.get(table).getJSONArray("columns");
        for (int i = 0; i < columns.length(); i++) {
            String column = columns.getJSONObject(i).getString("column_name");
            if (!(omitDate && "kvp_visit_date".equals(column)) && !"id".equals(column)) {
                definitions.add("\"" + column + "\" TEXT");
            }
        }
        db.execSQL("CREATE TABLE " + table + " (" + String.join(", ", definitions) + ")");
    }

    private String asset(String path) throws Exception {
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets().open(path);
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            return scanner.next();
        }
    }
}
