package org.smartregister.chw.presenter;

import android.app.Activity;

import org.smartregister.chw.activity.TbProfileActivity;
import org.smartregister.chw.contract.AncMemberProfileContract;
import org.smartregister.chw.contract.TbProfileContract;
import org.smartregister.chw.core.presenter.CoreTbProfilePresenter;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.tb.domain.TbMemberObject;
import org.smartregister.chw.util.Utils;

import java.util.List;

public class TbProfilePresenter extends CoreTbProfilePresenter
        implements AncMemberProfileContract.Presenter {

    private TbMemberObject tbMemberObject;

    public TbProfilePresenter(TbProfileContract.View view, TbProfileContract.Interactor interactor,
                              TbMemberObject tbMemberObject) {
        super(view, interactor, tbMemberObject);
        this.tbMemberObject = tbMemberObject;
    }

    @Override
    public void referToFacility() {
        List<ReferralTypeModel> referralTypeModels = ((TbProfileActivity) getView()).getReferralTypeModels();
        Utils.launchClientReferralActivity((Activity) getView(), referralTypeModels, tbMemberObject.getBaseEntityId());
    }
}
