package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.smartregister.chw.core.activity.CoreHarmReductionRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.HarmReductionRegisterFragment;
import org.smartregister.chw.harmreduction.util.Constants;
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
        return new Fragment[0];
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
