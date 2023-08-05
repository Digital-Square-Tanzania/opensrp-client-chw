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

public class IccmReportsActivity extends SecuredActivity implements View.OnClickListener {
    protected ConstraintLayout iccmClientsReport;

    protected ConstraintLayout dispensingSummary;

    protected ConstraintLayout malariaReport;

    protected AppBarLayout appBarLayout;

    Menu menu;

    private String reportPeriod = ReportUtils.getDefaultReportPeriod();

    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_iccm_reports);
        setUpToolbar();
        setupViews();
    }

    public void setupViews() {
        iccmClientsReport = findViewById(R.id.iccm_client_report);
        dispensingSummary = findViewById(R.id.iccm_dispensing_summary);
        malariaReport = findViewById(R.id.iccm_malaria_report);

        iccmClientsReport.setOnClickListener(this);
        dispensingSummary.setOnClickListener(this);
        malariaReport.setOnClickListener(this);
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
        int itemId = item.getItemId();
        if (itemId == R.id.action_select_month) {
            showMonthPicker(this, menu);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.iccm_client_report:
                IccmReportsViewActivity.startMe(this, Constants.ReportConstants.ReportPaths.ICCM_CLIENTS_REPORT_PATH, R.string.iccm_client_report, reportPeriod);
                break;
            case R.id.iccm_dispensing_summary:
                IccmReportsViewActivity.startMe(this, Constants.ReportConstants.ReportPaths.ICCM_DISPENSING_SUMMARY_PATH, R.string.iccm_dispensing_summary, reportPeriod);
                break;
            case R.id.iccm_malaria_report:
                IccmReportsViewActivity.startMe(this, Constants.ReportConstants.ReportPaths.MALARIA_MONTHLY_REPORT_PATH, R.string.iccm_malaria_report, reportPeriod);
                break;
            default:
                Toast.makeText(this, "Action Not Defined", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showMonthPicker(Context context, Menu menu) {
        //shows the month picker and returns selected period and updated the menu
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