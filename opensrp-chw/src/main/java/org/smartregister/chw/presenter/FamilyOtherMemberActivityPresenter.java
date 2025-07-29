package org.smartregister.chw.presenter;

import android.app.Activity;
import android.widget.Toast;

import org.smartregister.chw.activity.NcdHomeVisitActivity;
import org.smartregister.chw.core.contract.FamilyOtherMemberProfileExtendedContract;
import org.smartregister.chw.core.interactor.CoreFamilyProfileInteractor;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.interactor.FamilyInteractor;
import org.smartregister.chw.interactor.FamilyProfileInteractor;
import org.smartregister.chw.model.FamilyProfileModel;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.contract.FamilyOtherMemberContract;
import org.smartregister.family.contract.FamilyProfileContract;

public class FamilyOtherMemberActivityPresenter extends CoreFamilyOtherMemberActivityPresenter {

    public FamilyOtherMemberActivityPresenter(FamilyOtherMemberProfileExtendedContract.View view, FamilyOtherMemberContract.Model model, String viewConfigurationIdentifier, String familyBaseEntityId, String baseEntityId, String familyHead, String primaryCaregiver, String villageTown, String familyName) {
        super(view, model, viewConfigurationIdentifier, familyBaseEntityId, baseEntityId, familyHead, primaryCaregiver, villageTown, familyName);
    }

    @Override
    protected CoreFamilyProfileInteractor getFamilyProfileInteractor() {
        if (profileInteractor == null) {
            this.profileInteractor = new FamilyProfileInteractor();
        }
        return (CoreFamilyProfileInteractor) profileInteractor;
    }

    @Override
    protected FamilyProfileContract.Model getFamilyProfileModel(String familyName) {
        if (profileModel == null) {
            this.profileModel = new FamilyProfileModel(familyName);
        }
        return profileModel;
    }

    @Override
    protected void setProfileInteractor() {
        if (familyInteractor == null) {
            familyInteractor = new FamilyInteractor();
        }
    }

    public void recordNCDScreening(CommonPersonObjectClient client) {
        Toast.makeText(getView().getContext(), "NCD Screening feature is not implemented yet", Toast.LENGTH_SHORT).show();
        startFormForEdit(client);
        //NcdHomeVisitActivity.startMe((Activity) getView().getContext(), baseEntityId, false);
    }
}
