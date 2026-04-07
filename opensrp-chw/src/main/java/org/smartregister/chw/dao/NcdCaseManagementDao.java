package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.dao.AbstractDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NcdCaseManagementDao extends AbstractDao {

    private static final String TABLE = Constants.TableName.NCD_CASE_MANAGEMENT_FOLLOWUP;
    private static final String CONFIRMATION_TABLE = "ec_diabetes_hypertension_confirmation";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private NcdCaseManagementDao() {
        // no-op
    }

    /**
     * Returns the NCD confirmation date for a client from ec_diabetes_hypertension_confirmation,
     * or null if no confirmation record exists.
     */
    public static Date getConfirmationDate(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        String sql = String.format(Locale.US,
                "SELECT visit_date FROM %s WHERE base_entity_id = '%s' " +
                        "ORDER BY CASE WHEN last_interacted_with IS NOT NULL " +
                        "THEN last_interacted_with ELSE visit_date END DESC LIMIT 1",
                CONFIRMATION_TABLE, baseEntityId);

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "visit_date");
        List<String> results = readData(sql, dataMap);

        if (results != null && !results.isEmpty() && StringUtils.isNotBlank(results.get(0))) {
            try {
                return DATE_FORMAT.parse(results.get(0));
            } catch (ParseException e) {
                // Try parsing as timestamp (milliseconds)
                try {
                    return new Date(Long.parseLong(results.get(0)));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Returns the date of the most recent NCD case management follow-up visit,
     * or null if no follow-up has been completed.
     */
    public static Date getLastFollowUpDate(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        String sql = String.format(Locale.US,
                "SELECT visit_date FROM %s WHERE entity_id = '%s' " +
                        "ORDER BY visit_date DESC LIMIT 1",
                TABLE, baseEntityId);

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "visit_date");
        List<String> results = readData(sql, dataMap);

        if (results != null && !results.isEmpty() && StringUtils.isNotBlank(results.get(0))) {
            try {
                return DATE_FORMAT.parse(results.get(0));
            } catch (ParseException e) {
                try {
                    return new Date(Long.parseLong(results.get(0)));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Returns the most recent follow-up event data for a client, or null if none exists.
     * Returns a map with keys: visit_date, alert_status, is_red_alert, is_yellow_alert,
     * is_side_effects_alert, is_missed_clinic_alert.
     */
    public static Map<String, String> getLastFollowUpEvent(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        String sql = String.format(Locale.US,
                "SELECT visit_date, alert_status, is_red_alert, is_yellow_alert, " +
                        "is_side_effects_alert, is_missed_clinic_alert " +
                        "FROM %s WHERE entity_id = '%s' " +
                        "ORDER BY visit_date DESC LIMIT 1",
                TABLE, baseEntityId);

        DataMap<Map<String, String>> dataMap = cursor -> {
            Map<String, String> row = new HashMap<>();
            row.put("visit_date", getCursorValue(cursor, "visit_date"));
            row.put("alert_status", getCursorValue(cursor, "alert_status"));
            row.put("is_red_alert", getCursorValue(cursor, "is_red_alert"));
            row.put("is_yellow_alert", getCursorValue(cursor, "is_yellow_alert"));
            row.put("is_side_effects_alert", getCursorValue(cursor, "is_side_effects_alert"));
            row.put("is_missed_clinic_alert", getCursorValue(cursor, "is_missed_clinic_alert"));
            return row;
        };

        List<Map<String, String>> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    /**
     * Returns the last N follow-up visit summaries for a client, ordered by visit_date DESC.
     * Each entry contains: visit_date, alert_status, and clinical flag columns.
     */
    public static List<Map<String, String>> getVisitHistory(String baseEntityId, int limit) {
        if (StringUtils.isBlank(baseEntityId)) {
            return new ArrayList<>();
        }

        String sql = String.format(Locale.US,
                "SELECT visit_date, alert_status, clinic_attendance, medication_adherence, " +
                        "no_adherence_reason, non_healing_wounds, neuropathy, vision_changes, chest_pain " +
                        "FROM %s WHERE entity_id = '%s' " +
                        "ORDER BY visit_date DESC LIMIT %d",
                TABLE, baseEntityId, limit);

        DataMap<Map<String, String>> dataMap = cursor -> {
            Map<String, String> row = new HashMap<>();
            row.put("visit_date", getCursorValue(cursor, "visit_date"));
            row.put("alert_status", getCursorValue(cursor, "alert_status"));
            row.put("clinic_attendance", getCursorValue(cursor, "clinic_attendance"));
            row.put("medication_adherence", getCursorValue(cursor, "medication_adherence"));
            row.put("no_adherence_reason", getCursorValue(cursor, "no_adherence_reason"));
            row.put("non_healing_wounds", getCursorValue(cursor, "non_healing_wounds"));
            row.put("neuropathy", getCursorValue(cursor, "neuropathy"));
            row.put("vision_changes", getCursorValue(cursor, "vision_changes"));
            row.put("chest_pain", getCursorValue(cursor, "chest_pain"));
            return row;
        };

        List<Map<String, String>> results = readData(sql, dataMap);
        return results != null ? results : new ArrayList<>();
    }

    /**
     * Checks if there is an open (non-closed) referral task for the given client
     * of type ncd_urgent_referral or ncd_non_emergency_referral.
     * Reads from the local task store only — no network query.
     */
    public static boolean hasOpenReferral(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return false;
        }

        String sql = String.format(Locale.US,
                "SELECT COUNT(*) as cnt FROM task " +
                        "WHERE for_entity = '%s' " +
                        "AND (code = 'ncd_urgent_referral' OR code = 'ncd_non_emergency_referral') " +
                        "AND status IN ('READY', 'IN_PROGRESS') " +
                        "LIMIT 1",
                baseEntityId);

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "cnt");
        List<Integer> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty() && results.get(0) > 0);
    }
}
