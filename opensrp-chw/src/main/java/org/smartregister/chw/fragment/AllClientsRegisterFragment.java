package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import org.smartregister.chw.R;
import org.smartregister.chw.core.fragment.CoreAllClientsRegisterFragment;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.model.FamilyDetailsModel;
import org.smartregister.chw.provider.OpdRegisterProvider;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.family.util.Constants;
import org.smartregister.opd.utils.OpdDbConstants;

public class AllClientsRegisterFragment extends CoreAllClientsRegisterFragment {
    public static final String REGISTER_TYPE = "register_type";

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        View dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        dueOnlyLayout.setVisibility(View.GONE);
    }

    @Override
    protected void goToClientDetailActivity(@NonNull CommonPersonObjectClient commonPersonObjectClient) {

        String registerType = commonPersonObjectClient.getDetails().get(REGISTER_TYPE);

        Bundle bundle = new Bundle();
        FamilyDetailsModel familyDetailsModel = FamilyDao.getFamilyDetail(commonPersonObjectClient.entityId());

        if (familyDetailsModel != null) {
            bundle.putString(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, familyDetailsModel.getBaseEntityId());
            bundle.putString(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, familyDetailsModel.getFamilyHead());
            bundle.putString(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, familyDetailsModel.getPrimaryCareGiver());
            bundle.putString(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME, familyDetailsModel.getFamilyName());
            bundle.putString(Constants.INTENT_KEY.VILLAGE_TOWN, commonPersonObjectClient.getDetails().get(OpdDbConstants.KEY.HOME_ADDRESS));
        }

        if (registerType != null) {
            switch (registerType) {
                case CoreConstants.REGISTER_TYPE.CHILD:
                    AllClientsUtils.goToChildProfile(this.getActivity(), commonPersonObjectClient, bundle);
                    break;
                case CoreConstants.REGISTER_TYPE.ANC:
                    AllClientsUtils.goToAncProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.PNC:
                    AllClientsUtils.gotToPncProfile(this.getActivity(), commonPersonObjectClient, bundle);
                    break;
                case CoreConstants.REGISTER_TYPE.MALARIA:
                    AllClientsUtils.gotToMalariaProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.ICCM:
                    AllClientsUtils.gotToIccmProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.FAMILY_PLANNING:
                    AllClientsUtils.goToFamilyPlanningProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.TB:
                    AllClientsUtils.goToTbProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.HIV:
                case CoreConstants.REGISTER_TYPE.CBHS:
                    AllClientsUtils.goToHivProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.AGYW:
                    AllClientsUtils.goToAgywProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.KVP_PrEP:
                    AllClientsUtils.goToKvpPrepProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.SBC:
                    AllClientsUtils.goToSbcProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                case CoreConstants.REGISTER_TYPE.GBV:
                    AllClientsUtils.goToGbvProfile(this.getActivity(), commonPersonObjectClient);
                    break;
                default:
                    AllClientsUtils.goToOtherMemberProfile(this.getActivity(), commonPersonObjectClient, bundle,
                            familyDetailsModel.getFamilyHead(), familyDetailsModel.getPrimaryCareGiver());
                    break;
            }
        } else {
            if (familyDetailsModel != null) {
                AllClientsUtils.goToOtherMemberProfile(this.getActivity(), commonPersonObjectClient, bundle,
                        familyDetailsModel.getFamilyHead(), familyDetailsModel.getPrimaryCareGiver());
            }
        }
    }


    @Override
    public void initializeAdapter() {
        OpdRegisterProvider childRegisterProvider = new OpdRegisterProvider(getActivity(), registerActionHandler, paginationViewHandler);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, childRegisterProvider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }
}
