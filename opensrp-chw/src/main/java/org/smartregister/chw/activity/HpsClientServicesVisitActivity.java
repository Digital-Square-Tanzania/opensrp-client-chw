package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.hps.activity.BaseHpsVisitActivity;
import org.smartregister.chw.hps.interactor.BaseHpsServiceVisitInteractor;
import org.smartregister.chw.hps.presenter.BaseHpsVisitPresenter;
import org.smartregister.family.util.Utils;

public class HpsClientServicesVisitActivity extends BaseHpsVisitActivity {
    public static void startMe(Activity activity, String baseEntityID, Boolean isEditMode) {
        Intent intent = new Intent(activity, HpsClientServicesVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, isEditMode);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
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
        presenter = new BaseHpsVisitPresenter(memberObject, this, new BaseHpsServiceVisitInteractor(org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_CLIENT_FOLLOW_UP_VISIT));
    }
}
