package org.smartregister.chw.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.chw.R;
import org.smartregister.chw.core.fragment.CoreAllClientsRegisterFragment;
import org.smartregister.chw.provider.ChwAllClientsRegisterQueryProvider;
import org.smartregister.chw.provider.OpdRegisterProvider;
import org.smartregister.chw.configs.AllClientsRegisterRowOptions;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.opd.configuration.OpdConfiguration;
import org.smartregister.opd.utils.ConfigurationInstancesHelper;
import org.smartregister.family.util.Constants;

/**
 * Minimal client selection register fragment for picking an existing client.
 * - Reuses All Clients query provider and row binding
 * - No bottom navigation, menus, or add-client actions
 * - Returns selected client baseEntityId via Activity result
 */
public class ClientSelectionRegisterFragment extends CoreAllClientsRegisterFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Provide a minimal OPD configuration: reuse queries and rows, disable bottom nav
        OpdConfiguration opdConfiguration = new OpdConfiguration.Builder(ChwAllClientsRegisterQueryProvider.class)
                .setBottomNavigationEnabled(false)
                .setOpdRegisterRowOptions(AllClientsRegisterRowOptions.class)
                .build();
        setOpdRegisterQueryProvider(ConfigurationInstancesHelper.newInstance(opdConfiguration.getOpdRegisterQueryProvider()));

        // Inflate using our minimal layout through base
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);

        // Set a minimal, explicit title
        trySetTitle(view, org.smartregister.chw.R.string.select_clients);

        // Remove due-only toggle and sort/filter chrome for this selection UI
        safeHide(view, org.smartregister.chw.core.R.id.due_only_layout);
        safeHide(view, org.smartregister.chw.core.R.id.register_sort_filter_bar_layout);
        safeHide(view, org.smartregister.hivst.R.id.due_only_layout);
        safeHide(view, org.smartregister.hivst.R.id.register_sort_filter_bar_layout);
        safeHide(view, org.smartregister.chw.R.id.due_only_layout);
        safeHide(view, org.smartregister.chw.R.id.register_sort_filter_bar_layout);
    }

    private void trySetTitle(View root, int titleRes) {
        String title = getString(titleRes);
        android.widget.TextView tv = null;
        int[] candidateIds = new int[] {
                org.smartregister.chw.core.R.id.txt_title_label,
                org.smartregister.hivst.R.id.txt_title_label,
                org.smartregister.chw.R.id.txt_title_label
        };
        for (int id : candidateIds) {
            android.view.View v = root.findViewById(id);
            if (v instanceof android.widget.TextView) {
                tv = (android.widget.TextView) v;
                break;
            }
        }
        if (tv != null) {
            tv.setText(title);
        }
    }

    private void safeHide(View root, int id) {
        android.view.View v = root.findViewById(id);
        if (v != null) v.setVisibility(View.GONE);
    }

    @Override
    public void initializeAdapter() {
        OpdRegisterProvider registerProvider = new OpdRegisterProvider(getActivity(), registerActionHandler, paginationViewHandler);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, registerProvider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    protected void goToClientDetailActivity(@NonNull CommonPersonObjectClient commonPersonObjectClient) {
        Activity activity = getActivity();
        if (activity == null) return;

        String selectedBaseEntityId = commonPersonObjectClient.getCaseId();
        if (TextUtils.isEmpty(selectedBaseEntityId)) {
            selectedBaseEntityId = commonPersonObjectClient.entityId();
        }
        Intent data = new Intent();
        data.putExtra(Constants.INTENT_KEY.BASE_ENTITY_ID, selectedBaseEntityId);
        activity.setResult(Activity.RESULT_OK, data);
        activity.finish();
    }

    @Override
    protected void refreshSyncProgressSpinner() {
        // Hide sync affordances entirely in this minimal selection screen
        if (syncProgressBar != null) {
            syncProgressBar.setVisibility(View.GONE);
        }
        if (syncButton != null) {
            syncButton.setVisibility(View.GONE);
        }
    }
}
