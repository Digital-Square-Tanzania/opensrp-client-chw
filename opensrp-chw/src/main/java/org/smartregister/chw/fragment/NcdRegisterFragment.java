package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.NcdProfileActivity;
import org.smartregister.chw.core.fragment.CoreNcdRegisterFragment;
import org.smartregister.chw.model.NcdRegisterAtRiskFragmentModel;
import org.smartregister.chw.presenter.NcdRegisterFragmentPresenter;


public class NcdRegisterFragment extends CoreNcdRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        NcdProfileActivity.startProfileActivity(getActivity(), baseEntityId, false);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        View dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        dueOnlyLayout.setVisibility(View.GONE);
    }

    @Override
    protected void initializePresenter() {
        presenter = new NcdRegisterFragmentPresenter(this, new NcdRegisterAtRiskFragmentModel(), null);
    }
}
