package org.smartregister.chw.domain.hps_reports;

import android.util.Log;

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
            "hps-a-1" ,"hps-a-2" ,"hps-b-1" ,"hps-b-2" ,"hps-b-3","hps-x-1","hps-x-2","hps-w-1" ,"hps-w-2","hps-y-1" ,"hps-y-2","hps-z-1" ,"hps-z-2","hps-u-1","hps-u-2","hps-u-3",
            "hps-u-4","hps-u-5" ,"hps-u-6","hps-u-7","hps-u-8","hps-u-9","hps-u-10","hps-u-11" ,"hps-u-12","hps-u-13","hps-u-14","hps-u-15","hps-u-16","hps-u-17","hps-u-18",

    };
    private final String[] indicatorTableCodes = new String[]{
            "hps-c","hps-d","hps-e","hps-f","hps-g","hps-h","hps-i","hps-j","hps-k","hps-l","hps-m","hps-n","hps-o","hps-p","hps-q","hps-r","hps-s","hps-t","hps-v","hps-vv"
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
            jsonObject.put(indicatorCode, ReportDao.getAnnualReportPerIndicatorCode(indicatorCode, reportDate));
        }
        for (String indicatorTableCode : indicatorTableCodes) {   //rows
            for (String questionGroup : hpsQuestionsGroups) {
                    jsonObject.put( indicatorTableCode + "-" + questionGroup,
                            ReportDao.getAnnualReportPerIndicatorCode(indicatorTableCode + "-" + questionGroup, reportDate));
                totalofthewholehpsindicator+=ReportDao.getAnnualReportPerIndicatorCode(indicatorTableCode + "-" + questionGroup, reportDate);
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
