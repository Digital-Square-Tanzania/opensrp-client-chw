package org.smartregister.chw.domain.harm_reduction_sober_house_reports;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HarmReductionSoberHouseReportObject extends ReportObject {

    private static final SimpleDateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private static final String SERVICE_ID_COLUMN = "ehshs.entity_id";
    private static final String ENROLLMENT_ID_COLUMN = "ehshe.base_entity_id";

    private static final String SERVICE_FROM_CLAUSE =
            "ec_harm_reduction_sober_house_services ehshs " +
                    "LEFT JOIN ec_family_member efm " +
                    "ON efm.base_entity_id = ehshs.entity_id AND efm.date_removed IS NULL";

    private static final String SERVICE_WITH_ENROLLMENT_FROM_CLAUSE =
            SERVICE_FROM_CLAUSE + " " +
                    "INNER JOIN ec_harm_reduction_sober_house_enrollment ehshe " +
                    "ON ehshe.base_entity_id = ehshs.entity_id";

    private static final String ENROLLMENT_FROM_CLAUSE =
            "ec_harm_reduction_sober_house_enrollment ehshe " +
                    "LEFT JOIN ec_family_member efm " +
                    "ON efm.base_entity_id = ehshe.base_entity_id AND efm.date_removed IS NULL";

    private static final String HIV_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.hiv_result, '')) = 'positive' AND (" +
                    "lower(ifnull(ehshe.enrolled_into_ctc_services, '')) = 'yes' OR " +
                    "trim(ifnull(ehshe.ctc_id, '')) <> ''" +
                    ")";
    private static final String STIS_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.stis_result, '')) = 'has_symptoms' AND " +
                    "lower(ifnull(ehshe.stis_treatment_after_screening, '')) = 'yes'";
    private static final String HEART_DISEASES_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.heart_diseases_result, '')) = 'has_symptoms' AND " +
                    "lower(ifnull(ehshe.heart_diseases_treatment_after_screening, '')) = 'yes'";
    private static final String MENTAL_HEALTH_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.mental_health_result, '')) = 'has_symptoms' AND " +
                    "lower(ifnull(ehshe.mental_health_treatment_after_screening, '')) = 'yes'";
    private static final String HEPATITIS_B_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.hepatitis_b_result, '')) = 'has_symptoms' AND " +
                    "lower(ifnull(ehshe.hepatitis_b_treatment_after_screening, '')) = 'yes'";
    private static final String HEPATITIS_C_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.hepatitis_c_result, '')) = 'has_symptoms' AND " +
                    "lower(ifnull(ehshe.hepatitis_c_treatment_after_screening, '')) = 'yes'";
    private static final String OTHER_HEPATITIS_PROXY_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.other_conditions_specify, '')) LIKE '%hepat%' AND " +
                    "lower(ifnull(ehshe.other_conditions_treatment_after_screening, '')) = 'yes'";
    private static final String DIABETES_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.diabetes_result, '')) = 'has_symptoms' AND " +
                    "lower(ifnull(ehshe.diabetes_treatment_after_screening, '')) = 'yes'";
    private static final String TUBERCULOSIS_TREATMENT_CONDITION =
            "lower(ifnull(ehshe.tuberculosis_result, '')) = 'has_symptoms' AND " +
                    "lower(ifnull(ehshe.tuberculosis_treatment_after_screening, '')) = 'yes'";
    private static final String OTHER_CONDITIONS_TREATMENT_CONDITION =
            "trim(ifnull(ehshe.other_conditions_specify, '')) <> '' AND " +
                    "lower(ifnull(ehshe.other_conditions_specify, '')) NOT LIKE '%hepat%' AND " +
                    "lower(ifnull(ehshe.other_conditions_treatment_after_screening, '')) = 'yes'";
    private static final String ANY_REPORTED_CONDITION_TREATED_CONDITION =
            "(" +
                    HIV_TREATMENT_CONDITION + " OR " +
                    STIS_TREATMENT_CONDITION + " OR " +
                    HEART_DISEASES_TREATMENT_CONDITION + " OR " +
                    MENTAL_HEALTH_TREATMENT_CONDITION + " OR " +
                    HEPATITIS_B_TREATMENT_CONDITION + " OR " +
                    HEPATITIS_C_TREATMENT_CONDITION + " OR " +
                    OTHER_HEPATITIS_PROXY_TREATMENT_CONDITION + " OR " +
                    DIABETES_TREATMENT_CONDITION + " OR " +
                    TUBERCULOSIS_TREATMENT_CONDITION + " OR " +
                    OTHER_CONDITIONS_TREATMENT_CONDITION +
                    ")";

    private static final List<BreakdownColumn> BREAKDOWN_COLUMNS = createBreakdownColumns();
    private static final List<String> RESULT_COLUMNS = createResultColumns();
    private static final List<IndicatorDefinition> INDICATOR_DEFINITIONS = createIndicatorDefinitions();

    public HarmReductionSoberHouseReportObject(Date reportDate) {
        super(reportDate);
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        String reportMonth = QUERY_DATE_FORMAT.format(getReportDate());

        for (IndicatorDefinition definition : INDICATOR_DEFINITIONS) {
            Map<String, Integer> breakdown = definition.isZeroOnly()
                    ? Collections.emptyMap()
                    : ReportDao.getReportBreakdown(buildIndicatorQuery(definition, reportMonth), RESULT_COLUMNS);
            putIndicatorValues(indicatorDataObject, definition.getKey(), breakdown);
        }

        return indicatorDataObject;
    }

    private void putIndicatorValues(JSONObject indicatorDataObject, String indicatorKey, Map<String, Integer> breakdown) throws JSONException {
        indicatorDataObject.put(indicatorKey, getValueOrZero(breakdown, "total"));
        for (BreakdownColumn breakdownColumn : BREAKDOWN_COLUMNS) {
            indicatorDataObject.put(
                    indicatorKey + "-" + breakdownColumn.getJsonSuffix(),
                    getValueOrZero(breakdown, breakdownColumn.getColumnAlias())
            );
        }
    }

    private int getValueOrZero(Map<String, Integer> breakdown, String key) {
        Integer value = breakdown.get(key);
        return value == null ? 0 : value;
    }

    @NonNull
    private String buildIndicatorQuery(IndicatorDefinition definition, String reportMonth) {
        StringBuilder queryBuilder = new StringBuilder()
                .append("SELECT COUNT(DISTINCT ")
                .append(definition.getIdColumn())
                .append(") AS total");

        String ageExpression = buildAgeExpression(reportMonth);
        for (BreakdownColumn breakdownColumn : BREAKDOWN_COLUMNS) {
            queryBuilder.append(", COUNT(DISTINCT CASE WHEN ")
                    .append(breakdownColumn.getCondition(ageExpression))
                    .append(" THEN ")
                    .append(definition.getIdColumn())
                    .append(" END) AS ")
                    .append(breakdownColumn.getColumnAlias());
        }

        queryBuilder.append(" FROM ")
                .append(definition.getFromClause())
                .append(" WHERE ")
                .append(definition.getWhereClause())
                .append(" AND ")
                .append(buildMonthClause(definition.getMonthColumn(), reportMonth));

        return queryBuilder.toString();
    }

    @NonNull
    private String buildAgeExpression(String reportMonth) {
        return "CAST((julianday(date('" + reportMonth + "', 'start of month', '+1 month', '-1 day')) - " +
                "julianday(efm.dob)) / 365.25 AS INTEGER)";
    }

    @NonNull
    private String buildMonthClause(String monthColumn, String reportMonth) {
        return "strftime('%Y-%m', datetime(" + monthColumn + " / 1000, 'unixepoch', 'localtime')) = " +
                "strftime('%Y-%m', '" + reportMonth + "')";
    }

    @NonNull
    private static List<BreakdownColumn> createBreakdownColumns() {
        return Collections.unmodifiableList(Arrays.asList(
                new BreakdownColumn("male_total", "male-total", "lower(ifnull(efm.gender, '')) = 'male'"),
                new BreakdownColumn("male_18_25", "male-18-25", "lower(ifnull(efm.gender, '')) = 'male' AND %s BETWEEN 18 AND 25"),
                new BreakdownColumn("male_26_35", "male-26-35", "lower(ifnull(efm.gender, '')) = 'male' AND %s BETWEEN 26 AND 35"),
                new BreakdownColumn("male_36_45", "male-36-45", "lower(ifnull(efm.gender, '')) = 'male' AND %s BETWEEN 36 AND 45"),
                new BreakdownColumn("male_46_55", "male-46-55", "lower(ifnull(efm.gender, '')) = 'male' AND %s BETWEEN 46 AND 55"),
                // Keep the age buckets non-overlapping while preserving the source form label.
                new BreakdownColumn("male_55_plus", "male-55-plus", "lower(ifnull(efm.gender, '')) = 'male' AND %s >= 56"),
                new BreakdownColumn("female_total", "female-total", "lower(ifnull(efm.gender, '')) = 'female'"),
                new BreakdownColumn("female_18_25", "female-18-25", "lower(ifnull(efm.gender, '')) = 'female' AND %s BETWEEN 18 AND 25"),
                new BreakdownColumn("female_26_35", "female-26-35", "lower(ifnull(efm.gender, '')) = 'female' AND %s BETWEEN 26 AND 35"),
                new BreakdownColumn("female_36_45", "female-36-45", "lower(ifnull(efm.gender, '')) = 'female' AND %s BETWEEN 36 AND 45"),
                new BreakdownColumn("female_46_55", "female-46-55", "lower(ifnull(efm.gender, '')) = 'female' AND %s BETWEEN 46 AND 55"),
                new BreakdownColumn("female_56_plus", "female-56-plus", "lower(ifnull(efm.gender, '')) = 'female' AND %s >= 56")
        ));
    }

    @NonNull
    private static List<String> createResultColumns() {
        return Collections.unmodifiableList(Arrays.asList(
                "total",
                "male_total",
                "male_18_25",
                "male_26_35",
                "male_36_45",
                "male_46_55",
                "male_55_plus",
                "female_total",
                "female_18_25",
                "female_26_35",
                "female_36_45",
                "female_46_55",
                "female_56_plus"
        ));
    }

    @NonNull
    private static List<IndicatorDefinition> createIndicatorDefinitions() {
        return Collections.unmodifiableList(Arrays.asList(
                serviceIndicator("sh-1", "1 = 1"),
                serviceEnrollmentIndicator("sh-2", "trim(ifnull(ehshe.education_level, '')) <> ''"),
                serviceEnrollmentIndicator("sh-2a", "lower(ifnull(ehshe.education_level, '')) = 'no_education'"),
                serviceEnrollmentIndicator("sh-2b", "lower(ifnull(ehshe.education_level, '')) = 'incomplete_primary'"),
                serviceEnrollmentIndicator("sh-2c", "lower(ifnull(ehshe.education_level, '')) = 'completed_primary'"),
                serviceEnrollmentIndicator("sh-2d", "lower(ifnull(ehshe.education_level, '')) = 'completed_secondary'"),
                serviceEnrollmentIndicator("sh-2e", "lower(ifnull(ehshe.education_level, '')) = 'university'"),
                serviceEnrollmentIndicator("sh-4", "trim(ifnull(ehshe.nationality, '')) <> ''"),
                serviceEnrollmentIndicator("sh-4a", "lower(ifnull(ehshe.nationality, '')) = 'tanzanian'"),
                serviceEnrollmentIndicator("sh-4b", "lower(ifnull(ehshe.nationality, '')) = 'non_tanzanian'"),
                zeroIndicator("sh-5"),
                zeroIndicator("sh-5a"),
                zeroIndicator("sh-5b"),
                zeroIndicator("sh-5c"),
                zeroIndicator("sh-5d"),
                zeroIndicator("sh-5e"),
                serviceIndicator("sh-6", "trim(ifnull(efm.marital_status, '')) <> ''"),
                serviceIndicator("sh-6a", "lower(ifnull(efm.marital_status, '')) = 'single'"),
                serviceIndicator("sh-6b", "lower(ifnull(efm.marital_status, '')) IN ('married', 'cohabitation')"),
                serviceIndicator("sh-6c", "lower(ifnull(efm.marital_status, '')) = 'divorced'"),
                serviceIndicator("sh-6d", "lower(ifnull(efm.marital_status, '')) = 'widowed'"),
                serviceEnrollmentIndicator("sh-7", "trim(ifnull(ehshe.substances_leading_to_services, '')) <> ''"),
                serviceEnrollmentIndicator("sh-7a", "lower(ifnull(ehshe.substances_leading_to_services, '')) LIKE '%alcohol%'"),
                serviceEnrollmentIndicator("sh-7b", "lower(ifnull(ehshe.substances_leading_to_services, '')) LIKE '%cannabis%'"),
                serviceEnrollmentIndicator("sh-7c", "lower(ifnull(ehshe.substances_leading_to_services, '')) LIKE '%khat%'"),
                serviceEnrollmentIndicator("sh-7d", "lower(ifnull(ehshe.substances_leading_to_services, '')) LIKE '%heroin%'"),
                serviceEnrollmentIndicator("sh-7e", "lower(ifnull(ehshe.substances_leading_to_services, '')) LIKE '%valium_diazepam%'"),
                serviceEnrollmentIndicator("sh-7f", "lower(ifnull(ehshe.substances_leading_to_services, '')) LIKE '%tramadol%'"),
                serviceEnrollmentIndicator("sh-7g", "lower(ifnull(ehshe.substances_leading_to_services, '')) LIKE '%methamphetamine%'"),
                serviceEnrollmentIndicator("sh-7h",
                        "(lower(ifnull(ehshe.substances_leading_to_services, '')) LIKE '%other%' OR " +
                                "trim(ifnull(ehshe.other_substances_specify, '')) <> '')"),
                enrollmentIndicator("sh-8",
                        "(" +
                                "lower(ifnull(ehshe.hiv_result, '')) = 'positive' OR " +
                                "lower(ifnull(ehshe.stis_result, '')) = 'has_symptoms' OR " +
                                "lower(ifnull(ehshe.heart_diseases_result, '')) = 'has_symptoms' OR " +
                                "lower(ifnull(ehshe.mental_health_result, '')) = 'has_symptoms' OR " +
                                "lower(ifnull(ehshe.hepatitis_b_result, '')) = 'has_symptoms' OR " +
                                "lower(ifnull(ehshe.hepatitis_c_result, '')) = 'has_symptoms' OR " +
                                "lower(ifnull(ehshe.diabetes_result, '')) = 'has_symptoms' OR " +
                                "lower(ifnull(ehshe.tuberculosis_result, '')) = 'has_symptoms' OR " +
                                "trim(ifnull(ehshe.other_conditions_specify, '')) <> ''" +
                                ")"),
                enrollmentIndicator("sh-8a", "lower(ifnull(ehshe.hiv_result, '')) = 'positive'"),
                enrollmentIndicator("sh-8b", "lower(ifnull(ehshe.stis_result, '')) = 'has_symptoms'"),
                enrollmentIndicator("sh-8c", "lower(ifnull(ehshe.heart_diseases_result, '')) = 'has_symptoms'"),
                enrollmentIndicator("sh-8d", "lower(ifnull(ehshe.mental_health_result, '')) = 'has_symptoms'"),
                enrollmentIndicator("sh-8e", "lower(ifnull(ehshe.hepatitis_b_result, '')) = 'has_symptoms'"),
                enrollmentIndicator("sh-8f", "lower(ifnull(ehshe.hepatitis_c_result, '')) = 'has_symptoms'"),
                enrollmentIndicator("sh-8g", "lower(ifnull(ehshe.other_conditions_specify, '')) LIKE '%hepat%'"),
                enrollmentIndicator("sh-8h", "lower(ifnull(ehshe.diabetes_result, '')) = 'has_symptoms'"),
                enrollmentIndicator("sh-8i", "lower(ifnull(ehshe.tuberculosis_result, '')) = 'has_symptoms'"),
                enrollmentIndicator("sh-8j",
                        "trim(ifnull(ehshe.other_conditions_specify, '')) <> '' AND " +
                                "lower(ifnull(ehshe.other_conditions_specify, '')) NOT LIKE '%hepat%'"),
                enrollmentIndicator("sh-9", ANY_REPORTED_CONDITION_TREATED_CONDITION),
                enrollmentIndicator("sh-9a", HIV_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9b", STIS_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9c", HEART_DISEASES_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9d", MENTAL_HEALTH_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9e", HEPATITIS_B_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9f", HEPATITIS_C_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9g", OTHER_HEPATITIS_PROXY_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9h", DIABETES_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9i", TUBERCULOSIS_TREATMENT_CONDITION),
                enrollmentIndicator("sh-9j", OTHER_CONDITIONS_TREATMENT_CONDITION),
                serviceIndicator("sh-10",
                        "(" +
                                "lower(ifnull(ehshs.follow_up_status, '')) IN ('absconded', 'died') OR " +
                                "lower(ifnull(ehshs.client_type, '')) = 'relapsed_client'" +
                                ")"),
                serviceIndicator("sh-10a", "lower(ifnull(ehshs.follow_up_status, '')) = 'absconded'"),
                serviceIndicator("sh-10b", "lower(ifnull(ehshs.client_type, '')) = 'relapsed_client'"),
                serviceIndicator("sh-10c", "lower(ifnull(ehshs.follow_up_status, '')) = 'died'")
        ));
    }

    @NonNull
    private static IndicatorDefinition serviceIndicator(String key, String whereClause) {
        return new IndicatorDefinition(key, SERVICE_FROM_CLAUSE, SERVICE_ID_COLUMN, "ehshs.last_interacted_with", whereClause, false);
    }

    @NonNull
    private static IndicatorDefinition serviceEnrollmentIndicator(String key, String whereClause) {
        return new IndicatorDefinition(key, SERVICE_WITH_ENROLLMENT_FROM_CLAUSE, SERVICE_ID_COLUMN, "ehshs.last_interacted_with", whereClause, false);
    }

    @NonNull
    private static IndicatorDefinition enrollmentIndicator(String key, String whereClause) {
        return new IndicatorDefinition(key, ENROLLMENT_FROM_CLAUSE, ENROLLMENT_ID_COLUMN, "ehshe.last_interacted_with", whereClause, false);
    }

    @NonNull
    private static IndicatorDefinition zeroIndicator(String key) {
        return new IndicatorDefinition(key, "", "", "", "", true);
    }

    private static class BreakdownColumn {
        private final String columnAlias;
        private final String jsonSuffix;
        private final String conditionTemplate;

        BreakdownColumn(String columnAlias, String jsonSuffix, String conditionTemplate) {
            this.columnAlias = columnAlias;
            this.jsonSuffix = jsonSuffix;
            this.conditionTemplate = conditionTemplate;
        }

        String getColumnAlias() {
            return columnAlias;
        }

        String getJsonSuffix() {
            return jsonSuffix;
        }

        String getCondition(String ageExpression) {
            return String.format(Locale.ENGLISH, conditionTemplate, ageExpression);
        }
    }

    private static class IndicatorDefinition {
        private final String key;
        private final String fromClause;
        private final String idColumn;
        private final String monthColumn;
        private final String whereClause;
        private final boolean zeroOnly;

        IndicatorDefinition(String key, String fromClause, String idColumn, String monthColumn, String whereClause, boolean zeroOnly) {
            this.key = key;
            this.fromClause = fromClause;
            this.idColumn = idColumn;
            this.monthColumn = monthColumn;
            this.whereClause = whereClause;
            this.zeroOnly = zeroOnly;
        }

        String getKey() {
            return key;
        }

        String getFromClause() {
            return fromClause;
        }

        String getIdColumn() {
            return idColumn;
        }

        String getMonthColumn() {
            return monthColumn;
        }

        String getWhereClause() {
            return whereClause;
        }

        boolean isZeroOnly() {
            return zeroOnly;
        }
    }
}
