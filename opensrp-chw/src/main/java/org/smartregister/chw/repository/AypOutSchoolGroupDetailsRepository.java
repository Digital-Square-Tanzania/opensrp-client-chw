package org.smartregister.chw.repository;

import android.content.ContentValues;
import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.chw.domain.AypInSchoolGroupDetails;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Repository for persisting AYP in-school group details
 * captured by the form ayp_in_school_group_creation.json.
 */
public class AypOutSchoolGroupDetailsRepository extends BaseRepository {

    public static final String TABLE_NAME = "ec_ayp_out_school_group_details";

    public static final String COL_BASE_ENTITY_ID = "base_entity_id";
    public static final String COL_PROVIDER_ID = "provider_id";
    public static final String COL_GROUP_NAME = "group_name";
    public static final String COL_GROUP_TYPE = "group_type";
    public static final String COL_AGE_BAND = "age_band";
    public static final String COL_ENCOUNTER_DATE = "encounter_date"; // ISO string (yyyy-MM-dd or ISO8601)
    public static final String COL_LAST_INTERACTED_WITH = "last_interacted_with"; // epoch millis
    public static final String COL_IS_CLOSED = "is_closed"; // 0=open, 1=closed

    public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
            COL_BASE_ENTITY_ID + " TEXT," +
            COL_PROVIDER_ID + " TEXT," +
            COL_GROUP_NAME + " TEXT," +
            COL_GROUP_TYPE + " TEXT," +
            COL_AGE_BAND + " TEXT," +
            COL_ENCOUNTER_DATE + " TEXT," +
            COL_LAST_INTERACTED_WITH + " INTEGER," +
            COL_IS_CLOSED + " INTEGER DEFAULT 0" +
            ")";

    public AypOutSchoolGroupDetailsRepository() {
        ensureTableExists();
    }

    private void ensureTableExists() {
        try {
            getWritableDatabase().execSQL(CREATE_TABLE_SQL);
            // Future-proofing: ensure expected columns exist (handles upgrades gracefully)
            String[][] newCols = new String[][]{
                    {COL_BASE_ENTITY_ID, "TEXT"},
                    {COL_PROVIDER_ID, "TEXT"},
                    {COL_GROUP_NAME, "TEXT"},
                    {COL_GROUP_TYPE, "TEXT"},
                    {COL_AGE_BAND, "TEXT"},
                    {COL_ENCOUNTER_DATE, "TEXT"},
                    {COL_LAST_INTERACTED_WITH, "INTEGER"},
                    {COL_IS_CLOSED, "INTEGER DEFAULT 0"}
            };
            for (String[] c : newCols) {
                ensureColumnExists(c[0], c[1]);
            }
        } catch (Exception e) {
            Timber.e(e, "Error ensuring %s table exists", TABLE_NAME);
        }
    }

    private void ensureColumnExists(String column, String type) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getWritableDatabase();
            boolean found = false;
            cursor = db.rawQuery("PRAGMA table_info(" + TABLE_NAME + ")", null);
            int nameIdx = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (column.equalsIgnoreCase(cursor.getString(nameIdx))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + column + " " + type);
            }
        } catch (Exception e) {
            Timber.e(e, "Error ensuring column %s exists on %s", column, TABLE_NAME);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public long save(AypInSchoolGroupDetails record) {
        try {
            ensureTableExists();
            ContentValues values = toContentValues(record);
            return getWritableDatabase().insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Timber.e(e, "Error saving AypOutSchoolGroupCreation");
            return -1;
        }
    }

    public void saveAll(List<AypInSchoolGroupDetails> records) {
        if (records == null || records.isEmpty()) return;
        ensureTableExists();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (AypInSchoolGroupDetails r : records) {
                db.insertWithOnConflict(TABLE_NAME, null, toContentValues(r), SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, "Error bulk saving AypOutSchoolGroupCreation");
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toContentValues(AypInSchoolGroupDetails r) {
        ContentValues cv = new ContentValues();
        if (r.getBaseEntityId() != null) cv.put(COL_BASE_ENTITY_ID, r.getBaseEntityId());
        if (r.getProviderId() != null) cv.put(COL_PROVIDER_ID, r.getProviderId());
        if (r.getGroupName() != null) cv.put(COL_GROUP_NAME, r.getGroupName());
        if (r.getGroupType() != null) cv.put(COL_GROUP_TYPE, r.getGroupType());
        if (r.getAgeBand() != null) cv.put(COL_AGE_BAND, r.getAgeBand());
        if (r.getEncounterDate() != null) cv.put(COL_ENCOUNTER_DATE, r.getEncounterDate());
        if (r.getLastInteractedWith() != null) cv.put(COL_LAST_INTERACTED_WITH, r.getLastInteractedWith());

        // Handle is_closed explicitly; default to 0 (open)
        int isClosedVal = 0;
        try {
            if (r.getExtra() != null && r.getExtra().containsKey(COL_IS_CLOSED)) {
                String v = r.getExtra().get(COL_IS_CLOSED);
                if (v != null) {
                    isClosedVal = Integer.parseInt(v.trim());
                    if (isClosedVal != 0 && isClosedVal != 1) isClosedVal = 0; // sanitize
                }
            }
        } catch (Exception ignore) {
            isClosedVal = 0;
        }
        cv.put(COL_IS_CLOSED, isClosedVal);

        // Persist any extra key-value pairs provided
        for (java.util.Map.Entry<String, String> e : r.getExtra().entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            if (COL_IS_CLOSED.equalsIgnoreCase(e.getKey())) continue; // already handled as INTEGER
            cv.put(e.getKey(), e.getValue());
            ensureColumnExists(e.getKey(), "TEXT");
        }
        return cv;
    }

    public AypInSchoolGroupDetails getByBaseEntityId(String baseEntityId) {
        ensureTableExists();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, null, COL_BASE_ENTITY_ID + " = ?", new String[]{baseEntityId}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        } catch (Exception e) {
            Timber.e(e, "Error fetching AYP group by base_entity_id");
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public List<AypInSchoolGroupDetails> getAll() {
        ensureTableExists();
        List<AypInSchoolGroupDetails> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_NAME, null);
            while (cursor != null && cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } catch (Exception e) {
            Timber.e(e, "Error fetching all AYP groups");
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    private AypInSchoolGroupDetails fromCursor(Cursor c) {
        AypInSchoolGroupDetails r = new AypInSchoolGroupDetails();
        int idx;
        idx = c.getColumnIndex(COL_BASE_ENTITY_ID); if (idx >= 0) r.setBaseEntityId(c.getString(idx));
        idx = c.getColumnIndex(COL_PROVIDER_ID); if (idx >= 0) r.setProviderId(c.getString(idx));
        idx = c.getColumnIndex(COL_GROUP_NAME); if (idx >= 0) r.setGroupName(c.getString(idx));
        idx = c.getColumnIndex(COL_GROUP_TYPE); if (idx >= 0) r.setGroupType(c.getString(idx));
        idx = c.getColumnIndex(COL_AGE_BAND); if (idx >= 0) r.setAgeBand(c.getString(idx));
        idx = c.getColumnIndex(COL_ENCOUNTER_DATE); if (idx >= 0) r.setEncounterDate(c.getString(idx));
        idx = c.getColumnIndex(COL_LAST_INTERACTED_WITH); if (idx >= 0 && !c.isNull(idx)) r.setLastInteractedWith(c.getLong(idx));
        // is_closed is intentionally not mapped to the DTO as there is no field
        return r;
    }
}
