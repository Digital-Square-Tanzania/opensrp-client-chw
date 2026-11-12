package org.smartregister.chw.repository;

import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;

/** Adds the captured partner workflow and report sources in database version 58. */
final class MotherMentorPartnerWorkflowMigration {
    private static final String FOLLOW_UP = Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT;
    private static final String MOTHERS = Constants.TABLES.MOTHERMENTOR_ENROLLMENT;
    private static final String PARTNERS = Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER;
    static final String[] FOLLOW_UP_COLUMNS = {
        DBConstants.KEY.MOTHERMENTOR_VISIT_DATE,
        DBConstants.KEY.MOTHER_BASE_ENTITY_ID,
        DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA,
        DBConstants.KEY.PARTNER_LINKAGE_ID,
        DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID,
        DBConstants.KEY.PARTNER_MOTHER_PREGNANT,
        DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE,
        DBConstants.KEY.PARTNER_FACILITY_AGREEMENT,
        DBConstants.KEY.PARTNER_NOT_FOUND_REASON,
        DBConstants.KEY.PARTNER_NOT_FOUND_REASON_OTHER,
        DBConstants.KEY.PARTNER_FOLLOW_UP_END,
        DBConstants.KEY.PARTNER_HIV_TESTED,
        DBConstants.KEY.PARTNER_HIV_RESULT,
        DBConstants.KEY.PARTNER_HIV_NOT_TESTED_REASON,
        DBConstants.KEY.PARTNER_HIV_NOT_TESTED_REASON_OTHER,
        DBConstants.KEY.PARTNER_SYPHILIS_TESTED,
        DBConstants.KEY.PARTNER_SYPHILIS_RESULT,
        DBConstants.KEY.PARTNER_SYPHILIS_NOT_TESTED_REASON,
        DBConstants.KEY.PARTNER_SYPHILIS_NOT_TESTED_REASON_OTHER,
        DBConstants.KEY.PARTNER_HEPATITIS_B_TESTED,
        DBConstants.KEY.PARTNER_HEPATITIS_B_RESULT,
        DBConstants.KEY.PARTNER_HEPATITIS_B_NOT_TESTED_REASON,
        DBConstants.KEY.PARTNER_HEPATITIS_B_NOT_TESTED_REASON_OTHER,
        DBConstants.KEY.PARTNER_EDUCATION_PROVIDED,
        DBConstants.KEY.PARTNER_EDUCATION_TOPICS,
        DBConstants.KEY.CHILD_EID_FOLLOW_UP_SCHEMA,
        DBConstants.KEY.CHILD_EID_FINAL_HIV_TEST_DONE,
        DBConstants.KEY.CHILD_EID_FINAL_HIV_RESULT,
        DBConstants.KEY.CHILD_EID_FINAL_HIV_TEST_DATE
    };

    private MotherMentorPartnerWorkflowMigration() {}

    static void upgrade(SQLiteDatabase db) {
        for (String column : FOLLOW_UP_COLUMNS) addColumn(db, FOLLOW_UP, column);
        addColumn(db, MOTHERS, DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID);
        addColumn(db, PARTNERS, DBConstants.KEY.PARTNER_LINKAGE_ID);
        addColumn(db, PARTNERS, DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID);
        addColumn(db, PARTNERS, DBConstants.KEY.PARTNER_MOTHER_PREGNANT);
        if (tableExists(db, FOLLOW_UP)) {
            db.execSQL("CREATE INDEX IF NOT EXISTS mothermentor_partner_history ON " + FOLLOW_UP
                    + " (" + DBConstants.KEY.ENTITY_ID + ", "
                    + DBConstants.KEY.FOLLOW_UP_CATEGORY + ")");
        }
        if (tableExists(db, "visits")) {
            db.execSQL("CREATE INDEX IF NOT EXISTS mothermentor_partner_finalized_visit ON visits ("
                    + DBConstants.KEY.FORM_SUBMISSION_ID + ", " + DBConstants.KEY.BASE_ENTITY_ID
                    + ", " + DBConstants.KEY.PROCESSED + ")");
        }
        if (tableExists(db, PARTNERS)) {
            db.execSQL(
                    "UPDATE " + PARTNERS + " SET " + DBConstants.KEY.PARTNER_LINKAGE_ID
                            + " = 'legacy:' || " + DBConstants.KEY.BASE_ENTITY_ID
                            + " WHERE trim(coalesce(" + DBConstants.KEY.PARTNER_LINKAGE_ID
                            + ", '')) = ''");
        }
        if (!tableExists(db, "event")) return;
        backfillMaternalEpisodes(db);
        backfillCapturedFollowUpValues(db);
    }

