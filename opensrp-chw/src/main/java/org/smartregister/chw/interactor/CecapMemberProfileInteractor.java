package org.smartregister.chw.interactor;

import org.smartregister.chw.cecap.contract.CecapProfileContract;
import org.smartregister.chw.cecap.domain.MemberObject;
import org.smartregister.chw.cecap.interactor.BaseCecapProfileInteractor;
import org.smartregister.chw.cecap.util.Constants;

public class CecapMemberProfileInteractor extends BaseCecapProfileInteractor {
    @Override
    public void refreshProfileInfo(MemberObject memberObject, CecapProfileContract.InteractorCallBack callback) {
        Runnable runnable = () -> appExecutors.mainThread().execute(() -> {
            callback.refreshMedicalHistory(getVisit(Constants.EVENT_TYPE.CECAP_HOME_VISIT, memberObject) != null);
        });
        appExecutors.diskIO().execute(runnable);
    }
}
