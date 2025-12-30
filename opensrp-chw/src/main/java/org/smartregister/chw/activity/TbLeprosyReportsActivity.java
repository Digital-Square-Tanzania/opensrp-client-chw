package org.smartregister.chw.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.whiteelephant.monthpicker.MonthPickerDialog;

import org.smartregister.chw.R;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.ReportUtils;
import org.smartregister.view.activity.SecuredActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class TbLeprosyReportsActivity extends SecuredActivity implements View.OnClickListener {

    protected ConstraintLayout tbLeprosyCommunityReport;

    protected ConstraintLayout tbLeprosyHouseholdReport;

    protected ConstraintLayout tbLeprosySpecialAreasReport;

    protected ConstraintLayout tbLeprosyTreatmentStatusReport;

    protected ConstraintLayout tbLeprosyServiceChallengesReport;

    protected AppBarLayout appBarLayout;

    private Menu menu;

    private String reportPeriod = ReportUtils.getDefaultReportPeriod();

    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_tb_leprosy_reports);
        setUpToolbar();
        setupViews();
    }

    public void setupViews() {
        tbLeprosyCommunityReport = findViewById(R.id.tb_leprosy_report_community);
        tbLeprosyHouseholdReport = findViewById(R.id.tb_leprosy_report_household);
        tbLeprosySpecialAreasReport = findViewById(R.id.tb_leprosy_report_special_areas);
        tbLeprosyTreatmentStatusReport = findViewById(R.id.tb_leprosy_report_treatment);
        tbLeprosyServiceChallengesReport = findViewById(R.id.tb_leprosy_report_challenges);

        tbLeprosyCommunityReport.setOnClickListener(this);
        tbLeprosyHouseholdReport.setOnClickListener(this);
        tbLeprosySpecialAreasReport.setOnClickListener(this);
        tbLeprosyTreatmentStatusReport.setOnClickListener(this);
        tbLeprosyServiceChallengesReport.setOnClickListener(this);
    }

    public void setUpToolbar() {
        Toolbar toolbar = findViewById(org.smartregister.chw.core.R.id.back_to_nav_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            final Drawable upArrow = getResources().getDrawable(org.smartregister.chw.core.R.drawable.ic_arrow_back_white_24dp);
            actionBar.setHomeAsUpIndicator(upArrow);
            actionBar.setElevation(0);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        appBarLayout = findViewById(org.smartregister.chw.core.R.id.app_bar);
        appBarLayout.setOutlineProvider(null);
    }

    @Override
    protected void onResumption() {
        // intentionally left blank
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_menu, menu);
        this.menu = menu;
        this.menu.findItem(R.id.action_select_month).setTitle(ReportUtils.displayMonthAndYear());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select_month) {
            showMonthPicker(this, menu);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tb_leprosy_report_community) {
            TbLeprosyReportsViewActivity.startMe(this,
                    Constants.ReportConstants.ReportPaths.TBLEPROSY_COMMUNITY_REPORT_PATH,
                    R.string.tb_leprosy_report_section_community,
                    reportPeriod);
        } else if (id == R.id.tb_leprosy_report_household) {
            TbLeprosyReportsViewActivity.startMe(this,
                    Constants.ReportConstants.ReportPaths.TBLEPROSY_HOUSEHOLD_REPORT_PATH,
                    R.string.tb_leprosy_report_section_household,
                    reportPeriod);
        } else if (id == R.id.tb_leprosy_report_special_areas) {
            TbLeprosyReportsViewActivity.startMe(this,
                    Constants.ReportConstants.ReportPaths.TBLEPROSY_SPECIAL_REPORT_PATH,
                    R.string.tb_leprosy_report_section_special,
                    reportPeriod);
        } else if (id == R.id.tb_leprosy_report_treatment) {
            TbLeprosyReportsViewActivity.startMe(this,
                    Constants.ReportConstants.ReportPaths.TBLEPROSY_TREATMENT_STATUS_REPORT_PATH,
                    R.string.tb_leprosy_report_section_treatment,
                    reportPeriod);
        } else if (id == R.id.tb_leprosy_report_challenges) {
            TbLeprosyReportsViewActivity.startMe(this,
                    Constants.ReportConstants.ReportPaths.TBLEPROSY_SERVICE_CHALLENGES_REPORT_PATH,
                    R.string.tb_leprosy_report_section_challenges,
                    reportPeriod);
        } else {
            Toast.makeText(this, R.string.action_not_defined, Toast.LENGTH_SHORT).show();
        }
    }

    private void showMonthPicker(Context context, Menu menu) {
        MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(context, (selectedMonth, selectedYear) -> {
            int month = selectedMonth + 1;
            String monthString = String.valueOf(month);
            if (month < 10) {
                monthString = "0" + monthString;
            }
            String yearString = String.valueOf(selectedYear);
            reportPeriod = monthString + "-" + yearString;
            menu.findItem(R.id.action_select_month).setTitle(ReportUtils.displayMonthAndYear(selectedMonth, selectedYear));
        }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH));

        try {
            Date reportDate = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).parse(reportPeriod);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(reportDate);
            builder.setActivatedMonth(calendar.get(Calendar.MONTH));
            builder.setMinYear(2021);
            builder.setActivatedYear(calendar.get(Calendar.YEAR));
            builder.setMaxYear(Calendar.getInstance().get(Calendar.YEAR));
            builder.setMinMonth(Calendar.JANUARY);
            builder.setMaxMonth(Calendar.DECEMBER);
            builder.setTitle("Select Month 0");
            builder.build().show();
        } catch (ParseException e) {
            Timber.e(e);
        }
    }
}
