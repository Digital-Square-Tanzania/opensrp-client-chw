package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.chw.presenter.NcdCaseManagementVisitPresenter;

public class NcdCaseManagementVisitActivity extends NcdVisitActivity {

    public static void startMe(Activity activity, String baseEntityId, Boolean isEditMode) {
        Intent intent = new Intent(activity, NcdCaseManagementVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, isEditMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.SKELETON_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void registerPresenter() {
        presenter = new NcdCaseManagementVisitPresenter(memberObject, this);
    }
}
