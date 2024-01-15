package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.GbvMemberProfileActivity;
import org.smartregister.chw.activity.SbcMemberProfileActivity;
import org.smartregister.chw.core.fragment.CoreGbvRegisterFragment;
import org.smartregister.chw.core.fragment.CoreSbcRegisterFragment;
import org.smartregister.chw.gbv.presenter.BaseGbvRegisterFragmentPresenter;
import org.smartregister.chw.model.GbvRegisterFragmentModel;
import org.smartregister.chw.model.SbcRegisterFragmentModel;
import org.smartregister.chw.sbc.presenter.BaseSbcRegisterFragmentPresenter;

public class GbvRegisterFragment extends CoreGbvRegisterFragment {
    @Override
    protected void openProfile(String baseEntityId) {
        GbvMemberProfileActivity.startMe(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new BaseGbvRegisterFragmentPresenter(this, new GbvRegisterFragmentModel(), null);
    }

}
