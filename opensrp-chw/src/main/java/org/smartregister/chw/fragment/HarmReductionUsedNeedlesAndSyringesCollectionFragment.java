package org.smartregister.chw.fragment;

import android.view.View;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.adapter.HarmReductionUsedNeedlesAndSyringesCollectionRegisterAdapter;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.harmreduction.dao.HarmReductionUsedNeedlesAndSyringesCollectionDao;
import org.smartregister.chw.harmreduction.fragment.BaseHarmReductionUsedNeedlesAndSyringesCollectionRegisterFragment;
import org.smartregister.chw.harmreduction.model.HarmReductionUsedNeedlesAndSyringesCollectionModel;
import org.smartregister.chw.util.JsonFormUtils;

import java.util.List;

import timber.log.Timber;

public class HarmReductionUsedNeedlesAndSyringesCollectionFragment extends BaseHarmReductionUsedNeedlesAndSyringesCollectionRegisterFragment {
    private HarmReductionUsedNeedlesAndSyringesCollectionRegisterAdapter adapter;

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        try {
            NavigationMenu.getInstance(getActivity(), null, toolbar);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu.getInstance(getActivity(), null, toolbar);
    }

    @Override
    protected void startForm(JSONObject form) {
        requireActivity().startActivityForResult(
                org.smartregister.chw.core.utils.FormUtils.getStartFormActivity(
                        form,
                        requireActivity().getString(R.string.harm_reduction_used_needles_collection),
                        requireActivity()
                ),
                JsonFormUtils.REQUEST_CODE_GET_JSON
        );
    }

    @Override
    protected void setUpAdapter() {
        List<HarmReductionUsedNeedlesAndSyringesCollectionModel> collectionModels = HarmReductionUsedNeedlesAndSyringesCollectionDao.getCollectionSessions();
        if (collectionModels != null && !collectionModels.isEmpty()) {
            adapter = new HarmReductionUsedNeedlesAndSyringesCollectionRegisterAdapter(collectionModels, requireActivity());
            clientsView.setAdapter(adapter);
        } else {
            adapter = null;
            clientsView.setAdapter(null);
        }

        showEmptyState();
    }

    @Override
    protected void showEmptyState() {
        if (emptyViewLayout != null) {
            if (adapter != null && adapter.getItemCount() >= 1) {
                emptyViewLayout.setVisibility(View.GONE);
            } else {
                emptyViewLayout.setVisibility(View.VISIBLE);
            }
        }
    }
}
