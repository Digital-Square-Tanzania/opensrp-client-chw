package org.smartregister.chw.presenter;

import org.smartregister.chw.ncd.contract.NcdRegisterFragmentContract;
import org.smartregister.chw.ncd.presenter.BaseNcdRegisterFragmentPresenter;

public class NcdConfirmedRegisterFragmentPresenter extends BaseNcdRegisterFragmentPresenter {
    public NcdConfirmedRegisterFragmentPresenter(NcdRegisterFragmentContract.View view, NcdRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        String followUpStr = "(dhf.base_entity_id is not null AND (dhf.hypertension_test_result is not null OR dhf.diabetes_test_result is not null))";
        return " " + getMainTable() + ".is_closed = 0 AND ("+ followUpStr + " or dhc.base_entity_id is not null) ";
    }
}
