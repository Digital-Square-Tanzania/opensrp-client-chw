package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.ayp.activity.BaseAypInSchoolGroupVisitActivity;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.presenter.BaseAypVisitPresenter;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.ayp.interactor.BaseAypInSchoolGroupVisitInteractor;

public class AypInSchoolGroupVisitActivity extends BaseAypInSchoolGroupVisitActivity {

    public static void startAypInSchoolGroupVisitActivity(Activity activity, String baseEntityId, Boolean editMode, String groupId, String groupName) {
        Intent intent = new Intent(activity, AypInSchoolGroupVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.ayp_PROFILE);
        if (groupId != null) intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID, groupId);
        if (groupName != null) intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_NAME, groupName);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        // In the app module, rely on DAO resolving; default to base implementation's member assignment
        return super.getMemberObject(baseEntityId);
    }

    @Override
    protected void registerPresenter() {
        BaseAypInSchoolGroupVisitInteractor interactor = new BaseAypInSchoolGroupVisitInteractor();
        String groupId = getIntent() != null ? getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID) : null;
        interactor.setGroupId(groupId);
        presenter = new BaseAypVisitPresenter(memberObject, this, interactor);
    }
}

