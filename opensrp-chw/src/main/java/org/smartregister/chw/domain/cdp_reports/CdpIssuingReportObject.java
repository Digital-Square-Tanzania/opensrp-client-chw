package org.smartregister.chw.domain.cdp_reports;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class CdpIssuingReportObject extends ReportObject {
    private Date reportDate;

    public CdpIssuingReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }


    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONArray dataArray = new JSONArray();
        List<Map<String, String>> getHfCdpStockLogList = ReportDao.getHfIssuingCdpStockLog(reportDate);

        int i = 0;
        int flag_count_female = 0;
        int flag_count_male = 0;
        for (Map<String, String> getHfCdpStockLog : getHfCdpStockLogList) {
            JSONObject reportJsonObject = new JSONObject();
            reportJsonObject.put("id", ++i);

            if (getCdpClientDetails(getHfCdpStockLog, "visit_key").equals("restocked_male_condoms")) {
                reportJsonObject.put("outlet-name", getCdpClientDetails(getHfCdpStockLog, "outlet_name"));
                reportJsonObject.put("male-condoms-offset", getCdpClientDetails(getHfCdpStockLog, "details"));
                reportJsonObject.put("female-condoms-offset", getCdpClientDetails(getHfCdpStockLog, "0"));
                flag_count_male += Integer.parseInt(getCdpClientDetails(getHfCdpStockLog, "details"));
            }

            if (getCdpClientDetails(getHfCdpStockLog, "visit_key").equals("restocked_female_condoms")) {
                reportJsonObject.put("outlet-name", getCdpClientDetails(getHfCdpStockLog, "outlet_name"));
                reportJsonObject.put("male-condoms-offset", getCdpClientDetails(getHfCdpStockLog, "0"));
                reportJsonObject.put("female-condoms-offset", getCdpClientDetails(getHfCdpStockLog, "details"));
                flag_count_female += Integer.parseInt(getCdpClientDetails(getHfCdpStockLog, "details"));
            }

            dataArray.put(reportJsonObject);
        }

        //finally go display total of all
        if (flag_count_male > 0 || flag_count_female > 0) {
            JSONObject reportJsonObject = new JSONObject();
            reportJsonObject.put("total-id", i + 1);
            reportJsonObject.put("total", "TOTAL NUMBER OF CONDOMS ISSUED");
            reportJsonObject.put("total-male-condoms", flag_count_male);
            reportJsonObject.put("total-female-condoms", flag_count_female);
            dataArray.put(reportJsonObject);
        }


        JSONObject resultJsonObject = new JSONObject();
        resultJsonObject.put("reportData", dataArray);

        return resultJsonObject;
    }

    private String getCdpClientDetails(Map<String, String> chwRegistrationFollowupClient, String key) {
        String details = chwRegistrationFollowupClient.get(key);  //get all details
        if (StringUtils.isNotBlank(details)) {
            return details;
        } else {
            if (key.equals("0")) {
                return "0";
            } else {
                return "-";
            }
        }

    }

}