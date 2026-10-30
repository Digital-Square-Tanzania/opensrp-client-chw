package org.smartregister.chw.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import org.joda.time.DateTime;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.PinLoginActivity;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.contract.PinLoginContract;
import org.smartregister.chw.contract.PinViewContract;
import org.smartregister.chw.pinlogin.PinLogger;
import org.smartregister.chw.presenter.PinLoginPresenter;
import org.smartregister.util.Utils;

import timber.log.Timber;

/**
 * @author rkodev
 */
public class PinLoginFragment extends Fragment implements View.OnClickListener, PinLoginContract.View {

    public static final String TAG = "PinLoginFragment";

    private PinLoginContract.Presenter mLoginPresenter;
    private EditText passwordEditText;
    private boolean showPasswordChecked = false;
    private TextView showPinText;
    private CheckBox showPasswordCheck;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pin_login_fragment, container, false);
        initializeBuildDetails(view);

        mLoginPresenter = new PinLoginPresenter(this);

        showPasswordCheck = view.findViewById(R.id.login_show_password_checkbox);
        showPinText = view.findViewById(R.id.login_show_password_text_view);
        btnLogin = view.findViewById(R.id.login_login_btn);
        passwordEditText = view.findViewById(R.id.login_password_edit_text);
        progressBar = view.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        passwordEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == org.smartregister.R.integer.login || actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        TextView enterPinTextView = view.findViewById(R.id.pin_title_text_view);
        enterPinTextView.setText(getString(R.string.enter_pin_for_user, getController().getPinLogger().loggedInUser()));

        setListenerOnShowPasswordCheckbox();

        view.findViewById(R.id.forgot_pin).setOnClickListener(this);
        view.findViewById(R.id.use_your_password).setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        return view;
    }

    private void setListenerOnShowPasswordCheckbox() {
        showPinText.setOnClickListener(v -> {
            if (showPasswordChecked) {
                showPasswordCheck.setChecked(true);
                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                showPasswordChecked = false;
            } else {
                showPasswordCheck.setChecked(false);
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                showPasswordChecked = true;
            }
        });

        showPasswordCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showPasswordChecked = false;
                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                showPasswordChecked = true;
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });
    }

    private void initializeBuildDetails(View view) {
        TextView buildDetailsTextView = view.findViewById(org.smartregister.R.id.login_build_text_view);
        try {
            buildDetailsTextView.setText(String.format(getString(R.string.app_version), Utils.getVersion(ChwApplication.getInstance()
                    .getApplicationContext()), Utils.getBuildDate(true)));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_login_btn:
                attemptLogin();
                break;
            case R.id.forgot_pin:
            case R.id.use_your_password:
                revertToPassword();
                break;
            default:
                break;
        }
    }

    private void revertToPassword() {
        getController().getPinLogger().resetPinLogin();
        getController().startPasswordLogin();
    }

    private void attemptLogin() {
        String pin = passwordEditText.getText().toString();
        mLoginPresenter.localLogin(pin);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!(getActivity() instanceof PinViewContract.Controller))
            throw new IllegalStateException("Host activity does not implement Controller");
    }

    private PinViewContract.Controller getController() {
        return (PinLoginActivity) getActivity();
    }

    private void showProgress(final boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onLoginInitiated(@NonNull PinLogger.EventListener eventListener) {
        hideKeyboard();
        btnLogin.setClickable(false);
        showProgress(true);
    }

    @Override
    public void onLoginAttemptFailed(String error) {
        showErrorDialog(org.smartregister.R.string.login_failed_dialog_title, error);
        showProgress(false);
        btnLogin.setClickable(true);
    }

    @Override
    public void onLoginCompleted() {
        showProgress(false);
        btnLogin.setClickable(true);
        getController().startHomeActivity();
    }

    private void hideKeyboard() {
        try {
            Timber.i("Hiding Keyboard %s", DateTime.now().toString());
            if (getActivity() != null)
                Utils.hideKeyboard(getActivity());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void showErrorDialog(@StringRes int title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();
        alertDialog.show();
    }
}
