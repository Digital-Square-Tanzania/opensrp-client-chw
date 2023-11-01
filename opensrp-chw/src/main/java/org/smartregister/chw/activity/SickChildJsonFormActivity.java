package org.smartregister.chw.activity;

import static org.smartregister.chw.util.JsonFormUtils.ENCOUNTER_TYPE;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vijay.jsonwizard.activities.JsonWizardFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.rules.ChwRulesEngineFactory;

import java.util.HashMap;

import timber.log.Timber;

public class SickChildJsonFormActivity extends JsonWizardFormActivity {

    public void init(String json) {
        try {
            setmJSONObject(new JSONObject(json));
            if (!getmJSONObject().has(ENCOUNTER_TYPE)) {
                setmJSONObject(new JSONObject());
                throw new JSONException("Form encounter_type not set");
            }

            // Populate global vars
            if (getmJSONObject().has(JsonFormConstants.JSON_FORM_KEY.GLOBAL)) {
                globalValues = new Gson()
                        .fromJson(getmJSONObject().getJSONObject(JsonFormConstants.JSON_FORM_KEY.GLOBAL).toString(),
                                new TypeToken<HashMap<String, String>>() {
                                }.getType());
            } else {
                globalValues = new HashMap<>();
            }

            rulesEngineFactory = new ChwRulesEngineFactory(this, globalValues);
            setRulesEngineFactory(rulesEngineFactory);

            confirmCloseTitle = getString(com.vijay.jsonwizard.R.string.confirm_form_close);
            confirmCloseMessage = getString(com.vijay.jsonwizard.R.string.confirm_form_close_explanation);
            localBroadcastManager = LocalBroadcastManager.getInstance(this);

        } catch (JSONException e) {
            Timber.e(e, "Initialization error. Json passed is invalid : ");
        }
    }
}
