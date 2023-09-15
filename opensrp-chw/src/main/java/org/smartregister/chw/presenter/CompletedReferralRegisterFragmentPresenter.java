package org.smartregister.chw.presenter;

import static org.smartregister.chw.referral.util.Constants.ReferralType;
import static org.smartregister.chw.referral.util.Constants.Tables;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.ChwDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.referral.contract.BaseReferralRegisterFragmentContract;
import org.smartregister.chw.referral.presenter.BaseReferralRegisterFragmentPresenter;
import org.smartregister.chw.referral.util.DBConstants;
import org.smartregister.chw.util.Constants;

public class CompletedReferralRegisterFragmentPresenter extends BaseReferralRegisterFragmentPresenter {

    public CompletedReferralRegisterFragmentPresenter(BaseReferralRegisterFragmentContract.View view, BaseReferralRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    @NotNull
    public String getMainCondition() {
        return " " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.Key.DATE_REMOVED + " is null " +
                "AND " + Tables.REFERRAL + "." + DBConstants.Key.REFERRAL_TYPE + " = '" + ReferralType.COMMUNITY_TO_FACILITY_REFERRAL + "' " +
                "AND " + CoreConstants.TABLE_NAME.TASK + "." + ChwDBConstants.TaskTable.BUSINESS_STATUS + " = '" + CoreConstants.BUSINESS_STATUS.COMPLETE + "' ";

    }

    @Override
    @NotNull
    public String getDueFilterCondition() {
        return " ";

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
        return Tables.REFERRAL;
    }
}
