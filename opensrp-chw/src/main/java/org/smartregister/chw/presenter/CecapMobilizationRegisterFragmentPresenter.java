package org.smartregister.chw.presenter;

import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.cecap.contract.CecapRegisterFragmentContract;
import org.smartregister.chw.cecap.presenter.BaseCecapRegisterFragmentPresenter;

public class CecapMobilizationRegisterFragmentPresenter extends BaseCecapRegisterFragmentPresenter {
    public CecapMobilizationRegisterFragmentPresenter(CecapRegisterFragmentContract.View view, CecapRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.CECAP_MOBILIZATION_SESSIONS;
    }

}
