package org.smartregister.chw.presenter;

import org.smartregister.chw.hps.util.Constants;
import org.smartregister.chw.hps.contract.HpsRegisterFragmentContract;
import org.smartregister.chw.hps.presenter.BaseHpsRegisterFragmentPresenter;

public class HpsMobilizationRegisterFragmentPresenter extends BaseHpsRegisterFragmentPresenter {
    public HpsMobilizationRegisterFragmentPresenter(HpsRegisterFragmentContract.View view, HpsRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.HPS_MOBILIZATION_SESSIONS;
    }

    @Override
    public String getMainCondition() {
        return " "+getMainTable()+".is_closed = 0 ";
    }

}
