package org.smartregister.chw.domain.harm_reduction_sober_house_reports;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.smartregister.chw.dao.ReportDao;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HarmReductionSoberHouseReportObjectTest {

    @Test
    public void getIndicatorDataShouldExposeDisaggregatedBreakdown() throws Exception {
        HarmReductionSoberHouseReportObject reportObject = new HarmReductionSoberHouseReportObject(
                new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2026-03-01")
        );

        try (MockedStatic<ReportDao> reportDao = Mockito.mockStatic(ReportDao.class)) {
            reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), ArgumentMatchers.<String>anyList()))
                    .thenAnswer(invocation -> {
                        String sql = invocation.getArgument(0);
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
    }

    private static Map<String, Integer> emptyBreakdown(List<String> columns) {
        Map<String, Integer> values = new HashMap<>();
        for (String column : columns) {
            values.put(column, 0);
        }
        return values;
    }
}
