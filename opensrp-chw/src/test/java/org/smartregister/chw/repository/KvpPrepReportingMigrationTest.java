package org.smartregister.chw.repository;

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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.BuildConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, manifest = Config.NONE)
public class KvpPrepReportingMigrationTest {
    private static final String TABLE = "ec_kvp_prep_followup";
    private static final String[] SHARED = {"prep_type", "prep_facility", "currently_on_prep", "linked_to_prep_services"};
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void version51BackfillUsesEligibleBranchAndPreservesSourceAnswers() throws Exception {
        createOldTable(51);
        insert("a");
        insert("b", "hiv_tested_within_last_3_months", "no");
        insert("linked", "on_prep", "no", "linked_to_prep_recent", "yes");
        insert("not-referred", "hiv_tested_within_last_3_months", "no", "referred_for_hiv_test", "no");
        insert("positive", "hiv_positive", "true");
        insert("missing-result", "hiv_result_recent", "");
        insert("missing-screener", "hiv_tested_within_last_3_months", "");
        Map<String, String> original = row("a", "base_entity_id, hiv_tested_within_last_3_months, "
                + "hiv_result_recent, on_prep, prep_type_a, prep_facility_a, linked_to_prep_recent, "
                + "referred_for_hiv_test, tested_for_hiv, hiv_result, prep_follow_up, "
                + "prep_type_b, prep_facility_b, linked_to_prep");
        net.zetetic.database.sqlcipher.SQLiteDatabase adapter = adapter();

        ChwRepositoryFlv.onUpgrade(null, adapter, 51, 52);
        ChwRepositoryFlv.onUpgrade(null, adapter, 51, 52);

        assertTrue(BuildConfig.DATABASE_VERSION >= 52);
        assertShared("a", "oral_prep", "Facility a", "yes", "");
        assertShared("b", "injectable_prep", "Facility b", "yes", "");
        assertShared("linked", "", "", "no", "yes");
        for (String id : new String[]{"not-referred", "positive", "missing-result", "missing-screener"}) {
            assertShared(id, "", "", "", "");
        }
        Map<String, String> migrated = row("a");
        for (Map.Entry<String, String> entry : original.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(), migrated.get(entry.getKey()));
        }
        try (Cursor count = database.rawQuery("SELECT COUNT(*) FROM " + TABLE, null)) {
            assertTrue(count.moveToFirst());
            assertEquals(7, count.getInt(0));
        }
    }

    @Test
    public void version50UpgradeAddsBranchTypesAndSharedFieldsWithoutInventingType() throws Exception {
        createOldTable(50);
        insert("older");
        ChwRepositoryFlv.onUpgrade(null, adapter(), 50, 52);
        assertShared("older", "", "Facility a", "yes", "");
        assertEquals(null, row("older").get("prep_type_a"));
        assertEquals(null, row("older").get("prep_type_b"));
    }

    @Test
    public void partialUpgradeReusesExistingColumnAndRecalculatesStaleDerivedValue() throws Exception {
        createOldTable(51);
        insert("partial");
        database.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN prep_type VARCHAR");
        database.execSQL("UPDATE " + TABLE + " SET prep_type = 'stale'");
        ChwRepositoryFlv.onUpgrade(null, adapter(), 51, 52);
        assertShared("partial", "oral_prep", "Facility a", "yes", "");
    }

    private void createOldTable(int version) throws Exception {
        Path path = Paths.get("src/nacp/assets/ec_client_fields.json");
        if (!Files.exists(path)) {
            path = Paths.get("opensrp-chw").resolve(path);
        }
        JSONArray tables = new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))
                .getJSONArray("bindobjects");
        for (int i = 0; i < tables.length(); i++) {
            if (!TABLE.equals(tables.getJSONObject(i).getString("name"))) {
                continue;
            }
            JSONArray columns = tables.getJSONObject(i).getJSONArray("columns");
            StringBuilder schema = new StringBuilder();
            for (int j = 0; j < columns.length(); j++) {
                String column = columns.getJSONObject(j).getString("column_name");
                if (Arrays.asList(SHARED).contains(column)
                        || (version == 50 && ("prep_type_a".equals(column) || "prep_type_b".equals(column)))) {
                    continue;
                }
                if (schema.length() > 0) {
                    schema.append(", ");
                }
                schema.append(column).append(" VARCHAR");
            }
            database.execSQL("CREATE TABLE " + TABLE + " (" + schema + ")");
            return;
        }
        throw new AssertionError("Missing follow-up table mapping");
    }

    private void insert(String id, String... overrides) {
        ContentValues values = new ContentValues();
        String[] defaults = {"base_entity_id", id, "hiv_tested_within_last_3_months", "yes",
                "hiv_result_recent", "negative", "on_prep", "yes", "prep_facility_a", "Facility a",
                "linked_to_prep_recent", "no", "referred_for_hiv_test", "yes", "tested_for_hiv", "yes",
                "hiv_result", "negative", "prep_follow_up", "yes", "prep_facility_b", "Facility b",
                "linked_to_prep", "no"};
        for (int i = 0; i < defaults.length; i += 2) {
            values.put(defaults[i], defaults[i + 1]);
        }
        try (Cursor columns = database.rawQuery("SELECT * FROM " + TABLE + " LIMIT 0", null)) {
            if (columns.getColumnIndex("prep_type_a") >= 0) {
                values.put("prep_type_a", "oral_prep");
                values.put("prep_type_b", "injectable_prep");
            }
        }
        for (int i = 0; i < overrides.length; i += 2) {
            values.put(overrides[i], overrides[i + 1]);
        }
        database.insertOrThrow(TABLE, null, values);
    }

    private net.zetetic.database.sqlcipher.SQLiteDatabase adapter() {
        net.zetetic.database.sqlcipher.SQLiteDatabase db = mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(db.rawQuery(anyString(), any())).thenAnswer(call ->
                database.rawQuery(call.getArgument(0), (String[]) call.getArgument(1)));
        doAnswer(call -> {
            try {
                database.execSQL(call.getArgument(0));
            } catch (RuntimeException e) {
                throw new AssertionError("Migration SQL failed: " + call.getArgument(0), e);
            }
            return null;
        }).when(db).execSQL(anyString());
        return db;
    }

    private Map<String, String> row(String id) {
        return row(id, "*");
    }

    private Map<String, String> row(String id, String projection) {
        Map<String, String> values = new HashMap<>();
        try (Cursor cursor = database.rawQuery("SELECT " + projection + " FROM " + TABLE + " WHERE base_entity_id = ?", new String[]{id})) {
            assertTrue(cursor.moveToFirst());
            for (String column : cursor.getColumnNames()) {
                values.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
            }
        }
        return values;
    }

    private void assertShared(String id, String type, String facility, String currentUse, String linkage) {
        Map<String, String> values = row(id);
        String[] expected = {type, facility, currentUse, linkage};
        for (int i = 0; i < SHARED.length; i++) {
            assertEquals(id + ": " + SHARED[i], expected[i], values.get(SHARED[i]));
        }
    }
}
