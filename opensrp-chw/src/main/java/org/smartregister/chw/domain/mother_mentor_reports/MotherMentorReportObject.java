package org.smartregister.chw.domain.mother_mentor_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.util.MotherMentorFollowUpType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Monthly Mama Kinara workbook, using the same direct-query pipeline as Harm Reduction reports. */
public class MotherMentorReportObject extends ReportObject {
    private int failedQueries;
    private static final String NOT_CAPTURED = "—";
    private static final String LEGACY_SERVICE = "MotherMentor Services";
    private static final String[] AGE_BANDS = {
        "under-1m", "1-11m", "12-23m", "2-4", "5-9", "10-14", "15-19", "20-24", "25-59", "60-plus"
    };
    private static final int[] AGE_MONTH_LIMITS = {0, 1, 12, 24, 60, 120, 180, 240, 300, 720};
    private static final String[] SEXES = {"male", "female"};
    private static final String[] SUBROWS = {
        "4-returned",
        "4-not-found",
        "4-died",
        "4-refused",
        "8-hiv-positive",
        "8-hiv-negative",
        "8-syphilis-positive",
        "8-syphilis-negative",
        "8-hepatitis-b-positive",
        "8-hepatitis-b-negative",
        "9-boys",
        "9-girls",
        "10-boys",
        "10-girls",
        "11-positive",
        "11-negative",
        "12-hiv",
        "12-syphilis",
        "12-hepatitis-b",
        "18-returned",
        "18-not-found",
        "18-died",
        "18-transferred",
        "18-refused"
    };
    private static final List<String> RESULT_COLUMNS = createResultColumns();

    public MotherMentorReportObject(Date reportDate) {
        super(reportDate);
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        failedQueries = 0;
        JSONObject result = new JSONObject();
        // Unsupported indicators are not zero. They require additional data or agreed definitions.
        for (int indicator = 1; indicator <= 18; indicator++) {
            putUnavailable(result, "mm-" + indicator);
        }
        for (String subrow : SUBROWS) {
            putUnavailable(result, "mm-" + subrow);
        }

        putClientIndicator(
                result, "mm-1", DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP);
        putClientIndicator(result, "mm-2", DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP);
        putClientIndicator(result, "mm-7", DBConstants.KEY.EDUCATION_PROVIDED);
        Map<String, Integer> households =
                query(buildHouseholdQuery(), Collections.singletonList("total"));
        if (households != null) result.put("mm-13", value(households, "total"));
        putDemographicIndicator(result, "mm-3", buildNewArtQuery());
        putTracingIndicators(result, 4, true);
        putDemographicIndicator(result, "mm-5", buildReturnedToArtQuery());
        putTracingIndicators(result, 18, false);
        putChildAttendanceIndicator(result, 9, "week_6", 0);
        putChildAttendanceIndicator(result, 10, "month_9", 1);
        putCommunityIndicators(result);
        putAggregateIndicator(result, "mm-14", buildHouseholdCounsellingQuery());
        putTracingCoverage(result);
        result.put("query-errors", failedQueries);
        return result;
    }

