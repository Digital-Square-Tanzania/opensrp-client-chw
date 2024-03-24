package org.smartregister.chw.domain.cecap_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CecapReportObject extends ReportObject {
    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private final String[] indicatorCodes = new String[]{"cecap-2", "cecap-3", "cecap-4a", "cecap-4b", "cecap-4c", "cecap-4d", "cecap-5"};

    private final String[] indicatorSex = new String[]{"male", "female"};

    private final String[] indicatorAgeGroups = new String[]{"14", "15-24", "25-30", "31-50", "51-60", "61+"
    };

    private final Date reportDate;

    public CecapReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorCodesWithAgeGroups(indicatorCodesWithAgeGroups);
    }

    public static int calculateSbcSpecificTotal(HashMap<String, Integer> indicators, String specificKey) {
        int total = 0;

        for (Map.Entry<String, Integer> entry : indicators.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Integer value = entry.getValue();

            if (key.contains(specificKey.toLowerCase())) {
                total += value;
            }
        }

        return total;
    }

    public void setIndicatorCodesWithAgeGroups(List<String> indicatorCodesWithAgeGroups) {
        for (String indicatorCode : indicatorCodes) {
            for (String sex : indicatorSex) {
                for (String indicatorKey : indicatorAgeGroups) {
                    indicatorCodesWithAgeGroups.add(indicatorCode + "-" + sex + "-" + indicatorKey);
                }
            }
        }

    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        HashMap<String, Integer> indicatorsValues = new HashMap<>();
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : indicatorCodesWithAgeGroups) {
            int value = ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate);
            indicatorsValues.put(indicatorCode, value);
            indicatorDataObject.put(indicatorCode, value);
        }

        // Calculate and add total values for "totals"
        for (String indicatorCode : indicatorCodes) {
            int maleTotal = calculateSbcSpecificTotal(indicatorsValues, indicatorCode + "-male");
            int femaleTotal = calculateSbcSpecificTotal(indicatorsValues, indicatorCode + "-female");
            indicatorDataObject.put(indicatorCode + "-male-total", maleTotal);
            indicatorDataObject.put(indicatorCode + "-female-total", femaleTotal);
            indicatorDataObject.put(indicatorCode + "-grand-total", maleTotal + femaleTotal);
        }

        int cecap1MaleTotal = ReportDao.getReportPerIndicatorCode("cecap-1-male-total", reportDate);
        int cecap1FemaleTotal = ReportDao.getReportPerIndicatorCode("cecap-1-female-total", reportDate);
        indicatorDataObject.put("cecap-1-male-total", cecap1MaleTotal);
        indicatorDataObject.put("cecap-1-female-total", cecap1FemaleTotal);
        indicatorDataObject.put("cecap-1-grand-total", cecap1MaleTotal + cecap1FemaleTotal);

        return indicatorDataObject;
    }
}