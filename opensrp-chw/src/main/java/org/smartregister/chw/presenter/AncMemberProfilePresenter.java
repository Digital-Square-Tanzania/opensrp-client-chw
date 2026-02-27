package org.smartregister.chw.presenter;

import android.app.Activity;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.activity.AncMemberProfileActivity;
import org.smartregister.chw.activity.ReferralRegistrationActivity;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.core.contract.AncMemberProfileContract;
import org.smartregister.chw.core.presenter.CoreAncMemberProfilePresenter;
import org.smartregister.chw.dao.ChwAncDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;

import java.util.List;

import timber.log.Timber;

public class AncMemberProfilePresenter extends CoreAncMemberProfilePresenter
        implements org.smartregister.chw.contract.AncMemberProfileContract.Presenter {

    private List<ReferralTypeModel> referralTypeModels;
    private List<ReferralTypeModel> addoReferralTypeModels;

    public AncMemberProfilePresenter(AncMemberProfileContract.View view, AncMemberProfileContract.Interactor interactor,
                                     MemberObject memberObject) {
        super(view, interactor, memberObject);
    }

    @Override
    public void referToFacility() {
        referralTypeModels = ((AncMemberProfileActivity) getView()).getReferralTypeModels();
        if (referralTypeModels.size() == 1) {
            startAncReferralForm();
        } else {
            Utils.launchClientReferralActivity((Activity) getView(), referralTypeModels, getEntityId());
        }
    }

    public void linkToADDO(){
        addoReferralTypeModels = ((AncMemberProfileActivity) getView()).getAddoReferralTypeModels();
        if (addoReferralTypeModels.size() == 1) {
            startAncLinkageForm();
        } else {
            Utils.launchClientReferralActivity((Activity) getView(), referralTypeModels, getEntityId());
        }
    }

    @Override
    public void refreshProfileTopSection(MemberObject memberObject) {
        super.refreshProfileTopSection(memberObject);
        String riskLabel = org.smartregister.chw.anc.util.Constants.HOME_VISIT.PREGNANCY_RISK_LOW;
        if(ChwAncDao.isClientHighRisk(memberObject.getBaseEntityId())){
            riskLabel = org.smartregister.chw.anc.util.Constants.HOME_VISIT.PREGNANCY_RISK_HIGH;
        }
        getView().setPregnancyRiskLabel(riskLabel);
    }

    @Override
    public void setPregnancyRiskTransportProfileDetails(MemberObject memberObject) {
        super.setPregnancyRiskTransportProfileDetails(memberObject);
        String riskLabel = org.smartregister.chw.anc.util.Constants.HOME_VISIT.PREGNANCY_RISK_LOW;
        if(ChwAncDao.isClientHighRisk(memberObject.getBaseEntityId())){
            riskLabel = org.smartregister.chw.anc.util.Constants.HOME_VISIT.PREGNANCY_RISK_HIGH;
        }
        getView().setPregnancyRiskLabel(riskLabel);
    }

    @Override
    public void startAncReferralForm() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            try {
                Activity context = ((Activity) getView());
                JSONObject formJson = (new FormUtils()).getFormJsonFromRepositoryOrAssets(context, Constants.JSON_FORM.getAncUnifiedReferralForm());
                formJson.put(Constants.REFERRAL_TASK_FOCUS, referralTypeModels.get(0).getReferralType());
                ReferralRegistrationActivity.startGeneralReferralFormActivityForResults(context,
                        getEntityId(), formJson, false, false);
            } catch (Exception ex) {
                Timber.e(ex);
            }
        } else {
            super.startAncReferralForm();
        }

    }

    public void startAncLinkageForm(){
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            try {
                Activity context = ((Activity) getView());
                JSONObject formJson = (new FormUtils()).getFormJsonFromRepositoryOrAssets(context, Constants.JSON_FORM.getAncUnifiedLinkageForm());
                formJson.put(Constants.REFERRAL_TASK_FOCUS, addoReferralTypeModels.get(0).getFocus());
                ReferralRegistrationActivity.startGeneralReferralFormActivityForResults(context,
                        getEntityId(), formJson, false, true);
            } catch (Exception ex) {
                Timber.e(ex);
            }
        }
    }

}
