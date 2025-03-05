package org.smartregister.chw.presenter;

import org.smartregister.chw.hps.contract.HpsRegisterFragmentContract;
import org.smartregister.chw.hps.presenter.BaseHpsRegisterFragmentPresenter;
import org.smartregister.chw.hps.util.Constants;

public class HpsAnnualCensusRegisterFragmentPresenter extends BaseHpsRegisterFragmentPresenter {
    public HpsAnnualCensusRegisterFragmentPresenter(HpsRegisterFragmentContract.View view, HpsRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.HPS_ANNUAL_CENSUS_REGISTER;
    }

}
