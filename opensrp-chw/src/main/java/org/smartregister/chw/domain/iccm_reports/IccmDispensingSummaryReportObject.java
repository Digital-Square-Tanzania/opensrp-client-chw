package org.smartregister.chw.domain.iccm_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IccmDispensingSummaryReportObject extends ReportObject {
    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private final String[] indicatorCodes = new String[]{"iccm-dispensing-1", "iccm-dispensing-2", "iccm-dispensing-3", "iccm-dispensing-4"};

    private final String[] indicatorAgeGroups = new String[]{"0-3", "3-8", "8-12", "12+"};
    
    private final Date reportDate;

    public IccmDispensingSummaryReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorCodesWithAgeGroups(indicatorCodesWithAgeGroups);
    }

    public void setIndicatorCodesWithAgeGroups(List<String> indicatorCodesWithAgeGroups) {
        for (String indicatorCode : indicatorCodes) {
            for (String indicatorKey : indicatorAgeGroups) {
                indicatorCodesWithAgeGroups.add(indicatorCode + "-" + indicatorKey);
            }
        }

        // Generate total indicator codes using a separate method
        generateJumlaIndicatorCodes();
    }

    private void generateJumlaIndicatorCodes() {
        for (int i = 1; i <= 8; i++) {
            indicatorCodesWithAgeGroups.add("iccm-dispensing-" + i + "-jumla");
        }
    }


    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : indicatorCodesWithAgeGroups) {
            indicatorDataObject.put(indicatorCode, ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate));
        }

        // Calculate and add total values for "iccm-dispensing-1-jumla" to "iccm-dispensing-4-jumla"
        for (int i = 1; i <= 4; i++) {
            int jumlaValue = 0;
            for (String ageGroup : indicatorAgeGroups) {
                jumlaValue += ReportDao.getReportPerIndicatorCode("iccm-dispensing-" + i + "-" + ageGroup, reportDate);
            }
            indicatorDataObject.put("iccm-dispensing-" + i + "-jumla", jumlaValue);
        }

        return indicatorDataObject;
    }


}
