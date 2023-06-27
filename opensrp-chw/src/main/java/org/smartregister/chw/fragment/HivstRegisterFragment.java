package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.HivstProfileActivity;
import org.smartregister.chw.core.fragment.CoreHivstRegisterFragment;

public class HivstRegisterFragment extends CoreHivstRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        HivstProfileActivity.startProfile(requireActivity(), baseEntityId, false);
    }
}
