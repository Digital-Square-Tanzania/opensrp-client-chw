package org.smartregister.chw.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;

import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreFamilyRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.fragment.FamilyRegisterFragment;
import org.smartregister.chw.listener.ChwBottomNavigationListener;
import org.smartregister.chw.model.FamilyRegisterModel;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.chw.util.JsonFormUtils;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.presenter.BaseFamilyRegisterPresenter;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class FamilyRegisterActivity extends CoreFamilyRegisterActivity {

    private static final int REQUEST_SELECT_EXISTING_HEAD = 11001;
    private String pendingExistingHeadId;
    private CommonPersonObjectClient pendingExistingHeadClient;

    public static void startFamilyRegisterForm(Activity activity) {
        Intent intent = new Intent(activity, FamilyRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTION.START_REGISTRATION);
        activity.startActivity(intent);
    }

    @Override
    protected void initializePresenter() {
        presenter = new BaseFamilyRegisterPresenter(this, new FamilyRegisterModel());
    }

    public static void registerBottomNavigation(
            BottomNavigationHelper bottomNavigationHelper, BottomNavigationView bottomNavigationView, Activity activity
    ) {
        Utils.setupBottomNavigation(bottomNavigationHelper, bottomNavigationView, new ChwBottomNavigationListener(activity));
    }

    @Override
    protected void registerBottomNavigation() {
        super.registerBottomNavigation();
        FamilyRegisterActivity.registerBottomNavigation(bottomNavigationHelper, bottomNavigationView, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavigationMenu.getInstance(this, null, null);
        ChwApplication.getInstance().notifyAppContextChange(); // initialize the language (bug in translation)

        updateToolbarTitle(this, R.id.toolbar_title, getString(R.string.all_households));
        setTitle(R.string.all_households);

        action = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.ACTION);
        if (action != null && action.equals(Constants.ACTION.START_REGISTRATION)) {
            startRegistration();
        }
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new FamilyRegisterFragment();
    }

    @Override
    public void startRegistration() {
        showHeadSelectionDialog();
    }

    private void showHeadSelectionDialog() {
        CharSequence[] options = new CharSequence[]{
                getString(R.string.family_register_head_option_new),
                getString(R.string.family_register_head_option_existing)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.family_register_head_prompt)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startNewHeadRegistration();
                    } else {
                        launchExistingHeadPicker();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startNewHeadRegistration() {
        clearPendingExistingHead();
        launchFamilyRegistrationForm();
    }

    private void launchFamilyRegistrationForm() {
        FamilyRegisterActivity.super.startRegistration();
    }

    private void clearPendingExistingHead() {
        pendingExistingHeadId = null;
        pendingExistingHeadClient = null;
    }

    private void launchExistingHeadPicker() {
        Intent intent = new Intent(this, AllClientsRegisterActivity.class);
        intent.putExtra(Constants.EXTRA_CLIENT_PICKER_MODE, true);
        startActivityForResult(intent, REQUEST_SELECT_EXISTING_HEAD);
    }

    @Override
    public void startFormActivity(org.json.JSONObject jsonObject) {
        if (pendingExistingHeadClient != null) {
            try {
                JsonFormUtils.populateExistingHead(jsonObject, pendingExistingHeadClient);
            } catch (Exception e) {
                Timber.e(e);
                Toast.makeText(this, R.string.family_register_head_prefill_error, Toast.LENGTH_LONG).show();
            } finally {
                clearPendingExistingHead();
            }
        } else if (!TextUtils.isEmpty(pendingExistingHeadId)) {
            try {
                JsonFormUtils.populateExistingHead(jsonObject, pendingExistingHeadId);
            } catch (Exception e) {
                Timber.e(e);
                Toast.makeText(this, R.string.family_register_head_prefill_error, Toast.LENGTH_LONG).show();
            } finally {
                clearPendingExistingHead();
            }
        }
        super.startFormActivity(jsonObject);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Intercept family registration submission to enforce reuse of existing head
        if (resultCode == Activity.RESULT_OK && data != null
                && requestCode == org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON) {
            try {
                String json = data.getStringExtra("json");
                if (json != null) {
                    JSONObject form = new JSONObject(json);
                    if (form.has(org.smartregister.family.util.JsonFormUtils.STEP2)) {
                        JSONObject stepTwo = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.STEP2);
                        org.json.JSONArray fields = stepTwo.getJSONArray(com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS);
                        JSONObject existingHead = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "existing_head");
                        if (existingHead != null) {
                            String headId = existingHead.optString(com.vijay.jsonwizard.constants.JsonFormConstants.VALUE);
                            if (!TextUtils.isEmpty(headId)) {
                                form.put(org.smartregister.family.util.JsonFormUtils.ENTITY_ID, headId);
                                data.putExtra("json", form.toString());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Timber.w(e);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_EXISTING_HEAD) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String selectedBaseEntityId = data.getStringExtra(org.smartregister.family.util.Constants.INTENT_KEY.BASE_ENTITY_ID);
                if (!TextUtils.isEmpty(selectedBaseEntityId)) {
                    pendingExistingHeadId = selectedBaseEntityId;
                    try {
                        Object extra = data.getSerializableExtra(Constants.EXTRA_EXISTING_HEAD_CLIENT);
                        if (extra instanceof CommonPersonObjectClient) {
                            pendingExistingHeadClient = (CommonPersonObjectClient) extra;
                        }
                    } catch (Exception e) {
                        Timber.w(e);
                    }
                    launchFamilyRegistrationForm();
                } else {
                    Toast.makeText(this, R.string.family_register_head_prefill_error, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
