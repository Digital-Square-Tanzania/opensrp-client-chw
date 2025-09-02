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

import java.util.Arrays;
import java.util.LinkedHashMap;
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
        // Maintain canonical order based on interactor step sequence
        List<String> orderedKeys = Arrays.asList(
                "Population",
                "Number of households with basic nutrition source",
                "Healthcare services, education, child and elder care centers",
                "Social services and economic activities",
                "Committee meetings & Traditional medicine",
                "Environmental and sanitation Inspection report",
                "Building Inspection Report",
                "Workplace Inspection Report",
                "Food and Beverage Inspection Report",
                "Health Reports Affecting People in Workplaces",
                "Solid waste & waste collection equipments",
                "Identification and Control of Insect Breeding Sites"
        );

        LinkedHashMap<String, BaseHpsVisitAction> ordered = new LinkedHashMap<>();
        // First, add known actions in the defined order (skipping missing ones)
        for (String key : orderedKeys) {
            BaseHpsVisitAction action = map.get(key);
            if (action != null) {
                ordered.put(key, action);
            }
        }
        // Then, append any remaining actions that may not be in the canonical list
        for (Map.Entry<String, BaseHpsVisitAction> entry : map.entrySet()) {
            if (!ordered.containsKey(entry.getKey())) {
                ordered.put(entry.getKey(), entry.getValue());
            }
        }

        // Clear and repopulate the UI action list in the computed order
        actionList.clear();
        actionList.putAll(ordered);

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
