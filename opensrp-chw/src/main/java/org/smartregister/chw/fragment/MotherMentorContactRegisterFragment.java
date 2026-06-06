package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.MotherMentorProfileActivity;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorContactFragment;

public class MotherMentorContactRegisterFragment extends BaseMotherMentorContactFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        MotherMentorProfileActivity.startMe(requireActivity(), baseEntityId);
    }
}
