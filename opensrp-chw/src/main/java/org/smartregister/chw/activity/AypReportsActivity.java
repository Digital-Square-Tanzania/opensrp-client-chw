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

public class AypReportsActivity extends SecuredActivity implements View.OnClickListener {

    protected ConstraintLayout aypMonthlyReport;

    protected ConstraintLayout aypParentalMonthlyReport;

    protected AppBarLayout appBarLayout;

    private Menu menu;

    private String reportPeriod = ReportUtils.getDefaultReportPeriod();

    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_ayp_reports);
        setUpToolbar();
        setupViews();
    }

    public void setupViews() {
        aypMonthlyReport = findViewById(R.id.ayp_in_school_monthly_report);
        if (aypMonthlyReport != null) {
            aypMonthlyReport.setOnClickListener(this);
        }
        aypParentalMonthlyReport = findViewById(R.id.ayp_parental_monthly_report);
        if (aypParentalMonthlyReport != null) {
            aypParentalMonthlyReport.setOnClickListener(this);
        }
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
        View titleView = toolbar.findViewById(org.smartregister.chw.core.R.id.toolbar_title);
        if (titleView instanceof org.smartregister.view.customcontrols.CustomFontTextView) {
            ((org.smartregister.view.customcontrols.CustomFontTextView) titleView).setText(R.string.ayp_reports_screen_title);
        }
    }

    @Override
    protected void onResumption() {
        //overridden
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
        int viewId = v.getId();
        if (viewId == R.id.ayp_in_school_monthly_report) {
            AypReportsViewActivity.startMe(this,
                    Constants.ReportConstants.ReportPaths.AYP_IN_SCHOOL_REPORT_PATH,
                    R.string.ayp_in_school_reports_title,
                    reportPeriod,
                    Constants.ReportConstants.ReportTypes.AYP_REPORT);
            return;
        }
        if (viewId == R.id.ayp_parental_monthly_report) {
            AypReportsViewActivity.startMe(this,
                    Constants.ReportConstants.ReportPaths.AYP_PARENTAL_REPORT_PATH,
                    R.string.ayp_parental_reports_title,
                    reportPeriod,
                    Constants.ReportConstants.ReportTypes.AYP_REPORT);
            return;
        }
        Toast.makeText(this, "Action Not Defined", Toast.LENGTH_SHORT).show();
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
            builder.setTitle("Select Month");
            builder.build().show();
        } catch (ParseException e) {
            Timber.e(e);
        }
    }
}
