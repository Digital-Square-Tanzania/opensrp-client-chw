package org.smartregister.chw.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreNcdRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.fragment.NcdConfirmedRegisterFragment;
import org.smartregister.chw.fragment.NcdRegisterFragment;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class NcdRegisterActivity extends CoreNcdRegisterActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    // TODO: Confirm with reviewer that CoreNcdRegisterActivity is still the appropriate base class.
    private NcdRegisterFragment atRiskFragment;
    private NcdConfirmedRegisterFragment confirmedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instantiateFragments();
        try {
            NavigationMenu.getInstance(this, null, null);
        } catch (Exception navigationError) {
            Timber.e(navigationError, "NCD register navigation menu failed to init");
        }
        ensureRegisterProviderInitialized();
    }

    private void instantiateFragments() {
        if (atRiskFragment == null) {
            atRiskFragment = new NcdRegisterFragment();
        }

        if (confirmedFragment == null) {
            confirmedFragment = new NcdConfirmedRegisterFragment();
        }
    }

    private void ensureRegisterProviderInitialized() {
        // BaseNcdRegisterFragment wires the adapter/provider when attached; log for visibility.
        // TODO: Verify whether explicit adapter wiring is required here for other flavors.
        Timber.i("NCD register fragments ready | risk:%s confirmed:%s",
                atRiskFragment != null, confirmedFragment != null);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.nav_menu_ncd));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @NotNull
    @Override
    protected Fragment[] getOtherFragments() {
        instantiateFragments();
        return new Fragment[]{confirmedFragment};
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        instantiateFragments();
        return atRiskFragment;
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().clear();
            bottomNavigationView.inflateMenu(R.menu.ncd_bottom_nav_menu);
            bottomNavigationView.setOnNavigationItemSelectedListener(this);
            Timber.i("NCD bottom navigation inflated");
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        try {
            if (menuItem.getItemId() == R.id.action_at_risk_ncd) {
                switchToFragment(0);
                return true;
            } else if (menuItem.getItemId() == R.id.action_confirmed_ncd) {
                switchToFragment(1);
                return true;
            }
        } catch (Exception navigationError) {
            Timber.e(navigationError, "NCD navigation failure for %s", menuItem.getTitle());
        }
        return false;
    }

    public void openClientProfile(String baseEntityId, boolean confirmedClient) {
        if (baseEntityId == null) {
            Timber.e("Base entity id missing while opening profile");
            return;
        }
        try {
            NcdProfileActivity.startProfileActivity(this, baseEntityId, confirmedClient);
        } catch (Exception profileError) {
            Timber.e(profileError, "Unable to launch NCD profile for %s", baseEntityId);
        }
    }
}
