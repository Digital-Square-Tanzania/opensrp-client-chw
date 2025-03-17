package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.domain.Form;

import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreHpsRegisterActivity;
import org.smartregister.chw.fragment.HpsAnnualCensusRegisterFragment;
import org.smartregister.chw.fragment.HpsDeathRegisterFragment;
import org.smartregister.chw.fragment.HpsHouseholdRegisterFragment;
import org.smartregister.chw.fragment.HpsMobilizationRegisterFragment;
import org.smartregister.chw.fragment.HpsRegisterFragment;
import org.smartregister.chw.hps.util.Constants;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class HpsRegisterActivity extends CoreHpsRegisterActivity {

    public static void startRegistration(Activity activity, String baseEntityId, String formName) {
        Intent intent = new Intent(activity, HpsRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.HPS_FORM_NAME, formName);

        activity.startActivity(intent);
    }

    @Override
    public Form getFormConfig() {
        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setName(getString(R.string.hps_enrollment));
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
        form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
        form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));
        form.setSaveLabel(this.getResources().getString(org.smartregister.chw.core.R.string.save));
        return form;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new HpsRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{
                new HpsHouseholdRegisterFragment(),
                new HpsMobilizationRegisterFragment(),
                new HpsDeathRegisterFragment(),
                new HpsAnnualCensusRegisterFragment()
        };
    }
}
