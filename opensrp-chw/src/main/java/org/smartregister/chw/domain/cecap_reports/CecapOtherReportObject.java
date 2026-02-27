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

public class CecapOtherReportObject extends ReportObject {
    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private final String[] indicatorCodes = new String[]{"cecap-other-1a", "cecap-other-1b", "cecap-other-1c", "cecap-other-2", "cecap-other-3"};

    private final String[] indicatorSex = new String[]{"male", "female"};

    private final String[] indicatorAgeGroups = new String[]{"14", "15-24", "25-30", "31-50", "51-60", "61+"
    };

    private final Date reportDate;

    public CecapOtherReportObject(Date reportDate) {
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

        return indicatorDataObject;
    }
}