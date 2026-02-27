package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

public class AypReportsViewActivity extends ChwReportsViewActivity {
    public static void startMe(Activity activity, String reportPath, int reportTitle, String reportDate, String reportType) {
        Intent intent = new Intent(activity, AypReportsViewActivity.class);
        intent.putExtra(ARG_REPORT_PATH, reportPath);
        intent.putExtra(ARG_REPORT_DATE, reportDate);
        intent.putExtra(ARG_REPORT_TITLE, reportTitle);
        intent.putExtra(ARG_REPORT_TYPE, reportType);
        activity.startActivity(intent);
    }
}
