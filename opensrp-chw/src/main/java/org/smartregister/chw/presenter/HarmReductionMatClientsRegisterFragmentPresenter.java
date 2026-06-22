package org.smartregister.chw.presenter;

import org.smartregister.chw.harmreduction.contract.HarmReductionRegisterFragmentContract;
import org.smartregister.chw.harmreduction.presenter.BaseHarmReductionRegisterFragmentPresenter;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.chw.harmreduction.util.DBConstants;

public class HarmReductionMatClientsRegisterFragmentPresenter extends BaseHarmReductionRegisterFragmentPresenter {

    public HarmReductionMatClientsRegisterFragmentPresenter(HarmReductionRegisterFragmentContract.View view,
                                                            HarmReductionRegisterFragmentContract.Model model,
                                                            String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.HARM_REDUCTION_RISK_ASSESSMENT;
    }

    @Override
    public String getMainCondition() {
        return getMainTable() + ".is_closed = 0 AND (" + getMainTable() + ".client_started_mat = 'yes' OR " +
                getMainTable() + ".follow_up_status = 'started_mat_services')";
    }

    @Override
    public String getDefaultSortQuery() {
        return getMainTable() + "." + DBConstants.KEY.LAST_INTERACTED_WITH + " DESC ";
    }

    @Override
    public String getDueFilterCondition() {
        return getMainCondition();
    }
}
