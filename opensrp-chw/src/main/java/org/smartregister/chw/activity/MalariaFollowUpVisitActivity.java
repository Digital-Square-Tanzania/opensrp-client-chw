package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getMalariaFollowUpVisitForm;

import android.app.Activity;
import android.content.Intent;

public class MalariaFollowUpVisitActivity extends MalariaRegisterActivity {

    public static void startMalariaFollowUpActivity(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, MalariaRegisterActivity.class);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.MALARIA_FORM_NAME, getMalariaFollowUpVisitForm());
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.ACTION, org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD_TYPE.FOLLOW_UP_VISIT);
        activity.startActivity(intent);
    }

}