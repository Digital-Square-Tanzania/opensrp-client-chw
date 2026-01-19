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
 * Aggregates indicators for the TB/Leprosy special areas report.
 */
public class TbLeprosySpecialAreasReportObject extends ReportObject {

    private static final List<String> LOCATIONS = buildLocations();
    private static final List<String> COLUMNS = buildColumns();
    private static final List<String> GENDERS = buildGenders();
    private static final List<String> INDICATOR_CODES = buildIndicatorCodes();

    public TbLeprosySpecialAreasReportObject(Date reportDate) {
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
        for (String location : LOCATIONS) {
            for (String column : COLUMNS) {
                for (String gender : GENDERS) {
                    indicators.add("tbleprosy_area_" + column + "_" + gender + "_" + location);
                }
            }
        }
        return Collections.unmodifiableList(indicators);
    }

    private static List<String> buildLocations() {
        List<String> locations = new ArrayList<>();
        Collections.addAll(locations,
                "duka",
                "mganga",
                "migodi",
                "mikutano",
                "magereza",
                "ibada",
                "shule",
                "sokoni",
                "wavuvu",
                "maskani",
                "kwingineko");
        return Collections.unmodifiableList(locations);
    }

    private static List<String> buildColumns() {
        List<String> columns = new ArrayList<>();
        Collections.addAll(columns, "col1", "col2", "col3", "col4", "col5", "col6", "col7");
        return Collections.unmodifiableList(columns);
    }

    private static List<String> buildGenders() {
        List<String> genders = new ArrayList<>();
        Collections.addAll(genders, "ke", "me");
        return Collections.unmodifiableList(genders);
    }
}
