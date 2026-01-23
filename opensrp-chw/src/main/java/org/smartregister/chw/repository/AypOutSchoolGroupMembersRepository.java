package org.smartregister.chw.repository;

import android.content.ContentValues;
import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class AypOutSchoolGroupMembersRepository extends BaseRepository {
    public static final String TABLE = "ec_ayp_out_school_group_members";
    public static final String FOLLOWUP_TABLE = "ec_ayp_out_school_group_followup_visits";
    public static final String COL_GROUP_ID = "group_id";
    public static final String COL_MEMBER_BASE_ENTITY_ID = "member_base_entity_id";
    public static final String COL_BASE_ENTITY_ID = "base_entity_id";
    public static final String COL_PROVIDED_SBC_SERVICE = "provided_sbc_service";
    public static final String COL_SBC_SERVICE_PROVIDED = "choose_sbc_service_provided";
    public static final String COL_ECONOMIC_EMPOWERMENT_SERVICE = "choose_economic_empowerment_services";
    public static final String COL_NEXT_APPOINTMENT_DATE = "next_appointment_date";
    public static final String COL_LAST_INTERACTED_WITH = "last_interacted_with";
    public static final String COL_DATE_ADDED = "date_added";
    public static final String COL_PROVIDER_ID = "provider_id";

    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            COL_GROUP_ID + " TEXT, " +
            COL_MEMBER_BASE_ENTITY_ID + " TEXT, " +
            COL_DATE_ADDED + " INTEGER, " +
            COL_PROVIDER_ID + " TEXT" +
            ")";

    private void ensureTable() {
        try {
            getWritableDatabase().execSQL(CREATE_TABLE_SQL);
        } catch (Exception e) {
            Timber.e(e, "Error ensuring %s table", TABLE);
        }
    }

    public void addMembers(String groupId, List<String> memberBaseEntityIds, String providerId) {
        if (groupId == null || memberBaseEntityIds == null || memberBaseEntityIds.isEmpty()) return;
        ensureTable();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            for (String id : memberBaseEntityIds) {
                ContentValues cv = new ContentValues();
                cv.put(COL_GROUP_ID, groupId);
                cv.put(COL_MEMBER_BASE_ENTITY_ID, id);
                cv.put(COL_DATE_ADDED, now);
                if (providerId != null) cv.put(COL_PROVIDER_ID, providerId);
                db.insert(TABLE, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, "Error adding group members");
        } finally {
            db.endTransaction();
        }
    }

    public void addFollowUpForMembers(String groupId, List<String> memberBaseEntityIds, String providerId,String providedSbcService,List<String> chooseSbcServiceProvided,List<String> economicEmpowermentServices, String nextAppointmentDate) {
        if (groupId == null || memberBaseEntityIds == null || memberBaseEntityIds.isEmpty()) return;
        ensureTable();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            for (String id : memberBaseEntityIds) {
                ContentValues cv = new ContentValues();
                cv.put(COL_GROUP_ID, groupId);
                cv.put(COL_MEMBER_BASE_ENTITY_ID, id);
                cv.put(COL_PROVIDED_SBC_SERVICE, providedSbcService);
                cv.put(COL_SBC_SERVICE_PROVIDED, chooseSbcServiceProvided.toString());
                cv.put(COL_ECONOMIC_EMPOWERMENT_SERVICE, economicEmpowermentServices.toString());
                cv.put(COL_LAST_INTERACTED_WITH, getNowEpochMillis());
                cv.put(COL_NEXT_APPOINTMENT_DATE, nextAppointmentDate);
                cv.put(COL_DATE_ADDED, now);
                if (providerId != null) cv.put(COL_PROVIDER_ID, providerId);
                db.insert(FOLLOWUP_TABLE, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, "Error adding group members");
        } finally {
            db.endTransaction();
        }
    }

    public static long getNowEpochMillis() {
        return System.currentTimeMillis();
    }


    public List<String> getMemberIds(String groupId) {
        ensureTable();
        List<String> ids = new ArrayList<>();
        Cursor c = null;
        try {
            c = getReadableDatabase().query(TABLE, new String[]{COL_MEMBER_BASE_ENTITY_ID}, COL_GROUP_ID + " = ?", new String[]{groupId}, null, null, null);
            while (c != null && c.moveToNext()) {
                ids.add(c.getString(0));
            }
        } catch (Exception e) {
            Timber.e(e, "Error querying group member ids");
        } finally {
            if (c != null) c.close();
        }
        return ids;
    }
}
