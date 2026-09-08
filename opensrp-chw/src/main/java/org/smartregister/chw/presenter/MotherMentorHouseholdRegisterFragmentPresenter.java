package org.smartregister.chw.presenter;

import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorRegisterContactFragmentPresenter;
import org.smartregister.chw.mothermentor.util.Constants;

public class MotherMentorHouseholdRegisterFragmentPresenter extends BaseMotherMentorRegisterContactFragmentPresenter {
    public MotherMentorHouseholdRegisterFragmentPresenter(MotherMentorRegisterFragmentContract.View view, MotherMentorRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return org.smartregister.family.util.Utils.metadata().familyRegister.tableName;
    }

    @Override
    public String getMainCondition() {
        return " " + getMainTable() + ".is_closed = 0 AND EXISTS (SELECT 1 FROM " + Constants.TABLES.MAMA_KINARA_HOUSEHOLD_ENROLMENT +
                " WHERE " + Constants.TABLES.MAMA_KINARA_HOUSEHOLD_ENROLMENT + ".household_id = " + getMainTable() + ".base_entity_id " +
                "AND " + Constants.TABLES.MAMA_KINARA_HOUSEHOLD_ENROLMENT + ".is_closed = 0 AND " +
                Constants.TABLES.MAMA_KINARA_HOUSEHOLD_ENROLMENT + ".consent = 'ndiyo') ";
    }
}
