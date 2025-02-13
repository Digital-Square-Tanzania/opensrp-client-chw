package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.HpsMemberProfileActivity;
import org.smartregister.chw.core.CoreHpsRegisterFragment;
import org.smartregister.chw.hps.presenter.BaseHpsRegisterFragmentPresenter;
import org.smartregister.chw.model.HpsRegisterFragmentModel;

public class HpsRegisterFragment extends CoreHpsRegisterFragment {
    @Override
    protected void openProfile(String baseEntityId) {
        HpsMemberProfileActivity.startMe(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new BaseHpsRegisterFragmentPresenter(this, new HpsRegisterFragmentModel(), null);
    }
}