    private void putTracingCoverage(JSONObject result) throws JSONException {
        // Unknown older visits cannot override a later complete tracing outcome. An unknown
        // latest possible tracing visit still makes the month-end outcome unavailable.
        String candidates = tracingCandidates();
        String returnKnown =
                "v."
                        + DBConstants.KEY.RETURNED_TO_ART
                        + " IN ('yes', 'no', 'not_applicable') AND (v."
                        + DBConstants.KEY.RETURNED_TO_ART
                        + " <> 'yes' OR ("
                        + returnedOutcome()
                        + "))";
        String outcomeKnown =
                "v.tracing_flag = 'yes' AND v.outcome IN ('found', 'not_found', 'deceased',"
                        + " 'transferred', 'refused', 'still_followed') AND "
                        + returnKnown;
        for (int indicator : new int[] {4, 18}) {
            String sql =
                    "WITH coverage_visits AS ("
                            + candidates
                            + ") SELECT COUNT(DISTINCT v."
                            + DBConstants.KEY.BASE_ENTITY_ID
                            + ") AS total FROM coverage_visits v WHERE NOT EXISTS (SELECT 1 FROM"
                            + " coverage_visits n WHERE n."
                            + DBConstants.KEY.BASE_ENTITY_ID
                            + " = v."
                            + DBConstants.KEY.BASE_ENTITY_ID
                            + " AND "
                            + laterVisit()
                            + ")"
                            + (indicator == 4 ? " AND " + primaryAt("v.service_date") : "")
                            + " AND NOT coalesce(("
                            + outcomeKnown
                            + "), 0)";
            Map<String, Integer> missing = query(sql, Collections.singletonList("total"));
            if (missing != null)
                result.put("mm-" + indicator + "-missing-records", value(missing, "total"));
            if (missing == null || value(missing, "total") > 0) {
                result.remove("mm-" + indicator + "-other-outcomes");
                for (String suffix :
                        new String[] {
                            "", "-returned", "-not-found", "-died", "-refused", "-transferred"
                        }) putUnavailable(result, "mm-" + indicator + suffix);
            }
        }
        // Indicator 5 is any confirmed return in the month, independent of the latest outcome.
        // Missing observations for a client already counted cannot add another distinct client.
        String returnedClients = returnedToArtClients();
        Map<String, Integer> missingReturns =
                query(
                        "SELECT COUNT(DISTINCT v."
                                + DBConstants.KEY.BASE_ENTITY_ID
                                + ") AS total FROM ("
                                + candidates
                                + ") v WHERE "
                                + primaryAt("v.service_date")
                                + " AND NOT coalesce((v.tracing_flag = 'yes' AND "
                                + returnKnown
                                + "), 0) AND v."
                                + DBConstants.KEY.BASE_ENTITY_ID
                                + " NOT IN (SELECT client_id FROM ("
                                + returnedClients
                                + "))",
                        Collections.singletonList("total"));
        if (missingReturns != null)
            result.put("mm-5-missing-records", value(missingReturns, "total"));
        if (missingReturns == null || value(missingReturns, "total") > 0)
            putUnavailable(result, "mm-5");
    }

    private static String laterVisit() {
        return "(n.service_date > v.service_date OR (n.service_date = v.service_date AND"
                + " (coalesce(CAST(n."
                + DBConstants.KEY.VISIT_DATE
                + " AS INTEGER), 0) > coalesce(CAST(v."
                + DBConstants.KEY.VISIT_DATE
                + " AS INTEGER), 0) OR (coalesce(CAST(n."
                + DBConstants.KEY.VISIT_DATE
                + " AS INTEGER), 0) = coalesce(CAST(v."
                + DBConstants.KEY.VISIT_DATE
                + " AS INTEGER), 0) AND n."
                + DBConstants.KEY.VISIT_ID
                + " > v."
                + DBConstants.KEY.VISIT_ID
                + "))))";
    }

    private void putUnavailable(JSONObject result, String key) throws JSONException {
        for (String column : RESULT_COLUMNS) {
            if (!"unclassified".equals(column)) {
                result.put(jsonKey(key, column), NOT_CAPTURED);
            }
        }
    }

    private void putClientIndicator(JSONObject result, String key, String detailKey)
            throws JSONException {
        Map<String, Integer> counts = query(buildClientQuery(detailKey), RESULT_COLUMNS);
        if (counts == null) return;
        for (String column : RESULT_COLUMNS) {
            result.put(jsonKey(key, column), value(counts, column));
        }
    }

    private static String jsonKey(String key, String column) {
        return "total".equals(column) ? key : key + "-" + column.replace('_', '-');
    }

    private static int value(Map<String, Integer> counts, String column) {
        Integer count = counts == null ? null : counts.get(column);
        return count == null ? 0 : count;
    }

    private static List<String> createResultColumns() {
        List<String> columns = new ArrayList<>(Arrays.asList("total", "unclassified"));
        for (String sex : SEXES) {
            columns.add(sex + "_total");
            for (String band : AGE_BANDS) {
                columns.add(sex + "_" + band.replace('-', '_'));
            }
        }
        return Collections.unmodifiableList(columns);
    }

    private String demographicQuery(String source) {
        return demographicQuery(source, 120, Integer.MAX_VALUE);
    }

    private String demographicQuery(String source, int minimumMonths, int maximumMonths) {
        // Respect each workbook row's applicable age bands; expose unknown/out-of-band clients.
        String ageRange = "age_months >= " + minimumMonths + " AND age_months < " + maximumMonths;
        String eligible = ageRange + " AND " + DBConstants.KEY.GENDER + " IN ('male', 'female')";
        StringBuilder sql = new StringBuilder("SELECT ");
        appendCount(sql, eligible, "total");
        sql.append(", ");
        appendCount(sql, "NOT coalesce((" + eligible + "), 0)", "unclassified");
        for (String sex : SEXES) {
            sql.append(", ");
            appendCount(
                    sql,
                    DBConstants.KEY.GENDER + " = '" + sex + "' AND " + ageRange,
                    sex + "_total");
            for (int band = 0; band < AGE_BANDS.length; band++) {
                String condition =
                        DBConstants.KEY.GENDER
                                + " = '"
                                + sex
                                + "' AND age_months >= "
                                + AGE_MONTH_LIMITS[band];
                if (band + 1 < AGE_BANDS.length) {
                    condition += " AND age_months < " + AGE_MONTH_LIMITS[band + 1];
                }
                sql.append(", ");
                appendCount(
                        sql,
                        condition + " AND " + ageRange,
                        sex + "_" + AGE_BANDS[band].replace('-', '_'));
            }
        }
        sql.append(" FROM (").append(source).append(") report_clients");
        return sql.toString();
    }

