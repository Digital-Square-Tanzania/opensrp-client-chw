package org.smartregister.chw.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

import androidx.annotation.MenuRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreHarmReductionRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.HarmReductionMatClientsRegisterFragment;
import org.smartregister.chw.fragment.HarmReductionRegisterFragment;
import org.smartregister.chw.fragment.HarmReductionUsedNeedlesAndSyringesCollectionFragment;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.chw.harmreduction.util.JsonFormUtils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class HarmReductionRegisterActivity extends CoreHarmReductionRegisterActivity {
    static final String EXTRA_SHOW_REGISTRATION_CHOOSER = "show_harm_reduction_registration_chooser";
    static final String HARM_REDUCTION_REGISTER_EXISTING_CLIENT_FORM = "harm_reduction_register_existing_client";
    private static final String YES = "yes";
    private static final String ROC_MAT_PRE_SESSION_FIELD = "roc_mat_pre_session";
    private static final int EXISTING_CLIENT_REGISTRATION_INDEX = 1;

    public static void startRegistration(Activity activity, String memberBaseEntityID) {
        Intent intent = new Intent(activity, HarmReductionRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        intent.putExtra(EXTRA_SHOW_REGISTRATION_CHOOSER, true);
        activity.startActivity(intent);
    }

    @Override
    protected void onStartActivityWithAction() {
        if (shouldShowRegistrationChooser(getIntent())) {
            showRegistrationChooser(true);
            return;
        }

        super.onStartActivityWithAction();
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new HarmReductionRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{
                new HarmReductionUsedNeedlesAndSyringesCollectionFragment(),
                new HarmReductionMatClientsRegisterFragment()
        };
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_clients);
            bottomNavigationView.getMenu().removeItem(org.smartregister.chw.harmreduction.R.id.action_register);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_search);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_library);
            bottomNavigationView.inflateMenu(getMenuResource());
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                if (item.getItemId() == org.smartregister.chw.harmreduction.R.id.action_home) {
                    switchToBaseFragment();
                } else if (item.getItemId() == org.smartregister.chw.harmreduction.R.id.action_safety_boxes_collection) {
                    switchToFragment(1);
                } else if (item.getItemId() == R.id.action_mat_clients) {
                    switchToFragment(2);
                }
                return true;
            });
        }
    }

    @Override
    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_harm_reduction;
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.HARM_REDUCTION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        maybeOpenPreMatVisit(requestCode, resultCode, data);
    }

    void showRegistrationChooser(boolean finishOnDismiss) {
        String[] options = {
                getString(R.string.harm_reduction_new_client_registration),
                getString(R.string.harm_reduction_existing_client_registration)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.harm_reduction_registration_type_prompt)
                .setItems(options, (dialog, which) -> startFormActivity(resolveRegistrationFormName(which), BASE_ENTITY_ID, null))
                .setOnCancelListener(dialog -> {
                    if (finishOnDismiss) {
                        finish();
                    }
                })
                .show();
    }

    void maybeOpenPreMatVisit(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK
                || requestCode != org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON
                || data == null) {
            return;
        }

        String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
        if (StringUtils.isBlank(jsonString)) {
            return;
        }

        try {
            JSONObject form = new JSONObject(jsonString);
            if (!shouldLaunchPreMatSession(form)) {
                return;
            }

            String baseEntityId = form.optString(org.smartregister.util.JsonFormUtils.ENTITY_ID);
            if (StringUtils.isBlank(baseEntityId)) {
                return;
            }

            Intent intent = new Intent(this, HarmReductionVisitActivity.class);
            intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
            intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, false);
            intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.HARM_REDUCTION_PROFILE);
            startActivity(intent);
        } catch (JSONException e) {
            timber.log.Timber.e(e);
        }
    }

    static boolean shouldShowRegistrationChooser(Intent intent) {
        return intent != null && intent.getBooleanExtra(EXTRA_SHOW_REGISTRATION_CHOOSER, false);
    }

    static String resolveRegistrationFormName(int selectedIndex) {
        if (selectedIndex == EXISTING_CLIENT_REGISTRATION_INDEX) {
            return HARM_REDUCTION_REGISTER_EXISTING_CLIENT_FORM;
        }

        return Constants.FORMS.HARM_REDUCTION_RISK_ASSESSMENT;
    }

    static boolean shouldLaunchPreMatSession(JSONObject form) {
        return form != null
                && Constants.EVENT_TYPE.HARM_REDUCTION_RISK_ASSESSMENT.equalsIgnoreCase(
                form.optString(Constants.JSON_FORM_EXTRA.ENCOUNTER_TYPE)
        )
                && YES.equalsIgnoreCase(JsonFormUtils.getValue(form, ROC_MAT_PRE_SESSION_FIELD));
    }
}
