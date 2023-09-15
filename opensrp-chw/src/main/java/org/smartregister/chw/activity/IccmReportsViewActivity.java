package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.util.Constants;


public class IccmReportsViewActivity extends ChwReportsViewActivity {
    public static void startMe(Activity activity, String reportPath, int reportTitle, String reportDate) {
        Intent intent = new Intent(activity, IccmReportsViewActivity.class);
        intent.putExtra(ARG_REPORT_PATH, reportPath);
        intent.putExtra(ARG_REPORT_DATE, reportDate);
        intent.putExtra(ARG_REPORT_TITLE, reportTitle);
        intent.putExtra(ARG_REPORT_TYPE, Constants.ReportConstants.ReportTypes.ICCM_REPORT);
        activity.startActivity(intent);
    }
}
