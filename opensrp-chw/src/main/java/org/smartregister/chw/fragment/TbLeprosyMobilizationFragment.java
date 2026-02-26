package org.smartregister.chw.fragment;

import android.view.View;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.adapter.TbLeprosyMobilizationSessionsRegisterAdapter;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.tbleprosy.dao.TbLeprosyMobilizationDao;
import org.smartregister.chw.tbleprosy.fragment.BaseTbLeprosyMobilizationRegisterFragment;
import org.smartregister.chw.tbleprosy.model.TbLeprosyMobilizationModel;
import org.smartregister.chw.util.JsonFormUtils;

import java.util.List;

import timber.log.Timber;

public class TbLeprosyMobilizationFragment extends BaseTbLeprosyMobilizationRegisterFragment {

    private TbLeprosyMobilizationSessionsRegisterAdapter adapter;

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
    protected void setUpAdapter() {
        if (getActivity() == null) {
            return;
        }

        List<TbLeprosyMobilizationModel> sessionModels = TbLeprosyMobilizationDao.getMobilizationSessions();
        if (sessionModels != null && !sessionModels.isEmpty()) {
            adapter = new TbLeprosyMobilizationSessionsRegisterAdapter(sessionModels, requireActivity());
            clientsView.setAdapter(adapter);
        } else {
            adapter = null;
            clientsView.setAdapter(null);
        }

        showEmptyState();
    }

    @Override
    protected void startForm(JSONObject form) {
        if (form == null || getActivity() == null) {
            return;
        }

        requireActivity().startActivityForResult(
                FormUtils.getStartFormActivity(form, requireActivity().getString(R.string.tbleprosy_mobilization), requireActivity()),
                JsonFormUtils.REQUEST_CODE_GET_JSON
        );
    }

    @Override
    protected void showEmptyState() {
        if (emptyViewLayout == null) {
            return;
        }

        if (adapter != null && adapter.getItemCount() > 0) {
            emptyViewLayout.setVisibility(View.GONE);
        } else {
            emptyViewLayout.setVisibility(View.VISIBLE);
        }
    }
}
