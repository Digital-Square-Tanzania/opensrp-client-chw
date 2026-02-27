package org.smartregister.chw.presenter;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.ChwDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.referral.contract.BaseReferralRegisterFragmentContract;
import org.smartregister.chw.referral.presenter.BaseReferralRegisterFragmentPresenter;
import org.smartregister.chw.referral.util.DBConstants;
import org.smartregister.chw.util.Constants;
import org.smartregister.domain.Task;

/**
 * Author issyzac on 07/05/2024
 */
public class LinkageRegisterFragmentPresenter extends BaseReferralRegisterFragmentPresenter {

    public LinkageRegisterFragmentPresenter(BaseReferralRegisterFragmentContract.View view, BaseReferralRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    @NotNull
    public String getMainCondition() {
        return " " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.Key.DATE_REMOVED + " is null " +
                "AND " + org.smartregister.chw.referral.util.Constants.Tables.REFERRAL + "." + DBConstants.Key.REFERRAL_TYPE + " = '" + org.smartregister.chw.referral.util.Constants.ReferralType.COMMUNITY_TO_ADDO_REFERRAL + "' " +
                "AND " + CoreConstants.TABLE_NAME.TASK + "." + ChwDBConstants.TaskTable.STATUS + " <> '" + Task.TaskStatus.CANCELLED + "' " +
                "AND " + CoreConstants.TABLE_NAME.TASK + "." + ChwDBConstants.TaskTable.STATUS + " <> '"+Task.TaskStatus.COMPLETED+"'";

    }

    @Override
    @NotNull
    public String getDueFilterCondition() {
        return " " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.Key.DATE_REMOVED + " is null " +
                "AND " + Constants.TABLE_NAME.TASK + "." + ChwDBConstants.TaskTable.STATUS + " = '" + CoreConstants.BUSINESS_STATUS.EXPIRED + "' " +
                "AND " + org.smartregister.chw.referral.util.Constants.Tables.REFERRAL + "." + DBConstants.Key.REFERRAL_TYPE + " = '" + org.smartregister.chw.referral.util.Constants.ReferralType.COMMUNITY_TO_ADDO_REFERRAL + "' ";

    }

    @Override
    public void processViewConfigurations() {
        super.processViewConfigurations();
        if (getConfig().getSearchBarText() != null && getView() != null) {
            getView().updateSearchBarHint(getView().getContext().getString(R.string.search_name_or_id));
        }
    }

    @Override
    public String getMainTable() {
        return org.smartregister.chw.referral.util.Constants.Tables.REFERRAL;
    }

}
