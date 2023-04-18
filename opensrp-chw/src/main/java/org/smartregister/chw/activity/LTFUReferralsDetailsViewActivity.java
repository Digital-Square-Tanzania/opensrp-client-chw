package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.BaseReferralTaskViewActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.dao.ReferralDao;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.LocationRepository;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.sql.Date;

public class LTFUReferralsDetailsViewActivity extends BaseReferralTaskViewActivity implements View.OnClickListener {

    private static CommonPersonObjectClient commonPersonObjectClient;
    private static String baseEntityId;
    private static String locationId;
    private static String taskId;

    public static void startLTFUReferralsDetailsViewActivity(Activity activity, CommonPersonObjectClient personObjectClient, Task task, String startingActivity) {
        LTFUReferralsDetailsViewActivity.personObjectClient = personObjectClient;
        Intent intent = new Intent(activity, LTFUReferralsDetailsViewActivity.class);
        intent.putExtra(CoreConstants.INTENT_KEY.USERS_TASKS, task);
        intent.putExtra(CoreConstants.INTENT_KEY.CHILD_COMMON_PERSON, personObjectClient);
        intent.putExtra(CoreConstants.INTENT_KEY.STARTING_ACTIVITY, startingActivity);
        commonPersonObjectClient = personObjectClient;
        baseEntityId = Utils.getValue(personObjectClient.getColumnmaps(), CoreConstants.DB_CONSTANTS.BASE_ENTITY_ID, false);
        locationId = Utils.getValue(commonPersonObjectClient.getColumnmaps(), org.smartregister.chw.referral.util.DBConstants.Key.REFERRAL_HF, false);
        taskId = task.getIdentifier();
        passToolbarTitle(activity, intent);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.referrals_tasks_view_layout);
        if (getIntent().getExtras() != null) {
            extraClientTask();
            extraDetails();
            setStartingActivity((String) getIntent().getSerializableExtra(CoreConstants.INTENT_KEY.STARTING_ACTIVITY));
            inflateToolbar();
            setUpViews();
        }
    }

    @Override
    protected void onCreation() {
        //overridden
    }

    @Override
    protected void onResumption() {
        //Overridden
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON) {
            finish();
        }
    }

    public void setUpViews() {
        clientName = findViewById(R.id.client_name);
        careGiverName = findViewById(R.id.care_giver_name);
        childName = findViewById(R.id.child_name);
        careGiverPhone = findViewById(R.id.care_giver_phone);
        clientReferralProblem = findViewById(R.id.client_referral_problem);
        chwDetailsNames = findViewById(R.id.chw_details_names);
        referralDate = findViewById(R.id.referral_date);

        womanGaLayout = findViewById(R.id.woman_ga_layout);
        careGiverLayout = findViewById(R.id.care_giver_name_layout);
        childNameLayout = findViewById(R.id.child_name_layout);

        womanGa = findViewById(R.id.woman_ga);

        CustomFontTextView recordFeedbackBtn = findViewById(R.id.record_feedback);
        recordFeedbackBtn.setOnClickListener(this);

        LinearLayout lastAppointmentLayout = findViewById(R.id.last_visit_date_layout);
        CustomFontTextView tvLastAppointmentDate = findViewById(R.id.last_visit_date);

        getReferralDetails();
        LocationRepository locationRepository = new LocationRepository();
        String reasonReference = Utils.getValue(commonPersonObjectClient.getColumnmaps(), "reason_reference", false);
        Location location = locationRepository.getLocationById(locationId);
        if(location!= null){
        chwDetailsNames.setText(location.getProperties().getName());
        }else{
            chwDetailsNames.setText(locationId);
        }
        Date lastAppointmentDate = ReferralDao.getLastAppointmentDate(reasonReference);
        if (lastAppointmentDate != null) {
            lastAppointmentLayout.setVisibility(View.VISIBLE);
            tvLastAppointmentDate.setText(org.smartregister.chw.core.utils.Utils.dd_MMM_yyyy.format(lastAppointmentDate));
        }
    }

    public void setStartingActivity(String startingActivity) {
        this.startingActivity = startingActivity;
    }

    @Override
    protected void updateProblemDisplay() {
        clientReferralProblem.setText(getReferralClinic(Utils.getValue(commonPersonObjectClient.getColumnmaps(), "REFERRAL_CLINIC", false),this));
    }

    private String getReferralClinic(String key, Context context){
        switch (key.toLowerCase()){
            case "ctc":
                return context.getString(R.string.ltfu_clinic_ctc);
            case "pwid":
                return context.getString(R.string.ltfu_clinic_pwid);
            case "prep":
                return context.getString(R.string.ltfu_clinic_prep);
            case "pmtct":
                return context.getString(R.string.ltfu_clinic_pmtct);
            case "tb":
                return context.getString(R.string.ltfu_clinic_tb);
            default:
                return key.toUpperCase();
        }
    }

    public String getBaseEntityId() {
        return baseEntityId;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.record_feedback) {
            JSONObject formJSONObject = FormUtils.getFormUtils().getFormJson("ltfu_community_followup_feedback");
            try {
                formJSONObject.put(Constants.REFERRAL_TASK_FOCUS, "LTFU Community Followup Feedback");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            LTFURecordFeedbackActivity.startFeedbackFormActivityForResults(this, baseEntityId, formJSONObject, false, locationId, taskId);
        }

    }
}