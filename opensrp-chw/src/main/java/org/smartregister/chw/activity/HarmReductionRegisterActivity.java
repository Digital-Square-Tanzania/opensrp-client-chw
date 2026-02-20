package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.MenuRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreHarmReductionRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.HarmReductionMatClientsRegisterFragment;
import org.smartregister.chw.fragment.HarmReductionRegisterFragment;
import org.smartregister.chw.fragment.HarmReductionUsedNeedlesAndSyringesCollectionFragment;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class HarmReductionRegisterActivity extends CoreHarmReductionRegisterActivity {

    public static void startRegistration(Activity activity, String memberBaseEntityID) {
        Intent intent = new Intent(activity, HarmReductionRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.HARM_REDUCTION_FORM_NAME, Constants.FORMS.HARM_REDUCTION_RISK_ASSESSMENT);
        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new HarmReductionRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{
                new HarmReductionUsedNeedlesAndSyringesCollectionFragment(),
                new HarmReductionMatClientsRegisterFragment()
        };
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_clients);
            bottomNavigationView.getMenu().removeItem(org.smartregister.chw.harmreduction.R.id.action_register);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_search);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_library);
            bottomNavigationView.inflateMenu(getMenuResource());
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                if (item.getItemId() == org.smartregister.chw.harmreduction.R.id.action_home) {
                    switchToBaseFragment();
                } else if (item.getItemId() == org.smartregister.chw.harmreduction.R.id.action_safety_boxes_collection) {
                    switchToFragment(1);
                } else if (item.getItemId() == R.id.action_mat_clients) {
                    switchToFragment(2);
                }
                return true;
            });
        }
    }

    @Override
    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_harm_reduction;
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.HARM_REDUCTION);
        }
    }
}
