package org.smartregister.chw.presenter;

import android.content.Context;

import org.smartregister.chw.R;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.domain.FamilyMember;
import org.smartregister.chw.core.model.CoreChildRegisterModel;
import org.smartregister.chw.core.presenter.CoreFamilyProfilePresenter;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.interactor.FamilyChangeContractInteractor;
import org.smartregister.chw.interactor.FamilyProfileInteractor;
import org.smartregister.chw.model.ChildRegisterModel;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.activity.BaseFamilyProfileActivity;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.util.Utils;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.view.LocationPickerView;

import timber.log.Timber;

public class FamilyProfilePresenter extends CoreFamilyProfilePresenter {

    public FamilyProfilePresenter(FamilyProfileExtendedContract.View view, FamilyProfileContract.Model model, String familyBaseEntityId, String familyHead, String primaryCaregiver, String familyName) {
        super(view, model, familyBaseEntityId, familyHead, primaryCaregiver, familyName);
        interactor = new FamilyProfileInteractor();
        verifyHasPhone();
    }

    @Override
    protected CoreChildRegisterModel getChildRegisterModel() {
        return new ChildRegisterModel();
    }


    @Override
    public boolean updatePrimaryCareGiver(Context context, String jsonString, String familyBaseEntityId, String entityID) {

        boolean res = false;
        try {
            FamilyMember member = CoreJsonFormUtils.getFamilyMemberFromRegistrationForm(jsonString, familyBaseEntityId, entityID);
            if (member != null && member.getPrimaryCareGiver()) {
                LocationPickerView lpv = new LocationPickerView(context);
                lpv.init();
                String lastLocationId = LocationHelper.getInstance().getOpenMrsLocationId(lpv.getSelectedItem());

                new FamilyChangeContractInteractor().updateFamilyRelations(context, member, lastLocationId);
                res = true;
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return res;
    }

    @Override
    public void refreshProfileTopSection(CommonPersonObjectClient client) {
        super.refreshProfileTopSection(client);
        if (client != null && client.getColumnmaps() != null) {
            String firstName = Utils.getValue(client.getColumnmaps(), "first_name", true);
            String famName;
            if (Utils.getBooleanProperty("family.head.first.name.enabled")) {
                String familyHeadFirstName = Utils.getValue(client.getColumnmaps(), "family_head_name", true);
                famName = ((BaseFamilyProfileActivity) this.getView()).getString(R.string.family_profile_title_with_firstname, new Object[]{familyHeadFirstName, firstName});
            } else {
                famName = ((BaseFamilyProfileActivity) this.getView()).getString(R.string.family_profile_title, firstName);
            }
            this.getView().setProfileName(famName);
        }
    }

    @Override
    public void onEventSaveComplete(boolean b) {
        if(b) {
            getView().hideProgressDialog();
        }
    }
}
