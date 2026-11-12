package org.smartregister.chw.activity;

import android.view.View;
import android.widget.TextView;

import org.smartregister.chw.R;
import org.smartregister.chw.util.Constants;

public class MotherMentorReportsActivity extends CBHSReportsActivity {
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.cbhs_monthly_summary) {
            MotherMentorReportsViewActivity.startMe(this,
                    Constants.ReportConstants.ReportPaths.MOTHER_MENTOR_REPORT_PATH, reportPeriod);
        }
    }

    @Override
    public void setUpToolbar() {
        super.setUpToolbar();
        TextView title = findViewById(R.id.toolbar_title);
        title.setText(R.string.mother_mentor_reports);
    }
}
