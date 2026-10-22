package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.mothermentor.repository.MotherMentorReportingRepository;
import org.smartregister.util.DatabaseMigrationUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/** Reconciles the two different migrations previously released with database version 50. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorDevelopmentMigrationTest {
    private static final String REFERRAL = "ec_referral_followup";
    private static final String KVP = "ec_kvp_prep_followup";
    private static final String FOLLOW_UP = "ec_mothermentor_followup_visit";
    private static final String MOBILIZATION = "ec_mothermentor_mobilization";
    private static final String CONTACTS = "ec_mothermentor_contacts";
    private static final Set<String> REFERRAL_ANSWERS =
            new HashSet<>(
                    Arrays.asList(
                            "referral_task_id",
                            "client_attended_referral",
                            "reason_not_attended",
                            "attended_assigned_facility",
                            "reason_different_facility",
                            "facility_attended",
                            "services_received",
                            "client_condition",
                            "client_satisfied"));
    private static final Set<String> KVP_DATES =
            new HashSet<>(Arrays.asList("branch_a_test_date", "branch_b_test_date"));

    private SQLiteDatabase db;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;
    private MockedStatic<DatabaseMigrationUtils> migrations;
    private MockedStatic<CoreChwApplication> application;
    private final Map<String, JSONObject> schemas = new LinkedHashMap<>();
    private final Set<String> requestedTables = new HashSet<>();

    @Before
    public void setUp() throws Exception {
        try (InputStream input =
                        RuntimeEnvironment.getApplication()
                                .getAssets()
                                .open("ec_client_fields.json");
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            JSONArray bindings = new JSONObject(scanner.next()).getJSONArray("bindobjects");
            for (int i = 0; i < bindings.length(); i++) {
                JSONObject binding = bindings.getJSONObject(i);
                schemas.put(binding.getString("name"), binding);
            }
        }
        db = SQLiteDatabase.create(null);
        cipher = mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(cipher.rawQuery(anyString(), any(String[].class)))
                .thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
        // PRAGMA calls use null selection arguments, matching the existing migration tests.
        when(cipher.rawQuery(anyString(), org.mockito.ArgumentMatchers.<String[]>isNull()))
                .thenAnswer(call -> db.rawQuery(call.getArgument(0), null));
        doAnswer(
                        call -> {
                            db.execSQL(call.getArgument(0));
                            return null;
                        })
                .when(cipher)
                .execSQL(anyString());
        application = mockStatic(CoreChwApplication.class);
        application.when(CoreChwApplication::createCommonFtsObject).thenReturn(null);
        migrations = mockStatic(DatabaseMigrationUtils.class);
        migrations
                .when(() -> DatabaseMigrationUtils.createAddedECTables(eq(cipher), anySet(), any()))
                .thenAnswer(
                        call -> {
                            Set<String> requested = call.getArgument(1);
                            requestedTables.addAll(requested);
                            for (String table : requested) {
                                if (!tableExists(table)) createTable(table, Collections.emptySet());
                            }
                            return null;
                        });
    }

    @After
    public void tearDown() {
        if (migrations != null) migrations.close();
        if (application != null) application.close();
        if (db != null) db.close();
    }

    @Test
    public void development50GainsMotherMentorWithoutLosingReferralOrPrepAnswers()
            throws Exception {
        createTable(REFERRAL, Collections.emptySet());
        createTable(KVP, Collections.emptySet());
        db.execSQL(
                "INSERT INTO ec_referral_followup (base_entity_id, referral_task_id,"
                        + " client_satisfied) VALUES ('referral', 'task', 'yes')");
        db.execSQL(
                "INSERT INTO ec_kvp_prep_followup (base_entity_id, branch_a_test_date,"
                        + " branch_b_test_date) VALUES ('prep', '2026-10-01', '2026-10-02')");
        for (String table : motherMentorTables()) assertFalse(table, tableExists(table));

        upgradeAndReplay(50);

        assertEquals("task", value(REFERRAL, "referral_task_id", "referral"));
        assertEquals("yes", value(REFERRAL, "client_satisfied", "referral"));
        assertEquals("2026-10-01", value(KVP, "branch_a_test_date", "prep"));
        assertEquals("2026-10-02", value(KVP, "branch_b_test_date", "prep"));
    }

    @Test
    public void module53RepairsLegacyProjectionsAndKeepsMotherMentorData() throws Exception {
        for (String table : motherMentorTables()) {
            Set<String> omitted = Collections.emptySet();
            if (CONTACTS.equals(table)) omitted = Collections.singleton("screening_status");
            if (FOLLOW_UP.equals(table)) {
                omitted =
                        new HashSet<>(
                                Arrays.asList(
                                        "follow_up_category",
                                        "partner_arrived_at_facility",
                                        "child_eid_test_type",
                                        "child_eid_arrived_at_facility"));
            }
            if (MOBILIZATION.equals(table)) {
                omitted = new HashSet<>(Arrays.asList("sh_tarehe", "elim_m", "referred_male"));
            }
            createTable(table, omitted);
        }
        createTable(REFERRAL, REFERRAL_ANSWERS);
        createTable(KVP, KVP_DATES);
        db.execSQL(
                "INSERT INTO ec_mothermentor_enrollment (base_entity_id, ctc_number)"
                        + " VALUES ('mother', '12-34-5678-123456')");
        db.execSQL(
                "INSERT INTO ec_mothermentor_followup_visit (base_entity_id, follow_up_status)"
                        + " VALUES ('followup', 'found')");
        db.execSQL(
                "INSERT INTO ec_mothermentor_mobilization (base_entity_id, mobilization_date)"
                        + " VALUES ('community', '2026-10-03')");
        db.execSQL(
                "INSERT INTO ec_referral_followup (base_entity_id, chw_followup_feedback_id)"
                        + " VALUES ('referral', 'legacy-feedback')");
        db.execSQL("INSERT INTO ec_kvp_prep_followup (base_entity_id) VALUES ('prep')");
        assertFalse(columns(REFERRAL).contains("referral_task_id"));
        assertFalse(columns(KVP).contains("branch_a_test_date"));

        upgradeAndReplay(53);

        assertEquals(
                "12-34-5678-123456", value("ec_mothermentor_enrollment", "ctc_number", "mother"));
        assertEquals("found", value(FOLLOW_UP, "follow_up_status", "followup"));
        assertEquals("2026-10-03", value(MOBILIZATION, "mobilization_date", "community"));
        assertEquals("legacy-feedback", value(REFERRAL, "chw_followup_feedback_id", "referral"));
        assertEquals("prep", value(KVP, "base_entity_id", "prep"));
    }

    @Test
    public void version49CreatesBothBranchesTablesAndKeepsUnrelatedClients() throws Exception {
        db.execSQL("CREATE TABLE ec_family_member (base_entity_id TEXT, first_name TEXT)");
        db.execSQL("INSERT INTO ec_family_member VALUES ('client', 'Asha')");
        assertFalse(tableExists(REFERRAL));
        assertFalse(tableExists(KVP));

        upgradeAndReplay(49);

        assertEquals("Asha", value("ec_family_member", "first_name", "client"));
    }

    private void upgradeAndReplay(int oldVersion) throws Exception {
        assertEquals(54, BuildConfig.DATABASE_VERSION);
        ChwRepositoryFlv.onUpgrade(RuntimeEnvironment.getApplication(), cipher, oldVersion, 54);
        assertRequiredSchema();
        assertTrue(requestedTables.containsAll(motherMentorTables()));
        assertTrue(requestedTables.contains(REFERRAL));
        assertTrue(requestedTables.contains(KVP));
        Map<String, List<List<String>>> data = tableData();
        Map<String, Set<String>> columns = tableColumns();

        ChwRepositoryFlv.onUpgrade(RuntimeEnvironment.getApplication(), cipher, 53, 54);

        assertRequiredSchema();
        assertEquals(data, tableData());
        assertEquals(columns, tableColumns());
    }

    private void assertRequiredSchema() throws Exception {
        for (String table : motherMentorTables()) assertTrue(table, tableExists(table));
        assertTrue(columns(CONTACTS).contains("screening_status"));
        assertTrue(columns(FOLLOW_UP).containsAll(schemaColumns(FOLLOW_UP).keySet()));
        assertTrue(
                columns(MOBILIZATION)
                        .containsAll(MotherMentorReportingRepository.MOBILIZATION_COLUMNS));
        assertTrue(columns(REFERRAL).containsAll(REFERRAL_ANSWERS));
        assertTrue(columns(KVP).containsAll(KVP_DATES));
    }

    private Set<String> motherMentorTables() {
        Set<String> tables = new LinkedHashSet<>();
        for (String table : schemas.keySet()) {
            if (table.startsWith("ec_mothermentor_")) tables.add(table);
        }
        assertFalse(tables.isEmpty());
        return tables;
    }

    private Map<String, String> schemaColumns(String table) throws Exception {
        assertTrue("Missing JSON binding for " + table, schemas.containsKey(table));
        JSONArray columns = schemas.get(table).getJSONArray("columns");
        Map<String, String> definitions = new LinkedHashMap<>();
        for (int i = 0; i < columns.length(); i++) {
            JSONObject column = columns.getJSONObject(i);
            definitions.put(
                    column.getString("column_name"), column.optString("data_type", "VARCHAR"));
        }
        return definitions;
    }

    private void createTable(String table, Set<String> omitted) throws Exception {
        List<String> definitions = new ArrayList<>();
        for (Map.Entry<String, String> column : schemaColumns(table).entrySet()) {
            if (!omitted.contains(column.getKey())) {
                definitions.add("\"" + column.getKey() + "\" " + column.getValue());
            }
        }
        db.execSQL("CREATE TABLE " + table + " (" + String.join(", ", definitions) + ")");
    }

    private boolean tableExists(String table) {
        try (Cursor cursor =
                db.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                        new String[] {table})) {
            return cursor.moveToFirst();
        }
    }

    private Set<String> columns(String table) {
        Set<String> columns = new LinkedHashSet<>();
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
        }
        return columns;
    }

    private String value(String table, String column, String entityId) {
        try (Cursor cursor =
                db.rawQuery(
                        "SELECT " + column + " FROM " + table + " WHERE base_entity_id = ?",
                        new String[] {entityId})) {
            assertEquals(1, cursor.getCount());
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private Map<String, Set<String>> tableColumns() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String table : repairedTables()) result.put(table, columns(table));
        return result;
    }

    private Map<String, List<List<String>>> tableData() {
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        for (String table : repairedTables()) {
            List<List<String>> rows = new ArrayList<>();
            try (Cursor cursor = db.rawQuery("SELECT * FROM " + table + " ORDER BY rowid", null)) {
                while (cursor.moveToNext()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 0; i < cursor.getColumnCount(); i++) row.add(cursor.getString(i));
                    rows.add(row);
                }
            }
            result.put(table, rows);
        }
        return result;
    }

    private Set<String> repairedTables() {
        Set<String> tables = motherMentorTables();
        tables.add(REFERRAL);
        tables.add(KVP);
        return tables;
    }
}
