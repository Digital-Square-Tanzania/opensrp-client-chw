package org.smartregister.chw.domain.harm_reduction_reports;

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

public class HarmReductionReportObject extends ReportObject {

    private static final SimpleDateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private static final String RISK_ASSESSMENT_ID_COLUMN = "ehra.base_entity_id";
    private static final String FOLLOWUP_ID_COLUMN = "ehfv.entity_id";
    private static final String SOURCE_ID_COLUMN = "source.client_id";

    private static final String FAMILY_MEMBER_JOIN = " LEFT JOIN ec_family_member efm ";
    private static final String RISK_ASSESSMENT_FROM_CLAUSE =
            "ec_harm_reduction_risk_assessment ehra" + FAMILY_MEMBER_JOIN +
                    "ON efm.base_entity_id = ehra.base_entity_id AND efm.date_removed IS NULL";
    private static final String FOLLOWUP_FROM_CLAUSE =
            "ec_harm_reduction_followup_visit ehfv" + FAMILY_MEMBER_JOIN +
                    "ON efm.base_entity_id = ehfv.entity_id AND efm.date_removed IS NULL";
    private static final String MAT_REFERRAL_FROM_CLAUSE =
            "(SELECT ehfv.entity_id AS client_id, " +
                    "CASE WHEN lower(ifnull(ehfv.is_idu, '')) = 'yes' OR lower(ifnull(ehfv.substance_use_methods, '')) LIKE '%injecting%' " +
                    "THEN 'idu' ELSE 'nidu' END AS roc_group_type, " +
                    "ehfv.last_interacted_with AS last_interacted_with " +
                    "FROM ec_harm_reduction_followup_visit ehfv " +
                    "WHERE lower(ifnull(ehfv.referrals_provided, '')) LIKE '%methadone_services%' " +
                    "OR lower(ifnull(ehfv.linkage_to_other_services, '')) LIKE '%methadone_services%' " +
                    "OR lower(ifnull(ehfv.roc_consent_joining_mat_services, '')) = 'yes' " +
                    "UNION " +
                    "SELECT ehra.base_entity_id AS client_id, " +
                    "CASE WHEN lower(ifnull(ehra.roc_group_type, '')) = 'idu' OR lower(ifnull(ehra.route_of_substance_use, '')) LIKE '%injecting%' " +
                    "THEN 'idu' ELSE 'nidu' END AS roc_group_type, " +
                    "ehra.last_interacted_with AS last_interacted_with " +
                    "FROM ec_harm_reduction_risk_assessment ehra " +
                    "WHERE lower(ifnull(ehra.client_started_mat, '')) = 'yes') source" + FAMILY_MEMBER_JOIN +
                    "ON efm.base_entity_id = source.client_id AND efm.date_removed IS NULL";

    private static final String RISK_ASSESSMENT_IDU_CONDITION =
            "(lower(ifnull(ehra.roc_group_type, '')) = 'idu' OR lower(ifnull(ehra.route_of_substance_use, '')) LIKE '%injecting%')";
    private static final String RISK_ASSESSMENT_NIDU_CONDITION =
            "NOT " + RISK_ASSESSMENT_IDU_CONDITION;
    private static final String FOLLOWUP_IDU_CONDITION =
            "(lower(ifnull(ehfv.is_idu, '')) = 'yes' OR lower(ifnull(ehfv.substance_use_methods, '')) LIKE '%injecting%')";
    private static final String FOLLOWUP_NIDU_CONDITION =
            "NOT " + FOLLOWUP_IDU_CONDITION;
    private static final String SOURCE_IDU_CONDITION =
            "lower(ifnull(source.roc_group_type, '')) = 'idu'";
    private static final String SOURCE_NIDU_CONDITION =
            "lower(ifnull(source.roc_group_type, '')) = 'nidu'";
    private static final String HEPATITIS_SCREENING_HAS_VALUE_CONDITION =
            "(trim(ifnull(ehfv.hepatitis_bc_screening, '')) <> '' OR " +
                    "trim(ifnull(ehfv.hepatitis_b_screening, '')) <> '' OR " +
                    "trim(ifnull(ehfv.hepatitis_c_screening, '')) <> '')";
    private static final String HEPATITIS_SCREENING_REQUIRES_REFERRAL_CONDITION =
            "(lower(ifnull(ehfv.hepatitis_bc_screening, '')) IN ('has_symptoms', 'undergoing_treatment') OR " +
                    "lower(ifnull(ehfv.hepatitis_b_screening, '')) IN ('has_symptoms', 'undergoing_treatment') OR " +
                    "lower(ifnull(ehfv.hepatitis_c_screening, '')) IN ('has_symptoms', 'undergoing_treatment'))";
    private static final String LINKAGE_SERVICES_SELECTED_CONDITION =
            "(trim(ifnull(ehfv.linkage_to_other_services, '')) <> '' AND " +
                    "lower(ifnull(ehfv.linkage_to_other_services, '')) NOT LIKE '%none%')";

