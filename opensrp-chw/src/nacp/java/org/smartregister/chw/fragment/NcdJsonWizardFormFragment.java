package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.presenter.NcdJsonWizardFormFragmentPresenter;

import timber.log.Timber;

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
        super.onViewCreated(view, savedInstanceState);
    }

/*    @Override
    public void onResume() {
        super.onResume();
        if (!getJsonApi().isPreviousPressed()) {
            skipStepsOnNextPressed();
        }
    }*/

    /**
     * Skips blank by relevance steps when next is clicked on the json wizard forms.
     */
    public void skipStepsOnNextPressed() {
        if (skipBlankSteps()) {
            JSONObject formStep = getStep(getArguments().getString(JsonFormConstants.STEPNAME));
            String next = formStep.optString(JsonFormConstants.NEXT, "");
            if (StringUtils.isNotEmpty(next)) {
                checkIfStepIsBlank(formStep);
                if (shouldSkipStep()) {
                    next();
                }
            }
        }
    }

    /**
     * Checks if a given step is blank due to relevance hidding all the widgets
     *
     * @param formStep {@link JSONObject}
     */
    private void checkIfStepIsBlank(JSONObject formStep) {
        try {
            if (formStep.has(JsonFormConstants.FIELDS)) {
                JSONArray fields = formStep.getJSONArray(JsonFormConstants.FIELDS);
                for (int i = 0; i < fields.length(); i++) {
                    JSONObject field = fields.getJSONObject(i);
                    if (field.has(JsonFormConstants.TYPE) && !JsonFormConstants.HIDDEN.equals(field.getString(JsonFormConstants.TYPE))) {
                        boolean isVisible = field.optBoolean(JsonFormConstants.IS_VISIBLE, true);
                        if (isVisible) {
                            setShouldSkipStep(false);
                            break;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Timber.e(e, "%s --> checkIfStepIsBlank", this.getClass().getCanonicalName());
        }
    }

    @Override
    protected JsonFormFragmentPresenter createPresenter() {
        return new NcdJsonWizardFormFragmentPresenter(this, JsonFormInteractor.getInstance());
    }

    @Override
    public void updateVisibilityOfNextAndSave(boolean next, boolean save) {
        super.updateVisibilityOfNextAndSave(next, save);
        final String nextStep = getJsonApi().nextStep();
        if ("step4".equals(nextStep)) {
            getMenu().findItem(R.id.action_save).setVisible(false);
        }
    }
}
