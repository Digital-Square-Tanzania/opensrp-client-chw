package org.smartregister.chw.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.apache.commons.lang3.EnumUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.core.activity.CoreFamilyRegisterActivity;
import org.smartregister.chw.core.activity.CorePncRegisterActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.PncRegisterFragment;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class PncRegisterActivity extends CorePncRegisterActivity implements BottomNavigationView.OnNavigationItemSelectedListener  {

    public static void startPncRegistrationActivity(Activity activity, String memberBaseEntityID, String phoneNumber, String formName,
                                                    String uniqueId, String familyBaseID, String family_name, String last_menstrual_period) {
        Intent intent = new Intent(activity, PncRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        phone_number = phoneNumber;
        familyBaseEntityId = familyBaseID;
        form_name = formName;
        familyName = family_name;
        unique_id = uniqueId;
        lastMenstrualPeriod = last_menstrual_period;
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.TABLE_NAME, getFormTable());
        activity.startActivity(intent);
    }

    @Override
    public void onRegistrationSaved(String encounterType, boolean isEdit, boolean hasChildren) {
        if (encounterType.equalsIgnoreCase(Constants.EVENT_TYPE.PREGNANCY_OUTCOME)) {
            Timber.d("We are home - PNC Register");
        } else {
            super.onRegistrationSaved(encounterType, isEdit, hasChildren);
        }
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().clear();

        bottomNavigationView.inflateMenu(R.menu.anc_bottom_nav_menu);
        bottomNavigationView.getMenu().removeItem(R.id.action_received_referrals);

        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    protected Class<? extends CoreFamilyRegisterActivity> getFamilyRegisterActivity() {
        return FamilyRegisterActivity.class;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new PncRegisterFragment();
    }

    @SuppressLint("MissingSuperCall")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResultExtended(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            try {
                JSONObject form = new JSONObject(data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON));
                String encounter_type = form.optString(Constants.JSON_FORM_EXTRA.ENCOUNTER_TYPE);

                if (CoreConstants.EventType.PREGNANCY_OUTCOME.equals(encounter_type)) {
                    JSONArray fields = org.smartregister.util.JsonFormUtils.fields(form);
                    String pregnancyOutcome = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, org.smartregister.chw.util.Constants.pregnancyOutcome).optString(JsonFormUtils.VALUE);
                    if (EnumUtils.isValidEnum(org.smartregister.chw.util.Constants.FamilyRegisterOptionsUtil.class, pregnancyOutcome)) {
                        startRegisterActivity(FamilyRegisterActivity.class);
                        this.finish();
                        return;
                    }
                }
                SyncServiceJob.scheduleJobImmediately(SyncServiceJob.TAG);

            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }
}
