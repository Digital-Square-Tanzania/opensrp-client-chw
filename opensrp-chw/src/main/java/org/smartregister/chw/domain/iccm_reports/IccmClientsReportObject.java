package org.smartregister.chw.domain.iccm_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IccmClientsReportObject extends ReportObject {
    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private static final String[] INDICATOR_CODES = {"iccm-1", "iccm-2", "iccm-3", "iccm-4", "iccm-5", "iccm-6"};

    private static final String[] INDICATOR_AGE_GROUPS = {"less-than-1-month", "less-than-1-year", "between-1-to-5-years", "between-5-to-60-years", "60-and-above-years"};

    private static final String[] INDICATOR_SEX_GROUPS = {"ME", "KE", "jumla"};

    private final Date reportDate;

    public IccmClientsReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorCodesWithAgeGroups();
    }

    private void setIndicatorCodesWithAgeGroups() {
        for (String indicatorCode : INDICATOR_CODES) {
            for (String indicatorKey : INDICATOR_AGE_GROUPS) {
                for (String indicatorSex : INDICATOR_SEX_GROUPS) {
                    indicatorCodesWithAgeGroups.add(indicatorCode + "-" + indicatorKey + "-" + indicatorSex);
                }
            }
        }
    }


    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : indicatorCodesWithAgeGroups) {
            indicatorDataObject.put(indicatorCode, ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate));
        }

        for (String indicatorCode : INDICATOR_CODES) {
            int totalJumla = 0;
            for (String indicatorSex : INDICATOR_SEX_GROUPS) {
                int total = getTotal(indicatorCode, indicatorSex);
                indicatorDataObject.put(indicatorCode + "-" + indicatorSex + "-jumla", total);
                totalJumla += total;
            }
            indicatorDataObject.put(indicatorCode + "-jumla", totalJumla);
        }

        int sumMeJumla = 0;
        int sumKeJumla = 0;
        for (String ageGroup : INDICATOR_AGE_GROUPS) {
            int sumMe = ReportDao.getReportPerIndicatorCode("iccm-2-" + ageGroup + "-ME", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-3-" + ageGroup + "-ME", reportDate);
            int sumKe = ReportDao.getReportPerIndicatorCode("iccm-2-" + ageGroup + "-KE", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-3-" + ageGroup + "-KE", reportDate);
            int sumJumla = ReportDao.getReportPerIndicatorCode("iccm-2-" + ageGroup + "-jumla", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-3-" + ageGroup + "-jumla", reportDate);
            sumMeJumla += sumMe;
            sumKeJumla += sumKe;
            indicatorDataObject.put("iccm-2+3-" + ageGroup + "-ME", sumMe);
            indicatorDataObject.put("iccm-2+3-" + ageGroup + "-KE", sumKe);
            indicatorDataObject.put("iccm-2+3-" + ageGroup + "-jumla", sumJumla);
        }


        indicatorDataObject.put("iccm-2+3-ME-jumla", sumMeJumla);
        indicatorDataObject.put("iccm-2+3-KE-jumla", sumKeJumla);
        indicatorDataObject.put("iccm-2+3-jumla", sumMeJumla+sumKeJumla);

        return indicatorDataObject;
    }

    private int getTotal(String indicatorCode, String indicatorSex) {
        int total = 0;
        for (String indicatorKey : INDICATOR_AGE_GROUPS) {
            total += ReportDao.getReportPerIndicatorCode(indicatorCode + "-" + indicatorKey + "-" + indicatorSex, reportDate);
        }
        return total;
    }
}
