package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.core.activity.CoreHivRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.HivRegisterFragment;
import org.smartregister.chw.hiv.fragment.BaseHivCommunityFollowupRegisterFragment;
import org.smartregister.chw.hiv.fragment.BaseHivRegisterFragment;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.listener.BottomNavigationListener;

public class HivRegisterActivity extends CoreHivRegisterActivity {

    public static void startHIVFormActivity(Activity activity, String baseEntityID, String formName, String payloadType) {
        Intent intent = new Intent(activity, HivRegisterActivity.class);
        intent.putExtra(org.smartregister.chw.hiv.util.Constants.ActivityPayload.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(org.smartregister.chw.hiv.util.Constants.ActivityPayload.ACTION, payloadType);
        intent.putExtra(org.smartregister.chw.hiv.util.Constants.ActivityPayload.HIV_REGISTRATION_FORM_NAME, formName);
        activity.startActivity(intent);
    }

    @NotNull
    @Override
    protected BaseHivRegisterFragment getRegisterFragment() {
        return new HivRegisterFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavigationMenu.getInstance(this, null, null);
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_clients);
            bottomNavigationView.getMenu().removeItem(org.smartregister.chw.hiv.R.id.action_register);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_search);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_library);

            bottomNavigationView.inflateMenu(getMenuResource());
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);

            BottomNavigationListener hivBottomNavigationListener = getBottomNavigation(this);
            bottomNavigationView.setOnNavigationItemSelectedListener(hivBottomNavigationListener);

        }
    }

    @NotNull
    @Override
    protected BaseHivCommunityFollowupRegisterFragment[] getOtherFragments() {
        return new BaseHivCommunityFollowupRegisterFragment[0];
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.CBHS_CLIENTS);
        }
    }
}
 