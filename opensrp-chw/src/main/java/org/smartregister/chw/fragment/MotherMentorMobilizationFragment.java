package org.smartregister.chw.fragment;

import org.json.JSONObject;
import org.smartregister.chw.mothermentor.activity.BaseMotherMentorRegisterActivity;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorMobilizationRegisterFragment;

public class MotherMentorMobilizationFragment extends BaseMotherMentorMobilizationRegisterFragment {

    @Override
    protected void startForm(JSONObject form) {
        ((BaseMotherMentorRegisterActivity) requireActivity()).startFormActivity(form);
    }
}
