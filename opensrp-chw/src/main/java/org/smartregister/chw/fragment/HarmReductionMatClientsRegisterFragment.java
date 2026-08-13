package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.activity.HarmReductionMatClientsProfileActivity;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.fragment.CoreHarmReductionRegisterFragment;
import org.smartregister.chw.model.HarmReductionMatClientsRegisterFragmentModel;
import org.smartregister.chw.presenter.HarmReductionMatClientsRegisterFragmentPresenter;
import org.smartregister.chw.provider.HarmReductionMatClientsRegisterProvider;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.util.Set;

public class HarmReductionMatClientsRegisterFragment extends CoreHarmReductionRegisterFragment {

    @Override
    public void initializeAdapter(Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        HarmReductionMatClientsRegisterProvider registerProvider =
                new HarmReductionMatClientsRegisterProvider(
                        getActivity(), paginationViewHandler, registerActionHandler, visibleColumns
                );
        clientAdapter = new RecyclerViewPaginatedAdapter(
                null, registerProvider, context().commonrepository(this.tablename)
        );
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setText(org.smartregister.chw.R.string.harm_reduction_mat_clients);
        }
    }

    @Override
    protected void openProfile(String baseEntityId) {
        HarmReductionMatClientsProfileActivity.startProfileActivity(requireActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new HarmReductionMatClientsRegisterFragmentPresenter(
                this, new HarmReductionMatClientsRegisterFragmentModel(), null
        );
    }
}
