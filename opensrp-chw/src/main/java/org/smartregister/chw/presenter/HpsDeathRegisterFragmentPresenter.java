package org.smartregister.chw.presenter;

import org.smartregister.chw.hps.contract.HpsRegisterFragmentContract;
import org.smartregister.chw.hps.presenter.BaseHpsRegisterFragmentPresenter;
import org.smartregister.chw.hps.util.Constants;

public class HpsDeathRegisterFragmentPresenter extends BaseHpsRegisterFragmentPresenter {
    public HpsDeathRegisterFragmentPresenter(HpsRegisterFragmentContract.View view, HpsRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.HPS_DEATH_REGISTER;
    }

    @Override
    public String getMainCondition() {
        return " " + getMainTable() + ".is_closed = 0 AND " + getMainTable() + ".dod IS NOT NULL AND "  + getMainTable() + ".dob IS NOT NULL ";
    }

}
