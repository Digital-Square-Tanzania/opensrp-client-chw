package org.smartregister.chw.domain.harm_reduction_sober_house_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class HarmReductionSoberHouseReportObject extends ReportObject {
    private final Date reportDate;

    private final List<String> indicatorCodes = Arrays.asList(
            "sh-1",
            "sh-2",
            "sh-2a",
            "sh-2b",
            "sh-2c",
            "sh-2d",
            "sh-2e",
            "sh-4",
            "sh-4a",
            "sh-4b",
            "sh-5",
            "sh-5a",
            "sh-5b",
            "sh-5c",
            "sh-5d",
            "sh-5e",
            "sh-6",
            "sh-6a",
            "sh-6b",
            "sh-6c",
            "sh-6d",
            "sh-7",
            "sh-7a",
            "sh-7b",
            "sh-7c",
            "sh-7d",
            "sh-7e",
            "sh-7f",
            "sh-7g",
            "sh-7h",
            "sh-8",
            "sh-8a",
            "sh-8b",
            "sh-8c",
            "sh-8d",
            "sh-8e",
            "sh-8f",
            "sh-8g",
            "sh-8h",
            "sh-8i",
            "sh-8j",
            "sh-9",
            "sh-9a",
            "sh-9b",
            "sh-9c",
            "sh-9d",
            "sh-9e",
            "sh-9f",
            "sh-9g",
            "sh-9h",
            "sh-9i",
            "sh-9j",
            "sh-10",
            "sh-10a",
            "sh-10b",
            "sh-10c"
    );

    public HarmReductionSoberHouseReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : indicatorCodes) {
            indicatorDataObject.put(indicatorCode, ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate));
        }
        return indicatorDataObject;
    }
}
