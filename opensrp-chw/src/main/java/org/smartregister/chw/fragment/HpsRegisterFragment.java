package org.smartregister.chw.fragment;

import android.view.View;

import androidx.recyclerview.widget.DividerItemDecoration;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.HpsMemberProfileActivity;
import org.smartregister.chw.core.CoreHpsRegisterFragment;
import org.smartregister.chw.hps.presenter.BaseHpsRegisterFragmentPresenter;
import org.smartregister.chw.model.HpsRegisterFragmentModel;
import org.smartregister.chw.provider.HpsClientRegisterProvider;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.view.customcontrols.CustomFontTextView;
import org.smartregister.view.customcontrols.FontVariant;

import java.util.Set;

public class HpsRegisterFragment extends CoreHpsRegisterFragment {

    @Override
    public void initializeAdapter(
            Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        HpsClientRegisterProvider provider =
                new HpsClientRegisterProvider(
                        getActivity(),
                        paginationViewHandler,
                        registerActionHandler,
                        visibleColumns);
        clientAdapter =
                new RecyclerViewPaginatedAdapter(
                        null, provider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);

        for (int i = clientsView.getItemDecorationCount() - 1; i >= 0; i--) {
            if (clientsView.getItemDecorationAt(i) instanceof DividerItemDecoration) {
                clientsView.removeItemDecorationAt(i);
            }
        }
        if (getSearchView() != null) {
            getSearchView().setBackgroundResource(R.drawable.mama_kinara_search_background);
            getSearchView().setTextColor(getResources().getColor(R.color.mama_kinara_text));
            getSearchView()
                    .setHintTextColor(getResources().getColor(R.color.mama_kinara_secondary));
        }

        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setVisibility(View.VISIBLE);
            titleView.setText(getString(org.smartregister.chw.hps.R.string.hps_client_register));
            titleView.setFontVariant(FontVariant.REGULAR);
        }
    }

    @Override
    protected void openProfile(String baseEntityId) {
        HpsMemberProfileActivity.startMe(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter =
                new BaseHpsRegisterFragmentPresenter(this, new HpsRegisterFragmentModel(), null);
    }
}
