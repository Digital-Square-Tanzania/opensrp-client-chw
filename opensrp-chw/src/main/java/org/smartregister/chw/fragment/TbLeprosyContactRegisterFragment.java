package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.TbLeprosyProfileActivity;
import org.smartregister.chw.core.fragment.CoreTbLeprosyContactRegisterFragment;
import org.smartregister.chw.core.fragment.CoreTbLeprosyRegisterFragment;

public class TbLeprosyContactRegisterFragment extends CoreTbLeprosyContactRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        TbLeprosyProfileActivity.startProfileActivity(requireActivity(), baseEntityId);
    }
}
