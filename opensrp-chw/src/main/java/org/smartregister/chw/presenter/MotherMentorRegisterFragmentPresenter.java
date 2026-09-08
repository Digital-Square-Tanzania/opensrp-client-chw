package org.smartregister.chw.presenter;

import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorRegisterFragmentPresenter;
public class MotherMentorRegisterFragmentPresenter extends BaseMotherMentorRegisterFragmentPresenter {

    private static final String MOTHER_MENTOR_ENROLLMENT_TABLE = "ec_mothermentor_enrollment";

    public MotherMentorRegisterFragmentPresenter(MotherMentorRegisterFragmentContract.View view, MotherMentorRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        return " " + getMainTable() + ".is_closed = 0 AND (" + getMainTable() + ".status IS NULL OR " + getMainTable() + ".status = 'client') AND (" + getMainTable() + ".screening_status IS NULL OR " + getMainTable() + ".screening_status != '-') ";
    }

    @Override
    public String getMainTable() {
        return MOTHER_MENTOR_ENROLLMENT_TABLE;
    }
}
