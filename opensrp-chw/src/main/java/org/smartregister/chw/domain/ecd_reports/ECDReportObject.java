package org.smartregister.chw.domain.ecd_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ECDReportObject extends ReportObject {
    private static final List<String> ECD_INDICATORS_KEYS = Arrays.asList(
            "ecd-1-number-of-households-enrolled",
            "ecd-2-number-of-clients-enrolled-anc",
            "ecd-2-number-of-clients-enrolled-pnc",
            "ecd-2-number-of-clients-enrolled-child-under-5",
            "ecd-3-number-of-client-with-danger-sign-and-referral-issued-anc",
            "ecd-3-number-of-client-with-danger-sign-and-referral-issued-pnc",
            "ecd-3-number-of-client-with-danger-sign-and-referral-issued-child-under-5",
            "ecd-4-number-of-completed-referrals-anc",
            "ecd-4-number-of-completed-referrals-pnc",
            "ecd-4-number-of-completed-referrals-child-under-5",
            "ecd-5-number-of-caregiver-received-ecd-ME-years-10-24",
            "ecd-5-number-of-caregiver-received-ecd-ME-years-over-24",
            "ecd-5-number-of-caregiver-received-ecd-kE-years-10-24",
            "ecd-5-number-of-caregiver-received-ecd-KE-years-over-24",
            "ecd-6-number-of-children-received-ecd-0-5-years-with-caregiver-age-10-25-years",
            "ecd-6-number-of-children-received-ecd-0-5-years-with-caregiver-age-over-25-years",
            "ecd-7-number-of-caregiver-created-play-material-with-age-10-25-years",
            "ecd-7-number-of-caregiver-created-play-material-with-over-25-years"
    );

    private static final List<String> COMPUTED_INDICATORS_VALUE_KEYS = Arrays.asList(
            "ecd-2-number-of-clients-enrolled-total",
            "ecd-3-number-of-client-with-danger-sign-and-referral-issued-total",
            "ecd-4-number-of-completed-referrals-total",
            "ecd-5-number-of-caregiver-received-ecd-ME-total",
            "ecd-5-number-of-caregiver-received-ecd-KE-total",
            "ecd-5-number-of-caregiver-received-ecd-total_ME_KE",
            "ecd-6-number-of-children-received-ecd-0-5-years-with-caregiver-total",
            "ecd-7-number-of-caregiver-created-play-material-total"
    );

    private final Date reportDate;
    public ECDReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONObject indicatorDataObject = new JSONObject();

        for (String indicatorKey : ECD_INDICATORS_KEYS) {
            indicatorDataObject.put(indicatorKey, ReportDao.getReportPerIndicatorCode(indicatorKey, reportDate));
        }

        addComputedIndicators(indicatorDataObject);

        return indicatorDataObject;
    }

    private void addComputedIndicators(JSONObject indicatorDataObject) throws JSONException {
        for (String key : COMPUTED_INDICATORS_VALUE_KEYS) {
            int total = 0;
            switch (key) {
                case "ecd-2-number-of-clients-enrolled-total":
                    total = getTotal(indicatorDataObject,
                            "ecd-2-number-of-clients-enrolled-anc",
                            "ecd-2-number-of-clients-enrolled-pnc",
                            "ecd-2-number-of-clients-enrolled-child-under-5");
                    break;
                case "ecd-3-number-of-client-with-danger-sign-and-referral-issued-total":
                    total = getTotal(indicatorDataObject,
                            "ecd-3-number-of-client-with-danger-sign-and-referral-issued-anc",
                            "ecd-3-number-of-client-with-danger-sign-and-referral-issued-pnc",
                            "ecd-3-number-of-client-with-danger-sign-and-referral-issued-child-under-5");
                    break;
                case "ecd-4-number-of-completed-referrals-total":
                    total = getTotal(indicatorDataObject,
                            "ecd-4-number-of-completed-referrals-anc",
                            "ecd-4-number-of-completed-referrals-pnc",
                            "ecd-4-number-of-completed-referrals-child-under-5");
                    break;
                case "ecd-5-number-of-caregiver-received-ecd-ME-total":
                    total = getTotal(indicatorDataObject,
                            "ecd-5-number-of-caregiver-received-ecd-ME-years-10-24",
                            "ecd-5-number-of-caregiver-received-ecd-ME-years-over-24");
                    break;
                case "ecd-5-number-of-caregiver-received-ecd-KE-total":
                    total = getTotal(indicatorDataObject,
                            "ecd-5-number-of-caregiver-received-ecd-kE-years-10-24",
                            "ecd-5-number-of-caregiver-received-ecd-KE-years-over-24");
                    break;
                case "ecd-5-number-of-caregiver-received-ecd-total_ME_KE":
                    total = getTotal(indicatorDataObject,
                            "ecd-5-number-of-caregiver-received-ecd-ME-total",
                            "ecd-5-number-of-caregiver-received-ecd-KE-total");
                    break;
                case "ecd-6-number-of-children-received-ecd-0-5-years-with-caregiver-total":
                    total = getTotal(indicatorDataObject,
                            "ecd-6-number-of-children-received-ecd-0-5-years-with-caregiver-age-10-25-years",
                            "ecd-6-number-of-children-received-ecd-0-5-years-with-caregiver-age-over-25-years");
                    break;
                case "ecd-7-number-of-caregiver-created-play-material-total":
                    total = getTotal(indicatorDataObject,
                            "ecd-7-number-of-caregiver-created-play-material-with-age-10-25-years",
                            "ecd-7-number-of-caregiver-created-play-material-with-over-25-years");
                    break;
            }
            indicatorDataObject.put(key, total);
        }
    }

    private int getTotal(JSONObject jsonObject, String... keys) throws JSONException {
        int total = 0;
        for (String key : keys) {
            total += jsonObject.getInt(key);
        }
        return total;
    }
}
