package org.smartregister.chw.presenter;

import org.smartregister.chw.harmreduction.contract.HarmReductionRegisterFragmentContract;
import org.smartregister.chw.harmreduction.presenter.BaseHarmReductionRegisterFragmentPresenter;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.chw.harmreduction.util.DBConstants;

public class HarmReductionSoberHouseRegisterFragmentPresenter extends BaseHarmReductionRegisterFragmentPresenter {

    public HarmReductionSoberHouseRegisterFragmentPresenter(HarmReductionRegisterFragmentContract.View view,
                                                            HarmReductionRegisterFragmentContract.Model model,
                                                            String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.HARM_REDUCTION_SOBER_HOUSE_ENROLLMENT;
    }

    @Override
    public String getMainCondition() {
        return getMainTable() + ".is_closed = 0 AND " + getMainTable() + ".detoxification_done = 'yes'";
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
