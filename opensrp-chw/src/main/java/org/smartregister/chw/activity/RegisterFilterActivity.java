package org.smartregister.chw.activity;

import static org.smartregister.chw.util.Constants.ENABLE_DATE_RANGE_FILTER;
import static org.smartregister.chw.util.Constants.ENABLE_HIV_STATUS_FILTER;
import static org.smartregister.chw.util.Constants.ENABLE_PREP_STATUS_FILTER;
import static org.smartregister.chw.util.Constants.FILTERS_ENABLED;
import static org.smartregister.chw.util.Constants.FILTER_APPOINTMENT_DATE;
import static org.smartregister.chw.util.Constants.FILTER_APPOINTMENT_DATE_RANGE_END_DATE;
import static org.smartregister.chw.util.Constants.FILTER_APPOINTMENT_DATE_RANGE_START_DATE;
import static org.smartregister.chw.util.Constants.FILTER_HIV_STATUS;
import static org.smartregister.chw.util.Constants.FILTER_PREP_STATUS;
import static org.smartregister.chw.util.Constants.REQUEST_FILTERS;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import org.joda.time.DateTime;
import org.smartregister.chw.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegisterFilterActivity extends AppCompatActivity {
    private final Calendar mCalendar = Calendar.getInstance();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
    private SwitchCompat enableFilter;
    private TextView summaryAppointmentDate;
    private TextView summaryStartNextAppointmentDate;
    private TextView summaryEndNextAppointmentDate;
    private Spinner hivStatusFilter;
    private Spinner prepStatusFilter;
    private List<String> hivFilterOptions;
    private List<String> prepStatusFilterOptions;
    private boolean enableHivStatusFilter;
    private boolean enablePrepStatusFilter;
    private boolean enableDateRangeFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_filter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Filter");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            final Drawable upArrow = getResources().getDrawable(org.smartregister.chw.core.R.drawable.ic_arrow_back_white_24dp);
            actionBar.setHomeAsUpIndicator(upArrow);
            actionBar.setElevation(0);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        hivFilterOptions = Arrays.asList(getResources().getStringArray(R.array.hiv_status_filter_options));
        prepStatusFilterOptions = Arrays.asList(getResources().getStringArray(R.array.prep_status_filter_options));
        enableHivStatusFilter = getIntent().getBooleanExtra(ENABLE_HIV_STATUS_FILTER, true);
        enablePrepStatusFilter = getIntent().getBooleanExtra(ENABLE_PREP_STATUS_FILTER, false);
        enableDateRangeFilter = getIntent().getBooleanExtra(ENABLE_DATE_RANGE_FILTER, false);

        setupViews();
        boolean filterEnabled = getIntent().getBooleanExtra(FILTERS_ENABLED, false);
        String filterHivStatus = getIntent().getStringExtra(FILTER_HIV_STATUS);
        String filterPrepStatus = getIntent().getStringExtra(FILTER_PREP_STATUS);
        String appointmentDate = getIntent().getStringExtra(FILTER_APPOINTMENT_DATE);
        String appointmentDateRangeStartDate = getIntent().getStringExtra(FILTER_APPOINTMENT_DATE_RANGE_START_DATE);
        String appointmentDateRangeEndDate = getIntent().getStringExtra(FILTER_APPOINTMENT_DATE_RANGE_END_DATE);
        if (filterEnabled) {
            enableFilter.setChecked(true);

            if (appointmentDate != null) {
                summaryAppointmentDate.setText(appointmentDate);
            }


            if (appointmentDateRangeStartDate != null) {
                summaryStartNextAppointmentDate.setText(appointmentDateRangeStartDate);
            }


            if (appointmentDateRangeEndDate != null) {
                summaryEndNextAppointmentDate.setText(appointmentDateRangeEndDate);
            }

            if (filterHivStatus != null) {
                if (hivFilterOptions.contains(filterHivStatus)) {
                    hivStatusFilter.setSelection(hivFilterOptions.indexOf(filterHivStatus));
                }
            }

            if (filterPrepStatus != null && prepStatusFilterOptions.contains(filterPrepStatus)) {
                prepStatusFilter.setSelection(prepStatusFilterOptions.indexOf(filterPrepStatus));
            }
        }
    }

    public void setupViews() {
        enableFilter = findViewById(R.id.enable_filter_switch);
        TextView filterStatus = findViewById(R.id.filter_status);
        summaryAppointmentDate = findViewById(R.id.summary_appointment_date);
        summaryStartNextAppointmentDate = findViewById(R.id.summary_start_date);
        summaryEndNextAppointmentDate = findViewById(R.id.summary_end_date);
        LinearLayout filterList = findViewById(R.id.filters_list);
        hivStatusFilter = findViewById(R.id.hiv_status_filter);
        prepStatusFilter = findViewById(R.id.prep_status_filter);
        LinearLayout nextAppointmentDate = findViewById(R.id.next_appointment_date);
        LinearLayout startDateLinearLayout = findViewById(R.id.start_date);
        LinearLayout endDateLinearLayout = findViewById(R.id.end_date);

        if (enableFilter.isChecked()) {
            filterList.setVisibility(View.VISIBLE);
        } else {
            filterList.setVisibility(View.GONE);
        }

        enableFilter.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                filterStatus.setText(R.string.filter_status_on);
                filterList.setVisibility(View.VISIBLE);
            } else {
                filterStatus.setText(R.string.filter_status_off);
                filterList.setVisibility(View.GONE);
                summaryAppointmentDate.setText(R.string.none);
                summaryStartNextAppointmentDate.setText(R.string.none);
                summaryEndNextAppointmentDate.setText(R.string.none);
                hivStatusFilter.setSelection(0);
                prepStatusFilter.setSelection(0);
            }
        });

        if (enableHivStatusFilter) {
            findViewById(R.id.hiv_status_filter_rl).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.hiv_status_filter_rl).setVisibility(View.GONE);
        }

        if (enablePrepStatusFilter) {
            findViewById(R.id.prep_client_status_filter_rl).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.prep_client_status_filter_rl).setVisibility(View.GONE);
        }

        if (enableDateRangeFilter) {
            findViewById(R.id.next_appointment_date_range).setVisibility(View.VISIBLE);
            findViewById(R.id.next_appointment_date).setVisibility(View.GONE);
        } else {
            findViewById(R.id.next_appointment_date_range).setVisibility(View.GONE);
            findViewById(R.id.next_appointment_date).setVisibility(View.VISIBLE);
        }

        DatePickerDialog.OnDateSetListener datePickerListener = (mView, year, monthOfYear, dayOfMonth) -> {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, monthOfYear);
            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            summaryAppointmentDate.setText(mDateFormat.format(mCalendar.getTime()));
        };

        DatePickerDialog.OnDateSetListener startDatePickerListener = (mView, year, monthOfYear, dayOfMonth) -> {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, monthOfYear);
            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            summaryStartNextAppointmentDate.setText(mDateFormat.format(mCalendar.getTime()));

            if (summaryEndNextAppointmentDate.getText().equals(getResources().getString(R.string.none))) {
                summaryEndNextAppointmentDate.setText(mDateFormat.format(mCalendar.getTime()));
            }
        };

        DatePickerDialog.OnDateSetListener endDatePickerListener = (mView, year, monthOfYear, dayOfMonth) -> {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, monthOfYear);
            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            summaryEndNextAppointmentDate.setText(mDateFormat.format(mCalendar.getTime()));
        };

        nextAppointmentDate.setOnClickListener(view -> {
            DatePickerDialog dialog = new DatePickerDialog(RegisterFilterActivity.this, datePickerListener, mCalendar
                    .get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH));

            dialog.getDatePicker().setSpinnersShown(true);
            dialog.getDatePicker().setMinDate(new Date().getTime());
            dialog.getDatePicker().setMaxDate(new DateTime().plusMonths(6).toDate().getTime());

            dialog.show();
        });

        startDateLinearLayout.setOnClickListener(view -> {
            DatePickerDialog startDateDialog = new DatePickerDialog(RegisterFilterActivity.this, startDatePickerListener,
                    summaryStartNextAppointmentDate.getText().equals(getResources().getString(R.string.none)) ? mCalendar.get(Calendar.YEAR) : Integer.parseInt(summaryStartNextAppointmentDate.getText().toString().split("-")[2]),
                    summaryStartNextAppointmentDate.getText().equals(getResources().getString(R.string.none)) ? mCalendar.get(Calendar.MONTH) : Integer.parseInt(summaryStartNextAppointmentDate.getText().toString().split("-")[1]) - 1,
                    summaryStartNextAppointmentDate.getText().equals(getResources().getString(R.string.none)) ? mCalendar.get(Calendar.DAY_OF_MONTH) : Integer.parseInt(summaryStartNextAppointmentDate.getText().toString().split("-")[0])
            );

            startDateDialog.getDatePicker().setSpinnersShown(true);
            startDateDialog.getDatePicker().setMinDate(new Date().getTime());
            startDateDialog.getDatePicker().setMaxDate(new DateTime().plusMonths(6).toDate().getTime());

            startDateDialog.show();
        });

        endDateLinearLayout.setOnClickListener(view -> {
            DatePickerDialog endDateDialog = new DatePickerDialog(RegisterFilterActivity.this, endDatePickerListener,
                    summaryEndNextAppointmentDate.getText().equals(getResources().getString(R.string.none)) ? mCalendar.get(Calendar.YEAR) : Integer.parseInt(summaryEndNextAppointmentDate.getText().toString().split("-")[2]),
                    summaryEndNextAppointmentDate.getText().equals(getResources().getString(R.string.none)) ? mCalendar.get(Calendar.MONTH) : Integer.parseInt(summaryEndNextAppointmentDate.getText().toString().split("-")[1]) - 1,
                    summaryEndNextAppointmentDate.getText().equals(getResources().getString(R.string.none)) ? mCalendar.get(Calendar.DAY_OF_MONTH) : Integer.parseInt(summaryEndNextAppointmentDate.getText().toString().split("-")[0])
            );

            endDateDialog.getDatePicker().setSpinnersShown(true);
            endDateDialog.getDatePicker().setMinDate(new Date().getTime());
            endDateDialog.getDatePicker().setMaxDate(new DateTime().plusMonths(6).toDate().getTime());

            endDateDialog.show();
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filter_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save) {
            Intent intent = new Intent();
            intent.putExtra(FILTERS_ENABLED, enableFilter.isChecked());
            if (enableFilter.isChecked()) {
                intent.putExtra(FILTER_APPOINTMENT_DATE, summaryAppointmentDate.getText());
                intent.putExtra(FILTER_APPOINTMENT_DATE_RANGE_START_DATE, summaryStartNextAppointmentDate.getText());
                intent.putExtra(FILTER_APPOINTMENT_DATE_RANGE_END_DATE, summaryEndNextAppointmentDate.getText());
                intent.putExtra(FILTER_HIV_STATUS, hivFilterOptions.get(hivStatusFilter.getSelectedItemPosition()));
                intent.putExtra(FILTER_PREP_STATUS, prepStatusFilterOptions.get(prepStatusFilter.getSelectedItemPosition()));
            }

            setResult(REQUEST_FILTERS, intent);
            setResult(RESULT_OK, intent);
            finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
