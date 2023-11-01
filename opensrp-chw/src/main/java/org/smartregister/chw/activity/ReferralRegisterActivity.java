package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.ENTITY_ID;
import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getMalariaConfirmation;
import static org.smartregister.chw.referral.util.Constants.ActivityPayload;
import static org.smartregister.chw.referral.util.Constants.ActivityPayloadType;
import static org.smartregister.util.JsonFormUtils.VALUE;
import static org.smartregister.util.JsonFormUtils.getFieldJSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.fragment.CompletedReferralRegisterFragment;
import org.smartregister.chw.fragment.ReferralRegisterFragment;
import org.smartregister.chw.malaria.util.MalariaJsonFormUtils;
import org.smartregister.chw.referral.activity.BaseReferralRegisterActivity;
import org.smartregister.chw.util.Constants;
import org.smartregister.helper.BottomNavigationHelper;

import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class ReferralRegisterActivity extends BaseReferralRegisterActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    public static void startReferralRegistrationActivity(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, ReferralRegisterActivity.class);
        intent.putExtra(ActivityPayload.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(ActivityPayload.REFERRAL_FORM_NAME, getMalariaConfirmation());
        intent.putExtra(ActivityPayload.ACTION, ActivityPayloadType.REGISTRATION);
        activity.startActivity(intent);
    }

    @NotNull
    @Override
    protected Fragment[] getOtherFragments() {
        return new CompletedReferralRegisterFragment[]{new CompletedReferralRegisterFragment()};
    }

    @NotNull
    @Override
    protected ReferralRegisterFragment getRegisterFragment() {
        return new ReferralRegisterFragment();
    }

    @Override
    public List<String> getViewIdentifiers() {
        return Collections.singletonList(Constants.CONFIGURATION.MALARIA_REGISTER);
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
        bottomNavigationView.getMenu().clear();

        bottomNavigationView.inflateMenu(R.menu.referrals_bottom_nav_menu);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        //Implement
    }

    private void startRegisterActivity() {
        Intent intent = new Intent(this, ReferralRegisterActivity.class);
        this.startActivity(intent);
        this.overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        this.finish();
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(Constants.DrawerMenu.REFERRALS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == org.smartregister.chw.malaria.util.Constants.REQUEST_CODE_GET_JSON) {
            String jsonString = data.getStringExtra(org.smartregister.chw.malaria.util.Constants.JSON_FORM_EXTRA.JSON);
            try {
                JSONObject form = new JSONObject(jsonString);
                Triple<Boolean, JSONObject, JSONArray> registrationFormParams = MalariaJsonFormUtils.validateParameters(form.toString());
                JSONObject jsonForm = registrationFormParams.getMiddle();
                JSONArray fields = registrationFormParams.getRight();
                String encounter_type = jsonForm.optString(org.smartregister.chw.malaria.util.Constants.JSON_FORM_EXTRA.ENCOUNTER_TYPE);

                if (org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.MALARIA_FOLLOW_UP_VISIT.equals(encounter_type)) {
                    JSONObject fever_still_object = getFieldJSONObject(fields, "fever_still");
                    if (fever_still_object != null && "Yes".equalsIgnoreCase(fever_still_object.optString(VALUE))) {
                        ReferralRegisterActivity.startReferralRegistrationActivity(this, jsonForm.optString(ENTITY_ID));
                    }
                } else {
                    startRegisterActivity();
                }
            } catch (JSONException e) {
                Timber.e(e);
            }

        } else {
            finish();
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_home) {
            switchToFragment(0);
            return true;
        } else if (menuItem.getItemId() == R.id.action_completed_referrals) {
            switchToFragment(1);
            return true;
        } else
            return false;
    }
}
 