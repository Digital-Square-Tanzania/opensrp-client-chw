package org.smartregister.chw.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MAMA_TAREHE_KUJIFUNGUA;

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
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherDeliveryRepository;
import org.smartregister.util.DatabaseMigrationUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorMotherDeliveryMigrationTest {
    @Test
    public void database63DispatchesTheNewLibraryMigrationExactlyOnce() {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        try (MockedStatic<MotherMentorMotherDeliveryRepository> migration =
                mockStatic(MotherMentorMotherDeliveryRepository.class)) {
            ChwRepositoryFlv.onUpgrade(null, db, 62, 63);
            ChwRepositoryFlv.onUpgrade(null, db, 63, 63);
            migration.verify(() -> MotherMentorMotherDeliveryRepository.upgrade(db), times(1));
            assertTrue(BuildConfig.DATABASE_VERSION >= 63);
        }
    }

    @Test
    public void explicitTableCreationAlsoInitializesDeliveryProjection() {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        List<String> steps = new ArrayList<>();
        // Other composed projection upgrades see no enrolled clients in this dispatch fixture.
        when(db.rawQuery(anyString(), org.mockito.ArgumentMatchers.<String[]>any()))
                .thenAnswer(ignored -> new MatrixCursor(new String[] {"name"}));
        try (MockedStatic<DatabaseMigrationUtils> tables = mockStatic(DatabaseMigrationUtils.class);
                MockedStatic<CoreChwApplication> application = mockStatic(CoreChwApplication.class);
                MockedStatic<MotherMentorMotherDeliveryRepository> migration =
                        mockStatic(MotherMentorMotherDeliveryRepository.class)) {
            tables.when(() -> DatabaseMigrationUtils.createAddedECTables(eq(db), anySet(), isNull()))
                    .thenAnswer(invocation -> {
                        steps.add("create tables");
                        return null;
                    });
            migration.when(() -> MotherMentorMotherDeliveryRepository.upgrade(db))
                    .thenAnswer(invocation -> {
                        steps.add("initialize delivery projection");
                        return null;
                    });

            ChwRepositoryFlv.onUpgrade(null, db, 49, 50);

            assertEquals(Arrays.asList("create tables", "initialize delivery projection"), steps);
        }
    }

    @Test
    public void effectiveNacpSchemaProjectsOnlyTheCanonicalDateAsAnObservation() throws Exception {
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
                    if (!MAMA_TAREHE_KUJIFUNGUA.equals(column.getString("column_name"))) continue;
                    count++;
                    assertEquals("Event", column.getString("type"));
                    assertEquals("obs.fieldCode", column.getJSONObject("json_mapping").getString("field"));
                    assertEquals(MAMA_TAREHE_KUJIFUNGUA, column.getJSONObject("json_mapping").getString("concept"));
                }
            }
            assertEquals(1, count);
        }
    }
}
