package org.smartregister.chw.domain.iccm_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MalariaTestReportObject extends ReportObject {
    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private static final String[] INDICATOR_CODES = {
            "iccm-4-less-than-1-month",
            "iccm-4-negative-less-than-1-month",
            "iccm-4-less-than-1-month",
            "iccm-4-negative-less-than-1-month",
            "iccm-4-less-than-1-year",
            "iccm-4-negative-less-than-1-year",
            "iccm-4-less-than-1-year",
            "iccm-4-negative-less-than-1-year",
            "iccm-4-between-1-to-5-years",
            "iccm-4-negative-between-1-to-5-years",
            "iccm-4-between-1-to-5-years",
            "iccm-4-negative-between-1-to-5-years",
            "iccm-4-5-and-above-years",
            "iccm-4-negative-5-and-above-years",
            "iccm-4-5-and-above-years",
            "iccm-4-negative-5-and-above-years"
    };

    private static final String[] INDICATOR_SEX_GROUPS = {"ME", "KE", "jumla"};

    private final Date reportDate;

    public MalariaTestReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorCodesWithAgeGroups();
    }

    private void setIndicatorCodesWithAgeGroups() {
        for (String indicatorCode : INDICATOR_CODES) {
            for (String indicatorSex : INDICATOR_SEX_GROUPS) {
                indicatorCodesWithAgeGroups.add(indicatorCode + "-" + indicatorSex);
            }
        }
    }


    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : indicatorCodesWithAgeGroups) {
            indicatorDataObject.put(indicatorCode, ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate));
        }

        int iccm_4_negative_ME_jumla = ReportDao.getReportPerIndicatorCode("iccm-4-negative-less-than-1-month-ME", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-negative-less-than-1-year-ME", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-negative-between-1-to-5-years-ME", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-negative-5-and-above-years-ME", reportDate);

        int iccm_4_negative_KE_jumla = ReportDao.getReportPerIndicatorCode("iccm-4-negative-less-than-1-month-KE", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-negative-less-than-1-year-KE", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-negative-between-1-to-5-years-KE", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-negative-5-and-above-years-KE", reportDate);


        indicatorDataObject.put("iccm-4-negative-ME-jumla", iccm_4_negative_ME_jumla);
        indicatorDataObject.put("iccm-4-negative-KE-jumla", iccm_4_negative_KE_jumla);
        indicatorDataObject.put("iccm-4-negative-jumla", iccm_4_negative_ME_jumla + iccm_4_negative_KE_jumla);


        int iccm_4_ME_jumla = ReportDao.getReportPerIndicatorCode("iccm-4-less-than-1-month-ME", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-less-than-1-year-ME", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-between-1-to-5-years-ME", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-5-and-above-years-ME", reportDate);

        int iccm_4_KE_jumla = ReportDao.getReportPerIndicatorCode("iccm-4-less-than-1-month-KE", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-less-than-1-year-KE", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-between-1-to-5-years-KE", reportDate) +
                ReportDao.getReportPerIndicatorCode("iccm-4-5-and-above-years-KE", reportDate);


        indicatorDataObject.put("iccm-4-ME-jumla", iccm_4_ME_jumla);
        indicatorDataObject.put("iccm-4-KE-jumla", iccm_4_KE_jumla);
        indicatorDataObject.put("iccm-4-jumla", iccm_4_ME_jumla + iccm_4_KE_jumla);


        indicatorDataObject.put("iccm-4-jumla-less-than-1-month-ME", ReportDao.getReportPerIndicatorCode("iccm-4-negative-less-than-1-month-ME", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-4-less-than-1-month-ME", reportDate));
        indicatorDataObject.put("iccm-4-jumla-less-than-1-month-KE", ReportDao.getReportPerIndicatorCode("iccm-4-negative-less-than-1-month-KE", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-4-less-than-1-month-KE", reportDate));
        indicatorDataObject.put("iccm-4-jumla-less-than-1-year-ME", ReportDao.getReportPerIndicatorCode("iccm-4-negative-less-than-1-year-ME", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-4-less-than-1-year-ME", reportDate));
        indicatorDataObject.put("iccm-4-jumla-less-than-1-year-KE", ReportDao.getReportPerIndicatorCode("iccm-4-negative-less-than-1-year-KE", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-4-less-than-1-year-KE", reportDate));
        indicatorDataObject.put("iccm-4-jumla-between-1-to-5-years-ME", ReportDao.getReportPerIndicatorCode("iccm-4-negative-between-1-to-5-years-ME", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-4-between-1-to-5-years-ME", reportDate));
        indicatorDataObject.put("iccm-4-jumla-between-1-to-5-years-KE", ReportDao.getReportPerIndicatorCode("iccm-4-negative-between-1-to-5-years-KE", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-4-between-1-to-5-years-KE", reportDate));
        indicatorDataObject.put("iccm-4-jumla-5-and-above-years-ME", ReportDao.getReportPerIndicatorCode("iccm-4-negative-5-and-above-years-ME", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-4-5-and-above-years-ME", reportDate));
        indicatorDataObject.put("iccm-4-jumla-5-and-above-years-KE", ReportDao.getReportPerIndicatorCode("iccm-4-negative-5-and-above-years-KE", reportDate) + ReportDao.getReportPerIndicatorCode("iccm-4-5-and-above-years-KE", reportDate));


        indicatorDataObject.put("iccm-4-jumla-ME-jumla", iccm_4_negative_ME_jumla + iccm_4_ME_jumla);
        indicatorDataObject.put("iccm-4-jumla-KE-jumla", iccm_4_negative_KE_jumla + iccm_4_KE_jumla);
        indicatorDataObject.put("iccm-4-jumla-jumla", iccm_4_negative_ME_jumla + iccm_4_ME_jumla + iccm_4_negative_KE_jumla + iccm_4_KE_jumla);

        return indicatorDataObject;
    }


}
