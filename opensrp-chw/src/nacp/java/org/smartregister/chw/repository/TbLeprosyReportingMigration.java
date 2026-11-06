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
import java.util.Map;
import java.util.TreeMap;

/** Repairs captured TB/Leprosy reporting fields and retained report days in version 56. */
final class TbLeprosyReportingMigration {
    static final String SCREENING_TABLE = "ec_tbleprosy_screening";
    static final String RESULTS_TABLE = "ec_tbleprosy_observation_results";
    static final String SCREENING_EVENT = "TBLeprosy Screening";
    static final String RESULTS_EVENT = "TBLEPROSY Observation Results";
    static final String SCREENING_LOCATION = "screening_location";
    static final String HIV_TESTED = "hiv_tested";
    static final String TPT_DATE = "tpt_initiation_date";
    static final String REPORT_CONFIG = "config/tbleprosy-monthly-report.yml";
    private static final String TB_INDICATORS = "indicator_code GLOB 'tbleprosy_*'";
    private static final String TPT_INDICATORS = "indicator_code GLOB 'tbleprosy_house_col9_*'";

    private TbLeprosyReportingMigration() {}

    static void upgrade(SQLiteDatabase db, ReportingLibrary reporting) {
        addColumn(db, SCREENING_TABLE, SCREENING_LOCATION);
        addColumn(db, SCREENING_TABLE, HIV_TESTED);
        addColumn(db, RESULTS_TABLE, HIV_TESTED);
        addColumn(db, RESULTS_TABLE, TPT_DATE);
        if (hasEventTable(db)) {
            backfillScreening(db);
            backfillResults(db);
        }
        refreshReport(db, reporting);
    }

