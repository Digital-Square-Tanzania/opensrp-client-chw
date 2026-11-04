package org.smartregister.chw.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MAMA_CHILD_STATUS;

import android.app.Application;
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
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherChildRepository;
import org.smartregister.util.DatabaseMigrationUtils;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Scanner;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorMotherChildMigrationTest {
    @Test
    public void database64DispatchesTheNewLibraryMigrationExactlyOnce() {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        try (MockedStatic<MotherMentorMotherChildRepository> migration =
                mockStatic(MotherMentorMotherChildRepository.class)) {
            ChwRepositoryFlv.onUpgrade(null, db, 63, 64);
            ChwRepositoryFlv.onUpgrade(null, db, 64, 64);
            migration.verify(() -> MotherMentorMotherChildRepository.upgrade(db), times(1));
            assertTrue(BuildConfig.DATABASE_VERSION >= 64);
        }
    }

    @Test
    public void explicitTableCreationAlsoInitializesChildProjection() throws Exception {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        try (MockedStatic<DatabaseMigrationUtils> tables = mockStatic(DatabaseMigrationUtils.class);
                MockedStatic<CoreChwApplication> application = mockStatic(CoreChwApplication.class);
                MockedStatic<org.smartregister.chw.mothermentor.repository.MotherMentorMotherDeliveryRepository> delivery =
                        mockStatic(org.smartregister.chw.mothermentor.repository.MotherMentorMotherDeliveryRepository.class);
                MockedStatic<MotherMentorMotherChildRepository> migration =
                        mockStatic(MotherMentorMotherChildRepository.class)) {
            Method create = ChwRepositoryFlv.class.getDeclaredMethod("createMotherMentorTables", SQLiteDatabase.class);
            create.setAccessible(true); create.invoke(null, db);
            tables.verify(() -> DatabaseMigrationUtils.createAddedECTables(eq(db), anySet(), isNull()));
            migration.verify(() -> MotherMentorMotherChildRepository.upgrade(db), times(1));
        }
    }

    @Test
    public void effectiveNacpSchemaProjectsTheChildStatusAsAnObservation() throws Exception {
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets().open("ec_client_fields.json");
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            JSONArray tables = new JSONObject(scanner.next()).getJSONArray("bindobjects");
            int count = 0;
            for (int i = 0; i < tables.length(); i++) {
                JSONObject table = tables.getJSONObject(i);
                if (!"ec_mothermentor_enrollment".equals(table.getString("name"))) continue;
                JSONArray columns = table.getJSONArray("columns");
                for (int j = 0; j < columns.length(); j++) {
                    JSONObject column = columns.getJSONObject(j);
                    if (!MAMA_CHILD_STATUS.equals(column.getString("column_name"))) continue;
                    count++;
                    assertEquals("Event", column.getString("type"));
                    assertEquals("obs.fieldCode", column.getJSONObject("json_mapping").getString("field"));
                    assertEquals(MAMA_CHILD_STATUS, column.getJSONObject("json_mapping").getString("concept"));
                }
            }
            assertEquals(1, count);
        }
    }
}
