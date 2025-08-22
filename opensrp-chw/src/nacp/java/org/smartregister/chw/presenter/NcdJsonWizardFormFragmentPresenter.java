package org.smartregister.chw.presenter;

import android.widget.LinearLayout;

import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonWizardFormFragmentPresenter;
import com.vijay.jsonwizard.views.JsonFormFragmentView;
import com.vijay.jsonwizard.viewstates.JsonFormFragmentViewState;

import org.smartregister.chw.fragment.NcdJsonWizardFormFragment;

public class NcdJsonWizardFormFragmentPresenter extends JsonWizardFormFragmentPresenter {

    public NcdJsonWizardFormFragmentPresenter(JsonFormFragment jsonFormFragment, JsonFormInteractor jsonFormInteractor) {
        super(jsonFormFragment, jsonFormInteractor);
    }


    @Override
    public boolean onNextClick(LinearLayout mainView) {
        validateAndWriteValues();
        checkAndStopCountdownAlarm();
        boolean validateOnSubmit = validateOnSubmit();
        if (validateOnSubmit && getIncorrectlyFormattedFields().isEmpty()) {
            boolean isSkipped = this.executeRefreshLogicForNextStep();
            return !isSkipped && moveToNextWizardStep();
        } else if (isFormValid()) {
            boolean isSkipped = this.executeRefreshLogicForNextStep();
            return !isSkipped && moveToNextWizardStep();
        } else {
            getView().showSnackBar(getView().getContext().getResources()
                    .getString(com.vijay.jsonwizard.R.string.json_form_on_next_error_msg));
        }
        return false;
    }

    @Override
    public boolean executeRefreshLogicForNextStep() {
        boolean isSkipped = false;
        final String nextStep = getFormFragment().getJsonApi().nextStep();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(nextStep)) {
            getmJsonFormInteractor().fetchFormElements(nextStep, getFormFragment(), getFormFragment().getJsonApi().getmJSONObject().optJSONObject(nextStep), getView().getCommonListener(), false);
            getFormFragment().getJsonApi().initializeDependencyMaps();
            getFormFragment().getJsonApi().setNextStepRelevant(false);
            getFormFragment().getJsonApi().invokeRefreshLogic(null, false, null, null, nextStep, true);
            if (!getFormFragment().getJsonApi().isNextStepRelevant()) {
                com.vijay.jsonwizard.utils.Utils.checkIfStepHasNoSkipLogic(getFormFragment());
                // Clear data for skipped step
                clearStepData(nextStep);
            }
            isSkipped = getFormFragment().skipStepsOnNextPressed(nextStep);
        }
        return isSkipped;
    }

    /**
     * Clears user-input values for all fields in a given step, but keeps the field definitions
     */
    private void clearStepData(String stepName) {
        try {
            org.json.JSONObject formJson = getFormFragment().getJsonApi().getmJSONObject();
            if (formJson != null && formJson.has(stepName)) {
                org.json.JSONObject stepObj = formJson.optJSONObject(stepName);
                if (stepObj != null && stepObj.has("fields")) {
                    org.json.JSONArray fields = stepObj.optJSONArray("fields");
                    if (fields != null) {
                        for (int i = 0; i < fields.length(); i++) {
                            org.json.JSONObject field = fields.optJSONObject(i);
                            if (field != null) {
                                getView().writeValue(stepName, field.optString("key"), "",
                                        field.optString("openmrs_entity_parent", ""),
                                        field.optString("openmrs_entity", ""),
                                        field.optString("openmrs_entity_id", ""),
                                        field.optBoolean("popup", false));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            timber.log.Timber.e(e, "Error clearing values for step: %s", stepName);
        }
    }

    protected boolean moveToNextWizardStep() {
        final String nextStep = getFormFragment().getJsonApi().nextStep();
        if (!"".equals(nextStep)) {
            JsonFormFragment next = NcdJsonWizardFormFragment.getFormFragment(nextStep);
            getView().hideKeyBoard();
            getView().transactThis(next);
        }
        return false;
    }
}
