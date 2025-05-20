package org.smartregister.chw.presenter;

import android.app.Activity;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.model.IssueReferralActivityModel;
import org.smartregister.chw.referral.contract.BaseIssueReferralContract;
import org.smartregister.chw.referral.model.AbstractIssueReferralModel;
import org.smartregister.chw.referral.presenter.BaseIssueReferralPresenter;
import org.smartregister.chw.referral.util.DBConstants;
import org.smartregister.chw.util.Constants;

public class IssueReferralActivityPresenter extends BaseIssueReferralPresenter {

    public IssueReferralActivityPresenter(String baseEntityId, BaseIssueReferralContract.View view, Class<? extends AbstractIssueReferralModel> viewModelClass, BaseIssueReferralContract.Interactor interactor) {
        super(baseEntityId, view, viewModelClass, interactor);
    }

    @Override
    public Class<? extends AbstractIssueReferralModel> getViewModel() {
        return IssueReferralActivityModel.class;
    }

    @NotNull
    @Override
    public String getMainCondition() {
        return " " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.Key.BASE_ENTITY_ID + " = '" + getBaseEntityID() + "'";
    }

    @NotNull
    @Override
    public String getMainTable() {
        return Constants.TABLE_NAME.FAMILY_MEMBER;
    }

    @Override
    public void onRegistrationSaved(boolean saveSuccessful, boolean isAdoLinkage) {
        super.onRegistrationSaved(saveSuccessful, isAdoLinkage);
        NavigationMenu navigationMenu = NavigationMenu.getInstance((Activity) getView(),
                null, null);
        if (navigationMenu != null) {
            navigationMenu.refreshCount();
        }
    }
}
