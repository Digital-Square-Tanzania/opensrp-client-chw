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
import org.smartregister.chw.fragment.CompletedReferralRegisterFragment;
import org.smartregister.chw.fragment.NcdConfirmedRegisterFragment;
import org.smartregister.chw.fragment.NcdRegisterFragment;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class NcdRegisterActivity extends CoreNcdRegisterActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavigationMenu.getInstance(this, null, null);
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
        return new NcdConfirmedRegisterFragment[]{new NcdConfirmedRegisterFragment()};
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new NcdRegisterFragment();
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().clear();
            bottomNavigationView.inflateMenu(R.menu.ncd_bottom_nav_menu);
            bottomNavigationView.setOnNavigationItemSelectedListener(this);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_at_risk_ncd) {
            switchToFragment(0);
            return true;
        } else if (menuItem.getItemId() == R.id.action_confirmed_ncd) {
            switchToFragment(1);
            return true;
        } else
            return false;
    }
}
