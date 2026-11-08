package org.smartregister.chw.presenter;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorIitFollowUpDao;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorRegisterFragmentPresenter;

public class MotherMentorSecondaryEnrollmentRegisterFragmentPresenter
        extends BaseMotherMentorRegisterFragmentPresenter {
    private static final String DEFAULT_TABLE_NAME =
            org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_IIT;

    private final String tableName;
    private boolean historyMode;

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
        if (org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER
                .equals(getMainTable())) {
            return historyMode
                    ? org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao
                            .closedEnrollmentCondition(getMainTable())
                    : org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao
                            .activeEnrollmentCondition(getMainTable());
        }
        if (DEFAULT_TABLE_NAME.equals(getMainTable())) {
            return MotherMentorIitFollowUpDao.registerCondition(historyMode);
        }
        return MotherMentorDao.getActiveClientCondition(getMainTable());
    }

    public void setHistoryMode(boolean historyMode) {
        this.historyMode = historyMode;
    }

    @Override
    public String getMainTable() {
        return tableName;
    }
}
