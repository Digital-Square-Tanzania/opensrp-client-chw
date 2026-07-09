package org.smartregister.chw.fragment;

import android.os.Bundle;

import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;

import org.smartregister.chw.presenter.AncJsonWizardFormFragmentPresenter;

public class AncJsonWizardFormFragment extends JsonWizardFormFragment {

    public static AncJsonWizardFormFragment getFormFragment(String stepName) {
        AncJsonWizardFormFragment formFragment = new AncJsonWizardFormFragment();
        Bundle bundle = new Bundle();
        bundle.putString("stepName", stepName);
        formFragment.setArguments(bundle);
        return formFragment;
    }

    @Override
    protected JsonFormFragmentPresenter createPresenter() {
        return new AncJsonWizardFormFragmentPresenter(this, JsonFormInteractor.getInstance());
    }
}
