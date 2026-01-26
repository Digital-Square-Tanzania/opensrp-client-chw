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
 * Aggregates indicators for the TB/Leprosy service challenges report.
 */
public class TbLeprosyServiceChallengesReportObject extends ReportObject {

    private static final List<String> CONDITIONS = buildConditions();
    private static final List<String> CONTEXTS = buildContexts();
    private static final List<String> GENDERS = buildGenders();
    private static final List<String> INDICATOR_CODES = buildIndicatorCodes();

    public TbLeprosyServiceChallengesReportObject(Date reportDate) {
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
        for (String context : CONTEXTS) {
            for (String condition : CONDITIONS) {
                for (String gender : GENDERS) {
                    indicators.add("tbleprosy_challenge_" + condition + "_" + context + "_" + gender);
                }
            }
        }
        return Collections.unmodifiableList(indicators);
    }

    private static List<String> buildConditions() {
        List<String> conditions = new ArrayList<>();
        Collections.addAll(conditions, "tb", "lpr");
        return Collections.unmodifiableList(conditions);
    }

    private static List<String> buildContexts() {
        List<String> contexts = new ArrayList<>();
        Collections.addAll(contexts, "family", "facility", "community", "other");
        return Collections.unmodifiableList(contexts);
    }

    private static List<String> buildGenders() {
        List<String> genders = new ArrayList<>();
        Collections.addAll(genders, "ke", "me");
        return Collections.unmodifiableList(genders);
    }
}