    private static final String RECEIVED_HR_SERVICE_CONDITION =
            "(trim(ifnull(ehfv.health_education_provided, '')) <> '' OR " +
                    "trim(ifnull(ehfv.iec_materials_provided, '')) <> '' OR " +
                    "trim(ifnull(ehfv.safe_injection_tools, '')) <> '' OR " +
                    "trim(ifnull(ehfv.referrals_provided, '')) <> '' OR " +
                    LINKAGE_SERVICES_SELECTED_CONDITION + " OR " +
                    "trim(ifnull(ehfv.hiv_tested, '')) <> '' OR " +
                    "trim(ifnull(ehfv.tb_screening, '')) <> '' OR " +
                    "trim(ifnull(ehfv.stds_screening, '')) <> '' OR " +
                    HEPATITIS_SCREENING_HAS_VALUE_CONDITION + ")";
    private static final String IDU_RECEIVED_SYRINGES_CONDITION =
            FOLLOWUP_IDU_CONDITION + " AND (" +
                    "lower(ifnull(ehfv.safe_injection_tools, '')) LIKE '%syringes%' OR " +
                    numericExpression("ehfv.qty_syringes") + " > 0)";

    private static final List<BreakdownColumn> BREAKDOWN_COLUMNS = createBreakdownColumns();
    private static final List<String> RESULT_COLUMNS = createResultColumns();
    private static final List<IndicatorDefinition> INDICATOR_DEFINITIONS = createIndicatorDefinitions();

