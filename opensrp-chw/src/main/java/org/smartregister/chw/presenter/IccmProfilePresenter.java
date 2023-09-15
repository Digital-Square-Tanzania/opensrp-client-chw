package org.smartregister.chw.presenter;

import androidx.annotation.Nullable;

import org.smartregister.chw.malaria.contract.MalariaProfileContract;
import org.smartregister.chw.malaria.domain.MemberObject;
import org.smartregister.chw.malaria.presenter.BaseMalariaProfilePresenter;

public class IccmProfilePresenter extends BaseMalariaProfilePresenter {
    public IccmProfilePresenter(MalariaProfileContract.View view, MalariaProfileContract.Interactor interactor, MemberObject memberObject) {
        super(view, interactor, memberObject);
    }

    @Override
    public void recordMalariaButton(@Nullable String visitState) {
        //THis is not required in ICCM, the record malaria button should always be visible
    }
}
