package org.smartregister.chw.repository;

import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.reporting.dao.ReportIndicatorDaoImpl;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** Repairs the KVP visit-date projection and previously skipped report tallies in version 55. */
final class KvpReportingMigration {
    static final String FOLLOW_UP_TABLE = "ec_kvp_prep_followup";
    static final String VISIT_DATE = "kvp_visit_date";
    static final String FOLLOW_UP_EVENT = "Kvp PrEP Follow-up Visit";
    static final String REPORT_CONFIG = "config/kvp-monthly-report.yml";
    private static final String KVP_INDICATORS = "indicator_code LIKE 'kvp-%'";

    private KvpReportingMigration() {}

    static void upgrade(SQLiteDatabase db, ReportingLibrary reportingLibrary) {
        if (!hasVisitDateColumn(db)) {
            db.execSQL("ALTER TABLE " + FOLLOW_UP_TABLE + " ADD COLUMN " + VISIT_DATE + " VARCHAR");
        }
        backfillVisitDates(db);

        // Config loading inserts definitions, so remove this report's old definitions first.
        db.execSQL("DELETE FROM indicator_queries WHERE " + KVP_INDICATORS);
        db.execSQL("DELETE FROM indicators WHERE " + KVP_INDICATORS);
        reportingLibrary.readConfigFile(REPORT_CONFIG, db);
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM indicator_queries WHERE " + KVP_INDICATORS + " LIMIT 1", null)) {
            if (!cursor.moveToFirst()) {
                throw new IllegalStateException("KVP reporting definitions were not reloaded");
            }
        }

        // A failed query was treated as zero by the reporting worker, which still advanced its
        // shared checkpoint. Remove stale KVP results (including previous nonzero values) and
        // let that worker replay retained event dates using its usual idempotent tally writes.
        if (hasEventTable(db)) {
            // Retained tallies can outlive their events. Preserve days the worker cannot replay.
            db.execSQL("DELETE FROM indicator_daily_tally WHERE " + KVP_INDICATORS
                    + " AND date(day) IN (SELECT date(eventDate) FROM event "
                    + "WHERE eventType NOT LIKE '%tallies Report%' "
                    + "AND date(eventDate) IS NOT NULL "
                    + "AND date(eventDate) = date(eventDate, '+0 days') "
                    + "AND length(updatedAt) >= 19 AND substr(updatedAt, 11, 1) = ' ' "
                    + "AND datetime(updatedAt) IS NOT NULL)");
        }
        reportingLibrary.getContext().allSharedPreferences().savePreference(
                "APP_VERSION_CODE", String.valueOf(BuildConfig.VERSION_CODE));
        reportingLibrary.getContext().allSharedPreferences().savePreference(
                ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE, "");
    }

    private static boolean hasVisitDateColumn(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + FOLLOW_UP_TABLE + ")", null)) {
            while (cursor.moveToNext()) {
                if (VISIT_DATE.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasEventTable(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'event'", null)) {
            return cursor.moveToFirst();
        }
    }

    private static void backfillVisitDates(SQLiteDatabase db) {
        if (!hasEventTable(db)) return;
        String query = "SELECT e.json, e.formSubmissionId, e.baseEntityId FROM event e "
                + "INNER JOIN " + FOLLOW_UP_TABLE + " f ON f.base_entity_id = e.formSubmissionId "
                + "AND f.entity_id = e.baseEntityId WHERE e.eventType = ? "
                + "AND (f." + VISIT_DATE + " IS NULL OR TRIM(f." + VISIT_DATE + ") = '') "
                + "ORDER BY e.updatedAt DESC";
        try (Cursor cursor = db.rawQuery(query, new String[] {FOLLOW_UP_EVENT})) {
            while (cursor.moveToNext()) {
                String formSubmissionId = cursor.getString(1);
                String entityId = cursor.getString(2);
                String date = retainedVisitDate(cursor.getString(0), formSubmissionId, entityId);
                if (date != null) {
                    db.execSQL("UPDATE " + FOLLOW_UP_TABLE + " SET " + VISIT_DATE + " = ? "
                                    + "WHERE base_entity_id = ? AND entity_id = ? AND (" + VISIT_DATE
                                    + " IS NULL OR TRIM(" + VISIT_DATE + ") = '')",
                            new Object[] {date, formSubmissionId, entityId});
                }
            }
        }
    }

    private static String retainedVisitDate(String json, String submissionId, String entityId) {
        if (json == null || submissionId == null || entityId == null) return null;
        try {
            JSONObject event = new JSONObject(json);
            if (!matchesEvent(event, submissionId, entityId)) return null;
            return findVisitDate(event.optJSONArray("obs"));
        } catch (JSONException ignored) {
            // A malformed retained event must not prevent the schema repair. Do not log its data.
        }
        return null;
    }

    private static boolean matchesEvent(JSONObject event, String submissionId, String entityId) {
        return FOLLOW_UP_EVENT.equals(event.optString("eventType"))
                && submissionId.equals(event.optString("formSubmissionId"))
                && entityId.equals(event.optString("baseEntityId"));
    }

    private static String findVisitDate(JSONArray observations) {
        if (observations == null) return null;
        for (int i = 0; i < observations.length(); i++) {
            String date = observationVisitDate(observations.optJSONObject(i));
            if (date != null) return date;
        }
        return null;
    }

    private static String observationVisitDate(JSONObject observation) {
        if (observation == null || !VISIT_DATE.equals(observation.optString("fieldCode"))) {
            return null;
        }
        JSONArray values = observation.optJSONArray("values");
        if (values == null || values.length() == 0 || !(values.opt(0) instanceof String)) {
            return null;
        }
        String date = (String) values.opt(0);
        return isValidVisitDate(date) ? date : null;
    }

    private static boolean isValidVisitDate(String value) {
        if (!value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")) return false;
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        return format.parse(value, position) != null && position.getIndex() == value.length();
    }
}
