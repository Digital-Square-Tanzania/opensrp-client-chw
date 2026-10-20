package org.smartregister.chw.presenter;

import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorRegisterFragmentPresenter;
import org.smartregister.chw.mothermentor.util.DBConstants;

public class MotherMentorRegisterFragmentPresenter
        extends BaseMotherMentorRegisterFragmentPresenter {

    private static final String MOTHER_MENTOR_ENROLLMENT_TABLE =
            org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLLMENT;

    public MotherMentorRegisterFragmentPresenter(
            MotherMentorRegisterFragmentContract.View view,
            MotherMentorRegisterFragmentContract.Model model,
            String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        return " "
                + getMainTable()
                + "."
                + DBConstants.KEY.IS_CLOSED
                + " = 0 AND ("
                + getMainTable()
                + "."
                + DBConstants.KEY.STATUS
                + " IS NULL OR "
                + getMainTable()
                + "."
                + DBConstants.KEY.STATUS
                + " = 'client') AND ("
                + getMainTable()
                + "."
                + DBConstants.KEY.SCREENING_STATUS
                + " IS NULL OR "
                + getMainTable()
                + "."
                + DBConstants.KEY.SCREENING_STATUS
                + " != '-') ";
    }

    @Override
    public String getMainTable() {
        return MOTHER_MENTOR_ENROLLMENT_TABLE;
    }
}
