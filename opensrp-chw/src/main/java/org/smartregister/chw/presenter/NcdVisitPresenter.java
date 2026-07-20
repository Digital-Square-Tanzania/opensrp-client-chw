package org.smartregister.chw.presenter;

import org.smartregister.chw.interactor.NcdVisitInteractor;
import org.smartregister.chw.ncd.contract.BaseNcdVisitContract;
import org.smartregister.chw.ncd.domain.MemberObject;
import org.smartregister.chw.ncd.presenter.BaseNcdVisitPresenter;

public class NcdVisitPresenter extends BaseNcdVisitPresenter {

    public NcdVisitPresenter(MemberObject memberObject, BaseNcdVisitContract.View view) {
        super(memberObject, view, new NcdVisitInteractor());
    }
}
