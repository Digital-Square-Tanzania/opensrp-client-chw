package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.COUNT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.MultiSelectUtils.PROPERTY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.STEP1;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.pmtct.util.PmtctUtil.saveFormEvent;
import static org.smartregister.client.utils.constants.JsonFormConstants.JSON_FORM_KEY.GLOBAL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.koin.core.Koin;
import org.smartregister.AllConstants;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.BaseReferralTaskViewActivity;
import org.smartregister.chw.core.task.RunnableTask;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.ReferralDao;
import org.smartregister.chw.hps.util.HpsJsonFormUtils;
import org.smartregister.chw.presenter.LTFURecordFeedbackPresenter;
import org.smartregister.chw.referral.contract.BaseIssueReferralContract;
import org.smartregister.chw.referral.interactor.BaseIssueReferralInteractor;
import org.smartregister.chw.referral.model.BaseIssueReferralModel;
import org.smartregister.chw.referral.util.DBConstants;
import org.smartregister.chw.repository.ChwLocationRepository;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;
import org.smartregister.domain.Task;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.LocationRepository;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.sql.Date;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class LTFUReferralsDetailsViewActivity extends BaseReferralTaskViewActivity implements View.OnClickListener, BaseIssueReferralContract.View {

    private static CommonPersonObjectClient commonPersonObjectClient;
    private static String baseEntityId;
    private static String locationId;
    private static String taskId;
    public String reasonReference;
    private LTFURecordFeedbackPresenter presenter;

    public static void startLTFUReferralsDetailsViewActivity(Activity activity, CommonPersonObjectClient personObjectClient, Task task, String startingActivity) {
        LTFUReferralsDetailsViewActivity.personObjectClient = personObjectClient;
        Intent intent = new Intent(activity, LTFUReferralsDetailsViewActivity.class);
        intent.putExtra(CoreConstants.INTENT_KEY.USERS_TASKS, task);
        intent.putExtra(CoreConstants.INTENT_KEY.CHILD_COMMON_PERSON, personObjectClient);
        intent.putExtra(CoreConstants.INTENT_KEY.STARTING_ACTIVITY, startingActivity);
        commonPersonObjectClient = personObjectClient;
        baseEntityId = Utils.getValue(personObjectClient.getColumnmaps(), CoreConstants.DB_CONSTANTS.BASE_ENTITY_ID, false);
        locationId = Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.Key.REFERRAL_HF, false);
        taskId = task.getIdentifier();
        passToolbarTitle(activity, intent);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.referrals_tasks_view_layout);
        ((TextView) findViewById(R.id.last_visit_date_label)).setText(R.string.last_appointment_date);
        if (getIntent().getExtras() != null) {
            extraClientTask();
            extraDetails();
            setStartingActivity((String) getIntent().getSerializableExtra(CoreConstants.INTENT_KEY.STARTING_ACTIVITY));
            inflateToolbar();
            setUpViews();
        }

        String streetName = org.smartregister.chw.core.utils.Utils.getValue(this.getPersonObjectClient().getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.VILLAGE_TOWN, true);
        if (StringUtils.isNotBlank(streetName)) {
            findViewById(R.id.street_name_layout).setVisibility(View.VISIBLE);
            TextView streetNameTv = findViewById(R.id.street_name);
            streetNameTv.setText(streetName);
        }

        String mapCue = org.smartregister.chw.core.utils.Utils.getValue(this.getPersonObjectClient().getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.LANDMARK, true);
        if (StringUtils.isNotBlank(mapCue)) {
            findViewById(R.id.map_cue_layout).setVisibility(View.VISIBLE);
            TextView mapCueTv = findViewById(R.id.map_cue);
            mapCueTv.setText(mapCue);
        }

        String primaryCareGiver = org.smartregister.chw.core.utils.Utils.getValue(this.getPersonObjectClient().getColumnmaps(), "primary_caregiver_name", true);
        if (StringUtils.isNotBlank(primaryCareGiver)) {
            findViewById(R.id.care_giver_name_layout).setVisibility(View.VISIBLE);

            // Retrieve the formatted string resource and substitute the parameter
            String formattedString = getString(R.string.treatment_supporter_prefix, primaryCareGiver);

            // Convert HTML to styled text. Using HtmlCompat for backward compatibility.
            careGiverName.setText(
                    HtmlCompat.fromHtml(formattedString, HtmlCompat.FROM_HTML_MODE_LEGACY)
            );
        }
        String primaryCareGiverPhoneNumber = org.smartregister.chw.core.utils.Utils.getValue(this.getPersonObjectClient().getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.OTHER_PHONE_NUMBER, true);
        if (StringUtils.isNotBlank(primaryCareGiverPhoneNumber)) {
            findViewById(R.id.care_giver_phone_layout).setVisibility(View.VISIBLE);
            careGiverPhone.setText(primaryCareGiverPhoneNumber);
        }

        String clientPhoneNumber = org.smartregister.chw.core.utils.Utils.getValue(this.getPersonObjectClient().getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.PHONE_NUMBER, true);
        if (StringUtils.isNotBlank(clientPhoneNumber)) {
            findViewById(R.id.client_phone_layout).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.client_phone)).setText(clientPhoneNumber);
        }

        this.setFamilyHeadName((String) this.getIntent().getSerializableExtra("family_head_name"));

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
            Runnable runnable = () -> {
                try {
//                    JSONObject json = new JSONObject(data.getStringExtra(org.smartregister.chw.hps.util.Constants.JSON_FORM_EXTRA.JSON));
//                    JSONArray fields = json.getJSONObject(STEP1).getJSONArray(FIELDS);
////                    JSONObject chw_referral_hf = JsonFormUtils.getFieldJSONObject(fields, "chw_referral_hf");
////
////                    try {
////                        if (chw_referral_hf.has(VALUE)) {
////                            String mValue = chw_referral_hf.getString(VALUE);
////                            JSONArray value = new JSONArray(mValue);
////                            String stringValue = value.getJSONObject(0).getJSONObject(PROPERTY).getString("confirmed-id");
////                            chw_referral_hf.put(VALUE, stringValue);
////                        }
////                    } catch (Exception e) {
////                        Timber.e(e);
////                    }

                    saveFormEvent(data.getStringExtra(org.smartregister.chw.hps.util.Constants.JSON_FORM_EXTRA.JSON));
                    ((LTFURecordFeedbackPresenter) presenter()).saveCloseReferralEvent();
                    ((LTFURecordFeedbackPresenter) presenter()).completeTask();
                } catch (Exception e) {
                    Timber.e(e);
                }
            };
            org.smartregister.chw.util.Utils.startAsyncTask(new RunnableTask(runnable), null);
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
        reasonReference = Utils.getValue(commonPersonObjectClient.getColumnmaps(), "reason_reference", false);
        Location location = locationRepository.getLocationById(locationId);
        if (location != null) {
            chwDetailsNames.setText(location.getProperties().getName());
        } else {
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
        findViewById(R.id.referral_details_layout).setVisibility(View.VISIBLE);
        clientReferralProblem.setText(getReferralClinic(Utils.getValue(commonPersonObjectClient.getColumnmaps(), "REFERRAL_CLINIC", false), this));
    }

    private String getReferralClinic(String key, Context context) {
        switch (key.toLowerCase()) {
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

            JSONObject jsonForm = null;
            try {
                jsonForm = getFormAsJson("ltfu_community_followup_feedback", baseEntityId, org.smartregister.Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID));
            } catch (Exception e) {
                Timber.e(e);
            }

            try {
                JSONObject global = jsonForm.getJSONObject(GLOBAL);
                String referralType = Utils.getValue(commonPersonObjectClient.getColumnmaps(), "REFERRAL_CLINIC", false);
                global.put("clinic", referralType);
            } catch (Exception e) {
                Timber.e(e);
            }

            String ctcRecGuid = ReferralDao.getRecGuid(getTask().getReasonReference());
            if (StringUtils.isNotBlank(ctcRecGuid)) {
                try {
                    JSONArray fields = jsonForm.getJSONObject(STEP1).getJSONArray(FIELDS);

                    JSONObject recGuid = JsonFormUtils.getFieldJSONObject(fields, "rec_guid");
                    recGuid.put(VALUE, ctcRecGuid);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }

            ChwLocationRepository locationRepository = new ChwLocationRepository();
            List<Location> locations = locationRepository.getAllLocationsWithTags();
            if (locations != null && jsonForm != null) {
                try {
                    JSONArray fields = jsonForm.getJSONObject(STEP1).getJSONArray(FIELDS);
                    JSONObject referralHealthFacilities = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "chw_referral_hf");

                    JSONArray options = referralHealthFacilities.getJSONArray("options");
                    String healthFacilityTagName = "Facility";
                    for (Location location : locations) {
                        Set<LocationTag> locationTags = location.getLocationTags();
                        if (locationTags.iterator().next().getName().equalsIgnoreCase(healthFacilityTagName)) {
                            JSONObject optionNode = new JSONObject();
                            optionNode.put("text", StringUtils.capitalize(location.getProperties().getName()));
                            optionNode.put("key", StringUtils.capitalize(location.getProperties().getName()));

                            optionNode.put("openmrs_entity", "concept");
                            optionNode.put("openmrs_entity_id", location.getProperties().getUid());

                            JSONObject propertyObject = new JSONObject();
                            propertyObject.put("presumed-id", location.getProperties().getUid());
                            propertyObject.put("confirmed-id", location.getProperties().getUid());
                            optionNode.put("property", propertyObject);
                            options.put(optionNode);
                        }
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
            }

            Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
            intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

            Form form = new Form();
            form.setDatePickerDisplayFormat("dd-MM-yyyy");
            form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
            form.setName(getString(R.string.record_followup_feedback));
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);

            try {
                form.setWizard(jsonForm.getInt(COUNT) > 1);
            } catch (JSONException e) {
                Timber.e(e);
                form.setWizard(false);
            }
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

            startActivityForResult(intent, org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON);
        }

    }

    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception {
        JSONObject jsonObject = HpsJsonFormUtils.getFormAsJson(formName);
        HpsJsonFormUtils.getRegistrationForm(jsonObject, entityId, currentLocationId);

        return jsonObject;
    }

    @NonNull
    @Override
    public BaseIssueReferralContract.Presenter presenter() {
        return (BaseIssueReferralContract.Presenter) new LTFURecordFeedbackPresenter(baseEntityId, taskId, locationId, this,
                BaseIssueReferralModel.class, (BaseIssueReferralContract.Interactor) new BaseIssueReferralInteractor());
    }

    @Override
    public void setProfileViewWithData() {

    }

    @NonNull
    @Override
    public Koin getKoin() {
        return null;
    }
}