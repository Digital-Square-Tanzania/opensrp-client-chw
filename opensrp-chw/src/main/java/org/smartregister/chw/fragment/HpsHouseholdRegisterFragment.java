package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.HpsHouseholdProfileActivity;
import org.smartregister.chw.core.CoreHpsRegisterFragment;
import org.smartregister.chw.core.provider.CoreRegisterProvider;
import org.smartregister.chw.model.HpsHouseholdRegisterFragmentModel;
import org.smartregister.chw.presenter.HpsHouseholdRegisterFragmentPresenter;
import org.smartregister.chw.provider.FamilyRegisterProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.configurableviews.model.View;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.family.R;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.util.Set;

public class HpsHouseholdRegisterFragment extends CoreHpsRegisterFragment {
    @Override
    public void initializeAdapter(Set<View> visibleColumns) {
        CoreRegisterProvider chwRegisterProvider = new FamilyRegisterProvider(getActivity(), commonRepository(), visibleColumns, registerActionHandler, paginationViewHandler);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, chwRegisterProvider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new HpsHouseholdRegisterFragmentPresenter(this, new HpsHouseholdRegisterFragmentModel(), null);
    }


    @Override
    protected void onViewClicked(android.view.View view) {

        if (getActivity() == null) {
            return;
        }

        if (view.getTag() != null && view.getTag(R.id.VIEW_ID) == CLICK_VIEW_NORMAL) {
            goToPatientDetailActivity((CommonPersonObjectClient) view.getTag(), false);
        }
    }

    protected void goToPatientDetailActivity(CommonPersonObjectClient patient,
                                             boolean goToDuePage) {
        HpsHouseholdProfileActivity.startMe(getActivity(), Utils.getValue(patient.getColumnmaps(), DBConstants.KEY.FAMILY_HEAD, false));
    }

}
