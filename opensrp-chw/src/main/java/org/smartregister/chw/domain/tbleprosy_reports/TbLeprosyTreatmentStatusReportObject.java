package org.smartregister.chw.domain.tbleprosy_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Aggregates indicators for the TB/Leprosy treatment status report.
 */
public class TbLeprosyTreatmentStatusReportObject extends ReportObject {

    private static final List<String> STATUSES = buildStatuses();
    private static final List<String> GENDERS = buildGenders();
    private static final List<String> AGE_GROUPS = buildAgeGroups();
    private static final List<String> INDICATOR_CODES = buildIndicatorCodes();

    public TbLeprosyTreatmentStatusReportObject(Date reportDate) {
        super(reportDate);
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : INDICATOR_CODES) {
            indicatorDataObject.put(indicatorCode, ReportDao.getReportPerIndicatorCode(indicatorCode, getReportDate()));
        }
        return indicatorDataObject;
    }

    private static List<String> buildIndicatorCodes() {
        List<String> indicators = new ArrayList<>();
        for (String ageGroup : AGE_GROUPS) {
            for (String gender : GENDERS) {
                for (String status : STATUSES) {
                    indicators.add("tbleprosy_treat_" + status + "_" + gender + "_" + ageGroup);
                }
            }
        }
        return Collections.unmodifiableList(indicators);
    }

    private static List<String> buildStatuses() {
        List<String> statuses = new ArrayList<>();
        Collections.addAll(statuses, "followed", "outcome_a", "outcome_h", "outcome_k");
        return Collections.unmodifiableList(statuses);
    }

    private static List<String> buildGenders() {
        List<String> genders = new ArrayList<>();
        Collections.addAll(genders, "ke", "me");
        return Collections.unmodifiableList(genders);
    }

    private static List<String> buildAgeGroups() {
        List<String> ageGroups = new ArrayList<>();
        Collections.addAll(ageGroups, "0_14", "15_plus");
        return Collections.unmodifiableList(ageGroups);
    }
}
