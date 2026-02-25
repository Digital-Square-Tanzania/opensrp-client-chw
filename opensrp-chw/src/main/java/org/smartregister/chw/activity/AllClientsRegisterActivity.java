package org.smartregister.chw.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.AllConstants;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreAllClientsRegisterActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.AllClientsRegisterFragment;
import org.smartregister.chw.model.ChwAllClientsRegisterModel;
import org.smartregister.chw.presenter.ChwAllClientRegisterPresenter;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.presenter.BaseOpdRegisterActivityPresenter;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class AllClientsRegisterActivity extends CoreAllClientsRegisterActivity {

    private boolean clientPickerMode;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clientPickerMode = getIntent().getBooleanExtra(Constants.EXTRA_CLIENT_PICKER_MODE, false);
        ChwApplication.getInstance().notifyAppContextChange();
    }

    public boolean isClientPickerMode() {
        return clientPickerMode;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new AllClientsRegisterFragment();
    }

    @Override
    public void startRegistration() {
        startFormActivity(CoreConstants.JSON_FORM.getAllClientRegistrationForm(), null, null);
    }

    @Override
    public void startFormActivity(String formName, String entityId, String metaData) {
        try {
            String locationId = org.smartregister.family.util.Utils.context().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            ((ChwAllClientRegisterPresenter) presenter()).startForm(formName, entityId, metaData, locationId);

        } catch (Exception e) {
            Timber.e(e);
            displayToast(org.smartregister.family.R.string.error_unable_to_start_form);
        }
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        Utils.setupBottomNavigation(bottomNavigationHelper, bottomNavigationView, this);
        bottomNavigationView.getMenu().findItem(R.id.action_register).setTitle(R.string.add_client).setIcon(R.drawable.ic_input_add);
    }

    @Override
    protected BaseOpdRegisterActivityPresenter createPresenter(
            @NonNull OpdRegisterActivityContract.View view, @NonNull OpdRegisterActivityContract.Model model) {
        return new ChwAllClientRegisterPresenter(view, model);
    }

    @Override
    public void switchToBaseFragment() {
        Intent intent = new Intent(this, AllClientsRegisterActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_scan_qr:
                startQrCodeScanner();
                return true;
            case R.id.action_family:
                switchToBaseFragment();
                break;
            case R.id.action_register:
                startRegistration();
                break;
            default:
                return true;
        }
        return true;
    }

    public OpdRegisterActivityContract.Model createActivityModel() {
        return new ChwAllClientsRegisterModel(this);
    }
}
