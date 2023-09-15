package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.activity.MalariaFollowUpVisitActivity;
import org.smartregister.chw.activity.MalariaProfileActivity;
import org.smartregister.chw.core.fragment.CoreMalariaRegisterFragment;
import org.smartregister.chw.model.MalariaRegisterFragmentModel;
import org.smartregister.chw.presenter.MalariaRegisterFragmentPresenter;
import org.smartregister.view.activity.BaseRegisterActivity;

public class MalariaRegisterFragment extends CoreMalariaRegisterFragment {

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = ((BaseRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        presenter = new MalariaRegisterFragmentPresenter(this, new MalariaRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    protected void openProfile(String  baseEntityId) {
        MalariaProfileActivity.startMalariaActivity(getActivity(), baseEntityId);
    }

    @Override
    protected void openFollowUpVisit(String  baseEntityId) {
        MalariaFollowUpVisitActivity.startMalariaFollowUpActivity(getActivity(), baseEntityId);
    }

    @Override
    protected void toggleFilterSelection(View dueOnlyLayout) {
        super.toggleFilterSelection(dueOnlyLayout);
    }

    @Override
    protected String searchText() {
        return super.searchText();
    }

    @Override
    protected void dueFilter(View dueOnlyLayout) {
        super.dueFilter(dueOnlyLayout);
    }

    @Override
    protected void normalFilter(View dueOnlyLayout) {
        super.normalFilter(dueOnlyLayout);
    }
}


