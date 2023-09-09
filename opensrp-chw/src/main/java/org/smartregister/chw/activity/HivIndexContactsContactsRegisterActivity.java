package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.core.activity.CoreHivIndexContactsRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.HivIndexContactsRegisterFragment;
import org.smartregister.chw.hiv.R;
import org.smartregister.chw.hiv.fragment.BaseHivIndexContactsRegisterFragment;
import org.smartregister.chw.hiv.util.Constants;
import org.smartregister.helper.BottomNavigationHelper;

public class HivIndexContactsContactsRegisterActivity extends CoreHivIndexContactsRegisterActivity {

    public static void startHIVFormActivity(Activity activity, String baseEntityID, String formName, String payloadType) {
        Intent intent = new Intent(activity, HivIndexContactsContactsRegisterActivity.class);
        intent.putExtra(Constants.ActivityPayload.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ActivityPayload.ACTION, payloadType);
        intent.putExtra(Constants.ActivityPayload.HIV_REGISTRATION_FORM_NAME, formName);
        activity.startActivity(intent);
    }

    @NotNull
    @Override
    protected BaseHivIndexContactsRegisterFragment getRegisterFragment() {
        return new HivIndexContactsRegisterFragment();
    }

    @NotNull
    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[0];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_clients);
            bottomNavigationView.getMenu().removeItem(R.id.action_register);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_search);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_library);
            bottomNavigationView.getMenu().removeItem(R.id.action_received_referrals);

            bottomNavigationView.inflateMenu(getMenuResource());
            bottomNavigationView.getMenu().removeItem(R.id.action_received_referrals);
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);
        }
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.HIV_INDEX_CLIENTS);
        }
    }


}
 