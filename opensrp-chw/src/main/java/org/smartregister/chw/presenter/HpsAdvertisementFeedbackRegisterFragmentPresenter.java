package org.smartregister.chw.presenter;

import org.smartregister.chw.hps.contract.HpsRegisterFragmentContract;
import org.smartregister.chw.hps.presenter.BaseHpsRegisterFragmentPresenter;
import org.smartregister.chw.hps.util.Constants;

public class HpsAdvertisementFeedbackRegisterFragmentPresenter extends BaseHpsRegisterFragmentPresenter {
    public HpsAdvertisementFeedbackRegisterFragmentPresenter(HpsRegisterFragmentContract.View view, HpsRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.HPS_ADVERTISEMENT_FEEDBACK;
    }

    @Override
    public String getMainCondition() {
        return " "+getMainTable()+".is_closed = 0 ";
    }

}
