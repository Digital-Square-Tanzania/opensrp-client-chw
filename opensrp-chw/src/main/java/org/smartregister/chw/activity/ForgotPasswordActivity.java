package org.smartregister.chw.activity;

import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.core.activity.CoreForgotPasswordActivity;

public class ForgotPasswordActivity extends CoreForgotPasswordActivity {
    @Override
    public String getFormattedUrl() {
        return BuildConfig.DEBUG ? BuildConfig.reset_password_url_debug : BuildConfig.reset_password_url;
    }
}
