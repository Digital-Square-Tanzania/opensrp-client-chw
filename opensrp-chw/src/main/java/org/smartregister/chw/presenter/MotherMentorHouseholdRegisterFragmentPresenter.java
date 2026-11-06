package org.smartregister.chw.presenter;

import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.dao.MotherMentorHouseholdFollowUpDao;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorRegisterContactFragmentPresenter;

public class MotherMentorHouseholdRegisterFragmentPresenter
        extends BaseMotherMentorRegisterContactFragmentPresenter {
    private boolean historyMode;

    public MotherMentorHouseholdRegisterFragmentPresenter(
            MotherMentorRegisterFragmentContract.View view,
            MotherMentorRegisterFragmentContract.Model model,
            String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return org.smartregister.family.util.Utils.metadata().familyRegister.tableName;
    }

    public void setHistoryMode(boolean historyMode) {
        this.historyMode = historyMode;
    }

    @Override
    public String getMainCondition() {
        return MotherMentorHouseholdFollowUpDao.enrollmentCondition(getMainTable(), historyMode);
    }
}
