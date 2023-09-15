package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.core.activity.CoreKvpRegisterActivity;
import org.smartregister.chw.fragment.KvpPrEPRegisterFragment;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class KvpPrEPRegisterActivity extends CoreKvpRegisterActivity {
    public static void startRegistration(Activity activity, String memberBaseEntityID, String gender, int age) {
        Intent intent = new Intent(activity, KvpPrEPRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.KVP_FORM_NAME, Constants.FORMS.KVP_PrEP_REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.AGE, age);

        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new KvpPrEPRegisterFragment();
    }
}
