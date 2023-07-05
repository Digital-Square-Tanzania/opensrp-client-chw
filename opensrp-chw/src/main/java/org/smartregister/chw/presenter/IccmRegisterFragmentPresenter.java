package org.smartregister.chw.presenter;

import org.smartregister.chw.core.presenter.CoreMalariaRegisterFragmentPresenter;
import org.smartregister.chw.malaria.contract.MalariaRegisterFragmentContract;

public class IccmRegisterFragmentPresenter extends CoreMalariaRegisterFragmentPresenter {

    public IccmRegisterFragmentPresenter(MalariaRegisterFragmentContract.View view,
                                         MalariaRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    public String getMainCondition() {
        return " ec_family_member.date_removed is null AND date('now') <= date(strftime('%Y-%m-%d', ec_iccm_enrollment.last_interacted_with / 1000, 'unixepoch', 'localtime')) AND ec_iccm_enrollment.is_closed = 0";
    }

    @Override
    public String getMainTable() {
        return "ec_iccm_enrollment";
    }
}