    private static void addColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) return;
            }
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " VARCHAR");
    }

    private static boolean hasEventTable(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'event'", null)) {
            return cursor.moveToFirst();
        }
    }

    private static void backfillScreening(SQLiteDatabase db) {
        String query = "SELECT e.json, e.eventType, e.formSubmissionId, e.baseEntityId FROM event e "
                + "INNER JOIN " + SCREENING_TABLE + " s ON s.base_entity_id = e.baseEntityId "
                + "WHERE e.eventType IN (?, ?) ORDER BY e.eventDate DESC, e.updatedAt DESC";
        try (Cursor cursor = db.rawQuery(query, new String[] {SCREENING_EVENT, RESULTS_EVENT})) {
            while (cursor.moveToNext()) {
                JSONObject event = retainedEvent(cursor);
                if (event == null) continue;
                String entityId = cursor.getString(3);
                fillScreeningValue(db, event, SCREENING_LOCATION, entityId);
                fillScreeningValue(db, event, HIV_TESTED, entityId);
            }
        }
    }

    private static void fillScreeningValue(SQLiteDatabase db, JSONObject event, String field,
            String entityId) {
        String value = capturedValue(event, field);
        if (value != null) {
            db.execSQL("UPDATE " + SCREENING_TABLE + " SET " + field + " = ? "
                            + "WHERE base_entity_id = ? AND (" + field + " IS NULL OR TRIM(" + field + ") = '')",
                    new Object[] {value, entityId});
        }
    }

    private static void backfillResults(SQLiteDatabase db) {
        String query = "SELECT e.json, e.eventType, e.formSubmissionId, e.baseEntityId FROM event e "
                + "INNER JOIN " + RESULTS_TABLE + " r ON r.base_entity_id = e.formSubmissionId "
                + "AND r.entity_id = e.baseEntityId WHERE e.eventType = ? ORDER BY e.updatedAt DESC";
        try (Cursor cursor = db.rawQuery(query, new String[] {RESULTS_EVENT})) {
            while (cursor.moveToNext()) {
                JSONObject event = retainedEvent(cursor);
                if (event == null) continue;
                fillResultsValue(db, event, HIV_TESTED, cursor.getString(2), cursor.getString(3));
                fillResultsValue(db, event, TPT_DATE, cursor.getString(2), cursor.getString(3));
            }
        }
    }

    private static void fillResultsValue(SQLiteDatabase db, JSONObject event, String field,
            String submissionId, String entityId) {
        String value = capturedValue(event, field);
        if (value != null) {
            db.execSQL("UPDATE " + RESULTS_TABLE + " SET " + field + " = ? "
                            + "WHERE base_entity_id = ? AND entity_id = ? AND (" + field
                            + " IS NULL OR TRIM(" + field + ") = '')",
                    new Object[] {value, submissionId, entityId});
        }
    }

    private static JSONObject retainedEvent(Cursor cursor) {
        String json = cursor.getString(0);
        String type = cursor.getString(1);
        String submission = cursor.getString(2);
        String entity = cursor.getString(3);
        if (json == null || isBlank(submission) || isBlank(entity)) return null;
        try {
            JSONObject event = new JSONObject(json);
            return matchesEvent(event, type, submission, entity) ? event : null;
        } catch (JSONException ignored) {
            // Skip corrupt retained payloads without logging client data or inventing answers.
            return null;
        }
    }

    private static boolean matchesEvent(JSONObject event, String type, String submission,
            String entity) {
        return type.equals(event.optString("eventType"))
                && submission.equals(event.optString("formSubmissionId"))
                && entity.equals(event.optString("baseEntityId"))
                && !event.optBoolean("voided", false);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String capturedValue(JSONObject event, String field) {
        JSONArray observations = event.optJSONArray("obs");
        if (observations == null) return null;
        for (int i = 0; i < observations.length(); i++) {
            String value = observationValue(observations.optJSONObject(i), field);
            if (value != null) return value;
        }
        return null;
    }

    private static String observationValue(JSONObject observation, String field) {
        if (observation == null || !field.equals(observation.optString("fieldCode"))) return null;
        JSONArray values = observation.optJSONArray("values");
        if (values == null || !(values.opt(0) instanceof String)) return null;
        String value = (String) values.opt(0);
        if (value.trim().isEmpty()) return null;
        if (TPT_DATE.equals(field) && !isValidDate(value)) return null;
        return value;
    }

    private static boolean isValidDate(String value) {
        if (!value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")) return false;
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        return format.parse(value, position) != null && position.getIndex() == value.length();
    }

    private static void refreshReport(SQLiteDatabase db, ReportingLibrary reporting) {
        // readConfigFile inserts definitions; replacing only this report avoids duplicate keys.
        db.execSQL("DELETE FROM indicator_queries WHERE " + TB_INDICATORS);
        db.execSQL("DELETE FROM indicators WHERE " + TB_INDICATORS);
        reporting.readConfigFile(REPORT_CONFIG, db);
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM indicator_queries WHERE " + TB_INDICATORS + " LIMIT 1", null)) {
            if (!cursor.moveToFirst()) {
                throw new IllegalStateException("TB/Leprosy reporting definitions were not reloaded");
            }
        }
        if (hasEventTable(db)) {
            // Keep historical tallies with no retained, valid source day for the worker to replay.
            db.execSQL("DELETE FROM indicator_daily_tally WHERE " + TB_INDICATORS
                    + " AND date(day) IN (SELECT date(eventDate) FROM event "
                    + "WHERE eventType NOT LIKE '%tallies Report%' "
                    + "AND date(eventDate) IS NOT NULL "
                    + "AND date(eventDate) = date(eventDate, '+0 days') "
                    + "AND length(updatedAt) >= 19 AND substr(updatedAt, 11, 1) = ' ' "
                    + "AND datetime(updatedAt) IS NOT NULL)");
        }
        recoverTptTallies(db);
        reporting.getContext().allSharedPreferences().savePreference(
                "APP_VERSION_CODE", String.valueOf(BuildConfig.VERSION_CODE));
        // Earlier SQL failures still advanced this shared worker checkpoint. Replay through the
        // existing worker; it upserts unaffected modules without clearing their historical tallies.
        reporting.getContext().allSharedPreferences().savePreference(
                ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE, "");
    }

    private static void recoverTptTallies(SQLiteDatabase db) {
        Map<String, String> capturedMonths = capturedTptMonths(db);
        if (capturedMonths.isEmpty()) return;
        try (Cursor definitions = db.rawQuery(
                "SELECT indicator_code, indicator_query, grouping FROM indicator_queries WHERE "
                        + TPT_INDICATORS, null)) {
            while (definitions.moveToNext()) {
                for (Map.Entry<String, String> month : capturedMonths.entrySet()) {
                    recoverTptMonth(db, definitions, month.getKey(), month.getValue());
                }
            }
        }
    }

    private static Map<String, String> capturedTptMonths(SQLiteDatabase db) {
        Map<String, String> months = new TreeMap<>();
        try (Cursor dates = db.rawQuery("SELECT DISTINCT " + TPT_DATE + " FROM " + RESULTS_TABLE
                + " WHERE " + TPT_DATE + " IS NOT NULL", null)) {
            while (dates.moveToNext()) {
                String date = dates.getString(0);
                if (!isValidDate(date)) continue;
                String isoDay = date.substring(6, 10) + "-" + date.substring(3, 5) + "-" + date.substring(0, 2);
                String month = isoDay.substring(0, 7);
                String previous = months.get(month);
                if (previous == null || isoDay.compareTo(previous) > 0) months.put(month, isoDay);
            }
        }
        return months;
    }

    private static void recoverTptMonth(SQLiteDatabase db, Cursor definition, String month,
            String capturedDay) {
        String code = definition.getString(0);
        String grouping = definition.getString(2);
        String query = definition.getString(1).replace("%s", capturedDay);
        long count;
        try (Cursor result = db.rawQuery(query, null)) {
            if (!result.moveToFirst()) return;
            count = result.getLong(0);
        }
        // The report reads the latest tally in a month. Correct every existing row so a stale
        // later day cannot shadow the repaired count; use a captured day only when inserting.
        if (hasTptTally(db, code, month)) {
            db.execSQL("UPDATE indicator_daily_tally SET indicator_value = ?, indicator_grouping = ?, "
                            + "indicator_is_value_set = 0, indicator_value_set = NULL "
                            + "WHERE indicator_code = ? AND strftime('%Y-%m', day) = ?",
                    new Object[] {count, grouping, code, month});
        } else {
            db.execSQL("INSERT INTO indicator_daily_tally (indicator_code, indicator_grouping, "
                            + "indicator_value, day, indicator_is_value_set) VALUES (?, ?, ?, ?, 0)",
                    new Object[] {code, grouping, count, capturedDay});
        }
    }

    private static boolean hasTptTally(SQLiteDatabase db, String code, String month) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM indicator_daily_tally "
                        + "WHERE indicator_code = ? AND strftime('%Y-%m', day) = ? LIMIT 1",
                new String[] {code, month})) {
            return cursor.moveToFirst();
        }
    }
}
