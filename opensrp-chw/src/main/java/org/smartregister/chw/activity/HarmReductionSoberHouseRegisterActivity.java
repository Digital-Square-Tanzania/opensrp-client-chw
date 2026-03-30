package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.MenuRes;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreHarmReductionRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.HarmReductionSoberHouseRegisterFragment;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class HarmReductionSoberHouseRegisterActivity extends CoreHarmReductionRegisterActivity {

    private static final String HARM_REDUCTION_SOBER_HOUSE_ENROLLMENT_FORM = "harm_reduction_sober_house_enrollment";

    public static void startRegistration(Activity activity, String memberBaseEntityID) {
        Intent intent = new Intent(activity, HarmReductionSoberHouseRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.HARM_REDUCTION_FORM_NAME, HARM_REDUCTION_SOBER_HOUSE_ENROLLMENT_FORM);
        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new HarmReductionSoberHouseRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{};
    }

    @Override
    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_harm_reduction_sober_house;
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = FormUtils.getStartFormActivity(
                jsonForm,
                getString(R.string.harm_reduction_sober_house_enrollment),
                this
        );
        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.HARM_REDUCTION_SOBER_HOUSE);
        }
    }
}
