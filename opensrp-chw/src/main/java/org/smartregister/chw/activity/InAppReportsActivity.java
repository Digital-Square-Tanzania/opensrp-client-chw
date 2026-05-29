package org.smartregister.chw.activity;

import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.appbar.AppBarLayout;

import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.job.ChwIndicatorGeneratingJob;
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

    protected ConstraintLayout sbcReports;

    protected ConstraintLayout harmReductionReports;

    protected ConstraintLayout harmReductionSoberHouseReports;

    protected ConstraintLayout asrhReports;

    protected ConstraintLayout cecapReports;

    protected ConstraintLayout tbLeprosyReports;

    protected ConstraintLayout kvpReports;

    protected ConstraintLayout aypOutSchoolReports;

    protected ConstraintLayout hpsReports;

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
        sbcReports = findViewById(R.id.sbc_reports);
        harmReductionReports = findViewById(R.id.harm_reduction_reports);
        harmReductionSoberHouseReports = findViewById(R.id.harm_reduction_sober_house_reports);
        asrhReports = findViewById(R.id.asrh_reports);
        cecapReports = findViewById(R.id.cecap_reports);
        tbLeprosyReports = findViewById(R.id.tb_leprosy_reports);
        kvpReports = findViewById(R.id.kvp_reports);
        aypOutSchoolReports = findViewById(R.id.ayp_out_school_report);
        hpsReports = findViewById(R.id.hps_reports);

        AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
        SharedPreferences preferences = allSharedPreferences.getPreferences();
        String teamRoleIdentifier = "";
        if (preferences != null) {
            teamRoleIdentifier = preferences.getString(TEAM_ROLE_IDENTIFIER, "");
        }

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
                case "AYP_OUT_OF_SCHOOL":
                    aypOutSchoolReports.setVisibility(View.VISIBLE);
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

                    if (ChwApplication.getApplicationFlavor().hasICCM()) {
                        iccmReports.setVisibility(View.VISIBLE);
                    }

                    if (ChwApplication.getApplicationFlavor().hasSbc()) {
                        sbcReports.setVisibility(View.VISIBLE);
                    }

                    if (ChwApplication.getApplicationFlavor().hasHarmReduction()) {
                        harmReductionReports.setVisibility(View.VISIBLE);
                    }

                    if (ChwApplication.getApplicationFlavor().hasHarmReductionSoberHouse()) {
                        harmReductionSoberHouseReports.setVisibility(View.VISIBLE);
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

                    if (ChwApplication.getApplicationFlavor().hasHps()) {
                        hpsReports.setVisibility(View.VISIBLE);
                    }

                    if (ChwApplication.getApplicationFlavor().hasTbLeprosy()) {
                        tbLeprosyReports.setVisibility(View.VISIBLE);
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

            if (ChwApplication.getApplicationFlavor().hasSbc()) {
                sbcReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasHarmReduction()) {
                harmReductionReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasHarmReductionSoberHouse()) {
                harmReductionSoberHouseReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasAsrh()) {
                asrhReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasCecap()) {
                cecapReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasTbLeprosy()) {
                tbLeprosyReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasKvp()) {
                kvpReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasAyp()) {
                aypOutSchoolReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasCdp()) {
                condomDistributionReports.setVisibility(View.VISIBLE);
            }

            if (ChwApplication.getApplicationFlavor().hasHps()) {
                hpsReports.setVisibility(View.VISIBLE);
            }
        }


        motherChampionReportsLayout.setOnClickListener(this);
        condomDistributionReports.setOnClickListener(this);
        cbhsReportsLayout.setOnClickListener(this);
        agywReports.setOnClickListener(this);
        iccmReports.setOnClickListener(this);
        sbcReports.setOnClickListener(this);
        harmReductionReports.setOnClickListener(this);
        harmReductionSoberHouseReports.setOnClickListener(this);
        asrhReports.setOnClickListener(this);
        cecapReports.setOnClickListener(this);
        tbLeprosyReports.setOnClickListener(this);
        kvpReports.setOnClickListener(this);
        hpsReports.setOnClickListener(this);
        aypOutSchoolReports.setOnClickListener(this);
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
        // Route each report tile once to keep the back stack clean.
        if (id == R.id.cbhs_summary) {
            startActivity(new Intent(this, CBHSReportsActivity.class));
        } else if (id == R.id.mother_champion_reports) {
            startActivity(new Intent(this, MotherChampionReportsActivity.class));
        } else if (id == R.id.cdp_reports) {
            startActivity(new Intent(this, CdpReportsActivity.class));
        } else if (id == R.id.agyw_reports) {
            startActivity(new Intent(this, AGYWReportsActivity.class));
        } else if (id == R.id.iccm_reports) {
            startActivity(new Intent(this, IccmReportsActivity.class));
        } else if (id == R.id.sbc_reports) {
            startActivity(new Intent(this, SbcReportsActivity.class));
        } else if (id == R.id.asrh_reports) {
            startActivity(new Intent(this, AsrhReportsActivity.class));
        } else if (id == R.id.cecap_reports) {
            startActivity(new Intent(this, CecapReportsActivity.class));
        } else if (id == R.id.tb_leprosy_reports) {
            startActivity(new Intent(this, TbLeprosyReportsActivity.class));
        } else if (id == R.id.kvp_reports) {
            startActivity(new Intent(this, KvpReportsActivity.class));
        } else if (id == R.id.hps_reports) {
            startActivity(new Intent(this, HpsReportsActivity.class));
        } else if (id == R.id.ayp_out_school_report) {
            startActivity(new Intent(this, AypReportsActivity.class));
        } else if (id == R.id.harm_reduction_reports) {
            startActivity(new Intent(this, HarmReductionReportsActivity.class));
        } else if (id == R.id.harm_reduction_sober_house_reports) {
            startActivity(new Intent(this, HarmReductionSoberHouseReportsActivity.class));
        }
    }

}
