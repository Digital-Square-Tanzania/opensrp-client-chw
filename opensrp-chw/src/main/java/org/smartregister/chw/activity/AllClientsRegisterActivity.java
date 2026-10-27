package org.smartregister.chw.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.smartregister.AllConstants;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreAllClientsRegisterActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.AllClientsDashboardFragment;
import org.smartregister.chw.fragment.AllClientsRegisterFragment;
import org.smartregister.chw.model.ChwAllClientsRegisterModel;
import org.smartregister.chw.presenter.ChwAllClientRegisterPresenter;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.domain.FetchStatus;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.presenter.BaseOpdRegisterActivityPresenter;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class AllClientsRegisterActivity extends CoreAllClientsRegisterActivity {

    private boolean clientPickerMode;
    private static final int CLIENTS_PAGE = 0;
    private static final int HOME_PAGE = 1;
    private static final String STATE_PAGE = "all_clients_page";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        clientPickerMode = getIntent().getBooleanExtra(Constants.EXTRA_CLIENT_PICKER_MODE, false);
        super.onCreate(savedInstanceState);
        ChwApplication.getInstance().notifyAppContextChange();
        if (hasDashboard()) {
            mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    updateDashboardTab(position);
                }
            });
            switchToFragment(savedInstanceState == null ? HOME_PAGE
                    : savedInstanceState.getInt(STATE_PAGE, HOME_PAGE));
            updateDashboardTab(mPager.getCurrentItem());
        }
    }

    public boolean isClientPickerMode() {
        return clientPickerMode;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (hasDashboard()) {
            // Both ViewPager pages resume; bind the drawer to the visible page last.
            updateDashboardTab(mPager.getCurrentItem());
        }
    }

    private boolean hasDashboard() {
        return "nacp".equals(BuildConfig.FLAVOR) && !clientPickerMode;
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return hasDashboard() ? new Fragment[]{new AllClientsDashboardFragment()} : super.getOtherFragments();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (hasDashboard()) {
            outState.putInt(STATE_PAGE, mPager.getCurrentItem());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new AllClientsRegisterFragment();
    }

    @Override
    public void startRegistration() {
        startFormActivity(CoreConstants.JSON_FORM.getAllClientRegistrationForm(), null, null);
    }

    @Override
    public void startFormActivity(String formName, String entityId, String metaData) {
        try {
            String locationId = org.smartregister.family.util.Utils.context().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            ((ChwAllClientRegisterPresenter) presenter()).startForm(formName, entityId, metaData, locationId);

        } catch (Exception e) {
            Timber.e(e);
            displayToast(org.smartregister.family.R.string.error_unable_to_start_form);
        }
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (hasDashboard()) {
            installDashboardFooter();
            bottomNavigationView.getMenu().clear();
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_all_clients_dashboard);
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.setItemHorizontalTranslationEnabled(false);
            ColorStateList colors = new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{ContextCompat.getColor(this, R.color.all_clients_dashboard_blue_text),
                            ContextCompat.getColor(this, R.color.all_clients_dashboard_secondary)});
            bottomNavigationView.setItemIconTintList(colors);
            bottomNavigationView.setItemTextColor(colors);
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);
            bottomNavigationView.setOnNavigationItemSelectedListener(this);
            return;
        }
        Utils.setupBottomNavigation(bottomNavigationHelper, bottomNavigationView, this);
        bottomNavigationView.getMenu().findItem(R.id.action_register).setTitle(R.string.add_client).setIcon(R.drawable.ic_input_add);
    }

    private void installDashboardFooter() {
        // Keep registration outside the destination tabs without covering either pager page.
        ViewGroup parent = (ViewGroup) bottomNavigationView.getParent();
        ViewGroup.LayoutParams footerParams = bottomNavigationView.getLayoutParams();
        footerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        int index = parent.indexOfChild(bottomNavigationView);
        LinearLayout footer = (LinearLayout) getLayoutInflater().inflate(
                R.layout.all_clients_dashboard_footer, parent, false);
        parent.removeView(bottomNavigationView);
        footer.addView(bottomNavigationView, 0, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        parent.addView(footer, index, footerParams);
        RelativeLayout.LayoutParams pagerParams = (RelativeLayout.LayoutParams)
                findViewById(R.id.base_view_pager).getLayoutParams();
        pagerParams.addRule(RelativeLayout.ABOVE, R.id.dashboard_footer);
        findViewById(R.id.base_view_pager).setLayoutParams(pagerParams);
        footer.findViewById(R.id.dashboard_register_client)
                .setOnClickListener(v -> startRegistration());
    }

    @Override
    protected BaseOpdRegisterActivityPresenter createPresenter(
            @NonNull OpdRegisterActivityContract.View view, @NonNull OpdRegisterActivityContract.Model model) {
        return new ChwAllClientRegisterPresenter(view, model);
    }

    @Override
    public void switchToBaseFragment() {
        if (hasDashboard()) {
            switchToFragment(HOME_PAGE);
            updateDashboardTab(HOME_PAGE);
            return;
        }
        Intent intent = new Intent(this, AllClientsRegisterActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_dashboard_clients:
                showAllClients(false);
                return true;
            case R.id.action_scan_qr:
                startQrCodeScanner();
                return true;
            case R.id.action_family:
                switchToBaseFragment();
                break;
            case R.id.action_register:
                startRegistration();
                return !hasDashboard();
            default:
                return true;
        }
        return true;
    }

    public void showAllClients(boolean focusSearch) {
        switchToFragment(CLIENTS_PAGE);
        if (hasDashboard()) {
            updateDashboardTab(CLIENTS_PAGE);
        }
        if (focusSearch) {
            mPager.post(() -> {
                Fragment fragment = findFragmentByPosition(CLIENTS_PAGE);
                if (fragment != null && fragment.getView() != null) {
                    View search = fragment.getView().findViewById(R.id.edt_search);
                    if (search != null && search.requestFocus()) {
                        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                                .showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }
    }

    private void updateDashboardTab(int position) {
        bottomNavigationView.getMenu().findItem(position == HOME_PAGE
                ? R.id.action_family : R.id.action_dashboard_clients).setChecked(true);
        Fragment fragment = findFragmentByPosition(position);
        if (fragment != null && fragment.getView() != null) {
            org.smartregister.chw.core.custom_views.NavigationMenu.getInstance(this, null,
                    fragment.getView().findViewById(R.id.register_toolbar));
        }
        if (fragment instanceof AllClientsDashboardFragment) {
            ((AllClientsDashboardFragment) fragment).refreshDashboard();
        }
    }

    public void bindDashboardToolbar() {
        if (hasDashboard() && mPager.getCurrentItem() == HOME_PAGE) {
            Fragment fragment = findFragmentByPosition(HOME_PAGE);
            if (fragment != null && fragment.getView() != null) {
                org.smartregister.chw.core.custom_views.NavigationMenu.getInstance(this, null,
                        fragment.getView().findViewById(R.id.register_toolbar));
            }
        }
    }

    @Override
    public void refreshList(FetchStatus fetchStatus) {
        super.refreshList(fetchStatus);
        if (hasDashboard()) {
            Fragment fragment = findFragmentByPosition(HOME_PAGE);
            if (fragment instanceof AllClientsDashboardFragment) {
                ((AllClientsDashboardFragment) fragment).refreshDashboard();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (hasDashboard()) {
            androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
            if (drawer != null && drawer.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                drawer.closeDrawer(androidx.core.view.GravityCompat.START);
            } else if (mPager.getCurrentItem() == CLIENTS_PAGE) {
                switchToBaseFragment();
            } else {
                finish();
            }
            return;
        }
        super.onBackPressed();
    }

    public OpdRegisterActivityContract.Model createActivityModel() {
        return new ChwAllClientsRegisterModel(this);
    }
}
