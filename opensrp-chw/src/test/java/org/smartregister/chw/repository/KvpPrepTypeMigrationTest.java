package org.smartregister.chw.repository;

import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Test;
import org.smartregister.chw.BuildConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KvpPrepTypeMigrationTest {

    @Test
    public void upgradeAddsBothPrepTypeColumnsOnce() {
        Set<String> columns = new HashSet<>(Arrays.asList("base_entity_id", "prep_facility_a", "prep_facility_b"));
        SQLiteDatabase db = databaseWithColumns(columns);

        ChwRepositoryFlv.onUpgrade(null, db, 50, 51);
        ChwRepositoryFlv.onUpgrade(null, db, 50, 51);

        assertTrue(BuildConfig.DATABASE_VERSION >= 51);
        assertTrue(columns.containsAll(Arrays.asList("base_entity_id", "prep_facility_a", "prep_facility_b",
                "prep_type_a", "prep_type_b")));
        verify(db).execSQL("ALTER TABLE ec_kvp_prep_followup ADD COLUMN prep_type_a VARCHAR;");
        verify(db).execSQL("ALTER TABLE ec_kvp_prep_followup ADD COLUMN prep_type_b VARCHAR;");
        verify(db, times(2)).execSQL(anyString());
    }

    @Test
    public void partialUpgradeKeepsTheExistingPrepTypeColumn() {
        Set<String> columns = new HashSet<>(Arrays.asList("base_entity_id", "prep_type_a"));
        SQLiteDatabase db = databaseWithColumns(columns);

        ChwRepositoryFlv.onUpgrade(null, db, 50, 51);

        verify(db, never()).execSQL("ALTER TABLE ec_kvp_prep_followup ADD COLUMN prep_type_a VARCHAR;");
        verify(db).execSQL("ALTER TABLE ec_kvp_prep_followup ADD COLUMN prep_type_b VARCHAR;");
        verify(db, times(1)).execSQL(anyString());
    }

    private SQLiteDatabase databaseWithColumns(Set<String> columns) {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        when(db.rawQuery(eq("PRAGMA table_info(ec_kvp_prep_followup)"), isNull()))
                .thenAnswer(invocation -> {
                    List<String> snapshot = new ArrayList<>(columns);
                    AtomicInteger row = new AtomicInteger(-1);
                    Cursor cursor = mock(Cursor.class);
                    when(cursor.moveToNext()).thenAnswer(next -> row.incrementAndGet() < snapshot.size());
                    when(cursor.getColumnIndex("name")).thenReturn(0);
                    when(cursor.getString(0)).thenAnswer(value -> snapshot.get(row.get()));
                    return cursor;
                });
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            columns.add(sql.split(" ")[5]);
            return null;
        }).when(db).execSQL(anyString());
        return db;
    }
}