    String buildClientQuery(String detailKey) {
        return demographicQuery(
                clientSource(
                        finalizedVisitInMonth()
                                + " AND "
                                + primaryAt(serviceDate())
                                + " AND "
                                + hasDetail(
                                        detailKey,
                                        "lower(trim(d." + DBConstants.KEY.DETAILS + ")) = 'yes'")));
    }

    private String clientSource(String condition) {
        return clientSource("visits", condition);
    }

    private String clientSource(String source, String condition) {
        return "SELECT DISTINCT v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " AS client_id, lower(trim(ifnull(m."
                + DBConstants.KEY.GENDER
                + ", ''))) AS "
                + DBConstants.KEY.GENDER
                + ", "
                + ageInMonths()
                + " AS age_months FROM "
                + source
                + " v LEFT JOIN ec_family_member m ON m."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " WHERE "
                + condition;
    }

    String buildHouseholdQuery() {
        return "SELECT COUNT(DISTINCT v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + ") AS total FROM visits v WHERE "
                + finalizedVisitInMonth()
                + " AND EXISTS (SELECT 1 FROM ec_mothermentor_household_enrolment h WHERE h."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " AND lower(trim(h."
                + DBConstants.KEY.CONSENT
                + ")) = 'ndiyo') AND "
                + hasDetail(
                        DBConstants.KEY.PURPOSE_OF_VISIT,
                        "trim(ifnull(d." + DBConstants.KEY.DETAILS + ", '')) <> ''");
    }

    private void putDemographicIndicator(JSONObject result, String key, String sql)
            throws JSONException {
        Map<String, Integer> counts = query(sql, RESULT_COLUMNS);
        if (counts == null) return;
        for (String column : RESULT_COLUMNS)
            result.put(jsonKey(key, column), value(counts, column));
    }

    /** ReportDao's fallback zero must not turn a failed SQL query into a measured zero. */
    private Map<String, Integer> query(String sql, List<String> columns) {
        List<String> checkedColumns = new ArrayList<>(columns);
        checkedColumns.add("query_ok");
        Map<String, Integer> counts =
                ReportDao.getReportBreakdown(
                        "SELECT 1 AS query_ok, report.* FROM (" + sql + ") report", checkedColumns);
        if (value(counts, "query_ok") == 1) return counts;
        failedQueries++;
        return null;
    }

    private String enrollmentDate(String id, String eventType) {
        // eventDate can be an ISO timestamp with an offset. Enrollment is a recorded local
        // calendar date; converting to UTC can incorrectly move first-day events to last month.
        String date = "substr(e." + DBConstants.KEY.EVENT_DATE_TIMESTAMP + ", 1, 10)";
        return "(SELECT MIN("
                + date
                + ") FROM event e WHERE e."
                + DBConstants.KEY.EVENT_BASE_ENTITY_ID
                + " = "
                + id
                + " AND e."
                + DBConstants.KEY.EVENT_TYPE
                + " = '"
                + eventType
                + "' AND "
                + validDate(date)
                + " AND date(e."
                + DBConstants.KEY.EVENT_DATE_TIMESTAMP
                + ") IS NOT NULL)";
    }

    private String primaryAt(String date) {
        return "EXISTS (SELECT 1 FROM ec_mothermentor_enrollment e WHERE e."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + ") AND "
                + enrollmentDate("v." + DBConstants.KEY.BASE_ENTITY_ID, "Mother Mentor Enrollment")
                + " <= "
                + date;
    }

    private String iitAt(String date) {
        return "EXISTS (SELECT 1 FROM ec_mothermentor_enroll_it i WHERE i."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + ") AND "
                + enrollmentDate("v." + DBConstants.KEY.BASE_ENTITY_ID, "Mother Mentor Enroll IIT")
                + " <= "
                + date;
    }

    private String childEidAt(String date) {
        return "EXISTS (SELECT 1 FROM ec_mothermentor_enroll_child_eid c WHERE c."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + ") AND "
                + enrollmentDate(
                        "v." + DBConstants.KEY.BASE_ENTITY_ID, "Mother Mentor Enroll Child Eid")
                + " <= "
                + date;
    }

    private void putChildAttendanceIndicator(
            JSONObject result, int indicator, String stage, int minimumMonths)
            throws JSONException {
        String key = "mm-" + indicator;
        String selectedStage = "v.test_stage";
        String attendance = "v.attendance";
        String attendanceDate = "v.attendance_date";
        String visits = "(" + childAttendanceVisits() + ")";
        String knownStage =
                selectedStage
                        + " IN ('week_6', 'month_9', 'three_months_after_breastfeeding',"
                        + " 'month_18', 'not_applicable')";
        String knownAttendance =
                attendance
                        + " IN ('yes', 'no', 'not_applicable') AND ("
                        + attendance
                        + " <> 'yes' OR ("
                        + validDate(attendanceDate)
                        + " AND "
                        + attendanceDate
                        + " <= v.service_date AND "
                        + childEidAt(attendanceDate)
                        + "))";
        // An old visit with no test stage could belong to either indicator. A known different
        // stage does not make this indicator incomplete. Never backfill attendance from enrollment.
        String missingCondition =
                "NOT coalesce(("
                        + knownStage
                        + "), 0) OR ("
                        + selectedStage
                        + " = '"
                        + stage
                        + "' AND NOT coalesce(("
                        + knownAttendance
                        + "), 0))";
        Map<String, Integer> missing =
                query(
                        "SELECT COUNT(DISTINCT v."
                                + DBConstants.KEY.VISIT_ID
                                + ") AS total FROM "
                                + visits
                                + " v WHERE "
                                + missingCondition,
                        Collections.singletonList("total"));
        if (missing == null) return;
        result.put(key + "-missing-records", value(missing, "total"));
        if (value(missing, "total") > 0) return;
        String source =
                clientSource(
                        visits,
                        selectedStage
                                + " = '"
                                + stage
                                + "' AND "
                                + attendance
                                + " = 'yes' AND "
                                + knownAttendance
                                + " AND "
                                + inMonth(attendanceDate));
        putDemographicIndicator(result, key, demographicQuery(source, minimumMonths, 12));
        for (String sex : SEXES) {
            putDemographicIndicator(
                    result,
                    key + ("male".equals(sex) ? "-boys" : "-girls"),
                    demographicQuery(
                            "SELECT * FROM ("
                                    + source
                                    + ") children WHERE "
                                    + DBConstants.KEY.GENDER
                                    + " = '"
                                    + sex
                                    + "'",
                            minimumMonths,
                            12));
        }
    }

    private String childAttendanceVisits() {
        String stage =
                "CASE "
                        + detail(DBConstants.KEY.CHILD_EID_TEST_TYPE)
                        + " WHEN 'six_weeks' THEN 'week_6' WHEN 'nine_months' THEN 'month_9' WHEN"
                        + " 'three_months_after_weaning' THEN 'three_months_after_breastfeeding'"
                        + " WHEN 'eighteen_months' THEN 'month_18' END";
        MotherMentorFollowUpType type = MotherMentorFollowUpType.CHILD_EID;
        return "SELECT v.*, "
                + serviceDate()
                + " AS service_date, "
                + workflowAnswer(type, DBConstants.KEY.CHILD_FOLLOWUP_TEST_STAGE, whenFound(stage))
                + " AS test_stage, "
                + workflowAnswer(
                        type,
                        DBConstants.KEY.CHILD_REACHED_FACILITY,
                        whenFound(detail(DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY)))
                + " AS attendance, "
                + normalizeDate(
                        workflowAnswer(
                                type,
                                DBConstants.KEY.CHILD_FACILITY_DATE,
                                detail(DBConstants.KEY.CHILD_EID_FACILITY_ARRIVAL_DATE)))
                + " AS attendance_date FROM visits v WHERE "
                + finalizedVisitInMonth(LEGACY_SERVICE, type.getEventType())
                + " AND "
                + childEidAt(serviceDate());
    }

    String buildNewArtQuery() {
        String enrolled =
                enrollmentDate("m." + DBConstants.KEY.BASE_ENTITY_ID, "Mother Mentor Enrollment");
        String artDate = normalizeDate("e." + DBConstants.KEY.ART_START_DATE);
        // Use the enrollment ART date and later explicit initiation snapshots, never infer ART
        // initiation merely from continuing services or the time an imported event was saved.
        return demographicQuery(
                "SELECT DISTINCT m."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " AS client_id, lower(trim(ifnull(m."
                        + DBConstants.KEY.GENDER
                        + ", ''))) AS "
                        + DBConstants.KEY.GENDER
                        + ", "
                        + ageInMonths()
                        + " AS age_months FROM ec_family_member m JOIN ec_mothermentor_enrollment e"
                        + " ON e."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = m."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " WHERE "
                        + inMonth(enrolled)
                        + " AND ((lower(e."
                        + DBConstants.KEY.ART_STATUS
                        + ") = 'yes' AND "
                        + inMonth(artDate)
                        + ") OR EXISTS (SELECT 1 FROM visits v WHERE v."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = m."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " AND "
                        + finalizedVisitInMonth()
                        + " AND "
                        + hasDetail(
                                DBConstants.KEY.NEW_PMTCT_ART_INITIATION,
                                "d." + DBConstants.KEY.DETAILS + " = 'yes'")
                        + " AND "
                        + inMonth(normalizeDate(detail(DBConstants.KEY.VISIT_ART_START_DATE)))
                        + " AND "
                        + normalizeDate(detail(DBConstants.KEY.VISIT_ART_START_DATE))
                        + " <= "
                        + serviceDate()
                        + "))");
    }

    private String tracingCandidates() {
        MotherMentorFollowUpType type = MotherMentorFollowUpType.MOTHER_IIT;
        String outcome =
                "CASE "
                        + detail(DBConstants.KEY.FOLLOW_UP_STATUS)
                        + " WHEN 'transferred_facility' THEN 'transferred' WHEN 'refused_return'"
                        + " THEN 'refused' WHEN 'under_follow_up' THEN 'still_followed' ELSE "
                        + detail(DBConstants.KEY.FOLLOW_UP_STATUS)
                        + " END";
        return "SELECT v.*, "
                + serviceDate()
                + " AS service_date, "
                + workflowAnswer(type, DBConstants.KEY.IIT_TRACING_VISIT, "'yes'")
                + " AS tracing_flag, "
                + workflowAnswer(type, DBConstants.KEY.IIT_TRACING_OUTCOME, outcome)
                + " AS outcome, "
                + workflowAnswer(
                        type,
                        DBConstants.KEY.RETURNED_TO_ART,
                        whenFound(detail(DBConstants.KEY.RETURNED_TO_FACILITY)))
                + " AS "
                + DBConstants.KEY.RETURNED_TO_ART
                + ", "
                + normalizeDate(
                        workflowAnswer(
                                type,
                                DBConstants.KEY.ART_RETURN_DATE,
                                detail(DBConstants.KEY.FACILITY_RETURN_DATE)))
                + " AS return_date FROM visits v WHERE "
                + finalizedVisitInMonth(LEGACY_SERVICE, type.getEventType())
                + " AND "
                + iitAt(serviceDate())
                + " AND NOT coalesce(("
                + workflowAnswer(type, DBConstants.KEY.IIT_TRACING_VISIT, "'yes'")
                + " = 'no'), 0)";
    }

    private String tracingVisits() {
        return "SELECT * FROM (" + tracingCandidates() + ") WHERE tracing_flag = 'yes'";
    }

    private static String workflowAnswer(
            MotherMentorFollowUpType type, String legacyKey, String currentAnswer) {
        // Select by encounter contract, not COALESCE: missing new answers must not fall back
        // to stale fields from a different workflow.
        return "CASE WHEN v."
                + DBConstants.KEY.VISIT_TYPE
                + " = '"
                + type.getEventType()
                + "' THEN "
                + currentAnswer
                + " ELSE "
                + detail(legacyKey)
                + " END";
    }

    private static String whenFound(String answer) {
        // These sections are intentionally hidden (and cleared) for non-found outcomes.
        // Unknown status remains missing evidence, not an assumed negative answer.
        return "CASE WHEN "
                + detail(DBConstants.KEY.FOLLOW_UP_STATUS)
                + " = 'found' THEN "
                + answer
                + " WHEN "
                + detail(DBConstants.KEY.FOLLOW_UP_STATUS)
                + " IN ('not_found', 'deceased', 'transferred_facility', 'refused_return',"
                + " 'under_follow_up') THEN 'not_applicable' END";
    }

    private String latestTracingClients(boolean primary, String outcome) {
        // Latest clinical date wins. Save time and visit ID deterministically break same-day ties.
        return "WITH tracing AS ("
                + tracingVisits()
                + "), latest AS (SELECT v.* FROM tracing v WHERE NOT EXISTS (SELECT 1 FROM tracing"
                + " n WHERE n."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " AND "
                + laterVisit()
                + ")) "
                + clientSource(
                        "latest",
                        (primary ? primaryAt("v.service_date") + " AND " : "")
                                + "("
                                + outcome
                                + ")");
    }

    private String returnedOutcome() {
        return "v."
                + DBConstants.KEY.RETURNED_TO_ART
                + " = 'yes' AND "
                + validDate("v.return_date")
                + " AND v.return_date <= v.service_date";
    }

    private void putTracingIndicators(JSONObject result, int indicator, boolean primary)
            throws JSONException {
        String returned = returnedOutcome();
        String[][] outcomes = {
            {"returned", returned},
            {"not-found", "v.outcome = 'not_found' AND NOT (" + returned + ")"},
            {"died", "v.outcome = 'deceased' AND NOT (" + returned + ")"},
            {"refused", "v.outcome = 'refused' AND NOT (" + returned + ")"},
            {"transferred", "v.outcome = 'transferred' AND NOT (" + returned + ")"}
        };
        StringBuilder displayedOutcomes = new StringBuilder();
        for (String[] outcome : outcomes) {
            if (indicator == 4 && "transferred".equals(outcome[0])) continue;
            // COALESCE is necessary when no return date was captured for a non-return outcome.
            String condition =
                    outcome[1].replace(
                            "NOT (" + returned + ")", "NOT coalesce((" + returned + "), 0)");
            putDemographicIndicator(
                    result,
                    "mm-" + indicator + "-" + outcome[0],
                    demographicQuery(latestTracingClients(primary, condition)));
            if (displayedOutcomes.length() > 0) displayedOutcomes.append(" OR ");
            displayedOutcomes.append('(').append(condition).append(')');
        }
        putDemographicIndicator(
                result,
                "mm-" + indicator,
                demographicQuery(latestTracingClients(primary, displayedOutcomes.toString())));
        Map<String, Integer> other =
                query(
                        "SELECT COUNT(DISTINCT client_id) AS total FROM ("
                                + latestTracingClients(
                                        primary, "NOT coalesce((" + displayedOutcomes + "), 0)")
                                + ")",
                        Collections.singletonList("total"));
        if (other != null) result.put("mm-" + indicator + "-other-outcomes", value(other, "total"));
    }

    String buildReturnedToArtQuery() {
        return demographicQuery(returnedToArtClients());
    }

    private String returnedToArtClients() {
        // The current Mother IIT return-to-facility section replaces the legacy return
        // section. Count a confirmed, dated return; being found alone is insufficient.
        return clientSource(
                "(" + tracingVisits() + ")",
                primaryAt("v.service_date")
                        + " AND "
                        + returnedOutcome()
                        + " AND "
                        + inMonth("v.return_date"));
    }

    private String householdVisits() {
        return "SELECT v.* FROM visits v WHERE "
                + finalizedVisitInMonth()
                + " AND EXISTS (SELECT 1 FROM ec_mothermentor_household_enrolment h WHERE h."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " AND lower(trim(h."
                + DBConstants.KEY.CONSENT
                + ")) = 'ndiyo') AND "
                + hasDetail(
                        DBConstants.KEY.PURPOSE_OF_VISIT,
                        "trim(ifnull(d." + DBConstants.KEY.DETAILS + ", '')) <> ''");
    }

    String buildHouseholdCounsellingQuery() {
        return aggregateQuery(
                "SELECT "
                        + detail(DBConstants.KEY.COUNSELLED_MALE)
                        + " AS male, "
                        + detail(DBConstants.KEY.COUNSELLED_FEMALE)
                        + " AS female FROM ("
                        + householdVisits()
                        + ") v");
    }

    private String aggregateQuery(String source) {
        return "SELECT coalesce(SUM(CASE WHEN "
                + validCount("male")
                + " THEN CAST(male AS INTEGER) ELSE 0 END), 0) AS male_total, coalesce(SUM(CASE"
                + " WHEN "
                + validCount("female")
                + " THEN CAST(female AS INTEGER) ELSE 0 END), 0) AS female_total, COUNT(CASE WHEN"
                + " NOT ("
                + validCount("male")
                + ") OR NOT ("
                + validCount("female")
                + ") THEN 1 END) AS missing FROM ("
                + source
                + ")";
    }

    private void putAggregateIndicator(JSONObject result, String key, String sql)
            throws JSONException {
        Map<String, Integer> counts =
                query(sql, Arrays.asList("male_total", "female_total", "missing"));
        if (counts == null) return;
        result.put(key + "-missing-records", value(counts, "missing"));
        result.put(
                key + "-known-total", value(counts, "male_total") + value(counts, "female_total"));
        if (value(counts, "missing") > 0) return;
        result.put(key, value(counts, "male_total") + value(counts, "female_total"));
        result.put(key + "-male-total", value(counts, "male_total"));
        result.put(key + "-female-total", value(counts, "female_total"));
        // Aggregate counts cannot provide age bands or distinct people across activities.
    }

    private void putCommunityIndicators(JSONObject result) throws JSONException {
        String community =
                "SELECT *, "
                        + normalizeDate(DBConstants.KEY.SH_TAREHE)
                        + " AS service_date FROM ec_mothermentor_mobilization";
        String sessions = "SELECT * FROM (" + community + ") WHERE " + inMonth("service_date");
        String[] topics = {
            "pmtct", "hiv", "syphilis", "hepb", "fp", "nutrition", "tb", "gbv", "nyingine"
        };
        List<String> topicCounts = new ArrayList<>();
        for (String topic : topics)
            topicCounts.add(
                    "CASE WHEN " + hasToken(DBConstants.KEY.SH_MADA, topic) + " THEN 1 ELSE 0 END");
        Map<String, Integer> counts =
                query(
                        "SELECT coalesce(SUM("
                                + android.text.TextUtils.join(" + ", topicCounts)
                                + "), 0) AS topics, COUNT(CASE WHEN "
                                + hasToken(DBConstants.KEY.SH_MADA, "pmtct")
                                + " THEN 1 END) AS activities FROM ("
                                + sessions
                                + ")",
                        Arrays.asList("topics", "activities"));
        if (counts != null) {
            result.put("mm-6", value(counts, "topics"));
            result.put("mm-15", value(counts, "activities"));
        }
        putAggregateIndicator(
                result,
                "mm-16",
                aggregateQuery(
                        "SELECT "
                                + DBConstants.KEY.ELIM_M
                                + " AS male, "
                                + DBConstants.KEY.ELIM_F
                                + " AS female FROM ("
                                + sessions
                                + ") WHERE "
                                + hasToken(DBConstants.KEY.SH_MADA, "pmtct")));
        Map<String, Integer> schema =
                query(
                        "SELECT COUNT(*) AS total FROM"
                            + " pragma_table_info('ec_mothermentor_mobilization') WHERE name IN ('"
                                + DBConstants.KEY.REFERRED_MALE
                                + "', '"
                                + DBConstants.KEY.REFERRED_FEMALE
                                + "')",
                        Collections.singletonList("total"));
        if (schema != null && value(schema, "total") == 2) {
            putAggregateIndicator(
                    result,
                    "mm-17",
                    aggregateQuery(
                            "SELECT "
                                    + DBConstants.KEY.REFERRED_MALE
                                    + " AS male, "
                                    + DBConstants.KEY.REFERRED_FEMALE
                                    + " AS female FROM ("
                                    + sessions
                                    + ")"));
        } else if (schema != null) {
            Map<String, Integer> existing =
                    query(
                            "SELECT COUNT(*) AS total FROM (" + sessions + ")",
                            Collections.singletonList("total"));
            if (existing != null && value(existing, "total") == 0) {
                result.put("mm-17", 0);
                result.put("mm-17-male-total", 0);
                result.put("mm-17-female-total", 0);
            } else if (existing != null)
                result.put("mm-17-missing-records", value(existing, "total"));
        }
        // Household referral service codes do not identify or count the referred people.
        Map<String, Integer> householdReferrals =
                query(
                        "SELECT COUNT(*) AS total FROM ("
                                + householdVisits()
                                + ") v WHERE "
                                + hasDetail(
                                        DBConstants.KEY.HOUSEHOLD_REFERRAL_GIVEN,
                                        "d." + DBConstants.KEY.DETAILS + " = 'yes'"),
                        Collections.singletonList("total"));
        if (householdReferrals == null) putUnavailable(result, "mm-17");
        if (householdReferrals != null && value(householdReferrals, "total") > 0) {
            result.put("mm-17-household-referral-visits", value(householdReferrals, "total"));
            putUnavailable(result, "mm-17");
        }
    }

    private static String validCount(String column) {
        return "coalesce((trim("
                + column
                + ") <> '' AND trim("
                + column
                + ") NOT GLOB '*[^0-9]*'), 0)";
    }

    private static String hasToken(String column, String token) {
        return "instr(',' || replace(replace(replace(replace(ifnull("
                + column
                + ", ''), '[', ''), ']', ''), '\"', ''), ' ', '') || ',', ',"
                + token
                + ",') > 0";
    }

    private String serviceDate() {
        return detail(DBConstants.KEY.MOTHERMENTOR_VISIT_DATE);
    }

    private static String detail(String key) {
        return "(SELECT MAX(d."
                + DBConstants.KEY.DETAILS
                + ") FROM visit_details d WHERE d."
                + DBConstants.KEY.VISIT_ID
                + " = v."
                + DBConstants.KEY.VISIT_ID
                + " AND d."
                + DBConstants.KEY.VISIT_KEY
                + " = '"
                + key
                + "')";
    }

    private String inMonth(String column) {
        return validDate(column)
                + " AND date("
                + column
                + ") >= date('"
                + monthStart()
                + "') AND date("
                + column
                + ") < date('"
                + monthStart()
                + "', '+1 month')";
    }

    private static String normalizeDate(String column) {
        return "CASE WHEN "
                + column
                + " GLOB '[0-9][0-9]-[0-9][0-9]-[0-9][0-9][0-9][0-9]' THEN substr("
                + column
                + ", 7, 4) || '-' || substr("
                + column
                + ", 4, 2) || '-' || substr("
                + column
                + ", 1, 2) ELSE "
                + column
                + " END";
    }

    private static void appendCount(StringBuilder sql, String condition, String alias) {
        sql.append("COUNT(DISTINCT CASE WHEN ")
                .append(condition)
                .append(" THEN client_id END) AS ")
                .append(alias);
    }

    private String monthStart() {
        return new SimpleDateFormat("yyyy-MM-01", Locale.ENGLISH).format(getReportDate());
    }

    private String ageInMonths() {
        String monthEnd = "date('" + monthStart() + "', '+1 month', '-1 day')";
        String dob = "substr(m." + DBConstants.KEY.DOB + ", 1, 10)";
        // Completed calendar months keep infant and exact birthday boundaries intact.
        // Registration stores ISO timestamps as well as date-only values. Use the recorded
        // calendar birth date, without shifting it across midnight for its timezone offset.
        return "CASE WHEN "
                + validDate(dob)
                + " AND date(m."
                + DBConstants.KEY.DOB
                + ") IS NOT NULL AND date("
                + dob
                + ") <= "
                + monthEnd
                + " THEN (CAST(strftime('%Y', "
                + monthEnd
                + ") AS INTEGER) - CAST(strftime('%Y', "
                + dob
                + ") AS INTEGER)) * 12 + CAST(strftime('%m', "
                + monthEnd
                + ") AS INTEGER) - CAST(strftime('%m', "
                + dob
                + ") AS INTEGER) - CASE WHEN strftime('%d', "
                + monthEnd
                + ") < strftime('%d', "
                + dob
                + ") THEN 1 ELSE 0 END END";
    }

    private String finalizedVisitInMonth() {
        return finalizedVisitInMonth(LEGACY_SERVICE);
    }

    private String finalizedVisitInMonth(String... visitTypes) {
        // visits.visit_date is the import/save time, not necessarily the service date. The visit
        // interactor always records mothermentor_visit_date, normalized by NCUtils to yyyy-MM-dd.
        // processed is local finalization, not server sync; detail rows may remain processed=0.
        return "v."
                + DBConstants.KEY.VISIT_TYPE
                + " IN ('"
                + android.text.TextUtils.join("', '", visitTypes)
                + "') AND v."
                + DBConstants.KEY.PROCESSED
                + " = 1 AND "
                + hasDetail(
                        DBConstants.KEY.MOTHERMENTOR_VISIT_DATE,
                        validDate("d." + DBConstants.KEY.DETAILS)
                                + " AND date(d."
                                + DBConstants.KEY.DETAILS
                                + ") >= date('"
                                + monthStart()
                                + "') AND date(d."
                                + DBConstants.KEY.DETAILS
                                + ") < date('"
                                + monthStart()
                                + "', '+1 month')");
    }

    private static String hasDetail(String key, String condition) {
        return "EXISTS (SELECT 1 FROM visit_details d WHERE d."
                + DBConstants.KEY.VISIT_ID
                + " = v."
                + DBConstants.KEY.VISIT_ID
                + " AND d."
                + DBConstants.KEY.VISIT_KEY
                + " = '"
                + key
                + "' AND "
                + condition
                + ")";
    }

    private static String validDate(String column) {
        // SQLite accepts numeric Julian dates and can retain impossible dates without a modifier.
        return column
                + " GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]' AND date("
                + column
                + ", '+0 days') = "
                + column;
    }
}
