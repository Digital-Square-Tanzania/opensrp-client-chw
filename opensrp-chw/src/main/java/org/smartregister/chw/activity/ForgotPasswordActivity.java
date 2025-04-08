package org.smartregister.chw.activity;

import org.smartregister.chw.core.activity.CoreForgotPasswordActivity;

public class ForgotPasswordActivity extends CoreForgotPasswordActivity {
    @Override
    public String getFormattedUrl() {
        return "http://170.187.199.69:3035/api/v1/user/chw/forgot/";
    }
}
