package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.HarmReductionSoberHouseProfileActivity;
import org.smartregister.chw.core.fragment.CoreHarmReductionRegisterFragment;
import org.smartregister.chw.core.model.CoreHarmReductionRegisterFragmentModel;
import org.smartregister.chw.presenter.HarmReductionSoberHouseRegisterFragmentPresenter;

public class HarmReductionSoberHouseRegisterFragment extends CoreHarmReductionRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        HarmReductionSoberHouseProfileActivity.startProfileActivity(requireActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new HarmReductionSoberHouseRegisterFragmentPresenter(this, new CoreHarmReductionRegisterFragmentModel(), null);
    }
}
