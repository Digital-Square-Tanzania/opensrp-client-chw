package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.ayp.activity.BaseAypOutSchoolGroupVisitActivity;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.interactor.aypOutOfSchool.BaseAypOutSchoolGroupVisitInteractor;
import org.smartregister.chw.ayp.presenter.BaseAypVisitPresenter;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.family.util.Utils;

public class AypOutSchoolGroupVisitActivity extends BaseAypOutSchoolGroupVisitActivity {

    public static void startAypOutSchoolGroupVisitActivity(Activity activity, String baseEntityId, Boolean editMode, String groupId, String groupName) {
        Intent intent = new Intent(activity, AypOutSchoolGroupVisitActivity.class);
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
        MemberObject memberObject = new MemberObject();
        memberObject.setBaseEntityId(baseEntityId);
        return memberObject;
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }

        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void registerPresenter() {
        BaseAypOutSchoolGroupVisitInteractor interactor = new BaseAypOutSchoolGroupVisitInteractor();
        String groupId = getIntent() != null ? getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID) : null;
        interactor.setGroupId(groupId);
        presenter = new BaseAypVisitPresenter(memberObject, this, interactor);
    }
}
