package org.smartregister.chw.presenter;

import org.smartregister.chw.hps.contract.HpsRegisterFragmentContract;
import org.smartregister.chw.hps.presenter.BaseHpsRegisterFragmentPresenter;
import org.smartregister.chw.hps.util.Constants;

public class HpsHouseholdRegisterFragmentPresenter extends BaseHpsRegisterFragmentPresenter {
    public HpsHouseholdRegisterFragmentPresenter(HpsRegisterFragmentContract.View view, HpsRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }


    @Override
    public String getMainTable() {
        return Constants.TABLES.HPS_HOUSEHOLD_REGISTER;
    }

    @Override
    public String getMainCondition() {
        return " "+getMainTable()+".is_closed = 0 AND does_the_household_consent_to_be_enrolled_in_hps_services = 'yes' ";
    }
}
