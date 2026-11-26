package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.R;
import org.smartregister.chw.util.Constants;

public class MotherMentorReportsViewActivity extends ChwReportsViewActivity {
    public static void startMe(Activity activity, String reportPath, String reportDate) {
        Intent intent = new Intent(activity, MotherMentorReportsViewActivity.class);
        intent.putExtra(ARG_REPORT_PATH, reportPath);
        intent.putExtra(ARG_REPORT_DATE, reportDate);
        intent.putExtra(ARG_REPORT_TITLE, R.string.mother_mentor_reports);
        intent.putExtra(ARG_REPORT_TYPE, Constants.ReportConstants.ReportTypes.MOTHER_MENTOR_REPORT);
        activity.startActivity(intent);
    }
}