    private static void addColumn(SQLiteDatabase db, String table, String column) {
        if (!tableExists(db, table)) return;
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) return;
            }
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " VARCHAR");
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[] {table})) {
            return cursor.moveToFirst();
        }
    }

    private static void backfillMaternalEpisodes(SQLiteDatabase db) {
        if (!tableExists(db, MOTHERS)) return;
        try (Cursor cursor = db.rawQuery(
                "SELECT json, baseEntityId, formSubmissionId FROM event "
                        + "WHERE eventType = ? ORDER BY eventDate DESC, updatedAt DESC",
                new String[] {"Mother Mentor Enrollment"})) {
            while (cursor.moveToNext()) {
                JSONObject event = retainedEvent(cursor.getString(0));
                String submissionId = cursor.getString(2);
                if (event == null || submissionId == null || submissionId.trim().isEmpty()) continue;
                // Only an actual retained enrollment identifies an episode. Do not invent dates.
                db.execSQL("UPDATE " + MOTHERS + " SET "
                                + DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID + " = ? WHERE "
                                + DBConstants.KEY.BASE_ENTITY_ID + " = ? AND trim(coalesce("
                                + DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID + ", '')) = ''",
                        new Object[] {submissionId, cursor.getString(1)});
            }
        }
    }

    private static void backfillCapturedFollowUpValues(SQLiteDatabase db) {
        if (!tableExists(db, FOLLOW_UP)) return;
        try (Cursor cursor = db.rawQuery(
                "SELECT e.json, e.formSubmissionId, e.baseEntityId FROM event e JOIN "
                        + FOLLOW_UP + " f ON f." + DBConstants.KEY.BASE_ENTITY_ID
                        + " = e.formSubmissionId AND f." + DBConstants.KEY.ENTITY_ID
                        + " = e.baseEntityId ORDER BY e.updatedAt DESC", null)) {
            while (cursor.moveToNext()) {
                JSONObject event = retainedEvent(cursor.getString(0));
                if (event == null) continue;
                for (String column : FOLLOW_UP_COLUMNS) {
                    String value = observation(event, column);
                    if (value == null) continue;
                    db.execSQL("UPDATE " + FOLLOW_UP + " SET " + column + " = ? WHERE "
                                    + DBConstants.KEY.BASE_ENTITY_ID + " = ? AND "
                                    + DBConstants.KEY.ENTITY_ID + " = ? AND trim(coalesce("
                                    + column + ", '')) = ''",
                            new Object[] {value, cursor.getString(1), cursor.getString(2)});
                }
            }
        }
    }

    private static JSONObject retainedEvent(String json) {
        try {
            JSONObject event = new JSONObject(json);
            return event.optBoolean("voided") ? null : event;
        } catch (JSONException e) {
            return null;
        }
    }

    private static String observation(JSONObject event, String field) {
        JSONArray observations = event.optJSONArray("obs");
        if (observations == null) return null;
        for (int i = 0; i < observations.length(); i++) {
            JSONObject observation = observations.optJSONObject(i);
            if (observation == null || !field.equals(observation.optString("fieldCode"))) continue;
            JSONArray values = observation.optJSONArray("values");
            if (values == null || values.length() == 0 || values.isNull(0)) return null;
            return values.length() == 1 ? values.optString(0) : values.toString();
        }
        return null;
    }
}
