package org.smartregister.chw.activity;

import android.content.Intent;

import org.json.JSONObject;
import org.smartregister.R;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.core.activity.CoreChildRegisterActivity;
import org.smartregister.chw.core.contract.CoreChildRegisterContract;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.ChildRegisterFragment;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.Date;

import timber.log.Timber;

public class ChildRegisterActivity extends CoreChildRegisterActivity implements CoreChildRegisterContract.View {

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        FamilyRegisterActivity.registerBottomNavigation(bottomNavigationHelper, bottomNavigationView, this);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new ChildRegisterFragment();
    }

    @Override
    protected void onActivityResultExtended(int requestCode, int resultCode, Intent data) {
        super.onActivityResultExtended(requestCode, resultCode, data);
        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
//            process the form
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                String baseEnityId = form.optString(Constants.JSON_FORM_EXTRA.ENTITY_TYPE);
                String encounter_type = form.optString(Constants.JSON_FORM_EXTRA.ENCOUNTER_TYPE);

                if (encounter_type.equalsIgnoreCase(CoreConstants.EventType.CHILD_HOME_VISIT))
                    ChwScheduleTaskExecutor.getInstance().execute(baseEnityId, CoreConstants.EventType.CHILD_HOME_VISIT, new Date());
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }
}
