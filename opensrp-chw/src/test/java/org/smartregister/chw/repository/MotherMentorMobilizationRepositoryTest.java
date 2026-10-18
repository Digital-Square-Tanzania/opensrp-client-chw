package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.repository.MotherMentorReportingRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorMobilizationRepositoryTest {
    @Test
    public void version52UpgradeSupportsReportsBeforeFirstSessionAndPreservesExistingData() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL(
                    "CREATE TABLE ec_mothermentor_mobilization (base_entity_id TEXT,"
                            + " mobilization_date TEXT)");
            net.zetetic.database.sqlcipher.SQLiteDatabase cipher =
                    Mockito.mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
            Mockito.when(cipher.rawQuery(anyString(), Mockito.<String[]>any()))
                    .thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
            Mockito.doAnswer(
                            call -> {
                                db.execSQL(call.getArgument(0));
                                return null;
                            })
                    .when(cipher)
                    .execSQL(anyString());

            ChwRepositoryFlv.onUpgrade(null, cipher, 52, 53);
            try (Cursor cursor =
                    db.rawQuery(
                            "SELECT sh_tarehe, sh_mada, elim_m, elim_f, referred_male,"
                                    + " referred_female FROM ec_mothermentor_mobilization",
                            null)) {
                assertEquals(0, cursor.getCount());
            }
            db.execSQL(
                    "INSERT INTO ec_mothermentor_mobilization (base_entity_id, mobilization_date,"
                            + " sh_tarehe, elim_m) VALUES ('activity', '2026-10-18', '2026-10-18',"
                            + " '3')");
            ChwRepositoryFlv.onUpgrade(null, cipher, 52, 53);
            try (Cursor cursor = db.rawQuery("SELECT * FROM ec_mothermentor_mobilization", null)) {
                assertEquals(1, cursor.getCount());
                assertTrue(cursor.moveToFirst());
                assertEquals(
                        "2026-10-18",
                        cursor.getString(cursor.getColumnIndexOrThrow("mobilization_date")));
                assertEquals("3", cursor.getString(cursor.getColumnIndexOrThrow("elim_m")));
                for (String column : MotherMentorReportingRepository.MOBILIZATION_COLUMNS) {
                    assertTrue(column, cursor.getColumnIndex(column) >= 0);
                }
            }
        }
    }

    @Test
    public void syncedCommunityAnswersAreIncludedInFreshDatabaseMapping() throws Exception {
        JSONObject config =
                new JSONObject(
                        new String(
                                Files.readAllBytes(
                                        Paths.get("src/nacp/assets/ec_client_fields.json")),
                                StandardCharsets.UTF_8));
        JSONArray tables = config.getJSONArray("bindobjects");
        Set<String> mapped = new HashSet<>();
        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.getJSONObject(i);
            if (!org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_MOBILIZATION
                    .equals(table.getString("name"))) continue;
            JSONArray columns = table.getJSONArray("columns");
            for (int j = 0; j < columns.length(); j++) {
                JSONObject column = columns.getJSONObject(j);
                if (column.getString("column_name")
                        .equals(column.getJSONObject("json_mapping").optString("concept"))) {
                    mapped.add(column.getString("column_name"));
                }
            }
        }
        for (String column : MotherMentorReportingRepository.MOBILIZATION_COLUMNS)
            assertTrue(column, mapped.contains(column));
    }
}
