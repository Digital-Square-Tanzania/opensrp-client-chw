package org.smartregister.chw.fragment;

import static org.smartregister.chw.core.utils.ChildDBConstants.KEY.FAMILY_LAST_NAME;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.ChildHomeVisitActivity;
import org.smartregister.chw.activity.ChildProfileActivity;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.fragment.CoreChildRegisterFragment;
import org.smartregister.chw.model.ChildRegisterFragmentModel;
import org.smartregister.chw.presenter.ChildRegisterFragmentPresenter;
import org.smartregister.chw.provider.ChildRegisterProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.configurableviews.model.View;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.family.util.Utils;
import org.smartregister.view.activity.BaseRegisterActivity;

import java.util.Set;

import timber.log.Timber;

public class ChildRegisterFragment extends CoreChildRegisterFragment {

    @Override
    protected void onViewClicked(android.view.View view) {
        super.onViewClicked(view);
        if (view.getTag() instanceof CommonPersonObjectClient
                && view.getTag(R.id.VIEW_ID) == CLICK_VIEW_DOSAGE_STATUS) {
            CommonPersonObjectClient client = (CommonPersonObjectClient) view.getTag();
            ChildHomeVisitActivity.startMe(getActivity(), new MemberObject(client), false, ChildHomeVisitActivity.class);
        }
    }

    @Override
    public void goToChildDetailActivity(CommonPersonObjectClient patient, boolean launchDialog) {
        if (launchDialog) {
            Timber.i(patient.name);
        }
        MemberObject memberObject = new MemberObject(patient);
        memberObject.setFamilyName(Utils.getValue(patient.getColumnmaps(), FAMILY_LAST_NAME, false));
        ChildProfileActivity.startMe(getActivity(), memberObject, ChildProfileActivity.class);
    }

    @Override
    public void initializeAdapter(Set<View> visibleColumns) {
        ChildRegisterProvider childRegisterProvider = new ChildRegisterProvider(getActivity(), commonRepository(), visibleColumns, registerActionHandler, paginationViewHandler);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, childRegisterProvider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = ((BaseRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        presenter = new ChildRegisterFragmentPresenter(this, new ChildRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    public void setupViews(android.view.View view) {
        super.setupViews(view);

        if (ChwApplication.getApplicationFlavor().hasDefaultDueFilterForChildClient()) {
            android.view.View dueOnlyLayout = view.findViewById(org.smartregister.chw.core.R.id.due_only_layout);
            dueOnlyLayout.setVisibility(android.view.View.VISIBLE);
            dueOnlyLayout.setOnClickListener(registerActionHandler);
            dueOnlyLayout.setTag(null);
            toggleFilterSelection(dueOnlyLayout);
        }
    }

}
