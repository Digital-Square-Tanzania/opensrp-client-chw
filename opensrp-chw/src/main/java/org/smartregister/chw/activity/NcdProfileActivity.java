package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.ncd.activity.BaseNcdProfileActivity;
import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import timber.log.Timber;

/**
 * Concrete implementation of BaseNcdProfileActivity.
 * Register this activity in AndroidManifest.xml and use it to launch NCD profile screens.
 */
public class NcdProfileActivity extends BaseNcdProfileActivity {

    /**
     * Use this method to start the NcdProfileActivity.
     * @param activity The calling activity
     * @param baseEntityId The base entity id to pass
     */
    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, NcdProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Any additional setup can be done here if needed
    }

    @Override
    public void startServiceForm() {

    }

    @Override
    public void continueService() {

    }

    @Override
    public void continueDischarge() {

    }

    @Override
    public void openFollowupVisit() {
        // Implement the check here to see if the client has been confirmed with hypertension or diabetes
        try {
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(
                    NcdProfileActivity.this,
                    org.smartregister.chw.util.Constants.JsonForm.getDiabetesFollowupForm()
            );
            if (formJsonObject != null) {
                formJsonObject.put("entity_id", memberObject.getBaseEntityId());
                startFormActivity(formJsonObject);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void startFormActivity(JSONObject jsonForm) {

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());


        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
