package org.smartregister.chw.activity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.smartregister.chw.adapter.ChwSectionsPagerAdapter;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.activity.HIA2ReportsActivity;
import org.smartregister.chw.core.fragment.DraftMonthlyFragment;
import org.smartregister.chw.task.ChwStartDraftMonthlyFormTask;
import org.smartregister.util.Utils;

import java.util.Date;

import timber.log.Timber;

public class ChwHIA2ReportsActivity extends HIA2ReportsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_hia2_reports);
        Toolbar toolbar = findViewById(getToolbarId());
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.hia_tabs);
        ChwSectionsPagerAdapter mSectionsPagerAdapter = new ChwSectionsPagerAdapter(getSupportFragmentManager(), this);

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout.setupWithViewPager(mViewPager);
        refreshDraftMonthlyTitle();
        //  mSectionsPagerAdapter.getItem(1);
        mViewPager.setCurrentItem(1);
        findViewById(R.id.toggle_action_menu).setOnClickListener(v -> onClickReport(v));
    }

    @Override
    public void startMonthlyReportForm(String formName, Date date) {
        try {
            Fragment currentFragment = currentFragment();
            if (currentFragment instanceof DraftMonthlyFragment) {
                Utils.startAsyncTask(new ChwStartDraftMonthlyFormTask(this, date, formName), null);
            }
        } catch (Exception e) {
            Timber.e(e);
        }

    }

}