    public HarmReductionReportObject(Date reportDate) {
        super(reportDate);
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        String reportMonth = QUERY_DATE_FORMAT.format(getReportDate());

        for (IndicatorDefinition definition : INDICATOR_DEFINITIONS) {
            Map<String, Integer> breakdown = ReportDao.getReportBreakdown(
                    buildIndicatorQuery(definition, reportMonth),
                    RESULT_COLUMNS
            );
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
        StringBuilder queryBuilder = new StringBuilder("SELECT ");
        appendAggregateSelect(queryBuilder, definition, "1 = 1", "total");

        String ageExpression = buildAgeExpression(reportMonth);
        for (BreakdownColumn breakdownColumn : BREAKDOWN_COLUMNS) {
            queryBuilder.append(", ");
            appendAggregateSelect(
                    queryBuilder,
                    definition,
                    breakdownColumn.getCondition(definition, ageExpression),
                    breakdownColumn.getColumnAlias()
            );
        }

        queryBuilder.append(" FROM ")
                .append(definition.getFromClause())
                .append(" WHERE ")
                .append(definition.getWhereClause())
                .append(" AND ")
                .append(buildMonthClause(definition.getMonthColumn(), reportMonth));

        return queryBuilder.toString();
    }

    private void appendAggregateSelect(StringBuilder queryBuilder,
                                       IndicatorDefinition definition,
                                       String condition,
                                       String alias) {
        if (definition.isSum()) {
            queryBuilder.append("COALESCE(SUM(CASE WHEN ")
                    .append(condition)
                    .append(" THEN ")
                    .append(definition.getValueExpression())
                    .append(" ELSE 0 END), 0) AS ")
                    .append(alias);
            return;
        }

        queryBuilder.append("COUNT(DISTINCT CASE WHEN ")
                .append(condition)
                .append(" THEN ")
                .append(definition.getIdColumn())
                .append(" END) AS ")
                .append(alias);
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

    private static String numericExpression(String column) {
        return "CAST(CASE WHEN trim(ifnull(" + column + ", '')) = '' THEN '0' ELSE " + column + " END AS INTEGER)";
    }

    @NonNull
    private static List<BreakdownColumn> createBreakdownColumns() {
        return Collections.unmodifiableList(Arrays.asList(
                new BreakdownColumn("idu_female_le_15", "idu-female-le-15", "idu", "lower(ifnull(efm.gender, '')) = 'female' AND %s <= 15"),
                new BreakdownColumn("idu_female_15_24", "idu-female-15-24", "idu", "lower(ifnull(efm.gender, '')) = 'female' AND %s BETWEEN 16 AND 24"),
                new BreakdownColumn("idu_female_25_49", "idu-female-25-49", "idu", "lower(ifnull(efm.gender, '')) = 'female' AND %s BETWEEN 25 AND 49"),
                new BreakdownColumn("idu_female_50_plus", "idu-female-50-plus", "idu", "lower(ifnull(efm.gender, '')) = 'female' AND %s >= 50"),
                new BreakdownColumn("idu_female_total", "idu-female-total", "idu", "lower(ifnull(efm.gender, '')) = 'female'"),
                new BreakdownColumn("idu_male_le_18", "idu-male-le-18", "idu", "lower(ifnull(efm.gender, '')) = 'male' AND %s <= 18"),
                new BreakdownColumn("idu_male_19_24", "idu-male-19-24", "idu", "lower(ifnull(efm.gender, '')) = 'male' AND %s BETWEEN 19 AND 24"),
                new BreakdownColumn("idu_male_25_49", "idu-male-25-49", "idu", "lower(ifnull(efm.gender, '')) = 'male' AND %s BETWEEN 25 AND 49"),
                new BreakdownColumn("idu_male_50_plus", "idu-male-50-plus", "idu", "lower(ifnull(efm.gender, '')) = 'male' AND %s >= 50"),
                new BreakdownColumn("idu_male_total", "idu-male-total", "idu", "lower(ifnull(efm.gender, '')) = 'male'"),
                new BreakdownColumn("idu_total", "idu-total", "idu", "1 = 1"),
                new BreakdownColumn("nidu_female_le_15", "nidu-female-le-15", "nidu", "lower(ifnull(efm.gender, '')) = 'female' AND %s <= 15"),
                new BreakdownColumn("nidu_female_15_24", "nidu-female-15-24", "nidu", "lower(ifnull(efm.gender, '')) = 'female' AND %s BETWEEN 16 AND 24"),
                new BreakdownColumn("nidu_female_25_49", "nidu-female-25-49", "nidu", "lower(ifnull(efm.gender, '')) = 'female' AND %s BETWEEN 25 AND 49"),
                new BreakdownColumn("nidu_female_50_plus", "nidu-female-50-plus", "nidu", "lower(ifnull(efm.gender, '')) = 'female' AND %s >= 50"),
                new BreakdownColumn("nidu_female_total", "nidu-female-total", "nidu", "lower(ifnull(efm.gender, '')) = 'female'"),
                new BreakdownColumn("nidu_male_le_15", "nidu-male-le-15", "nidu", "lower(ifnull(efm.gender, '')) = 'male' AND %s <= 15"),
                new BreakdownColumn("nidu_male_15_24", "nidu-male-15-24", "nidu", "lower(ifnull(efm.gender, '')) = 'male' AND %s BETWEEN 16 AND 24"),
                new BreakdownColumn("nidu_male_25_49", "nidu-male-25-49", "nidu", "lower(ifnull(efm.gender, '')) = 'male' AND %s BETWEEN 25 AND 49"),
                new BreakdownColumn("nidu_male_50_plus", "nidu-male-50-plus", "nidu", "lower(ifnull(efm.gender, '')) = 'male' AND %s >= 50"),
                new BreakdownColumn("nidu_male_total", "nidu-male-total", "nidu", "lower(ifnull(efm.gender, '')) = 'male'"),
                new BreakdownColumn("nidu_total", "nidu-total", "nidu", "1 = 1")
        ));
    }

    @NonNull
    private static List<String> createResultColumns() {
        return Collections.unmodifiableList(Arrays.asList(
                "total",
                "idu_female_le_15",
                "idu_female_15_24",
                "idu_female_25_49",
                "idu_female_50_plus",
                "idu_female_total",
                "idu_male_le_18",
                "idu_male_19_24",
                "idu_male_25_49",
                "idu_male_50_plus",
                "idu_male_total",
                "idu_total",
                "nidu_female_le_15",
                "nidu_female_15_24",
                "nidu_female_25_49",
                "nidu_female_50_plus",
                "nidu_female_total",
                "nidu_male_le_15",
                "nidu_male_15_24",
                "nidu_male_25_49",
                "nidu_male_50_plus",
                "nidu_male_total",
                "nidu_total"
        ));
    }

    @NonNull
    private static List<IndicatorDefinition> createIndicatorDefinitions() {
        return Collections.unmodifiableList(Arrays.asList(
                riskAssessmentCount("hr-1", "ifnull(ehra.is_closed, 0) = 0"),
                followupCount("hr-2", RECEIVED_HR_SERVICE_CONDITION),
                riskAssessmentCount("hr-3", "lower(ifnull(ehra.client_status, '')) IN ('new_client', 'new')"),
                followupCount("hr-4", IDU_RECEIVED_SYRINGES_CONDITION),
                followupSum("hr-5", "1 = 1", numericExpression("ehfv.qty_syringes")),
                followupSum("hr-6", "1 = 1", numericExpression("ehfv.qty_needles")),
                new IndicatorDefinition("hr-7", MAT_REFERRAL_FROM_CLAUSE, SOURCE_ID_COLUMN, "source.last_interacted_with",
                        "1 = 1", SOURCE_IDU_CONDITION, SOURCE_NIDU_CONDITION, null),
                followupCount("hr-8", "(lower(ifnull(ehfv.referrals_provided, '')) LIKE '%hiv_testing%' OR lower(ifnull(ehfv.hiv_tested, '')) = 'yes')"),
                followupCount("hr-9", "(lower(ifnull(ehfv.referrals_provided, '')) LIKE '%tb_leprosy%' OR lower(ifnull(ehfv.tb_screening, '')) IN ('has_symptoms', 'undergoing_treatment'))"),
                followupCount("hr-10", "(lower(ifnull(ehfv.referrals_provided, '')) LIKE '%stis_stds%' OR lower(ifnull(ehfv.linkage_to_other_services, '')) LIKE '%stis_stds%' OR lower(ifnull(ehfv.stds_screening, '')) IN ('has_symptoms', 'undergoing_treatment'))"),
                followupCount("hr-11", "(lower(ifnull(ehfv.referrals_provided, '')) LIKE '%hepatitis_bc%' OR " + HEPATITIS_SCREENING_REQUIRES_REFERRAL_CONDITION + ")"),
                followupCount("hr-12", "((trim(ifnull(ehfv.referrals_provided, '')) <> '' AND lower(ifnull(ehfv.referrals_provided, '')) NOT LIKE '%none%') OR " + LINKAGE_SERVICES_SELECTED_CONDITION + ")"),
                followupCount("hr-12a", "(lower(ifnull(ehfv.linkage_to_other_services, '')) LIKE '%income_generating%' OR lower(ifnull(ehfv.referrals_provided, '')) LIKE '%income_generating%')"),
                followupCount("hr-12b", "(lower(ifnull(ehfv.linkage_to_other_services, '')) LIKE '%sober_house%' OR lower(ifnull(ehfv.referrals_provided, '')) LIKE '%sober_house%')"),
                followupCount("hr-12c", "(lower(ifnull(ehfv.linkage_to_other_services, '')) LIKE '%mental_health%' OR lower(ifnull(ehfv.referrals_provided, '')) LIKE '%mental_health%')"),
                followupCount("hr-12d", "(lower(ifnull(ehfv.linkage_to_other_services, '')) LIKE '%legal_issues%' OR lower(ifnull(ehfv.referrals_provided, '')) LIKE '%legal_issues%')"),
                followupCount("hr-12e", "(lower(ifnull(ehfv.linkage_to_other_services, '')) LIKE '%other%' OR trim(ifnull(ehfv.linkage_to_other_services_specify, '')) <> '' OR lower(ifnull(ehfv.referrals_provided, '')) LIKE '%other%' OR trim(ifnull(ehfv.referrals_other_specify, '')) <> '')"),
                followupCount("hr-13", "(lower(ifnull(ehfv.client_status, '')) LIKE '%overdose%' OR lower(ifnull(ehfv.cause_of_death, '')) LIKE '%overdose%')"),
                followupCount("hr-14", "lower(ifnull(ehfv.client_status, '')) LIKE '%client_deceased%' AND lower(ifnull(ehfv.cause_of_death, '')) LIKE '%overdose%'")
        ));
    }

    private static IndicatorDefinition riskAssessmentCount(String key, String whereClause) {
        return new IndicatorDefinition(key, RISK_ASSESSMENT_FROM_CLAUSE, RISK_ASSESSMENT_ID_COLUMN, "ehra.last_interacted_with",
                whereClause, RISK_ASSESSMENT_IDU_CONDITION, RISK_ASSESSMENT_NIDU_CONDITION, null);
    }

    private static IndicatorDefinition followupCount(String key, String whereClause) {
        return new IndicatorDefinition(key, FOLLOWUP_FROM_CLAUSE, FOLLOWUP_ID_COLUMN, "ehfv.last_interacted_with",
                whereClause, FOLLOWUP_IDU_CONDITION, FOLLOWUP_NIDU_CONDITION, null);
    }

    private static IndicatorDefinition followupSum(String key, String whereClause, String valueExpression) {
        return new IndicatorDefinition(key, FOLLOWUP_FROM_CLAUSE, FOLLOWUP_ID_COLUMN, "ehfv.last_interacted_with",
                whereClause, FOLLOWUP_IDU_CONDITION, FOLLOWUP_NIDU_CONDITION, valueExpression);
    }

    private static class BreakdownColumn {
        private final String columnAlias;
        private final String jsonSuffix;
        private final String groupType;
        private final String conditionTemplate;

        BreakdownColumn(String columnAlias, String jsonSuffix, String groupType, String conditionTemplate) {
            this.columnAlias = columnAlias;
            this.jsonSuffix = jsonSuffix;
            this.groupType = groupType;
            this.conditionTemplate = conditionTemplate;
        }

        String getColumnAlias() {
            return columnAlias;
        }

        String getJsonSuffix() {
            return jsonSuffix;
        }

        String getCondition(IndicatorDefinition definition, String ageExpression) {
            String groupCondition = "idu".equals(groupType)
                    ? definition.getIduCondition()
                    : definition.getNiduCondition();
            return groupCondition + " AND " + String.format(Locale.ENGLISH, conditionTemplate, ageExpression);
        }
    }

    private static class IndicatorDefinition {
        private final String key;
        private final String fromClause;
        private final String idColumn;
        private final String monthColumn;
        private final String whereClause;
        private final String iduCondition;
        private final String niduCondition;
        private final String valueExpression;

        IndicatorDefinition(String key,
                            String fromClause,
                            String idColumn,
                            String monthColumn,
                            String whereClause,
                            String iduCondition,
                            String niduCondition,
                            String valueExpression) {
            this.key = key;
            this.fromClause = fromClause;
            this.idColumn = idColumn;
            this.monthColumn = monthColumn;
            this.whereClause = whereClause;
            this.iduCondition = iduCondition;
            this.niduCondition = niduCondition;
            this.valueExpression = valueExpression;
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

        String getIduCondition() {
            return iduCondition;
        }

        String getNiduCondition() {
            return niduCondition;
        }

        String getValueExpression() {
            return valueExpression;
        }

        boolean isSum() {
            return valueExpression != null;
        }
    }
}
