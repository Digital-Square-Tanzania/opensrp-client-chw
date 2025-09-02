package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.hps.activity.BaseHpsVisitActivity;
import org.smartregister.chw.hps.domain.MemberObject;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.chw.hps.presenter.BaseHpsVisitPresenter;
import org.smartregister.chw.hps.util.Constants;
import org.smartregister.chw.interactor.HpsAnnualCensusVisitInteractor;
import org.smartregister.family.util.Utils;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Action-based HPS Annual Census visit flow.
 */
public class HpsAnnualCensusVisitActivity extends BaseHpsVisitActivity {

    public static void startMe(Activity activity, String baseEntityID, Boolean isEditMode) {
        Intent intent = new Intent(activity, HpsAnnualCensusVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, isEditMode);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter.initialize();
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
        presenter = new BaseHpsVisitPresenter(memberObject, this, new HpsAnnualCensusVisitInteractor());
    }

    @Override
    public void initializeActions(LinkedHashMap<String, BaseHpsVisitAction> map) {
        //Clearing the action List before recreation
        actionList.clear();

        actionList.putAll(map);

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        displayProgressBar(false);
    }

    @Override
    public void redrawHeader(MemberObject memberObject) {
        tvTitle.setText(R.string.annual_census);
    }
}

