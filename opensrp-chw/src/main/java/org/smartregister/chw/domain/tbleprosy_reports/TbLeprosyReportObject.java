package org.smartregister.chw.domain.tbleprosy_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.Date;

/**
 * Aggregates indicator values for the TB/Leprosy monthly report.
 */
public class TbLeprosyReportObject extends ReportObject {

    private static final String[] INDICATOR_CODES = new String[]{
            "tbleprosy-1-male-total",
            "tbleprosy-1-female-total",
            "tbleprosy-1-sessions",
            "tbleprosy-2-total",
            "tbleprosy-2-male-total",
            "tbleprosy-2-female-total",
            "tbleprosy-2-tb-presumptive",
            "tbleprosy-2-leprosy-presumptive",
            "tbleprosy-2-both-presumptive",
            "tbleprosy-2-special-group",
            "tbleprosy-3-tb-pulmonary",
            "tbleprosy-3-tb-extra-pulmonary",
            "tbleprosy-3-tb-drug-resistant",
            "tbleprosy-3-tb-undetected",
            "tbleprosy-3-tb-poor-quality",
            "tbleprosy-3-clinical-suggestive",
            "tbleprosy-3-clinical-non-suggestive",
            "tbleprosy-3-leprosy-confirmed",
            "tbleprosy-3-leprosy-not-detected",
            "tbleprosy-4-reason-interrupted",
            "tbleprosy-4-reason-never-started",
            "tbleprosy-4-outcome-found",
            "tbleprosy-4-outcome-not-found",
            "tbleprosy-4-outcome-deceased",
            "tbleprosy-4-returned-yes",
            "tbleprosy-4-returned-no",
            "tbleprosy-5-sample-collected-yes",
            "tbleprosy-5-sample-collected-no",
            "tbleprosy-5-container-provided-yes",
            "tbleprosy-5-container-provided-no"
    };

    private final Date reportDate;

    public TbLeprosyReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : INDICATOR_CODES) {
            int value = ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate);
            indicatorDataObject.put(indicatorCode, value);
        }

        addDerivedTotals(indicatorDataObject);
        return indicatorDataObject;
    }

    private void addDerivedTotals(JSONObject indicatorDataObject) throws JSONException {
        int mobilizationMaleTotal = indicatorDataObject.optInt("tbleprosy-1-male-total", 0);
        int mobilizationFemaleTotal = indicatorDataObject.optInt("tbleprosy-1-female-total", 0);
        indicatorDataObject.put("tbleprosy-1-grand-total", mobilizationMaleTotal + mobilizationFemaleTotal);

        int screeningMaleTotal = indicatorDataObject.optInt("tbleprosy-2-male-total", 0);
        int screeningFemaleTotal = indicatorDataObject.optInt("tbleprosy-2-female-total", 0);
        indicatorDataObject.put("tbleprosy-2-gender-total", screeningMaleTotal + screeningFemaleTotal);

        int sampleCollectedYes = indicatorDataObject.optInt("tbleprosy-5-sample-collected-yes", 0);
        int sampleCollectedNo = indicatorDataObject.optInt("tbleprosy-5-sample-collected-no", 0);
        indicatorDataObject.put("tbleprosy-5-sample-collected-total", sampleCollectedYes + sampleCollectedNo);

        int containerProvidedYes = indicatorDataObject.optInt("tbleprosy-5-container-provided-yes", 0);
        int containerProvidedNo = indicatorDataObject.optInt("tbleprosy-5-container-provided-no", 0);
        indicatorDataObject.put("tbleprosy-5-container-provided-total", containerProvidedYes + containerProvidedNo);
    }
}
