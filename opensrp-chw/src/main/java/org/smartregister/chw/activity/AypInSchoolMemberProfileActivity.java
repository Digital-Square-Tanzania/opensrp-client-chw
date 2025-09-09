package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.activity.CoreAypProfileActivity;

public class AypInSchoolMemberProfileActivity extends CoreAypProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AypInSchoolMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    public void startHivstRegistration() {
        // Launch HIVST registration from AYP profile using member gender
        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), memberObject.getGender());
    }

    @Override
    public void startServiceForm() {

    }

    @Override
    public void continueService() {

    }

    @Override
    public void continueDischarge() {

    }
}

