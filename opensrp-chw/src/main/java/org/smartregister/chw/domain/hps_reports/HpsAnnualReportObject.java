package org.smartregister.chw.domain.hps_reports;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;
import org.apache.commons.lang3.StringUtils;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HpsAnnualReportObject extends ReportObject {
    private final String[] indicatorCodes = new String[]{
            "a-1", "a-2", "b-1", "b-2", "b-3","x-1","x-2","w-1", "w-2","y-1", "y-2","z-1", "z-2","u-1","u-2","u-3",
            "u-4","u-5", "u-6","u-7","u-8","u-9","u-10","u-11", "u-12","u-13","u-14","u-15","u-16","u-17","u-18",

    };
    private final String[] indicatorTableCodes = new String[]{
            "c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","v","vv"
    };
    private final String[] hpsQuestionsGroups = new String[]{"1","2","3","4","5","6"};

    private final Date reportDate;

    public HpsAnnualReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;

    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        JSONArray dataArray = new JSONArray();
        List<Map<String, String>> getHpsDynamicTablesList = ReportDao.getHpsAnnualDynamicTablesreports(reportDate);
        int totalofthewholehpsindicator = 0;

        for (String indicatorCode : indicatorCodes) {
            jsonObject.put(indicatorCode, ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate));
        }
        for (String indicatorTableCode : indicatorTableCodes) {   //rows
            for (String questionGroup : hpsQuestionsGroups) {
                    jsonObject.put( indicatorTableCode + "-" + questionGroup,
                            ReportDao.getReportPerIndicatorCode(indicatorTableCode + "-" + questionGroup, reportDate));
                totalofthewholehpsindicator+=ReportDao.getReportPerIndicatorCode(indicatorTableCode + "-" + questionGroup, reportDate);
                jsonObject.put(indicatorTableCode+"-total",totalofthewholehpsindicator); //total for all hps groups
            }
        }

        // Dyanamic Tables
        int i = 0;
        for (Map<String, String> getHpsDynamicTables : getHpsDynamicTablesList) {
            JSONObject reportJsonObject = new JSONObject();
            reportJsonObject.put("id", ++i);
            reportJsonObject.put("names", getHpsDynamicTable(getHpsDynamicTables, "names"));
            reportJsonObject.put("uic_id", getHpsDynamicTable(getHpsDynamicTables, "uic_id"));
            reportJsonObject.put("gender", getHpsDynamicTable(getHpsDynamicTables, "gender"));
            reportJsonObject.put("age", getHpsDynamicTable(getHpsDynamicTables, "age"));
            reportJsonObject.put("last_visit_date", getHpsDynamicTable(getHpsDynamicTables, "last_visit_date"));
            reportJsonObject.put("most_recent_appointment_date", getHpsDynamicTable(getHpsDynamicTables, "most_recent_appointment_date"));
            reportJsonObject.put("days_dispenses_last_visit", getHpsDynamicTable(getHpsDynamicTables, "days_dispenses_last_visit"));
            dataArray.put(reportJsonObject);
        }
        jsonObject.put("reportData", dataArray);
        return jsonObject;
    }

    private String getHpsDynamicTable(Map<String, String> chwRegistrationFollowupClient, String key) {
        String details = chwRegistrationFollowupClient.get(key);
        if (StringUtils.isNotBlank(details)) {
            return details;
        }
        return "-";
    }


}
