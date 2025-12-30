package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.HarmReductionProfileActivity;
import org.smartregister.chw.core.fragment.CoreHarmReductionRegisterFragment;

public class HarmReductionRegisterFragment extends CoreHarmReductionRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        HarmReductionProfileActivity.startProfileActivity(requireActivity(), baseEntityId);
    }
}
