package org.smartregister.chw.activity;

import androidx.annotation.NonNull;

import org.smartregister.chw.core.activity.CoreAllClientsRegisterActivity;
import org.smartregister.chw.fragment.ClientSelectionRegisterFragment;
import org.smartregister.chw.model.ChwAllClientsRegisterModel;
import org.smartregister.chw.presenter.ChwAllClientRegisterPresenter;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.presenter.BaseOpdRegisterActivityPresenter;
import org.smartregister.view.fragment.BaseRegisterFragment;

/**
 * Minimal activity that hosts ClientSelectionRegisterFragment.
 * No bottom nav, no register actions; returns a selected client baseEntityId.
 */
public class ClientSelectionRegisterActivity extends CoreAllClientsRegisterActivity {

    @Override
    public void onCreate(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.view.View bottomNav = findViewById(org.smartregister.chw.R.id.bottom_navigation);
        if (bottomNav != null) bottomNav.setVisibility(android.view.View.GONE);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new ClientSelectionRegisterFragment();
    }

    @Override
    protected void registerBottomNavigation() {
        // No bottom navigation for selection-only UI
    }

    @Override
    public void startRegistration() {
        // Disabled for selection-only flow
    }

    @Override
    protected BaseOpdRegisterActivityPresenter createPresenter(@NonNull OpdRegisterActivityContract.View view,
                                                               @NonNull OpdRegisterActivityContract.Model model) {
        // Presenter exists to satisfy base wiring; no registration started in this flow
        return new ChwAllClientRegisterPresenter(view, model);
    }

    public OpdRegisterActivityContract.Model createActivityModel() {
        return new ChwAllClientsRegisterModel(this);
    }
}
