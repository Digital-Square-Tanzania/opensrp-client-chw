package org.smartregister.chw.util;

import android.app.Activity;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.activity.ReferralRegistrationActivity;
import org.smartregister.chw.core.utils.CoreConstants;

public class HivstUtils {

    public static void startHIVSTReferral(Activity context, String baseEntityId) {
        JSONObject formJsonObject;
        try {
            formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(context, org.smartregister.chw.hivst.util.Constants.FORMS.HIVST_REFERRAL_FORM);
            if (formJsonObject != null) {
                formJsonObject.put(Constants.REFERRAL_TASK_FOCUS, CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST);
                ReferralRegistrationActivity.startGeneralReferralFormActivityForResults(context, baseEntityId, formJsonObject, false, false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
