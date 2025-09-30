package org.smartregister.chw.interactor;

import androidx.annotation.NonNull;

import com.nerdstone.neatformcore.domain.model.NFormViewData;

import org.json.JSONObject;
import org.koin.core.Koin;
import org.smartregister.chw.referral.contract.BaseIssueReferralContract;
import org.smartregister.chw.referral.interactor.BaseIssueReferralInteractor;

import java.util.HashMap;

public class IssueReferralInteractor implements BaseIssueReferralContract.Interactor {
    private final BaseIssueReferralInteractor kotlinInteractor = new BaseIssueReferralInteractor();

    @Override
    public void saveRegistration(@NonNull String baseEntityId, @NonNull HashMap<String, NFormViewData> valuesHashMap, @NonNull JSONObject jsonObject, @NonNull BaseIssueReferralContract.InteractorCallBack callBack, boolean isAddoLinkage) throws Exception {
        kotlinInteractor.saveRegistration(baseEntityId, valuesHashMap, jsonObject, callBack, isAddoLinkage);
    }

    @NonNull
    @Override
    public Koin getKoin() {
        return null;
    }
}
