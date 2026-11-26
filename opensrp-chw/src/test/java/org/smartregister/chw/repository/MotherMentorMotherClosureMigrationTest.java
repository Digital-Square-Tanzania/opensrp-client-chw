package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorMotherClosureMigrationTest {
    private SQLiteDatabase data;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;

    @Before
    public void setUp() {
        data = SQLiteDatabase.create(null);
        data.execSQL("CREATE TABLE " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT
                + " (base_entity_id TEXT, pregnancy_status TEXT, child_dob TEXT, event_date TEXT,"
                + " enrollment_date TEXT, last_interacted_with INTEGER, is_closed INTEGER)");
        data.execSQL("INSERT INTO " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT
                + " VALUES ('mother','pregnant','','2026-10-01','2026-10-01',42,0)");
        cipher = mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(cipher.rawQuery(anyString(), any(String[].class)))
                .thenAnswer(call -> data.rawQuery(call.getArgument(0), call.getArgument(1)));
        doAnswer(call -> { data.execSQL(call.getArgument(0)); return null; })
                .when(cipher).execSQL(anyString());
        when(cipher.update(anyString(), any(ContentValues.class), anyString(), any(String[].class)))
                .thenAnswer(call -> data.update(call.getArgument(0), call.getArgument(1),
                        call.getArgument(2), call.getArgument(3)));
        when(cipher.insert(anyString(), nullable(String.class), any(ContentValues.class)))
                .thenAnswer(call -> data.insert(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
        when(cipher.delete(anyString(), anyString(), any(String[].class)))
                .thenAnswer(call -> data.delete(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
    }

    @After
    public void tearDown() { data.close(); }

    @Test
    public void version65AddsAndBackfillsClosureWithoutChangingIdentityOrClinicalValues() {
        ChwRepositoryFlv.onUpgrade(null, cipher, 64, 65);
        ChwRepositoryFlv.onUpgrade(null, cipher, 64, 65);
        Set<String> columns = new HashSet<>();
        try (Cursor rows = data.rawQuery("PRAGMA table_info("
                + Constants.TABLES.MOTHERMENTOR_ENROLLMENT + ")", null)) {
            while (rows.moveToNext()) columns.add(rows.getString(rows.getColumnIndexOrThrow("name")));
        }
        assertTrue(columns.containsAll(MotherMentorMotherClosureRepository.COLUMNS));
        try (Cursor row = data.rawQuery("SELECT * FROM " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT, null)) {
            assertTrue(row.moveToFirst());
            assertEquals("mother", row.getString(row.getColumnIndexOrThrow(DBConstants.KEY.BASE_ENTITY_ID)));
            assertEquals("pregnant", row.getString(row.getColumnIndexOrThrow(DBConstants.KEY.PREGNANCY_STATUS)));
            assertEquals("active", row.getString(row.getColumnIndexOrThrow(DBConstants.KEY.MAMA_STATUS)));
            assertEquals(0, row.getInt(row.getColumnIndexOrThrow(DBConstants.KEY.MAMA_COUNTER_NYINGINE)));
            assertEquals(42, row.getLong(row.getColumnIndexOrThrow(DBConstants.KEY.LAST_INTERACTED_WITH)));
            assertEquals(1, row.getCount());
        }
    }
}
