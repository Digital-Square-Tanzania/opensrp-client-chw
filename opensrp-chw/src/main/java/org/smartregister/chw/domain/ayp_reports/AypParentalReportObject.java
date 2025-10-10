package org.smartregister.chw.domain.ayp_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.Date;

public class AypParentalReportObject extends ReportObject {

    private final String[] indicatorCodes = new String[]{
            "ayp-parental-1-grand-total",
            "ayp-parental-1-male",
            "ayp-parental-1-female",
            "ayp-parental-2-grand-total",
            "ayp-parental-2-male",
            "ayp-parental-2-female",
            "ayp-parental-3-grand-total",
            "ayp-parental-3-male",
            "ayp-parental-3-female",
            "ayp-parental-4-introduction",
            "ayp-parental-4-hiv-module",
            "ayp-parental-4-protection-module",
            "ayp-parental-4-understanding-youth",
            "ayp-parental-4-skills-education",
            "ayp-parental-4-reproductive-health",
            "ayp-parental-4-promote-hiv-services"
    };

    public AypParentalReportObject(Date reportDate) {
        super(reportDate);
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : indicatorCodes) {
            indicatorDataObject.put(indicatorCode, ReportDao.getReportPerIndicatorCode(indicatorCode, getReportDate()));
        }
        return indicatorDataObject;
    }
}
