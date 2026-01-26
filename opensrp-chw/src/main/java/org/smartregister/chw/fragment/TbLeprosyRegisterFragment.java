package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.TbLeprosyProfileActivity;
import org.smartregister.chw.core.fragment.CoreTbLeprosyRegisterFragment;

public class TbLeprosyRegisterFragment extends CoreTbLeprosyRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        TbLeprosyProfileActivity.startProfileActivity(requireActivity(), baseEntityId);
    }

//    @Override
//    protected int getTitleString() {
//        return R.string.menu_tbleprosy;
//    }
}
