package org.smartregister.chw.domain.ayp_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AypInSchoolReportObject extends ReportObject {

    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private final String[] indicatorCodes = new String[]{"ayp-1", "ayp-2", "ayp-3", "ayp-4", "ayp-5"};

    private final String[] indicatorSex = new String[]{"male", "female"};

    private final String[] indicatorAgeGroups = new String[]{"10-14", "15-19", "20-24"};

    private final Date reportDate;

    public AypInSchoolReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorCodesWithAgeGroups(indicatorCodesWithAgeGroups);
    }

    private static int calculateTotal(HashMap<String, Integer> indicators, String specificKey) {
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

    private void setIndicatorCodesWithAgeGroups(List<String> list) {
        for (String indicatorCode : indicatorCodes) {
            for (String sex : indicatorSex) {
                for (String ageGroup : indicatorAgeGroups) {
                    list.add(indicatorCode + "-" + sex + "-" + ageGroup);
                }
            }
        }
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        HashMap<String, Integer> indicatorValues = new HashMap<>();
        JSONObject indicatorDataObject = new JSONObject();

        for (String indicatorCode : indicatorCodesWithAgeGroups) {
            int value = ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate);
            indicatorValues.put(indicatorCode, value);
            indicatorDataObject.put(indicatorCode, value);
        }

        for (String indicatorCode : indicatorCodes) {
            int maleTotal = calculateTotal(indicatorValues, indicatorCode + "-male");
            int femaleTotal = calculateTotal(indicatorValues, indicatorCode + "-female");
            indicatorDataObject.put(indicatorCode + "-male-total", maleTotal);
            indicatorDataObject.put(indicatorCode + "-female-total", femaleTotal);
            indicatorDataObject.put(indicatorCode + "-grand-total", maleTotal + femaleTotal);
        }

        return indicatorDataObject;
    }
}
