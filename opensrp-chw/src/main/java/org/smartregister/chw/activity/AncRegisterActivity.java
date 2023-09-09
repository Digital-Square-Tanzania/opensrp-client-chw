package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.EventType.ANC_REGISTRATION;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.core.activity.CoreAncRegisterActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.fragment.AncPartnerFollowupRegisterFragment;
import org.smartregister.chw.fragment.AncRegisterFragment;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class AncRegisterActivity extends CoreAncRegisterActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    public static void startAncRegistrationActivity(Activity activity, String memberBaseEntityID, String phoneNumber, String formName,
                                                    String uniqueId, String familyBaseID, String family_name) {
        Intent intent = new Intent(activity, AncRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        phone_number = phoneNumber;
        familyBaseEntityId = familyBaseID;
        form_name = formName;
        familyName = family_name;
        unique_id = uniqueId;
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.TABLE_NAME, getFormTable());
        activity.startActivity(intent);
    }

    @Override
    public Class getRegisterActivity(String register) {
        if (register.equals(ANC_REGISTRATION))
            return AncRegisterActivity.class;
        else
            return PncRegisterActivity.class;
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().clear();

        bottomNavigationView.inflateMenu(R.menu.anc_bottom_nav_menu);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new AncRegisterFragment();
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {

        try {
            JSONObject stepOne = jsonForm.getJSONObject(JsonFormUtils.STEP1);
            JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);

            Map<String, String> values = new HashMap<>();

            values.put(DBConstants.KEY.TEMP_UNIQUE_ID, unique_id);
            values.put(CoreConstants.JsonAssets.FAM_NAME, familyName);
            values.put(CoreConstants.JsonAssets.FAMILY_MEMBER.PHONE_NUMBER, phone_number);
            values.put(org.smartregister.family.util.DBConstants.KEY.RELATIONAL_ID, familyBaseEntityId);
            values.put(DBConstants.KEY.LAST_MENSTRUAL_PERIOD, lastMenstrualPeriod);
            try {
                JSONObject min_date = CoreJsonFormUtils.getFieldJSONObject(jsonArray, "delivery_date");
                min_date.put("min_date", lastMenstrualPeriod);
            }catch (Exception e){
                Timber.e(e);
            }

            FormUtils.updateFormField(jsonArray, values);

            Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
            intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

            Form form = new Form();
            form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
            form.setName(getString(R.string.anc_registration));
            form.setWizard(true);
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

            startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public void switchToBaseFragment() {
        Intent intent = new Intent(this, AncRegisterActivity.class);
        startActivity(intent);
        this.finish();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new AncPartnerFollowupRegisterFragment[]{
                new AncPartnerFollowupRegisterFragment()};
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.REQUEST_CODE_GET_JSON) {
//            process the form
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                String baseEnityId = form.optString(Constants.JSON_FORM_EXTRA.ENTITY_TYPE);
                String encounter_type = form.optString(Constants.JSON_FORM_EXTRA.ENCOUNTER_TYPE);

                if (encounter_type.equalsIgnoreCase(CoreConstants.EventType.PNC_HOME_VISIT)) {
                    ChwScheduleTaskExecutor.getInstance().execute(baseEnityId, CoreConstants.EventType.PNC_HOME_VISIT, new Date());
                } else if (encounter_type.equalsIgnoreCase(CoreConstants.EventType.ANC_HOME_VISIT)) {
                    ChwScheduleTaskExecutor.getInstance().execute(baseEnityId, CoreConstants.EventType.ANC_HOME_VISIT, new Date());
                }
                SyncServiceJob.scheduleJobImmediately(SyncServiceJob.TAG);
            } catch (Exception e) {
                Timber.e(e);
            }
        } else {
            finish();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.action_anc){
            switchToFragment(0);
            return true;
        } else if (menuItem.getItemId() == R.id.action_received_referrals){
            switchToFragment(1);
            return true;
        }
        return false;
    }
}
