package org.smartregister.chw.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import android.database.MatrixCursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.util.DatabaseMigrationUtils;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildEidMigrationTest {
    @Test
    public void database66CallsTheLibraryUpgradeOnceAndSkipsAlreadyUpgradedDatabases() {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        try (MockedStatic<org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository> migration =
                mockStatic(org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository.class)) {
            ChwRepositoryFlv.onUpgrade(null, db, 65, 66);
            ChwRepositoryFlv.onUpgrade(null, db, 66, 66);
            migration.verify(() -> org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository.upgrade(db), times(1));
            assertEquals(66, BuildConfig.DATABASE_VERSION);
        }
    }

    @Test
    public void freshTableCreationCallsTheSameExplicitLibraryUpgrade() throws Exception {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        when(db.rawQuery(anyString(), org.mockito.ArgumentMatchers.<String[]>any()))
                .thenAnswer(ignored -> new MatrixCursor(new String[] {"name"}));
        try (MockedStatic<DatabaseMigrationUtils> tables = mockStatic(DatabaseMigrationUtils.class);
                MockedStatic<CoreChwApplication> app = mockStatic(CoreChwApplication.class);
                MockedStatic<org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository> migration =
                        mockStatic(org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository.class)) {
            Method create = ChwRepositoryFlv.class.getDeclaredMethod("createMotherMentorTables", SQLiteDatabase.class);
            create.setAccessible(true);
            create.invoke(null, db);
            migration.verify(() -> org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository.upgrade(db), times(1));
        }
    }

    @Test
    public void effectiveNacpAssetBindsEveryLibraryStateColumnExactlyOnce() throws Exception {
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets().open("ec_client_fields.json");
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            JSONArray tables = new JSONObject(scanner.next()).getJSONArray("bindobjects");
            Set<String> found = new HashSet<>();
            for (int i = 0; i < tables.length(); i++) {
                JSONObject table = tables.getJSONObject(i);
                if (!"ec_mothermentor_enroll_child_eid".equals(table.getString("name"))) continue;
                JSONArray columns = table.getJSONArray("columns");
                for (int j = 0; j < columns.length(); j++) {
                    JSONObject column = columns.getJSONObject(j);
                    String key = column.getString("column_name");
                    if (!org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository.COLUMNS.contains(key)) continue;
                    assertTrue("Duplicate " + key, found.add(key));
                    assertEquals("obs.fieldCode", column.getJSONObject("json_mapping").getString("field"));
                    assertEquals(key, column.getJSONObject("json_mapping").getString("concept"));
                }
            }
            assertEquals(new HashSet<>(org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository.COLUMNS), found);
        }
    }
}
