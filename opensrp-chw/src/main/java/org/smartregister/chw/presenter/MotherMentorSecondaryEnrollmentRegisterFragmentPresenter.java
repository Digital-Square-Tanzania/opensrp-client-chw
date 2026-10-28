package org.smartregister.chw.presenter;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorRegisterFragmentPresenter;

public class MotherMentorSecondaryEnrollmentRegisterFragmentPresenter
        extends BaseMotherMentorRegisterFragmentPresenter {
    private static final String DEFAULT_TABLE_NAME =
            org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_IIT;

    private final String tableName;

    public MotherMentorSecondaryEnrollmentRegisterFragmentPresenter(
            MotherMentorRegisterFragmentContract.View view,
            MotherMentorRegisterFragmentContract.Model model,
            String viewConfigurationIdentifier,
            String tableName) {
        super(view, model, viewConfigurationIdentifier);
        this.tableName = StringUtils.defaultIfBlank(tableName, DEFAULT_TABLE_NAME);
    }

    @Override
    public String getMainCondition() {
        return MotherMentorDao.getActiveClientCondition(getMainTable());
    }

    @Override
    public String getMainTable() {
        return tableName;
    }
}
