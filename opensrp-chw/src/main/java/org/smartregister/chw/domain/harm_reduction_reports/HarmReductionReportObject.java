package org.smartregister.chw.domain.harm_reduction_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class HarmReductionReportObject extends ReportObject {
    private final Date reportDate;

    private final List<String> indicatorCodes = Arrays.asList(
            "hr-1",
            "hr-2",
            "hr-3",
            "hr-4",
            "hr-5",
            "hr-6",
            "hr-7",
            "hr-8",
            "hr-9",
            "hr-10",
            "hr-11",
            "hr-12",
            "hr-12a",
            "hr-12b",
            "hr-12c",
            "hr-12d",
            "hr-12e",
            "hr-13",
            "hr-14"
    );

    public HarmReductionReportObject(Date reportDate) {
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
