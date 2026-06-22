package org.smartregister.chw.domain.harm_reduction_sober_house_reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.smartregister.chw.dao.ReportDao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HarmReductionSoberHouseReportObjectTest {

    @Test
    public void getIndicatorDataShouldExposeDisaggregatedBreakdown() throws Exception {
        HarmReductionSoberHouseReportObject reportObject = new HarmReductionSoberHouseReportObject(
                new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2026-03-01")
        );
        List<String> capturedSql = new ArrayList<>();

        try (MockedStatic<ReportDao> reportDao = Mockito.mockStatic(ReportDao.class)) {
            reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), ArgumentMatchers.<String>anyList()))
                    .thenAnswer(invocation -> {
                        String sql = invocation.getArgument(0);
                        capturedSql.add(sql);
                        List<String> columns = invocation.getArgument(1);
                        Map<String, Integer> values = emptyBreakdown(columns);

                        if (sql.contains("WHERE 1 = 1")) {
                            values.put("total", 12);
                            values.put("male_total", 7);
                            values.put("male_18_25", 2);
                            values.put("male_26_35", 3);
                            values.put("male_36_45", 1);
                            values.put("male_46_55", 1);
                            values.put("female_total", 5);
                            values.put("female_18_25", 1);
                            values.put("female_26_35", 2);
                            values.put("female_36_45", 1);
                            values.put("female_56_plus", 1);
                        } else if (sql.contains("lower(ifnull(ehshe.education_level, '')) = 'no_education'")) {
                            values.put("total", 4);
                            values.put("male_total", 3);
                            values.put("male_18_25", 1);
                            values.put("male_26_35", 1);
                            values.put("male_46_55", 1);
                            values.put("female_total", 1);
                            values.put("female_26_35", 1);
                        }

                        return values;
                    });

            JSONObject indicatorDataObject = reportObject.getIndicatorData();

            assertEquals(12, indicatorDataObject.getInt("sh-1"));
            assertEquals(7, indicatorDataObject.getInt("sh-1-male-total"));
            assertEquals(3, indicatorDataObject.getInt("sh-1-male-26-35"));
            assertEquals(1, indicatorDataObject.getInt("sh-1-female-56-plus"));

            assertEquals(4, indicatorDataObject.getInt("sh-2a"));
            assertEquals(3, indicatorDataObject.getInt("sh-2a-male-total"));
            assertEquals(1, indicatorDataObject.getInt("sh-2a-male-18-25"));
            assertEquals(1, indicatorDataObject.getInt("sh-2a-female-26-35"));

            assertEquals(0, indicatorDataObject.getInt("sh-5a"));
            assertEquals(0, indicatorDataObject.getInt("sh-5a-female-total"));
        }

        assertTrue(containsSql(capturedSql, "lower(ifnull(ehshe.enrolled_into_ctc_services, '')) = 'yes'"));
        assertTrue(containsSql(capturedSql, "trim(ifnull(ehshe.ctc_id, '')) <> ''"));
        assertTrue(containsSql(capturedSql, "lower(ifnull(ehshe.other_conditions_treatment_after_screening, '')) = 'yes'"));
        assertFalse(containsSql(capturedSql, "ehshe.treatment_after_screening"));
    }

    @Test
    public void soberHouseEnrollmentBindObjectShouldMapIndicatorNineTreatmentFields() throws Exception {
        JSONObject clientFields = readJson("src/nacp/assets/ec_client_fields.json");
        Set<String> mappedColumns = mappedColumns(clientFields, "ec_harm_reduction_sober_house_enrollment");

        for (String column : Arrays.asList(
                "uic_id",
                "enrolled_into_ctc_services",
                "ctc_id",
                "stis_treatment_after_screening",
                "heart_diseases_treatment_after_screening",
                "mental_health_treatment_after_screening",
                "hepatitis_b_treatment_after_screening",
                "hepatitis_c_treatment_after_screening",
                "other_hepatitis_treatment_after_screening",
                "diabetes_treatment_after_screening",
                "tuberculosis_treatment_after_screening",
                "other_conditions_treatment_after_screening"
        )) {
            assertTrue("Missing ec_harm_reduction_sober_house_enrollment mapping for " + column,
                    mappedColumns.contains(column));
        }
    }

    @Test
    public void monthlyReportConfigShouldUseIndicatorNineTreatmentFields() throws Exception {
        String reportConfig = readText("src/nacp/assets/config/harm-reduction-sober-house-monthly-report.yml");

        assertTrue(reportConfig.contains("lower(ifnull(ehshe.enrolled_into_ctc_services, '')) = 'yes'"));
        assertTrue(reportConfig.contains("trim(ifnull(ehshe.ctc_id, '')) <> ''"));
        assertTrue(reportConfig.contains("lower(ifnull(ehshe.other_conditions_treatment_after_screening, '')) = 'yes'"));
        assertFalse(reportConfig.contains("ehshe.treatment_after_screening"));
    }

    private static Map<String, Integer> emptyBreakdown(List<String> columns) {
        Map<String, Integer> values = new HashMap<>();
        for (String column : columns) {
            values.put(column, 0);
        }
        return values;
    }

    private static boolean containsSql(List<String> sqlQueries, String expectedSql) {
        for (String sql : sqlQueries) {
            if (sql.contains(expectedSql)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> mappedColumns(JSONObject clientFields, String bindObjectName) throws Exception {
        JSONArray columns = getBindObjectColumns(clientFields, bindObjectName);
        Set<String> mappedColumns = new LinkedHashSet<>();
        for (int i = 0; i < columns.length(); i++) {
            mappedColumns.add(columns.getJSONObject(i).optString("column_name"));
        }
        return mappedColumns;
    }

    private static JSONArray getBindObjectColumns(JSONObject clientFields, String bindObjectName) throws Exception {
        JSONArray bindObjects = clientFields.getJSONArray("bindobjects");
        for (int i = 0; i < bindObjects.length(); i++) {
            JSONObject bindObject = bindObjects.getJSONObject(i);
            if (bindObjectName.equals(bindObject.optString("name"))) {
                return bindObject.getJSONArray("columns");
            }
        }

        throw new AssertionError("Missing bindobject: " + bindObjectName);
    }

    private static JSONObject readJson(String relativePath) throws Exception {
        return new JSONObject(readText(relativePath));
    }

    private static String readText(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolvePath(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path resolvePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }

        Path modulePath = Paths.get("opensrp-chw").resolve(relativePath);
        if (Files.exists(modulePath)) {
            return modulePath;
        }

        throw new AssertionError("Could not resolve path: " + relativePath);
    }
}
