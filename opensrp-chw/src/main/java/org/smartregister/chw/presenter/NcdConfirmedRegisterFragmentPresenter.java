package org.smartregister.chw.presenter;

import org.smartregister.chw.ncd.contract.NcdRegisterFragmentContract;
import org.smartregister.chw.ncd.presenter.BaseNcdRegisterFragmentPresenter;

public class NcdConfirmedRegisterFragmentPresenter extends BaseNcdRegisterFragmentPresenter {
    public NcdConfirmedRegisterFragmentPresenter(NcdRegisterFragmentContract.View view, NcdRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        return " " + getMainTable() + ".is_closed = 0 AND (dhf.base_entity_id is not null or dhc.base_entity_id is not null) ";
    }
}
