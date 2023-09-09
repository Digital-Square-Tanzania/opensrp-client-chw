package org.smartregister.chw.presenter;

import android.app.Activity;

import org.smartregister.chw.activity.HivProfileActivity;
import org.smartregister.chw.contract.AncMemberProfileContract;
import org.smartregister.chw.contract.HivProfileContract;
import org.smartregister.chw.core.presenter.CoreHivProfilePresenter;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.util.Utils;

import java.util.List;

public class HivProfilePresenter extends CoreHivProfilePresenter
        implements AncMemberProfileContract.Presenter {

    private HivMemberObject hivMemberObject;

    public HivProfilePresenter(HivProfileContract.View view, HivProfileContract.Interactor interactor,
                               HivMemberObject hivMemberObject) {
        super(view, interactor, hivMemberObject);
        this.hivMemberObject = hivMemberObject;
    }

    @Override
    public void referToFacility() {
        List<ReferralTypeModel> referralTypeModels = ((HivProfileActivity) getView()).getReferralTypeModels();
        Utils.launchClientReferralActivity((Activity) getView(), referralTypeModels, hivMemberObject.getBaseEntityId());
    }
}
