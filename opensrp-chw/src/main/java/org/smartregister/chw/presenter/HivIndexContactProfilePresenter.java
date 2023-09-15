package org.smartregister.chw.presenter;

import android.app.Activity;

import org.smartregister.chw.activity.HivIndexContactProfileActivity;
import org.smartregister.chw.contract.HivIndexContactProfileContract;
import org.smartregister.chw.core.presenter.CoreHivIndexContactProfilePresenter;
import org.smartregister.chw.hiv.domain.HivIndexContactObject;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.util.Utils;

import java.util.List;

public class HivIndexContactProfilePresenter extends CoreHivIndexContactProfilePresenter
        implements org.smartregister.chw.contract.AncMemberProfileContract.Presenter {

    private HivIndexContactObject hivIndexContactObject;

    public HivIndexContactProfilePresenter(HivIndexContactProfileContract.View view, HivIndexContactProfileContract.Interactor interactor,
                                           HivIndexContactObject hivIndexContactObject) {
        super(view, interactor, hivIndexContactObject);
        this.hivIndexContactObject = hivIndexContactObject;
    }

    @Override
    public void referToFacility() {
        List<ReferralTypeModel> referralTypeModels = ((HivIndexContactProfileActivity) getView()).getReferralTypeModels();
        Utils.launchClientReferralActivity((Activity) getView(), referralTypeModels, hivIndexContactObject.getBaseEntityId());
    }
}