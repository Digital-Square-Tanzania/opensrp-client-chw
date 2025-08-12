package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;

import org.apache.commons.collections.KeyValue;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.presenter.NcdJsonWizardFormFragmentPresenter;
import org.smartregister.chw.referral.util.LocationUtils;

import java.util.List;
import java.util.Map;

public class NcdJsonWizardFormFragment extends JsonWizardFormFragment {

    public static NcdJsonWizardFormFragment getFormFragment(String stepName) {
        NcdJsonWizardFormFragment jsonFormFragment = new NcdJsonWizardFormFragment();
        Bundle bundle = new Bundle();
        bundle.putString(com.vijay.jsonwizard.constants.JsonFormConstants.JSON_FORM_KEY.STEPNAME, stepName);
        jsonFormFragment.setArguments(bundle);
        return jsonFormFragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            String stepName = getArguments().getString(JsonFormConstants.STEPNAME);
            String medicineSelectedString = getArguments().getString("medicine_selected");
            if (StringUtils.isNotBlank(medicineSelectedString) && "step4".equalsIgnoreCase(stepName)) {
                JSONObject step = getStep(stepName);
                Map<String, String> facilityOptions = LocationUtils.INSTANCE.getFacilitiesKeyAndName();
                //checkForMedsDispensedAndModifyForm(step, selectedMeds);
            }

        }
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected JsonFormFragmentPresenter createPresenter() {
        return new NcdJsonWizardFormFragmentPresenter(this, JsonFormInteractor.getInstance());
    }
}
