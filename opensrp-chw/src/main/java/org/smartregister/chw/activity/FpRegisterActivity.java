package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreFpRegisterActivity;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.fragment.FpRegisterFragment;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class FpRegisterActivity extends CoreFpRegisterActivity {

    private static String baseEntityId;
    private static String fpFormName;

    public static void startFpRegistrationActivity(Activity activity, String baseEntityID, String formName) {
        Intent intent = new Intent(activity, FpRegisterActivity.class);
        intent.putExtra(FamilyPlanningConstants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(FamilyPlanningConstants.ACTIVITY_PAYLOAD.FP_FORM_NAME, formName);
        intent.putExtra(FamilyPlanningConstants.ACTIVITY_PAYLOAD.ACTION, FamilyPlanningConstants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        fpFormName = formName;
        baseEntityId = baseEntityID;
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
        return new FpRegisterFragment();
    }

//    @Override
//    public JSONObject getFpFormForEdit() {
//
//        NativeFormsDataBinder binder = new NativeFormsDataBinder(this, baseEntityId);
//        binder.setDataLoader(new FPDataLoader(getString(R.string.fp_update_family_planning)));
//
//        JSONObject form = binder.getPrePopulatedForm(fpFormName);
//
//        try {
//            form.put(JsonFormUtils.ENCOUNTER_TYPE, FamilyPlanningConstants.EventType.UPDATE_FAMILY_PLANNING_REGISTRATION);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return form;
//    }
}