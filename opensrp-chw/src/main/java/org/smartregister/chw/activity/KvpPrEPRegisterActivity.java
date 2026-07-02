package org.smartregister.chw.activity;

import static org.smartregister.chw.util.Constants.REQUEST_FILTERS;

import android.app.Activity;
import android.content.Intent;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreKvpRegisterActivity;
import org.smartregister.chw.fragment.KvpPrEPRegisterFragment;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        normalizeHivStatusPayload(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_FILTERS) {
            ((KvpPrEPRegisterFragment) mBaseFragment).onFiltersUpdated(requestCode, data);
        }
    }

    private void normalizeHivStatusPayload(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || requestCode != JsonFormUtils.REQUEST_CODE_GET_JSON || data == null) {
            return;
        }

        String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
        if (StringUtils.isBlank(jsonString)) {
            return;
        }

        try {
            JSONObject form = new JSONObject(jsonString);
            JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
            String hivStatus = getFieldValue(fields, "hivStatus");
            boolean hivPositive = StringUtils.equalsIgnoreCase(hivStatus, "positive");

            setFieldValue(fields, "hivPositive", String.valueOf(hivPositive));
            if (!hivPositive) {
                setFieldValue(fields, "ctcNumber", "");
            }

            data.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, form.toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    private String getFieldValue(JSONArray fields, String key) {
        JSONObject field = getField(fields, key);
        return field != null ? field.optString("value") : null;
    }

    private void setFieldValue(JSONArray fields, String key, String value) throws JSONException {
        JSONObject field = getField(fields, key);
        if (field != null) {
            field.put("value", value);
        }
    }

    private JSONObject getField(JSONArray fields, String key) {
        if (fields == null || StringUtils.isBlank(key)) {
            return null;
        }

        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && StringUtils.equals(key, field.optString("key"))) {
                return field;
            }
        }

        return null;
    }
}
