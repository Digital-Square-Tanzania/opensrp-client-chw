package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.CecapMemberProfileActivity;
import org.smartregister.chw.cecap.presenter.BaseCecapRegisterFragmentPresenter;
import org.smartregister.chw.core.fragment.CoreCecapRegisterFragment;
import org.smartregister.chw.model.CecapRegisterFragmentModel;

public class CecapRegisterFragment extends CoreCecapRegisterFragment {
    @Override
    protected void openProfile(String baseEntityId) {
        CecapMemberProfileActivity.startMe(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new BaseCecapRegisterFragmentPresenter(this, new CecapRegisterFragmentModel(), null);
    }

}
