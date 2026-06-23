package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.utils.CoreConstants;
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
        if (StringUtils.isBlank(baseEntityId)) return null;

        // Primary: facility confirmation record
        String sql = String.format(Locale.US,
                "SELECT visit_date FROM %s WHERE base_entity_id = '%s' " +
                        "ORDER BY CASE WHEN last_interacted_with IS NOT NULL " +
                        "THEN last_interacted_with ELSE visit_date END DESC LIMIT 1",
                CONFIRMATION_TABLE, baseEntityId);
        Date date = parseDateFromQuery(sql);
        if (date != null) return date;

        // Fallback: 3-day follow-up visit with a positive test result
        String fallback = String.format(Locale.US,
                "SELECT visit_date FROM ec_diabetes_hypertension_followup " +
                        "WHERE base_entity_id = '%s' " +
                        "AND (LOWER(IFNULL(diabetes_test_result,'')) = 'positive' " +
                        "  OR LOWER(IFNULL(hypertension_test_result,'')) = 'positive') " +
                        "ORDER BY COALESCE(last_interacted_with, visit_date) ASC LIMIT 1",
                baseEntityId);
        return parseDateFromQuery(fallback);
    }

    private static Date parseDateFromQuery(String sql) {
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "visit_date");
        List<String> results = readData(sql, dataMap);
        if (results != null && !results.isEmpty() && StringUtils.isNotBlank(results.get(0))) {
            try {
                return DATE_FORMAT.parse(results.get(0));
            } catch (ParseException e) {
                try { return new Date(Long.parseLong(results.get(0))); } catch (
                        NumberFormatException ex) {
                    throw new RuntimeException(ex);
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
     * Returns the diagnosis type for a confirmed NCD client: "DM", "HTN", or "DM_HTN".
     * Returns null if no confirmation record exists.
     */
    public static String getDiagnosisType(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) return null;

        // Primary: facility confirmation record
        String sql = String.format(Locale.US,
                "SELECT diabetes_result, hypertension_result FROM %s " +
                        "WHERE base_entity_id = '%s' " +
                        "ORDER BY CASE WHEN last_interacted_with IS NOT NULL " +
                        "THEN last_interacted_with ELSE visit_date END DESC LIMIT 1",
                CONFIRMATION_TABLE, baseEntityId);
        String type = deriveDiagnosisTypeFromQueryV2(sql, "diabetes_result", "hypertension_result");
        if (type != null) return type;

        // Fallback: 3-day follow-up with positive results
        String fallback = String.format(Locale.US,
                "SELECT diabetes_test_result, hypertension_test_result " +
                        "FROM ec_diabetes_hypertension_followup " +
                        "WHERE base_entity_id = '%s' " +
                        "AND (LOWER(IFNULL(diabetes_test_result,'')) = 'positive' " +
                        "  OR LOWER(IFNULL(hypertension_test_result,'')) = 'positive') " +
                        "ORDER BY COALESCE(last_interacted_with, visit_date) ASC LIMIT 1",
                baseEntityId);
        return deriveDiagnosisTypeFromQueryV2(fallback, "diabetes_test_result", "hypertension_test_result");
    }

    private static String deriveDiagnosisTypeFromQueryV2(String sql, String dmCol, String htnCol){
        DataMap<String[]> dataMap = cursor -> new String[]{
                getCursorValue(cursor, dmCol),
                getCursorValue(cursor, htnCol)
        };

        List<String[]> results = readData(sql, dataMap);

        // Ensure the list is safely populated before accessing index 0
        if (results != null && !results.isEmpty()) {
            String[] firstRow = results.get(0);

            // Extra safeguard: Ensure the array itself isn't null and has our 2 expected columns
            if (firstRow != null && firstRow.length >= 2) {
                boolean dm  = isPositiveResult(firstRow[0]);
                boolean htn = isPositiveResult(firstRow[1]);

                if (dm && htn) return "DM_HTN";
                if (dm)        return "DM";
                if (htn)       return "HTN";
            }
        }

        return null;
    }

    private static String deriveDiagnosisTypeFromQuery(String sql, String dmCol, String htnCol){
        DataMap<String[]> dataMap = cursor -> new String[]{
                getCursorValue(cursor, dmCol),
                getCursorValue(cursor, htnCol)
        };
        List<String[]> results = readData(sql, dataMap);
        if (results != null && !results.isEmpty()) {
            boolean dm  = isPositiveResult(results.get(0)[0]);
            boolean htn = isPositiveResult(results.get(0)[1]);
            if (dm && htn) return "DM_HTN";
            if (dm)        return "DM";
            if (htn)       return "HTN";
        }
        return null;
    }



    /**
     * Returns the focus of the latest open NCD referral task, or null if none exists.
     * Values: "NCD Danger Signs" (urgent) or "NCD Clinical Concern" (non-emergency).
     * Filters on focus rather than code because the urgent task code "Referral" is
     * shared across the canonical referral workflow and is not NCD-specific.
     */
    public static String getOpenReferralType(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        String sql = String.format(Locale.US,
                "SELECT focus FROM task " +
                        "WHERE for_entity = '%s' " +
                        "AND focus IN ('NCD Danger Signs', 'NCD Clinical Concern') " +
                        "AND status IN ('READY', 'IN_PROGRESS') " +
                        "ORDER BY authored_on DESC LIMIT 1",
                baseEntityId);

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "focus");
        List<String> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    private static boolean isPositiveResult(String result) {
        if (StringUtils.isBlank(result)) {
            return false;
        }
        String normalized = result.trim().toLowerCase(Locale.US);
        return "positive".equals(normalized) || "confirmed".equals(normalized)
                || "yes".equals(normalized) || "true".equals(normalized);
    }

    /**
     * Checks if there is an open (non-closed) NCD referral task for the given client.
     * Filters on focus ('NCD Danger Signs' / 'NCD Clinical Concern') to scope the
     * dedup check to NCD-specific referrals only.
     * Reads from the local task store only — no network query.
     */
    public static boolean hasOpenReferral(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return false;
        }

        String sql = String.format(Locale.US,
                "SELECT COUNT(*) as cnt FROM task " +
                        "WHERE for_entity = '%s' " +
                        "AND focus IN ('NCD Danger Signs', 'NCD Clinical Concern') " +
                        "AND status IN ('READY', 'IN_PROGRESS') " +
                        "LIMIT 1",
                baseEntityId);

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "cnt");
        List<Integer> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty() && results.get(0) > 0);
    }

    public static boolean hasDeathRegisterRecord(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return false;
        }

        String sql = String.format(Locale.US,
                "SELECT COUNT(*) as cnt FROM ec_hps_death_register " +
                        "WHERE base_entity_id = '%s' " +
                        "AND IFNULL(dod, '') <> '' " +
                        "LIMIT 1",
                baseEntityId);

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "cnt");
        List<Integer> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty() && results.get(0) > 0);
    }

    public static boolean hasRemoveMemberEvent(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return false;
        }

        String sql = String.format(Locale.US,
                "SELECT COUNT(*) as cnt FROM event " +
                        "WHERE baseEntityId = '%s' COLLATE NOCASE " +
                        "AND eventType = '%s' COLLATE NOCASE " +
                        "LIMIT 1",
                baseEntityId, CoreConstants.EventType.REMOVE_MEMBER);

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "cnt");
        List<Integer> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty() && results.get(0) > 0);
    }

    public static boolean hasMortalityRecord(String baseEntityId) {
        return hasRemoveMemberEvent(baseEntityId) || hasDeathRegisterRecord(baseEntityId);
    }

    public static boolean isNcdCaseClosed(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return false;
        }

        String sql = String.format(Locale.US,
                "SELECT COUNT(*) as cnt FROM ec_ncd_register " +
                        "WHERE base_entity_id = '%s' " +
                        "AND IFNULL(is_closed, 0) = 1 " +
                        "LIMIT 1",
                baseEntityId);

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "cnt");
        List<Integer> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty() && results.get(0) > 0);
    }

    /**
     * Cancels all open NCD-related tasks (READY or IN_PROGRESS) for the given client.
     * Called when a close record event is submitted.
     */
    public static void cancelOpenTasks(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) return;

        String sql = String.format(Locale.US,
                "UPDATE task SET status = 'CANCELLED', last_modified = strftime('%%s','now') * 1000 " +
                        "WHERE for_entity = '%s' " +
                        "AND status IN ('READY', 'IN_PROGRESS')",
                baseEntityId);
        updateDB(sql);
    }

    /**
     * Voids all open NCD referral tasks for the given client by setting status to CANCELLED.
     * Called when a close record event is submitted.
     */
    public static void voidOpenReferrals(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) return;

        String sql = String.format(Locale.US,
                "UPDATE task SET status = 'CANCELLED', last_modified = strftime('%%s','now') * 1000 " +
                        "WHERE for_entity = '%s' " +
                        "AND focus IN ('NCD Danger Signs', 'NCD Clinical Concern') " +
                        "AND status IN ('READY', 'IN_PROGRESS')",
                baseEntityId);
        updateDB(sql);
    }
}
