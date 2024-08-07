package org.smartregister.chw.fragment;

import org.smartregister.chw.activity.PncChildNoMotherProfileActivity;
import org.smartregister.chw.activity.PncHomeVisitActivity;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.core.provider.ChwPncRegisterProvider;
import org.smartregister.chw.model.PncChildNoMotherFragmentModel;
import org.smartregister.chw.presenter.PncChildNoMotherFragmentPresenter;
import org.smartregister.chw.provider.ChildRegisterProvider;
import org.smartregister.chw.provider.PncChildWithNoMotherProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.configurableviews.model.View;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;

import java.util.Set;

import provider.PncRegisterProvider;

public class PncChildNoMotherFragment extends PncRegisterFragment {


    @Override
    protected String getMainCondition() {
        return "";
    }

    @Override
    public void initializeAdapter(Set<View> visibleColumns) {
        PncRegisterProvider provider = new PncChildWithNoMotherProvider(getActivity(), commonRepository(), visibleColumns, registerActionHandler, paginationViewHandler);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, provider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new PncChildNoMotherFragmentPresenter( this, new PncChildNoMotherFragmentModel(), null);
    }

    @Override
    protected void openPncMemberProfile(CommonPersonObjectClient client) {
        MemberObject memberObject = new MemberObject(client);
        PncChildNoMotherProfileActivity.startMe(getActivity(), memberObject);

    }
}
