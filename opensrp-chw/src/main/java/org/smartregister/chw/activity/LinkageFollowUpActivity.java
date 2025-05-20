package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.JSON_FORM_KEY.JSON;
import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getLinkageFollowUpForm;
import static org.smartregister.chw.ld.util.LDJsonFormUtils.validateParameters;
import static org.smartregister.chw.util.JsonFormUtils.ENCOUNTER_TYPE;
import static org.smartregister.chw.util.JsonFormUtils.REQUEST_CODE_GET_JSON;
import static org.smartregister.util.JsonFormUtils.VALUE;
import static org.smartregister.util.JsonFormUtils.getFieldJSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.domain.Task;
import org.smartregister.repository.TaskRepository;

import timber.log.Timber;

/**
 * Author issyzac on 09/05/2024
 */
public class LinkageFollowUpActivity extends MalariaRegisterActivity {

    protected String BASE_ENTITY_ID;
    protected String FAMILY_BASE_ENTITY_ID;
    protected String ACTION;

    public static final String TASK_ID = "taskId";
    private String taskId = "";

    public static void startAddoLinkageRegisterActivity(Activity activity, String baseEntityId, String taskId){
        Intent intent = new Intent(activity, LinkageFollowUpActivity.class);
        intent.putExtra(org.smartregister.chw.referral.util.Constants.ActivityPayload.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.MALARIA_FORM_NAME, getLinkageFollowUpForm());
        intent.putExtra(org.smartregister.chw.referral.util.Constants.ActivityPayload.ACTION, org.smartregister.chw.referral.util.Constants.ActivityPayloadType.FOLLOW_UP_VISIT);
        intent.putExtra(TASK_ID, taskId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        taskId = getIntent().getStringExtra(TASK_ID);

    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        this.startActivityForResult(FormUtils.getStartFormActivity(jsonForm, this.getString(R.string.follow_up_visit), this), REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onActivityResultExtended(int requestCode, int resultCode, Intent data) {
        super.onActivityResultExtended(requestCode, resultCode, data);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_GET_JSON) {
            String jsonString = data.getStringExtra(JSON);
            try {
                assert jsonString != null;
                JSONObject form = new JSONObject(jsonString);
                Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(form.toString());
                JSONObject jsonForm = registrationFormParams.getMiddle();
                String encounter_type = jsonForm.optString(ENCOUNTER_TYPE);

                if ("Linkage Followup".equals(encounter_type)) {
                    completeReferralTask(jsonString);
                }

            } catch (JSONException e) {
                Timber.e(e);
            }

            super.onActivityResult(requestCode, resultCode, data);
            finish();

        }
        finish();

    }


    protected void completeReferralTask(String jsonString) {
        try {
            JSONObject form = new JSONObject(jsonString);
            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(form.toString());
            JSONObject jsonForm = registrationFormParams.getMiddle();
            String encounter_type = jsonForm.optString(ENCOUNTER_TYPE);

            if (Constants.EncounterType.LINKAGE_FOLLOWUP.equals(encounter_type)) {
                JSONArray fields = registrationFormParams.getRight();

                // update task
                TaskRepository taskRepository = ChwApplication.getInstance().getTaskRepository();
                Task task = taskRepository.getTaskByIdentifier(getTaskIdentifier());

                //Update the task status
                task.setBusinessStatus(org.smartregister.chw.referral.util.Constants.BusinessStatus.COMPLETE);
                task.setStatus(Task.TaskStatus.COMPLETED);
                taskRepository.addOrUpdate(task);

            }

            finish();

        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    public String getTaskIdentifier() {
        return taskId;
    }

    @Override
    public void finish() {
        Intent intent = new Intent(this, AddoLinkageRegisterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        super.finish();
    }
}
