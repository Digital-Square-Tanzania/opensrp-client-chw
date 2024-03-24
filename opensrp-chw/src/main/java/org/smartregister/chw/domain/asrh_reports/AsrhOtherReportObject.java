package org.smartregister.chw.domain.asrh_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsrhOtherReportObject extends ReportObject {
    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private final String[] indicatorCodes = new String[]{"asrh-other-1-1", "asrh-other-1-2", "asrh-other-1-3", "asrh-other-1-4", "asrh-other-1-5", "asrh-other-1-6", "asrh-other-1-7", "asrh-other-1-8", "asrh-other-1-9", "asrh-other-2-1", "asrh-other-2-2", "asrh-other-2-3", "asrh-other-2-4", "asrh-other-2-5", "asrh-other-2-6", "asrh-other-3", "asrh-other-4"};

    private final String[] indicatorSex = new String[]{"male", "female"};

    private final String[] indicatorAgeGroups = new String[]{"10-14", "15-19", "20-24"

    };

    private final Date reportDate;

    public AsrhOtherReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorCodesWithAgeGroups(indicatorCodesWithAgeGroups);
    }

    public static int calculateAsrhSpecificTotal(HashMap<String, Integer> indicators, String specificKey) {
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
            int maleTotal = calculateAsrhSpecificTotal(indicatorsValues, indicatorCode + "-male");
            int femaleTotal = calculateAsrhSpecificTotal(indicatorsValues, indicatorCode + "-female");
            indicatorDataObject.put(indicatorCode + "-male-total", maleTotal);
            indicatorDataObject.put(indicatorCode + "-female-total", femaleTotal);
            indicatorDataObject.put(indicatorCode + "-grand-total", maleTotal + femaleTotal);
        }
        return indicatorDataObject;
    }
}