package org.smartregister.chw.repository;

import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.smartregister.chw.BuildConfig;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.reporting.dao.ReportIndicatorDaoImpl;

/** Refreshes AGYW report queries and replayable tallies after the version-57 SQL repair. */
final class AgywReportingMigration {
    static final String REPORT_CONFIG = "config/agyw-monthly-report.yml";
    private static final String AGYW_INDICATORS = "indicator_code LIKE 'agyw-%'";

    private AgywReportingMigration() {}

    static void upgrade(SQLiteDatabase db, ReportingLibrary reportingLibrary) {
        refreshDefinitions(db, reportingLibrary);
        removeReplayableTallies(db);
        // Preserve other report definitions and let the normal worker replay retained dates.
        reportingLibrary.getContext().allSharedPreferences().savePreference(
                "APP_VERSION_CODE", String.valueOf(BuildConfig.VERSION_CODE));
        reportingLibrary.getContext().allSharedPreferences().savePreference(
                ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE, "");
    }

    private static void refreshDefinitions(SQLiteDatabase db, ReportingLibrary reportingLibrary) {
        // Loading inserts definitions. Remove only this report's obsolete copies first.
        db.execSQL("DELETE FROM indicator_queries WHERE " + AGYW_INDICATORS);
        db.execSQL("DELETE FROM indicators WHERE " + AGYW_INDICATORS);
        reportingLibrary.readConfigFile(REPORT_CONFIG, db);
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM indicator_queries WHERE " + AGYW_INDICATORS + " LIMIT 1", null)) {
            if (!cursor.moveToFirst()) {
                throw new IllegalStateException("AGYW reporting definitions were not reloaded");
            }
        }
    }

    private static void removeReplayableTallies(SQLiteDatabase db) {
        if (!hasEventTable(db)) return;
        // A failed query could leave stale zero or nonzero values while advancing the shared
        // checkpoint. Keep days whose events are gone or cannot be read by the normal worker.
        db.execSQL("DELETE FROM indicator_daily_tally WHERE " + AGYW_INDICATORS
                + " AND date(day) IN (SELECT date(eventDate) FROM event "
                + "WHERE eventType NOT LIKE '%tallies Report%' "
                + "AND date(eventDate) IS NOT NULL "
                + "AND date(eventDate) = date(eventDate, '+0 days') "
                + "AND length(updatedAt) >= 19 AND substr(updatedAt, 11, 1) = ' ' "
                + "AND datetime(updatedAt) IS NOT NULL)");
    }

    private static boolean hasEventTable(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'event'", null)) {
            return cursor.moveToFirst();
        }
    }
}
