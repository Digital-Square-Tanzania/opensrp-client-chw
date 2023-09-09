package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;

import org.smartregister.R;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.core.activity.CoreMalariaRegisterActivity;
import org.smartregister.chw.fragment.MalariaRegisterFragment;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getMalariaConfirmation;

public class MalariaRegisterActivity extends CoreMalariaRegisterActivity {

    public static void startMalariaRegistrationActivity(Activity activity, String baseEntityID, @Nullable String familyBaseEntityID) {
        Intent intent = new Intent(activity, MalariaRegisterActivity.class);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.FAMILY_BASE_ENTITY_ID, familyBaseEntityID);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.MALARIA_FORM_NAME, getMalariaConfirmation());
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        activity.startActivity(intent);
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        FamilyRegisterActivity.registerBottomNavigation(bottomNavigationHelper, bottomNavigationView, this);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new MalariaRegisterFragment();
    }

}