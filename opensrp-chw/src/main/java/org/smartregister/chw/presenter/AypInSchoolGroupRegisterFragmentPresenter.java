package org.smartregister.chw.presenter;

import org.smartregister.chw.ayp.contract.AypRegisterFragmentContract;
import org.smartregister.chw.ayp.presenter.BaseAypRegisterFragmentPresenter;
import org.smartregister.chw.ayp.util.Constants;

public class AypInSchoolGroupRegisterFragmentPresenter extends BaseAypRegisterFragmentPresenter {
    public AypInSchoolGroupRegisterFragmentPresenter(AypRegisterFragmentContract.View view, AypRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.AYP_IN_SCHOOL_GROUP_DETAILS;
    }

    @Override
    public String getMainCondition() {
        return " 1=1 ";
    }

}
