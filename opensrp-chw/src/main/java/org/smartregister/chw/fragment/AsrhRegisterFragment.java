package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.AsrhMemberProfileActivity;
import org.smartregister.chw.asrh.presenter.BaseAsrhRegisterFragmentPresenter;
import org.smartregister.chw.core.fragment.CoreAsrhRegisterFragment;
import org.smartregister.chw.model.AsrhRegisterFragmentModel;

public class AsrhRegisterFragment extends CoreAsrhRegisterFragment {
    @Override
    protected void openProfile(String baseEntityId) {
        AsrhMemberProfileActivity.startMe(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new BaseAsrhRegisterFragmentPresenter(this, new AsrhRegisterFragmentModel(), null);
    }

}
