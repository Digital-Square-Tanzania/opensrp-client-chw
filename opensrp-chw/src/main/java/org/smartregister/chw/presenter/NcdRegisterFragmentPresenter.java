package org.smartregister.chw.presenter;

import org.smartregister.chw.ncd.contract.NcdRegisterContract;
import org.smartregister.chw.ncd.contract.NcdRegisterFragmentContract;
import org.smartregister.chw.ncd.presenter.BaseNcdRegisterFragmentPresenter;

public class NcdRegisterFragmentPresenter extends BaseNcdRegisterFragmentPresenter {

    public NcdRegisterFragmentPresenter(NcdRegisterFragmentContract.View view, NcdRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        return " " + this.getMainTable() + ".is_closed = 0 AND dhf.base_entity_id is null AND dhc.base_entity_id is null ";
    }
}
