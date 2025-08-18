package org.smartregister.chw.presenter;

import android.widget.LinearLayout;

import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonWizardFormFragmentPresenter;
import com.vijay.jsonwizard.views.JsonFormFragmentView;

import org.smartregister.chw.fragment.NcdJsonWizardFormFragment;

public class NcdJsonWizardFormFragmentPresenter extends JsonWizardFormFragmentPresenter {

    public NcdJsonWizardFormFragmentPresenter(JsonFormFragment jsonFormFragment, JsonFormInteractor jsonFormInteractor) {
        super(jsonFormFragment, jsonFormInteractor);
    }


/*    @Override
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
    }*/

/*    protected boolean moveToNextWizardStep() {
        String nextStep = this.getFormFragment().getJsonApi().nextStep();
        if ("step4".equals(nextStep)) {
            NcdJsonWizardFormFragment next = NcdJsonWizardFormFragment.getFormFragment(nextStep);
            ((JsonFormFragmentView<?>)this.getView()).hideKeyBoard();
            ((JsonFormFragmentView<?>)this.getView()).transactThis(next);
        }
        return super.moveToNextWizardStep();
    }*/
}
