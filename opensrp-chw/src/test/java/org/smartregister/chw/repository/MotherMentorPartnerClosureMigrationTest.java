package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

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
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorPartnerClosureMigrationTest {
    @Test
    public void existingVersion60DatabaseRunsOnlyTheNewLibraryMigration() {
        SQLiteDatabase database = mock(SQLiteDatabase.class);
        try (MockedStatic<MotherMentorPartnerClosureRepository> migration =
                     mockStatic(MotherMentorPartnerClosureRepository.class)) {
            ChwRepositoryFlv.onUpgrade(null, database, 60, 61);
            migration.verify(() -> MotherMentorPartnerClosureRepository.upgrade(database));
            migration.verifyNoMoreInteractions();
        }
        assertTrue(BuildConfig.DATABASE_VERSION >= 61);
    }

    @Test
    public void alreadyUpgradedDatabaseDoesNotRepeatTheMigration() {
        try (MockedStatic<MotherMentorPartnerClosureRepository> migration =
                     mockStatic(MotherMentorPartnerClosureRepository.class)) {
            ChwRepositoryFlv.onUpgrade(null, mock(SQLiteDatabase.class), 61, 61);
            migration.verifyNoInteractions();
        }
    }

    @Test
    public void enrollmentAndFollowUpEventsBothMapAllClosureFields() throws Exception {
        String[] fields = {
                DBConstants.KEY.MWENZI_STATUS, DBConstants.KEY.MWENZI_TAREHE_YA_KUFUNGWA,
                DBConstants.KEY.MWENZI_SABABU_YA_KUFUNGWA, DBConstants.KEY.MWENZI_COUNTER_AMEKATAA,
                DBConstants.KEY.MWENZI_COUNTER_HAJAHUDHURIA, DBConstants.KEY.MWENZI_COUNTER_AMESAFIRI,
                DBConstants.KEY.MWENZI_COUNTER_NYINGINE, DBConstants.KEY.MWENZI_COUNTER_HAJAPATIKANA
        };
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets().open("ec_client_fields.json")) {
            JSONObject mapping = new JSONObject(new Scanner(input, "UTF-8").useDelimiter("\\A").next());
            JSONArray tables = mapping.getJSONArray("bindobjects");
            for (String table : new String[] {Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER,
                    Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT}) {
                Map<String, JSONObject> columns = new HashMap<>();
                for (int i = 0; i < tables.length(); i++) {
                    JSONObject definition = tables.getJSONObject(i);
                    if (!table.equals(definition.getString("name"))) continue;
                    JSONArray entries = definition.getJSONArray("columns");
                    for (int j = 0; j < entries.length(); j++) {
                        JSONObject column = entries.getJSONObject(j);
                        columns.put(column.getString("column_name"), column);
                    }
                }
                for (String field : fields) {
                    assertTrue(table + "." + field, columns.containsKey(field));
                    JSONObject column = columns.get(field);
                    assertEquals("Event", column.getString("type"));
                    assertEquals("obs.fieldCode", column.getJSONObject("json_mapping").getString("field"));
                    assertEquals(field, column.getJSONObject("json_mapping").getString("concept"));
                }
            }
        }
    }
}
