package org.smartregister.chw.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.fragment.ChooseLoginMethodFragment;
import org.smartregister.chw.fragment.PinLoginFragment;
import org.smartregister.chw.pinlogin.PinLogger;
import org.smartregister.chw.pinlogin.PinLoginUtil;
import org.smartregister.chw.presenter.LoginPresenter;
import org.smartregister.chw.util.Utils;
import org.smartregister.family.util.Constants;
import org.smartregister.growthmonitoring.service.intent.WeightForHeightIntentService;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.task.SaveTeamLocationsTask;
import org.smartregister.view.activity.BaseLoginActivity;
import org.smartregister.view.contract.BaseLoginContract;

import java.util.Objects;

public class LoginActivity extends BaseLoginActivity implements BaseLoginContract.View {

    private static final String WFH_CSV_PARSED = "WEIGHT_FOR_HEIGHT_CSV_PARSED";

    private PinLogger pinLogger = PinLoginUtil.getPinLogger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageView imageView = findViewById(R.id.login_logo);
        if (BuildConfig.BUILD_FOR_BORESHA_AFYA_SOUTH) {
            imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_logo));
        } else {
            imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_logo_ba));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoginPresenter.processViewCustomizations();

        if (hasPinLogin()) {
            pinLoginAttempt();
            return;
        }

        if (!mLoginPresenter.isUserLoggedOut()) {
            goToHome(false);
        }
    }

    private void pinLoginAttempt() {
        // if the user has pin
        if (mLoginPresenter.isUserLoggedOut()) {
            if (pinLogger.isPinSet()) {
                Intent intent = new Intent(this, PinLoginActivity.class);
                intent.putExtra(PinLoginActivity.DESTINATION_FRAGMENT, PinLoginFragment.TAG);
                startActivity(intent);
                finish();
            }
        } else {
            goToHome(false);
        }
    }

    private boolean hasPinLogin() {
        return ChwApplication.getApplicationFlavor().hasPinLogin();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (hasPinLogin() && !pinLogger.isFirstAuthentication()) {
            menu.add("Reset Pin Login");
        }
        menu.add("Privacy Policy");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().toString().equalsIgnoreCase("Reset Pin Login")) {
            pinLogger.resetPinLogin();
            this.recreate();
            return true;
        }
        if (Objects.requireNonNull(item.getTitle()).toString().equalsIgnoreCase("Privacy Policy")) {
            this.startActivity(new Intent(this, PrivacyPolicyActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_login;
    }

    @Override
    protected void initializePresenter() {
        mLoginPresenter = new LoginPresenter(this);
    }

    @Override
    public void goToHome(boolean remote) {
        if (remote) {
            Utils.startAsyncTask(new SaveTeamLocationsTask(), null);
            processWeightForHeightZscoreCSV();
        }

        if (hasPinLogin()) {
            startPinHome(remote);
        } else {
            startHome(remote);
        }

        finish();
    }

    private void startHome(boolean remote) {
        Intent intent;
        if (BuildConfig.BUILD_FOR_BORESHA_AFYA_SOUTH) {
            intent = new Intent(this, ChwApplication.getApplicationFlavor().launchChildClientsAtLogin() ?
                    ChildRegisterActivity.class : AllClientsRegisterActivity.class);
        } else {
            intent = new Intent(this, ChwApplication.getApplicationFlavor().launchChildClientsAtLogin() ?
                    ChildRegisterActivity.class : FamilyRegisterActivity.class);
        }
        intent.putExtra(Constants.INTENT_KEY.IS_REMOTE_LOGIN, remote);
        startActivity(intent);
    }

    private void startPinHome(boolean remote) {
        if (remote)
            pinLogger.resetPinLogin();

        if (pinLogger.isFirstAuthentication()) {
            EditText passwordEditText = findViewById(org.smartregister.R.id.login_password_edit_text);
            pinLogger.savePassword(passwordEditText.getText().toString());
        }

        if (pinLogger.isFirstAuthentication()) {
            Intent intent = new Intent(this, PinLoginActivity.class);
            intent.putExtra(PinLoginActivity.DESTINATION_FRAGMENT, ChooseLoginMethodFragment.TAG);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(this, ChwApplication.getApplicationFlavor().launchChildClientsAtLogin() ?
                    ChildRegisterActivity.class : FamilyRegisterActivity.class);
            intent.putExtra(Constants.INTENT_KEY.IS_REMOTE_LOGIN, remote);
            startActivity(intent);
        }
    }

    private void processWeightForHeightZscoreCSV() {
        AllSharedPreferences allSharedPreferences = ChwApplication.getInstance().getContext().allSharedPreferences();
        if (ChwApplication.getApplicationFlavor().hasChildSickForm() && !allSharedPreferences.getPreference(WFH_CSV_PARSED).equals("true")) {
            WeightForHeightIntentService.startParseWFHZScores(this);
            allSharedPreferences.savePreference(WFH_CSV_PARSED, "true");
        }
    }
}