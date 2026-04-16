package org.smartregister.chw.presenter;

import org.smartregister.chw.interactor.NcdCaseManagementInteractor;
import org.smartregister.chw.ncd.contract.BaseNcdVisitContract;
import org.smartregister.chw.ncd.domain.MemberObject;
import org.smartregister.chw.ncd.presenter.BaseNcdVisitPresenter;

public class NcdCaseManagementVisitPresenter extends BaseNcdVisitPresenter {

    public NcdCaseManagementVisitPresenter(MemberObject memberObject, BaseNcdVisitContract.View view) {
        super(memberObject, view, new NcdCaseManagementInteractor());
    }

    public NcdCaseManagementInteractor getCaseManagementInteractor() {
        return (NcdCaseManagementInteractor) interactor;
    }
}
