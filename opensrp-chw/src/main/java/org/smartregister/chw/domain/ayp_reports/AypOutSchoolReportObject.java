package org.smartregister.chw.domain.ayp_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AypOutSchoolReportObject extends ReportObject {

    private final List<String> indicatorKeys = new ArrayList<>();

    private final String[] indicatorCodes = new String[]{"ayp-1", "ayp-2", "ayp-3", "ayp-4", "ayp-5", "ayp-6", "ayp-7"};

    private final String[] indicatorSex = new String[]{"female", "male"};

    private final String[] indicatorAgeGroups = new String[]{"10-14", "15-19", "20-24"};

    private final Date reportDate;

    public AypOutSchoolReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorKeys(indicatorKeys);
    }

    private void setIndicatorKeys(List<String> list) {
        for (String indicatorCode : indicatorCodes) {
            list.add(indicatorCode + "-grand-total");
            for (String sex : indicatorSex) {
                for (String ageGroup : indicatorAgeGroups) {
                    list.add(indicatorCode + "-" + sex + "-" + ageGroup);
                }
                list.add(indicatorCode + "-" + sex + "-total");
            }
        }
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();

        for (String indicatorCode : indicatorKeys) {
            int value = ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate);
            indicatorDataObject.put(indicatorCode, value);
        }

        return indicatorDataObject;
    }
}
