package org.smartregister.chw.activity;

import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.appbar.AppBarLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.job.ChwIndicatorGeneratingJob;
import org.smartregister.reporting.domain.TallyStatus;
import org.smartregister.reporting.event.IndicatorTallyEvent;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.SecuredActivity;
import org.smartregister.view.customcontrols.CustomFontTextView;

public class InAppReportsActivity extends SecuredActivity implements View.OnClickListener {
    protected CustomFontTextView toolBarTextView;

    protected AppBarLayout appBarLayout;

    protected ConstraintLayout cbhsReportsLayout;

    protected ConstraintLayout motherChampionReportsLayout;

    protected ConstraintLayout condomDistributionReports;

    protected ConstraintLayout agywReports;

    protected ConstraintLayout iccmReports;
    protected ConstraintLayout ecdReports;

    protected ConstraintLayout sbcReports;

    protected TextView textViewLogs;

    protected ConstraintLayout asrhReports;

    protected ConstraintLayout cecapReports;

    protected ConstraintLayout kvpReports;

    @Override
    protected void onCreation() {
        ChwIndicatorGeneratingJob.scheduleJobImmediately(ChwIndicatorGeneratingJob.TAG);
        setContentView(R.layout.activity_in_app_reports);
        setUpToolbar();
        setUpViews();
    }

    @Override
    protected void onResumption() {
        //overridden
    }


    public void setUpViews() {
        cbhsReportsLayout = findViewById(R.id.cbhs_summary);
        motherChampionReportsLayout = findViewById(R.id.mother_champion_reports);
        condomDistributionReports = findViewById(R.id.cdp_reports);
        agywReports = findViewById(R.id.agyw_reports);
        iccmReports = findViewById(R.id.iccm_reports);
        ecdReports = findViewById(R.id.ecd_reports);
        sbcReports = findViewById(R.id.sbc_reports);
        textViewLogs = findViewById(R.id.textView_logs);

        AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
        SharedPreferences preferences = allSharedPreferences.getPreferences();
        String teamRoleIdentifier = "";
        if (preferences != null) {
            teamRoleIdentifier = preferences.getString(TEAM_ROLE_IDENTIFIER, "");
        }
        asrhReports = findViewById(R.id.asrh_reports);
        cecapReports = findViewById(R.id.cecap_reports);
        kvpReports = findViewById(R.id.kvp_reports);

        if (!teamRoleIdentifier.isEmpty()) {
            switch (teamRoleIdentifier) {
                case "mother_champion":
                    motherChampionReportsLayout.setVisibility(View.VISIBLE);
                    break;
                case "cbhs_provider":
                    cbhsReportsLayout.setVisibility(View.VISIBLE);
                    break;
                case "iccm_provider":
                    iccmReports.setVisibility(View.VISIBLE);
                    break;
                default:
                    if (ChwApplication.getApplicationFlavor().hasHIV()) {
                        cbhsReportsLayout.setVisibility(View.VISIBLE);
                    }

                    if (ChwApplication.getApplicationFlavor().hasPmtct()) {
                        motherChampionReportsLayout.setVisibility(View.VISIBLE);
                    }

                    if (ChwApplication.getApplicationFlavor().hasAGYW()) {
                        agywReports.setVisibility(View.VISIBLE);
                    }

                    if (ChwApplication.getApplicationFlavor().hasSbc()) {
                        sbcReports.setVisibility(View.VISIBLE);
                    }

                    if (ChwApplication.getApplicationFlavor().hasCdp()) {
                        condomDistributionReports.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        } else {
            if (ChwApplication.getApplicationFlavor().hasHIV()) {
                cbhsReportsLayout.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasPmtct()) {
                motherChampionReportsLayout.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasAGYW()) {
                agywReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasICCM()) {
                iccmReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasSbc()) {
                sbcReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasCdp()) {
                condomDistributionReports.setVisibility(View.VISIBLE);
            }
            if (ChwApplication.getApplicationFlavor().hasAsrh()) {
                asrhReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasCecap()) {
                cecapReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasKvp()) {
                kvpReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasCdp()) {
                condomDistributionReports.setVisibility(View.VISIBLE);
            }
        }

        ecdReports.setVisibility(View.VISIBLE);

        motherChampionReportsLayout.setOnClickListener(this);
        condomDistributionReports.setOnClickListener(this);
        cbhsReportsLayout.setOnClickListener(this);
        agywReports.setOnClickListener(this);
        iccmReports.setOnClickListener(this);
        ecdReports.setOnClickListener(this);
        sbcReports.setOnClickListener(this);
        asrhReports.setOnClickListener(this);
        cecapReports.setOnClickListener(this);
        kvpReports.setOnClickListener(this);
    }

    public void setUpToolbar() {
        Toolbar toolbar = findViewById(org.smartregister.chw.core.R.id.back_to_nav_toolbar);
        toolBarTextView = toolbar.findViewById(org.smartregister.chw.core.R.id.toolbar_title);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            final Drawable upArrow = getResources().getDrawable(org.smartregister.chw.core.R.drawable.ic_arrow_back_white_24dp);
            actionBar.setHomeAsUpIndicator(upArrow);
            actionBar.setElevation(0);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolBarTextView.setText(R.string.reports_title);
        toolBarTextView.setOnClickListener(v -> finish());
        appBarLayout = findViewById(org.smartregister.chw.core.R.id.app_bar);
        appBarLayout.setOutlineProvider(null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cbhs_summary) {
            Intent intent = new Intent(this, CBHSReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.mother_champion_reports) {
            Intent intent = new Intent(this, MotherChampionReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.cdp_reports) {
            Intent intent = new Intent(this, CdpReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.agyw_reports) {
            Intent intent = new Intent(this, AGYWReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.iccm_reports) {
            Intent intent = new Intent(this, IccmReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.ecd_reports) {
            Intent intent = new Intent(this, ECDReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.sbc_reports) {
            Intent intent = new Intent(this, SbcReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.asrh_reports) {
            Intent intent = new Intent(this, AsrhReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.cecap_reports) {
            Intent intent = new Intent(this, CecapReportsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.kvp_reports) {
            Intent intent = new Intent(this, KvpReportsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(IndicatorTallyEvent event) {
        if (event.getStatus().equals(TallyStatus.STARTED)) {
            textViewLogs.setVisibility(View.VISIBLE);
            textViewLogs.setText(R.string.started_refreshing_reports);
            Utils.showToast(this, "Imeanza kuchakata Ripoti Upya");
        } else if (event.getStatus().equals(TallyStatus.INPROGRESS)) {
            textViewLogs.setVisibility(View.VISIBLE);
            if (event.getMessage() != null) {
                textViewLogs.setText(event.getMessage());
            } else {
                Utils.showToast(this, "Uchakataji wa Ripoti Unaendelea");
            }
        } else if (event.getStatus().equals(TallyStatus.COMPLETE)) {
            textViewLogs.setVisibility(View.GONE);
            Utils.showToast(this, "Uchakataji wa Ripoti Umemalizika");
        }
    }

}