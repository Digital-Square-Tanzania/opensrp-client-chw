package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.isMultiPartForm;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_EXTRA.STEP3;
import static org.smartregister.util.JsonFormUtils.FIELDS;
import static org.smartregister.util.JsonFormUtils.STEP1;

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
import org.smartregister.chw.fragment.AypOutSchoolGroupsRegisterFragment;
import org.smartregister.chw.fragment.AypOutSchoolRegisterFragment;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.Calendar;

import timber.log.Timber;

public class AypOutSchoolRegisterActivity extends CoreAypRegisterActivity {

    public static void startRegistration(Activity activity, String baseEntityId, String gender,int age) {
        Intent intent = new Intent(activity, AypOutSchoolRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        intent.putExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.AGE, age);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.AYP_FORM_NAME, Constants.FORMS.AYP_OUT_SCHOOL_ENROLLMENT);
        activity.startActivity(intent);
    }

    @Override
    public Form getFormConfig() {
        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setName(getString(R.string.ayp_screening));
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
        form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
        form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));
        form.setSaveLabel(this.getResources().getString(org.smartregister.chw.core.R.string.save));
        return form;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new AypOutSchoolRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{ new AypOutSchoolGroupsRegisterFragment() };
    }

    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_ayp_out_school;
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.AYP_OUT_SCHOOL);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);

        String gender = getIntent().getStringExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.GENDER);
        int age = getIntent().getIntExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.AGE, -1);

        try {
            if (jsonForm.getString("encounter_type").equals("AYP Out-school Enrollment")) {

                JSONObject global = jsonForm.getJSONObject("global");

                if (gender != null) {
                    global.put("age", age);
                    global.put("gender", gender);
                }

            }
        } catch (Exception e) {
            Timber.e(e);
        }

        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setHomeAsUpIndicator(org.smartregister.chw.core.R.mipmap.ic_cross_white);
        form.setSaveLabel(getResources().getString(org.smartregister.chw.core.R.string.save));

        if (isMultiPartForm(jsonForm)) {
            form.setWizard(true);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
            form.setName(null);
            form.setNextLabel(getResources().getString(org.smartregister.chw.core.R.string.next));
            form.setPreviousLabel(getResources().getString(org.smartregister.chw.core.R.string.back));
        }
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }
}
