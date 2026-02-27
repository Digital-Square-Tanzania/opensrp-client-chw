package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getLinkageFollowUpForm;
import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getMalariaConfirmation;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.R;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.fragment.CompletedLinkageRegisterFragment;
import org.smartregister.chw.fragment.LinkageRegisterFragment;
import org.smartregister.chw.fragment.ReferralRegisterFragment;
import org.smartregister.chw.util.Constants;
import org.smartregister.helper.BottomNavigationHelper;

/**
 * Author issyzac on 06/05/2024
 */
public class AddoLinkageRegisterActivity extends ReferralRegisterActivity {

    @Override
    protected @NotNull ReferralRegisterFragment getRegisterFragment() {
        return new LinkageRegisterFragment();
    }

    @NonNull
    @Override
    protected @NotNull Fragment[] getOtherFragments() {
        return new CompletedLinkageRegisterFragment[]{new CompletedLinkageRegisterFragment()};
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(Constants.DrawerMenu.ADDO_LINKAGE);
        }
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().clear();
            bottomNavigationView.inflateMenu(R.menu.linkages_bottom_nav_menu);
            bottomNavigationView.setOnNavigationItemSelectedListener(this);
        }
    }

    @Override
    protected void startRegisterActivity() {
        Intent intent = new Intent(this, AddoLinkageRegisterActivity.class);
        this.startActivity(intent);
        this.overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        this.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_home) {
            switchToFragment(0);
            return true;
        } else if (menuItem.getItemId() == R.id.action_completed_linkages) {
            switchToFragment(1);
            return true;
        } else
            return false;
    }

}
