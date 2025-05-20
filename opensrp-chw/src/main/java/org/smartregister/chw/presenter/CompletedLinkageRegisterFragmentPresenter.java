package org.smartregister.chw.presenter;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.core.utils.ChwDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.referral.contract.BaseReferralRegisterFragmentContract;
import org.smartregister.chw.referral.util.DBConstants;
import org.smartregister.chw.util.Constants;
import org.smartregister.domain.Task;

/**
 * Author issyzac on 07/05/2024
 */
public class CompletedLinkageRegisterFragmentPresenter extends CompletedReferralRegisterFragmentPresenter {

    public CompletedLinkageRegisterFragmentPresenter(BaseReferralRegisterFragmentContract.View view, BaseReferralRegisterFragmentContract.Model model, String viewConfigurationIdentifier){
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public @NotNull String getMainCondition() {
        return " " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.Key.DATE_REMOVED + " is null " +
                "AND " + org.smartregister.chw.referral.util.Constants.Tables.REFERRAL + "." + DBConstants.Key.REFERRAL_TYPE + " = '" + org.smartregister.chw.referral.util.Constants.ReferralType.COMMUNITY_TO_ADDO_REFERRAL + "' " +
                "AND " + CoreConstants.TABLE_NAME.TASK + "." + ChwDBConstants.TaskTable.STATUS + "= '" +Task.TaskStatus.COMPLETED+"' ";

    }
}
