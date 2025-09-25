package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.MenuRes;
import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.activity.CoreAypRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.AypParentalRegisterFragment;
import org.smartregister.family.util.Utils;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class AypParentalRegisterActivity extends CoreAypRegisterActivity {

    public static void startRegistration(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AypParentalRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.AYP_FORM_NAME, Constants.FORMS.AYP_PARENTAL_ENROLLMENT);
        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new AypParentalRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[0];
    }

    @Override
    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_ayp_parental;
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.AYP_PARENTAL);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }
}
