package org.smartregister.chw.activity;

import static org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_SCHOOL_GROUP_FOLLOW_UP_VISIT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.ayp.activity.BaseAypOutSchoolGroupVisitActivity;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.interactor.aypOutOfSchool.BaseAypOutSchoolGroupVisitInteractor;
import org.smartregister.chw.ayp.presenter.BaseAypVisitPresenter;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.task.RunnableTask;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

import java.util.Date;

public class AypOutSchoolGroupVisitActivity extends BaseAypOutSchoolGroupVisitActivity {

    public static void startAypOutSchoolGroupVisitActivity(Activity activity, Boolean editMode, String groupId, String groupName) {
        Intent intent = new Intent(activity, AypOutSchoolGroupVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, groupId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.AYP_OUT_SCHOOL_PROFILE);
        if (groupId != null) intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID, groupId);
        if (groupName != null) intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_NAME, groupName);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        String groupId = getIntent() != null ? getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID) : null;
        // In the app module, rely on DAO resolving; default to base implementation's member assignment
        MemberObject memberObject = new MemberObject();
        memberObject.setBaseEntityId(groupId);
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
        BaseAypOutSchoolGroupVisitInteractor interactor = new BaseAypOutSchoolGroupVisitInteractor(AYP_OUT_SCHOOL_GROUP_FOLLOW_UP_VISIT);
        String groupId = getIntent() != null ? getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID) : null;
        interactor.setGroupId(groupId);
        presenter = new BaseAypVisitPresenter(memberObject, this, interactor);
    }

    @Override
    public void submittedAndClose() {
        String groupId = getIntent() != null ? getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID) : null;
        Runnable runnable = () -> ChwScheduleTaskExecutor.getInstance().execute(groupId, Constants.EVENT_TYPE.AYP_OUT_SCHOOL_GROUP_FOLLOW_UP_VISIT, new Date());
        Utils.startAsyncTask(new RunnableTask(runnable), null);
        super.submittedAndClose();
    }

    @Override
    protected void attachBaseContext(Context base) {
        // get language from prefs
        String lang = LangUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(LangUtils.setAppLocale(base, lang));
    }
}
