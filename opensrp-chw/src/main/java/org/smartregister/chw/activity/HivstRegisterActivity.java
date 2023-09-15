package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.smartregister.chw.core.activity.CoreHivstRegisterActivity;
import org.smartregister.chw.fragment.HivstMobilizationFragment;
import org.smartregister.chw.fragment.HivstRegisterFragment;
import org.smartregister.chw.hivst.util.Constants;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class HivstRegisterActivity extends CoreHivstRegisterActivity {


    public static void startHivstRegistrationActivity(Activity activity, String memberBaseEntityID, String gender) {
        Intent intent = new Intent(activity, HivstRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.HIVST_FORM_NAME, Constants.FORMS.HIVST_REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new HivstRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{
                new HivstMobilizationFragment()
        };
    }

}
