package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.FamilyPlanningMemberProfileActivity;
import org.smartregister.chw.core.fragment.CoreFpRegisterFragment;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.model.FpRegisterFragmentModel;
import org.smartregister.chw.presenter.FpRegisterFragmentPresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;

public class FpRegisterFragment extends CoreFpRegisterFragment {

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new FpRegisterFragmentPresenter(this, new FpRegisterFragmentModel());
    }

    @Override
    protected void openProfile(String baseEntityId) {
        FamilyPlanningMemberProfileActivity.startFpMemberProfileActivity(getActivity(), FpDao.getMember(baseEntityId));
    }
}


