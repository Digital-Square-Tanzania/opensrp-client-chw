package org.smartregister.chw.domain.mother_mentor_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorDeliveryChildren;
import org.smartregister.chw.mothermentor.dao.MotherMentorHouseholdCounsellingDao;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

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
    private static final String PRIMARY_ENROLLMENT_EVENT = "Mother Mentor Enrollment";
    private static final String[] PRIMARY_ENROLLMENT_EVENTS = {
        PRIMARY_ENROLLMENT_EVENT, Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLLMENT
    };
    private static final String[] AGE_BANDS = {
        "under-1m", "1-11m", "12-23m", "2-4", "5-9", "10-14", "15-19", "20-24", "25-59", "60-plus"
    };
    private static final int[] AGE_MONTH_LIMITS = {0, 1, 12, 24, 60, 120, 180, 240, 300, 720};
    private static final String[] SEXES = {"male", "female"};
    private static final String[] TRACING_OUTCOMES = {
        "returned", "agreed", "refused", "not-found", "died", "transferred"
    };

    private static final List<String> RESULT_COLUMNS = createResultColumns();
    public MotherMentorReportObject(Date reportDate) {
        super(reportDate);
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        failedQueries = 0;
        JSONObject result = new JSONObject();
        for (int indicator = 1; indicator <= 19; indicator++) {
            putUnavailable(result, "mm-" + indicator);
        }
        for (String indicator : new String[]{"7", "8", "9"}) {
            putUnavailable(result, "mm-" + indicator + "-boys");
            putUnavailable(result, "mm-" + indicator + "-girls");
        }
        for (String outcome : TRACING_OUTCOMES) putUnavailable(result, "mm-11-" + outcome);
        for (String outcome : new String[]{"positive", "negative"}) putUnavailable(result, "mm-10-" + outcome);
        for (String topic : new String[]{"hiv", "syphilis", "hepatitis-b"}) putUnavailable(result, "mm-13-" + topic);

        putMotherVisitIndicators(result);
        putClientIndicator(result, "mm-3", DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP);
        putClientIndicator(result, "mm-4", DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP);
        putClientIndicator(result, "mm-5", DBConstants.KEY.EDUCATION_PROVIDED);
        putLiveBirths(result);
        putChildTestIndicator(result, 8, "week_6", 0, 12);
        putChildTestIndicator(result, 9, "month_9", 1, 12);
        putChildTestIndicator(result, 10, "month_18", 1, 24);
        putTracingIndicators(result);
        putPartnerReferralIndicator(result);
        putPartnerEducationIndicators(result);
        Map<String, Integer> households = query(buildHouseholdQuery(), Collections.singletonList("total"));
        if (households != null) result.put("mm-14", value(households, "total"));
        putHouseholdCounsellingIndicator(result);
        putCommunityIndicators(result);
        result.put("query-errors", failedQueries);
        return result;
    }

    private void putLiveBirths(JSONObject result) throws JSONException {
        List<String> columns = Arrays.asList("total", "male_total", "female_total");
        Map<String, Integer> counts = query(buildLiveBirthsQuery(), columns);
        if (counts == null) return;
        int total = value(counts, "total");
        int boys = value(counts, "male_total");
        int girls = value(counts, "female_total");
        result.put("mm-7", total);
        putBirthSex(result, "male", "boys", boys);
        putBirthSex(result, "female", "girls", girls);
        result.put("mm-7-unknown-sex", total - boys - girls);
    }

    private void putBirthSex(JSONObject result, String sex, String row, int count) throws JSONException {
        result.put("mm-7-" + sex + "-total", count);
        result.put("mm-7-" + sex + "-under-1m", count);
        String key = "mm-7-" + row;
        result.put(key, count);
        result.put(key + "-" + sex + "-total", count);
        result.put(key + "-" + sex + "-under-1m", count);
    }

    String buildLiveBirthsQuery() {
        String type = Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES;
        String visitedMothers = "SELECT DISTINCT v.base_entity_id FROM visits v WHERE "
                + finalizedVisitInMonth(type) + " AND " + notVoided()
                + " AND " + primaryAt(serviceDate())
                + " AND " + hasDetail(DBConstants.KEY.ATTENDANCE_TYPE, "d.details IN ('new', 'repeat')");
        String delivery = normalizeDate(detail(DBConstants.KEY.MAMA_TAREHE_KUJIFUNGUA));
        String count = detail(MotherMentorDeliveryChildren.COUNT);
        String enrollment = detail(DBConstants.KEY.MAMA_ENROLLMENT_ID);
        // Select mothers by visit month and use actual delivery dates for the month-end cutoff.
        String visits = "WITH visited_mothers AS (" + visitedMothers + "), ranked AS (SELECT v.*, "
                + "row_number() OVER (PARTITION BY v.base_entity_id, coalesce(nullif(" + enrollment + ", ''), "
                + delivery + ") ORDER BY " + serviceDate() + ", v.visit_date, v.visit_id) AS delivery_row"
                + " FROM visits v JOIN visited_mothers m ON m.base_entity_id = v.base_entity_id"
                + " WHERE v.visit_type = '" + type + "' AND v.processed = 1 AND " + notVoided()
                + " AND " + detail(DBConstants.KEY.MAMA_PREGNANCY_OUTCOME) + " IN ('delivered', 'preterm')"
                + " AND coalesce(" + detail(DBConstants.KEY.MAMA_CARRIED_DELIVERY) + ", '') <> 'true'"
                + " AND " + validDate(delivery) + " AND " + delivery + " <= " + serviceDate()
                + " AND " + delivery + " < date('" + monthStart() + "', '+1 month')"
                + " AND " + validDate(serviceDate()) + ")";
        StringBuilder births = new StringBuilder();
        for (int number = 1; number <= MotherMentorDeliveryChildren.MAX_COUNT; number++) {
            if (number > 1) births.append(" UNION ALL ");
            String status = detail(MotherMentorDeliveryChildren.observationKey(number, DBConstants.KEY.MAMA_CHILD_STATUS));
            if (number == 1) {
                status = "CASE WHEN " + detail(MotherMentorDeliveryChildren.SECTIONS) + " = 'true' THEN " + status
                        + " ELSE " + detail(DBConstants.KEY.MAMA_CHILD_STATUS) + " END";
            }
            births.append("SELECT ").append(detail(MotherMentorDeliveryChildren.observationKey(
                            number, MotherMentorDeliveryChildren.SEX))).append(" AS sex FROM ranked v")
                    .append(" WHERE v.delivery_row = 1")
                    .append(" AND ").append(status).append(" IN ('alive', 'deceased')");
            if (number > 1) births.append(" AND CAST(").append(count).append(" AS INTEGER) >= ").append(number);
        }
        return visits + ", births AS (" + births + ") SELECT count(*) AS total,"
                + " count(CASE WHEN sex = 'male' THEN 1 END) AS male_total,"
                + " count(CASE WHEN sex = 'female' THEN 1 END) AS female_total FROM births";
    }

    private void putPartnerReferralIndicator(JSONObject result) throws JSONException {
        String answer = partnerValue(DBConstants.KEY.PARTNER_TESTING_REFERRAL_GIVEN);
        String phase = partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE);
        String visits = "SELECT * FROM (" + partnerReportVisits() + ") v WHERE "
                + partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA) + " = '3' AND ("
                + phase + " = 'referral' OR (" + phase + " = 'tracing' AND "
                + partnerValue(DBConstants.KEY.FOLLOW_UP_STATUS) + " = 'found' AND "
                + partnerValue(DBConstants.KEY.PARTNER_FACILITY_AGREEMENT) + " = 'agreed'))";
        Integer missing = missingRecords(visits, inMonth("v.service_date"), answer + " IN ('yes', 'no')");
        if (missing != null) result.put("mm-12-missing-records", missing);
        String referred = "SELECT * FROM (" + visits + ") v WHERE "
                + inMonth("v.service_date") + " AND " + answer + " = 'yes'";
        putDemographicIndicator(result, "mm-12", demographicQuery(
                serviceClientSource(firstClientVisit(referred), true), 180, Integer.MAX_VALUE));
    }

    private void putPartnerEducationIndicators(JSONObject result) throws JSONException {
        String visits = partnerReportVisits();
        Integer missing = missingRecords(visits, inMonth("v.service_date"), partnerEducationKnown());
        if (missing == null) return;
        result.put("mm-13-missing-records", missing);
        if (missing > 0) return;
        String source = "SELECT * FROM (" + visits + ") v WHERE "
                + inMonth("v.service_date") + " AND " + partnerPregnant()
                + " AND " + partnerEducationPath() + " AND "
                + partnerValue(DBConstants.KEY.PARTNER_EDUCATION_PROVIDED) + " = 'yes'";
        putDemographicIndicator(result, "mm-13", demographicQuery(
                serviceClientSource(firstClientVisit(source), true), 180, Integer.MAX_VALUE));
        for (String topic : new String[]{"hiv", "syphilis", "hepatitis_b"}) {
            String educated = source + " AND "
                    + hasToken(partnerValue(DBConstants.KEY.PARTNER_EDUCATION_TOPICS), topic);
            putDemographicIndicator(result, "mm-13-" + topic.replace('_', '-'), demographicQuery(
                    serviceClientSource(firstClientVisit(educated), true), 180, Integer.MAX_VALUE));
        }
    }

    private String partnerReportVisits() {
        String[] fields = {
            DBConstants.KEY.FOLLOW_UP_STATUS, DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA,
            DBConstants.KEY.MOTHER_BASE_ENTITY_ID,
            DBConstants.KEY.PARTNER_MOTHER_PREGNANT, DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE,
            DBConstants.KEY.PARTNER_FACILITY_AGREEMENT, DBConstants.KEY.PARTNER_TESTING_REFERRAL_GIVEN,
            DBConstants.KEY.PARTNER_NOT_FOUND_REASON,
            DBConstants.KEY.PARTNER_NOT_FOUND_REASON_OTHER, DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
            DBConstants.KEY.PARTNER_EDUCATION_PROVIDED, DBConstants.KEY.PARTNER_EDUCATION_TOPICS
        };
        StringBuilder selected = new StringBuilder("SELECT v.*, " + serviceDate() + " AS service_date");
        for (String key : fields) selected.append(", ").append(detail(key)).append(" AS ").append(key);
        selected.append(", ").append(normalizeDate(detail(DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE)))
                .append(" AS ").append(DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE);
        String category = "upper(trim(ifnull(" + detail(DBConstants.KEY.FOLLOW_UP_CATEGORY) + ", '')))";
        String legacyEvidence = hasDetail(DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY, "1 = 1")
                + " OR " + hasDetail(DBConstants.KEY.PARTNER_REACHED_FACILITY, "1 = 1")
                + " OR " + hasDetail(DBConstants.KEY.PARTNER_NON_ATTENDANCE_REASON, "1 = 1");
        return selected + " FROM visits v WHERE v." + DBConstants.KEY.PROCESSED + " = 1 AND "
                + validDate(serviceDate()) + " AND " + category + " IN ('', 'PARTNER') AND (v."
                + DBConstants.KEY.VISIT_TYPE + " = '" + MotherMentorFollowUpType.PARTNER.getEventType()
                + "' OR (v." + DBConstants.KEY.VISIT_TYPE + " = '" + LEGACY_SERVICE + "' AND ("
                + category + " = 'PARTNER' OR (" + category + " = '' AND NOT "
                + hasDetail(DBConstants.KEY.ATTENDANCE_TYPE, "1 = 1") + " AND ("
                + legacyEvidence + ")))))";
    }

    private static String partnerValue(String key) {
        return "lower(trim(ifnull(v." + key + ", '')))";
    }

    private static String answered(String column) {
        return "trim(ifnull(" + column + ", '')) <> ''";
    }

    private static String partnerPregnant() {
        return partnerValue(DBConstants.KEY.PARTNER_MOTHER_PREGNANT) + " = 'yes' AND "
                + answered("v." + DBConstants.KEY.MOTHER_BASE_ENTITY_ID);
    }

    private static String partnerArrivalKnown() {
        String date = "v." + DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE;
        return "(" + validDate(date) + " AND " + date + " <= v.service_date)";
    }

    private static String partnerTracingKnown() {
        String reason = partnerValue(DBConstants.KEY.PARTNER_NOT_FOUND_REASON);
        return partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE) + " = 'tracing' AND (("
                + partnerValue(DBConstants.KEY.FOLLOW_UP_STATUS) + " = 'found' AND "
                + partnerValue(DBConstants.KEY.PARTNER_FACILITY_AGREEMENT)
                + " IN ('agreed', 'refused')) OR ("
                + partnerValue(DBConstants.KEY.FOLLOW_UP_STATUS) + " = 'not_found' AND "
                + reason + " IN ('deceased', 'moved', 'travelled', 'other') AND ("
                + reason + " <> 'other' OR "
                + answered("v." + DBConstants.KEY.PARTNER_NOT_FOUND_REASON_OTHER) + ")))";
    }

    private static String partnerEvidenceKnown() {
        String phase = partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE);
        String arrival = partnerValue(DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY);
        String referral = partnerValue(DBConstants.KEY.PARTNER_TESTING_REFERRAL_GIVEN);
        return partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA) + " IN ('2', '3') AND ("
                + partnerValue(DBConstants.KEY.PARTNER_MOTHER_PREGNANT) + " = 'no' OR ("
                + partnerPregnant() + " AND ((" + partnerTracingKnown() + ") OR ("
                + phase + " = 'referral' AND " + referral + " IN ('yes', 'no')) OR ("
                + phase + " = 'attendance' AND (" + arrival + " = 'no' OR (" + arrival
                + " = 'yes' AND " + partnerArrivalKnown() + "))))))";
    }

    private static String partnerEducationPath() {
        return "(" + partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE) + " IN ('attendance', 'referral') OR ("
                + partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE) + " = 'tracing' AND "
                + partnerValue(DBConstants.KEY.FOLLOW_UP_STATUS) + " = 'found'))";
    }

    private static String partnerEducationKnown() {
        String provided = partnerValue(DBConstants.KEY.PARTNER_EDUCATION_PROVIDED);
        String topics = partnerValue(DBConstants.KEY.PARTNER_EDUCATION_TOPICS);
        String phase = partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE);
        String optionalEducation = "((" + phase + " = 'referral' OR (" + phase
                + " = 'attendance' AND (" + partnerValue(DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY)
                + " = 'no' OR " + partnerValue(DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA)
                + " = '3'))) AND NOT "
                + answered("v." + DBConstants.KEY.PARTNER_EDUCATION_PROVIDED) + " AND NOT "
                + answered("v." + DBConstants.KEY.PARTNER_EDUCATION_TOPICS) + ")";
        return partnerEvidenceKnown() + " AND (NOT " + partnerEducationPath() + " OR "
                + partnerValue(DBConstants.KEY.PARTNER_MOTHER_PREGNANT) + " = 'no' OR "
                + optionalEducation + " OR "
                + provided + " = 'no' OR (" + provided + " = 'yes' AND ("
                + hasToken(topics, "hiv") + " OR " + hasToken(topics, "syphilis")
                + " OR " + hasToken(topics, "hepatitis_b") + ")))";
    }

    private Integer missingRecords(String source, String condition, String known) {
        return missingRecords("", "(" + source + ")", condition, known);
    }

    private Integer missingRecords(String commonTables, String source, String condition, String known) {
        Map<String, Integer> count = query(commonTables + " SELECT COUNT(DISTINCT v." + DBConstants.KEY.VISIT_ID
                + ") AS total FROM " + source + " v WHERE (" + condition
                + ") AND NOT coalesce((" + known + "), 0)", Collections.singletonList("total"));
        return count == null ? null : value(count, "total");
    }

    private String serviceClientSource(String source, boolean partner) {
        String gender = "lower(trim(ifnull(m." + DBConstants.KEY.GENDER + ", '')))";
        if (partner) gender = "CASE WHEN " + gender + " = 'male' THEN 'male' ELSE '' END";
        return "SELECT v.*, v." + DBConstants.KEY.BASE_ENTITY_ID + " AS client_id, " + gender
                + " AS " + DBConstants.KEY.GENDER + ", " + ageInMonths("v.service_date")
                + " AS age_months FROM (" + source + ") v LEFT JOIN ec_family_member m ON m."
                + DBConstants.KEY.BASE_ENTITY_ID + " = v." + DBConstants.KEY.BASE_ENTITY_ID;
    }

    private static String firstClientVisit(String source) {
        // Use one monthly reference date per person so a birthday cannot populate two age bands
        // for a distinct-client summary or topic row.
        return "SELECT v.* FROM (" + source + ") v WHERE NOT EXISTS (SELECT 1 FROM (" + source
                + ") n WHERE n." + DBConstants.KEY.BASE_ENTITY_ID + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID + " AND " + earlierVisit() + ")";
    }

    private static String earlierVisit() {
        return "(n.service_date < v.service_date OR (n.service_date = v.service_date AND"
                + " (coalesce(CAST(n." + DBConstants.KEY.VISIT_DATE
                + " AS INTEGER), 0) < coalesce(CAST(v." + DBConstants.KEY.VISIT_DATE
                + " AS INTEGER), 0) OR (coalesce(CAST(n." + DBConstants.KEY.VISIT_DATE
                + " AS INTEGER), 0) = coalesce(CAST(v." + DBConstants.KEY.VISIT_DATE
                + " AS INTEGER), 0) AND n." + DBConstants.KEY.VISIT_ID + " < v."
                + DBConstants.KEY.VISIT_ID + "))))";
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
        return demographicQuery(source, minimumMonths, maximumMonths, "client_id");
    }

    private String demographicQuery(
            String source, int minimumMonths, int maximumMonths, String countIdentity) {
        // Respect each workbook row's applicable age bands; expose unknown/out-of-band clients.
        String ageRange = "age_months >= " + minimumMonths + " AND age_months < " + maximumMonths;
        String eligible = ageRange + " AND " + DBConstants.KEY.GENDER + " IN ('male', 'female')";
        StringBuilder sql = new StringBuilder("SELECT ");
        appendCount(sql, eligible, "total", countIdentity);
        sql.append(", ");
        appendCount(sql, "NOT coalesce((" + eligible + "), 0)", "unclassified", countIdentity);
        for (String sex : SEXES) {
            sql.append(", ");
            appendCount(
                    sql,
                    DBConstants.KEY.GENDER + " = '" + sex + "' AND " + ageRange,
                    sex + "_total", countIdentity);
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
                        sex + "_" + AGE_BANDS[band].replace('-', '_'), countIdentity);
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
        return "SELECT COUNT(DISTINCT base_entity_id) AS total FROM (" + householdVisits() + ")";
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

    private String enrollmentDate(String id, String... eventTypes) {
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
                + " IN ('"
                + android.text.TextUtils.join("', '", eventTypes)
                + "') AND "
                + validDate(date)
                + " AND date(e."
                + DBConstants.KEY.EVENT_DATE_TIMESTAMP
                + ") IS NOT NULL)";
    }

    private String primaryAt(String date, String... enrollmentEvents) {
        return "EXISTS (SELECT 1 FROM ec_mothermentor_enrollment e WHERE e."
                + DBConstants.KEY.BASE_ENTITY_ID
                + " = v."
                + DBConstants.KEY.BASE_ENTITY_ID
                + ") AND "
                + enrollmentDate("v." + DBConstants.KEY.BASE_ENTITY_ID,
                        enrollmentEvents.length == 0 ? new String[]{PRIMARY_ENROLLMENT_EVENT} : enrollmentEvents)
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

    private void putMotherVisitIndicators(JSONObject result) throws JSONException {
        String visits = "SELECT v.*, " + serviceDate() + " AS service_date, "
                + detail(DBConstants.KEY.ATTENDANCE_TYPE) + " AS attendance_type, "
                + detail(DBConstants.KEY.TYPE_OF_REFERRAL_GIVEN) + " AS referral_services"
                + " FROM visits v WHERE " + finalizedVisitInMonth() + " AND " + notVoided()
                + " AND " + primaryAt(serviceDate());
        String newClients = "SELECT * FROM (" + visits + ") WHERE attendance_type = 'new'";
        String returning = "SELECT r.* FROM (" + visits + ") r WHERE r.attendance_type = 'repeat'"
                + " AND NOT EXISTS (SELECT 1 FROM (" + newClients + ") n WHERE n.base_entity_id = r.base_entity_id)";
        String referrals = "SELECT * FROM (" + visits + ") WHERE attendance_type IN ('new', 'repeat')"
                + " AND " + answered("referral_services") + " AND trim(referral_services) NOT IN ('[]', '{}')";
        putMotherVisitIndicator(result, "mm-1", newClients);
        putMotherVisitIndicator(result, "mm-2", returning);
        putMotherVisitIndicator(result, "mm-6", referrals);
    }

    private void putMotherVisitIndicator(JSONObject result, String key, String visits) throws JSONException {
        putDemographicIndicator(result, key, demographicQuery(serviceClientSource(firstClientVisit(visits), false)));
    }

    private void putChildTestIndicator(JSONObject result, int indicator, String stage,
            int minimumMonths, int maximumMonths) throws JSONException {
        String key = "mm-" + indicator;
        String samples = "SELECT v.* FROM (" + childTestVisits() + ") v WHERE v.test_stage = '" + stage
                + "' AND v.tested = 'yes' AND " + validDate("v.service_date")
                + " AND " + validDate("v.test_date") + " AND v.test_date <= v.service_date"
                + " AND " + inMonth("v.test_date");
        if (indicator == 10) samples += " AND v.test_result IN ('positive', 'negative')";
        String unique = "SELECT v.* FROM (" + samples + ") v WHERE NOT EXISTS (SELECT 1 FROM ("
                + samples + ") n WHERE n.base_entity_id = v.base_entity_id AND " + laterVisit() + ")";
        String demographics = "SELECT v.*, v.base_entity_id AS client_id, lower(trim(m.gender)) AS gender, "
                + ageInMonths("v.test_date") + " AS age_months FROM (" + unique
                + ") v LEFT JOIN ec_family_member m ON m.base_entity_id = v.base_entity_id";
        putDemographicIndicator(result, key, demographicQuery(demographics, minimumMonths, maximumMonths));
        String[][] breakdown = indicator == 10
                ? new String[][]{{"positive", "test_result = 'positive'"}, {"negative", "test_result = 'negative'"}}
                : new String[][]{{"boys", "gender = 'male'"}, {"girls", "gender = 'female'"}};
        for (String[] row : breakdown) {
            putDemographicIndicator(result, key + "-" + row[0], demographicQuery(
                    "SELECT * FROM (" + demographics + ") WHERE " + row[1], minimumMonths, maximumMonths));
        }
    }

    private String childTestVisits() {
        MotherMentorFollowUpType type = MotherMentorFollowUpType.CHILD_EID;
        String current = "v.visit_type = '" + type.getEventType() + "'";
        String returnFlow = detail(DBConstants.KEY.MTOTO_RETURN_FLOW) + " = '1'";
        String stage = "CASE " + detail(DBConstants.KEY.CHILD_EID_TEST_TYPE)
                + " WHEN 'six_weeks' THEN 'week_6' WHEN 'wiki6' THEN 'week_6' WHEN 'week_6' THEN 'week_6'"
                + " WHEN 'nine_months' THEN 'month_9' WHEN 'miezi9' THEN 'month_9' WHEN 'month_9' THEN 'month_9'"
                + " WHEN 'eighteen_months' THEN 'month_18' WHEN 'miezi18' THEN 'month_18' WHEN 'month_18' THEN 'month_18' END";
        return "SELECT v.*, " + serviceDate() + " AS service_date, " + stage + " AS test_stage, "
                + "CASE WHEN " + returnFlow + " THEN " + detail(DBConstants.KEY.MAMA_HEI_TEST_DONE)
                + " ELSE " + detail(DBConstants.KEY.CHILD_EID_FINAL_HIV_TEST_DONE) + " END AS tested, "
                + normalizeDate("CASE WHEN " + returnFlow + " THEN " + detail(DBConstants.KEY.MAMA_HEI_TEST_DATE)
                    + " ELSE " + detail(DBConstants.KEY.CHILD_EID_FINAL_HIV_TEST_DATE) + " END") + " AS test_date, "
                + "CASE WHEN " + returnFlow + " THEN " + detail(DBConstants.KEY.MAMA_HEI_TEST_RESULT)
                + " ELSE " + detail(DBConstants.KEY.CHILD_EID_FINAL_HIV_RESULT) + " END AS test_result"
                + " FROM visits v WHERE v.processed = 1 AND ((" + current + " AND "
                + detail(DBConstants.KEY.FOLLOW_UP_STATUS) + " = 'found')"
                + " OR (v.visit_type = '" + LEGACY_SERVICE + "' AND "
                + answered(detail(DBConstants.KEY.CHILD_EID_TEST_TYPE)) + "))"
                + " AND " + childEidAt(serviceDate()) + " AND " + notVoided();
    }

    private static String notVoided() {
        return "instr(coalesce(v.visit_json, ''), '\""
                + DBConstants.KEY.EVENT_VOIDED + "\":true') = 0";
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

    private void putTracingIndicators(JSONObject result) throws JSONException {
        MotherMentorFollowUpType type = MotherMentorFollowUpType.MOTHER_IIT;
        String status = workflowAnswer(type, DBConstants.KEY.IIT_TRACING_OUTCOME,
                detail(DBConstants.KEY.FOLLOW_UP_STATUS));
        String reason = detail(DBConstants.KEY.IIT_NOT_FOUND_REASON);
        String agreement = detail(DBConstants.KEY.IIT_RETURN_AGREEMENT);
        String returned = workflowAnswer(type, DBConstants.KEY.RETURNED_TO_ART,
                detail(DBConstants.KEY.RETURNED_TO_FACILITY));
        String returnDate = normalizeDate(workflowAnswer(type, DBConstants.KEY.ART_RETURN_DATE,
                detail(DBConstants.KEY.FACILITY_RETURN_DATE)));
        String missed = normalizeDate(detail(DBConstants.KEY.IIT_MISSED_APPOINTMENT_DATE));
        String returnState = detail(DBConstants.KEY.IIT_RETURN_STATE);
        String legacy = "v.visit_type = '" + LEGACY_SERVICE + "'";
        String outcome = "CASE WHEN (" + legacy + " OR (" + status + " = 'found' AND "
                + returnState + " = 'referred')) AND " + returned + " = 'yes' AND " + inMonth(returnDate)
                + " AND " + returnDate + " <= " + serviceDate() + " AND (NOT coalesce("
                + validDate(missed) + ", 0) OR " + returnDate + " >= " + missed + ") THEN 'returned'"
                + " WHEN " + status + " = 'found' AND ((" + returnState + " = 'none' AND "
                + agreement + " = 'agreed') OR (" + returnState + " = 'referred' AND "
                + returned + " = 'no')) THEN 'agreed'"
                + " WHEN (" + status + " = 'found' AND " + agreement + " = 'refused')"
                + " OR " + status + " IN ('refused_return', 'refused') THEN 'refused'"
                + " WHEN (" + status + " = 'not_found' AND " + reason + " = 'deceased')"
                + " OR " + status + " = 'deceased' THEN 'died'"
                + " WHEN (" + status + " = 'not_found' AND " + reason + " IN ('moved_facility', 'moved_area'))"
                + " OR " + status + " IN ('transferred', 'transferred_facility') THEN 'transferred'"
                + " WHEN " + status + " = 'lost_to_follow_up' OR (" + status + " = 'not_found' AND ("
                + legacy + " OR " + reason + " IN ('travelled', 'other'))) THEN 'not-found'"
                + " ELSE NULL END";
        String visits = "SELECT v.*, " + serviceDate() + " AS service_date, " + outcome
                + " AS outcome FROM visits v WHERE " + finalizedVisitInMonth(LEGACY_SERVICE, type.getEventType())
                + " AND " + iitAt(serviceDate()) + " AND " + notVoided()
                + " AND (" + workflowAnswer(type, DBConstants.KEY.IIT_TRACING_VISIT, "'yes'") + " = 'yes')";
        String latest = "WITH tracing AS (" + visits + "), latest AS (SELECT v.* FROM tracing v"
                + " WHERE NOT EXISTS (SELECT 1 FROM tracing n WHERE n.base_entity_id = v.base_entity_id"
                + " AND " + laterVisit() + ")) ";
        Integer missing = missingRecords(latest, "latest", "1 = 1", "v.outcome IS NOT NULL");
        if (missing != null) result.put("mm-11-missing-records", missing);
        String classified = "SELECT * FROM latest WHERE outcome IS NOT NULL";
        putDemographicIndicator(result, "mm-11", latest + demographicQuery(
                serviceClientSource(classified, false), 180, Integer.MAX_VALUE));
        for (String row : TRACING_OUTCOMES) {
            putDemographicIndicator(result, "mm-11-" + row, latest + demographicQuery(
                    serviceClientSource(classified + " AND outcome = '" + row + "'", false), 180, Integer.MAX_VALUE));
        }
    }

    private String householdVisits() {
        String status = detail(DBConstants.KEY.HOUSEHOLD_FOLLOW_UP_STATUS);
        String visitType = detail(DBConstants.KEY.HOUSEHOLD_VISIT_TYPE);
        String schema = detail(DBConstants.KEY.HOUSEHOLD_FOLLOW_UP_SCHEMA);
        String found = "(" + status + " = 'found' OR (coalesce(" + status + ", '') = '' AND ("
                + visitType + " = 'new' OR (coalesce(" + schema + ", '') <> '2' AND "
                + answered(detail(DBConstants.KEY.PURPOSE_OF_VISIT)) + "))))";
        return "SELECT v.* FROM visits v WHERE " + finalizedVisitInMonth() + " AND " + notVoided()
                + " AND EXISTS (SELECT 1 FROM ec_mothermentor_household_enrolment h"
                + " WHERE h.base_entity_id = v.base_entity_id AND lower(trim(h.consent)) = 'ndiyo')"
                + " AND " + found;
    }

    String buildHouseholdCounsellingQuery() {
        return MotherMentorHouseholdCounsellingDao.reportQuery(householdVisits());
    }

    private void putHouseholdCounsellingIndicator(JSONObject result) throws JSONException {
        Map<String, Integer> counts = query(buildHouseholdCounsellingQuery(),
                MotherMentorHouseholdCounsellingDao.REPORT_COLUMNS);
        if (counts == null) return;
        result.put("mm-15-missing-records", value(counts, "missing"));
        result.put("mm-15-known-total", value(counts, "total"));
        result.put("mm-15-role-visits", value(counts, "role_visits"));
        result.put("mm-15-legacy-visits", value(counts, "legacy_visits"));
        result.put("mm-15-legacy-male-total", value(counts, "male_total"));
        result.put("mm-15-legacy-female-total", value(counts, "female_total"));
        if (value(counts, "missing") > 0) return;
        result.put("mm-15", value(counts, "total"));
        // Role counts do not capture a complete sex breakdown for the reporting month.
        if (value(counts, "role_visits") == 0) {
            result.put("mm-15-male-total", value(counts, "male_total"));
            result.put("mm-15-female-total", value(counts, "female_total"));
        }
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
            result.put("mm-16", value(counts, "topics"));
            result.put("mm-17", value(counts, "activities"));
        }
        putAggregateIndicator(
                result,
                "mm-18",
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
                    "mm-19",
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
                result.put("mm-19", 0);
                result.put("mm-19-male-total", 0);
                result.put("mm-19-female-total", 0);
            } else if (existing != null)
                result.put("mm-19-missing-records", value(existing, "total"));
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
                + " GLOB '[0-9][0-9]-[0-9][0-9]-[0-9][0-9][0-9][0-9]' OR "
                + column
                + " GLOB '[0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]' THEN substr("
                + column
                + ", 7, 4) || '-' || substr("
                + column
                + ", 4, 2) || '-' || substr("
                + column
                + ", 1, 2) ELSE "
                + column
                + " END";
    }

    private static void appendCount(
            StringBuilder sql, String condition, String alias, String countIdentity) {
        sql.append("COUNT(DISTINCT CASE WHEN ")
                .append(condition)
                .append(" THEN ").append(countIdentity).append(" END) AS ")
                .append(alias);
    }

    private String monthStart() {
        return new SimpleDateFormat("yyyy-MM-01", Locale.ENGLISH).format(getReportDate());
    }

    private String ageInMonths() {
        return ageInMonths("date('" + monthStart() + "', '+1 month', '-1 day')");
    }

    private String ageInMonths(String referenceDate) {
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
                + referenceDate
                + " THEN (CAST(strftime('%Y', "
                + referenceDate
                + ") AS INTEGER) - CAST(strftime('%Y', "
                + dob
                + ") AS INTEGER)) * 12 + CAST(strftime('%m', "
                + referenceDate
                + ") AS INTEGER) - CAST(strftime('%m', "
                + dob
                + ") AS INTEGER) - CASE WHEN strftime('%d', "
                + referenceDate
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
