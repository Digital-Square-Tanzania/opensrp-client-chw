package org.smartregister.chw.domain.harm_reduction_reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HarmReductionReportObjectTest {

    @Test
    public void getIndicatorDataShouldExposeAgeGenderAndRocGroupBreakdown() throws Exception {
        HarmReductionReportObject reportObject = new HarmReductionReportObject(
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

                        if (sql.contains("ec_harm_reduction_risk_assessment ehra")) {
                            values.put("total", 12);
                            values.put("idu_female_le_15", 1);
                            values.put("idu_male_19_24", 2);
                            values.put("idu_total", 6);
                            values.put("nidu_female_total", 4);
                            values.put("nidu_male_25_49", 2);
                        } else if (sql.contains("qty_syringes")) {
                            values.put("total", 40);
                            values.put("idu_female_total", 10);
                            values.put("nidu_male_total", 30);
                        }

                        return values;
                    });

            JSONObject indicatorDataObject = reportObject.getIndicatorData();

            assertEquals(12, indicatorDataObject.getInt("hr-1"));
            assertEquals(1, indicatorDataObject.getInt("hr-1-idu-female-le-15"));
            assertEquals(2, indicatorDataObject.getInt("hr-1-idu-male-19-24"));
            assertEquals(6, indicatorDataObject.getInt("hr-1-idu-total"));
            assertEquals(4, indicatorDataObject.getInt("hr-1-nidu-female-total"));
            assertEquals(2, indicatorDataObject.getInt("hr-1-nidu-male-25-49"));

            assertEquals(40, indicatorDataObject.getInt("hr-5"));
            assertEquals(10, indicatorDataObject.getInt("hr-5-idu-female-total"));
            assertEquals(30, indicatorDataObject.getInt("hr-5-nidu-male-total"));
        }

        assertTrue(containsSql(capturedSql, "ec_family_member efm"));
        assertTrue(containsSql(capturedSql, "julianday(efm.dob"));
        assertTrue(containsSql(capturedSql, "COUNT(DISTINCT CASE WHEN"));
        assertTrue(containsSql(capturedSql, "COALESCE(SUM(CASE WHEN"));
        assertTrue(containsSql(capturedSql, "ec_harm_reduction_followup_visit ehfv"));
        assertTrue(containsSql(capturedSql, "ec_harm_reduction_risk_assessment ehra"));
        assertTrue(containsSql(capturedSql, "ehfv.hepatitis_b_screening"));
        assertTrue(containsSql(capturedSql, "ehfv.hepatitis_c_screening"));
    }

    @Test
    public void reportTemplateShouldRenderDisaggregationColumns() throws Exception {
        String reportTemplate = readText("src/nacp/assets/reports/harm_reduction_reports/harm-reduction-report.html");

        assertTrue(reportTemplate.contains("idu-female-le-15"));
        assertTrue(reportTemplate.contains("idu-male-le-18"));
        assertTrue(reportTemplate.contains("nidu-female-15-24"));
        assertTrue(reportTemplate.contains("nidu-male-total"));
        assertTrue(reportTemplate.contains("\"hr-14\""));
        assertTrue(reportTemplate.contains("colSpan = 24"));
    }

    @Test
    public void monthlyReportConfigShouldUseSeparateHepatitisScreeningFields() throws Exception {
        String reportConfig = readText("src/nacp/assets/config/harm-reduction-monthly-report.yml");

        assertTrue(reportConfig.contains("ehfv.hepatitis_b_screening"));
        assertTrue(reportConfig.contains("ehfv.hepatitis_c_screening"));
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
