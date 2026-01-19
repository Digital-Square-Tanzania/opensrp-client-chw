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
 * Aggregates indicators for the TB/Leprosy household report.
 */
public class TbLeprosyHouseholdReportObject extends ReportObject {

    private static final List<String> HOUSEHOLD_COLUMNS = buildColumns();
    private static final List<String> GENDERS = buildGenders();
    private static final List<String> AGE_GROUPS = buildAgeGroups();
    private static final List<String> INDICATOR_CODES = buildIndicatorCodes();

    public TbLeprosyHouseholdReportObject(Date reportDate) {
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
        for (String column : HOUSEHOLD_COLUMNS) {
            for (String ageGroup : AGE_GROUPS) {
                for (String gender : GENDERS) {
                    indicators.add("tbleprosy_house_" + column + "_" + gender + "_" + ageGroup);
                }
            }
        }
        return Collections.unmodifiableList(indicators);
    }

    private static List<String> buildColumns() {
        List<String> columns = new ArrayList<>();
        Collections.addAll(columns, "col1", "col2", "col3", "col4", "col5", "col6", "col7", "col8", "col9");
        return Collections.unmodifiableList(columns);
    }

    private static List<String> buildGenders() {
        List<String> genders = new ArrayList<>();
        Collections.addAll(genders, "ke", "me");
        return Collections.unmodifiableList(genders);
    }

    private static List<String> buildAgeGroups() {
        List<String> ages = new ArrayList<>();
        Collections.addAll(ages, "0_5", "6_14", "15_plus");
        return Collections.unmodifiableList(ages);
    }
}
