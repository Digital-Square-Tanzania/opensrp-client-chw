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
        return super.getMainCondition() + " AND (dhf.diabetes_test_result is null or dhf.hypertension_test_result is null) ";
    }
}
