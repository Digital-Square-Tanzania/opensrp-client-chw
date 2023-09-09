package org.smartregister.chw.activity;

import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreHivProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.task.RunnableTask;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.custom_view.HivFloatingMenu;
import org.smartregister.chw.dao.ChwCBHSDao;
import org.smartregister.chw.hiv.activity.BaseHivFormsActivity;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.hiv.util.Constants;
import org.smartregister.chw.hiv.util.DBConstants;
import org.smartregister.chw.hiv.util.HivUtil;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.interactor.CbhsProfileInteractor;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.pmtct.PmtctLibrary;
import org.smartregister.chw.pmtct.domain.Visit;
import org.smartregister.chw.presenter.HivProfilePresenter;
import org.smartregister.chw.referral.domain.NeatFormMetaData;
import org.smartregister.chw.referral.domain.NeatFormOption;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.util.CbhsUtils;
import org.smartregister.chw.util.UtilsFlv;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.AlertStatus;
import org.smartregister.domain.Location;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.LocationRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

public class HivProfileActivity extends CoreHivProfileActivity implements FamilyProfileExtendedContract.PresenterCallBack, OnRetrieveNotifications {

    public static final String UPDATE_HIV_REGISTRATION = "Update CBHS Registration";
    public static final String ENCOUNTER_TYPE = "encounter_type";
    public static final String NAME = "name";
    public static final String PROPERTIES = "properties";
    public static final String TEXT = "text";
    public static final String SELECTION = "selection";
    private static final String FOLLOWUP_STATUS_DECEASED = "deceased";
    private static final String FOLLOWUP_STATUS_QUALIFIED_FROM_SERVICE = "completed_and_qualified_from_the_services";
    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private final NotificationListAdapter notificationListAdapter = new NotificationListAdapter();
    private Flavor flavor = new HivProfileActivityFlv();

    public static void startHivProfileActivity(Activity activity, HivMemberObject memberObject) {
        Intent intent = new Intent(activity, HivProfileActivity.class);
        intent.putExtra(Constants.ActivityPayload.HIV_MEMBER_OBJECT, memberObject);
        activity.startActivity(intent);
    }

    public static void startHivFollowupActivity(Activity activity, String baseEntityID) throws JSONException {
        Intent intent = new Intent(activity, BaseHivFormsActivity.class);
        intent.putExtra(Constants.ActivityPayload.BASE_ENTITY_ID, baseEntityID);

        HivMemberObject hivMemberObject = HivDao.getMember(baseEntityID);
        JSONObject formJsonObject;
        formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(activity, org.smartregister.chw.util.Constants.CBHSJsonForms.getCbhsFollowupForm());

        JSONArray steps = null;
        if (formJsonObject != null) {
            steps = formJsonObject.getJSONArray("steps");
        }
        JSONObject step = null;
        if (steps != null) {
            step = steps.getJSONObject(0);
        }
        JSONArray fields = null;
        if (step != null) {
            fields = step.getJSONArray("fields");
        }

        if (fields != null && hivMemberObject != null) {

            if (ChwCBHSDao.hasFollowupVisits(hivMemberObject.getBaseEntityId())) { //Removing the New Client option as followup status for clients with previous followup visits
                JSONObject registrationOrFollowupStatus = getJsonObject(fields, "registration_or_followup_status");
                if (registrationOrFollowupStatus != null) {
                    removeField(registrationOrFollowupStatus.getJSONArray("options"), "new_client");
                }
            } else { //Removing options for completed referrals to other services completed for first visit
                for (int i = fields.length() - 1; i >= 0; i--) {
                    JSONObject jsonObject = fields.getJSONObject(i);
                    if (jsonObject != null && jsonObject.has("name") && jsonObject.getString("name").equalsIgnoreCase("referrals_to_other_services_completed")) {
                        fields.remove(i);
                    }
                }
            }


            if (StringUtils.isNotBlank(hivMemberObject.getCtcNumber())) {
                removeField(fields, "client_hiv_status_after_testing");
                removeField(fields, "was_the_client_tested_for_hiv");
                JSONObject stateOfHivCareAndTreatment = getJsonObject(fields, "state_of_hiv_care_and_treatment");
                removeField(stateOfHivCareAndTreatment.getJSONArray("options"), "na");
                removeField(stateOfHivCareAndTreatment.getJSONArray("options"), "not_registered_in_ctc_clinic");
            }

            String clientHivStatus;
            if (StringUtils.isNotBlank(hivMemberObject.getClientHivStatusAfterTesting())) {
                clientHivStatus = hivMemberObject.getClientHivStatusAfterTesting();
            } else {
                clientHivStatus = hivMemberObject.getClientHivStatusDuringRegistration();
            }
            if (clientHivStatus != null && (clientHivStatus.equalsIgnoreCase("negative") || clientHivStatus.equalsIgnoreCase("unknown"))) {
                removeField(fields, "state_of_hiv_care_and_treatment");
            }

            int age = org.smartregister.chw.util.Utils.getAgeFromDate(hivMemberObject.getAge());

            JSONObject referralsIssuedToOtherServices = getJsonObject(fields, "referrals_issued_to_other_services");
            JSONObject completedReferralsToOtherServices = getJsonObject(fields, "referrals_to_other_services_completed");

            if (age < 15) { //Removing condoms and HIV self testing kits as supplies for children below 15 years
                JSONObject supplies = getJsonObject(fields, "supplies_provided");
                if (supplies != null) {
                    removeField(supplies.getJSONArray("options"), "hiv_self_test_kits");
                    removeField(supplies.getJSONArray("options"), "condoms");
                }
            }

            if (age < 50) { //Removing Elderly service for clients below 50 years
                if (referralsIssuedToOtherServices != null)
                    removeField(referralsIssuedToOtherServices.getJSONArray("options"), "elderly_centers");
                if (completedReferralsToOtherServices != null)
                    removeField(completedReferralsToOtherServices.getJSONArray("options"), "elderly_centers");
            }

            if (age > 18) {  //Removing OVC (Orphans and Vulnerable Children) as referral services for clients above 18 years
                if (referralsIssuedToOtherServices != null)
                    removeField(referralsIssuedToOtherServices.getJSONArray("options"), "ovc_services");

                if (completedReferralsToOtherServices != null)
                    removeField(completedReferralsToOtherServices.getJSONArray("options"), "ovc_services");
            }


            if (ChwCBHSDao.tbStatusAfterTestingDone(baseEntityID)) {
                removeField(fields, "was_the_client_tested_for_tb");
                removeField(fields, "client_tb_status_after_testing");
            }

            if (StringUtils.isNotBlank(hivMemberObject.getTbNumber())) {
                JSONObject stateOfRegistrationInTbAndPwidClinics = getJsonObject(fields, "state_of_registration_in_tb_and_pwid_clinics");
                removeField(stateOfRegistrationInTbAndPwidClinics.getJSONArray("options"), "na");

                //Setting Registered in TB Clinic to Checked by default
                JSONObject registeredInTbClinic = getJsonObject(stateOfRegistrationInTbAndPwidClinics.getJSONArray("options"), "registered_in_tb_clinic");
                JSONObject properties = new JSONObject();
                properties.put("checked", true);
                registeredInTbClinic.put("properties", properties);
            }

        }

        intent.putExtra(Constants.ActivityPayload.JSON_FORM, initializeHealthFacilitiesList(formJsonObject).toString());
        intent.putExtra(Constants.ActivityPayload.ACTION, Constants.ActivityPayloadType.FOLLOW_UP_VISIT);
        intent.putExtra(Constants.ActivityPayload.USE_DEFAULT_NEAT_FORM_LAYOUT, false);

        activity.startActivityForResult(intent, CoreConstants.ProfileActivityResults.CHANGE_COMPLETED);
    }

    private static void removeField(JSONArray fields, String fieldName) throws JSONException {
        int position = 0;
        boolean found = false;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field.getString("name").equalsIgnoreCase(fieldName)) {
                position = i;
                found = true;
                break;
            }
        }
        if (found) {
            fields.remove(position);
        }
    }


    private static JSONObject getJsonObject(JSONArray fields, String fieldName) throws JSONException {
        int position = 0;
        boolean found = false;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field.getString("name").equalsIgnoreCase(fieldName)) {
                position = i;
                found = true;
                break;
            }
        }
        if (found) {
            return fields.getJSONObject(position);
        }
        return null;
    }

    private static JSONObject initializeHealthFacilitiesList(JSONObject form) {
        LocationRepository locationRepository = new LocationRepository();
        List<Location> locations = locationRepository.getAllLocations();
        if (locations != null && form != null) {
            try {
                JSONArray fields = form.getJSONArray(JsonFormConstants.STEPS).getJSONObject(0).getJSONArray(JsonFormConstants.FIELDS);
                JSONObject referralHealthFacilities = null;
                for (int i = 0; i < fields.length(); i++) {
                    if (fields.getJSONObject(i).getString(JsonFormConstants.NAME).equals(org.smartregister.chw.util.Constants.JsonFormConstants.CLIENT_MOVED_LOCATION)) {
                        referralHealthFacilities = fields.getJSONObject(i);
                        break;
                    }
                }

                ArrayList<NeatFormOption> healthFacilitiesOptions = new ArrayList<>();

                for (Location location : locations) {
                    NeatFormOption healthFacilityOption = new NeatFormOption();
                    healthFacilityOption.name = location.getProperties().getName();
                    healthFacilityOption.text = location.getProperties().getName();

                    NeatFormMetaData metaData = new NeatFormMetaData();
                    metaData.openmrsEntity = "location_uuid";
                    metaData.openmrsEntityId = location.getProperties().getUid();

                    healthFacilityOption.neatFormMetaData = metaData;
                    healthFacilitiesOptions.add(healthFacilityOption);
                }
                /*
                 * Other Option field
                 */
                NeatFormOption otherFacilityOption = new NeatFormOption();
                otherFacilityOption.text = "Other";
                otherFacilityOption.name = "Other";

                NeatFormMetaData metaData = new NeatFormMetaData();
                metaData.openmrsEntity = "concept";
                metaData.openmrsEntityId = "Other";

                otherFacilityOption.neatFormMetaData = metaData;

                healthFacilitiesOptions.add(otherFacilityOption);


                if (referralHealthFacilities != null) {
                    JSONArray optionsArray = new JSONArray();
                    for (int i = 0; i < referralHealthFacilities.getJSONArray(JsonFormConstants.OPTIONS).length(); i++) {
                        optionsArray.put(referralHealthFacilities.getJSONArray(JsonFormConstants.OPTIONS).get(i));
                    }
                    referralHealthFacilities.put(JsonFormConstants.OPTIONS, (new JSONArray((new Gson()).toJson(healthFacilitiesOptions))));
                }
            } catch (JSONException e) {
                Timber.e(e);
            }

        }
        return form;
    }

    @Override
    public void setProfileViewDetails(@Nullable HivMemberObject hivMemberObject) {
        super.setProfileViewDetails(hivMemberObject);

        if (!getHivMemberObject().getClientFollowupStatus().equals("")) {
            int labelTextColor;
            int background;
            String labelText;

            getTvStatus().setVisibility(View.VISIBLE);
            switch (getHivMemberObject().getClientFollowupStatus()) {
                case FOLLOWUP_STATUS_DECEASED:
                    labelTextColor = context().getColorResource(org.smartregister.chw.opensrp_chw_anc.R.color.high_risk_text_red);
                    background = org.smartregister.chw.opensrp_chw_anc.R.drawable.high_risk_label;
                    labelText = getResources().getString(R.string.client_followup_status_deceased);
                    hideFollowUpVisitButton();
                    break;
                case FOLLOWUP_STATUS_QUALIFIED_FROM_SERVICE:
                    labelTextColor = context().getColorResource(org.smartregister.chw.opensrp_chw_anc.R.color.low_risk_text_green);
                    background = org.smartregister.chw.opensrp_chw_anc.R.drawable.low_risk_label;
                    labelText = getResources().getString(R.string.client_followup_status_qualified_from_service);
                    hideFollowUpVisitButton();
                    break;
                default:
                    labelTextColor = context().getColorResource(org.smartregister.chw.opensrp_chw_anc.R.color.default_risk_text_black);
                    background = org.smartregister.chw.opensrp_chw_anc.R.drawable.risk_label;
                    labelText = "";
                    getTvStatus().setVisibility(View.GONE);
                    break;
            }

            getTvStatus().setText(labelText);
            getTvStatus().setTextColor(labelTextColor);
            getTvStatus().setBackgroundResource(background);
        }

    }

    @Override
    protected void onCreation() {
        super.onCreation();
        addHivReferralTypes();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationAndReferralRecyclerView.setAdapter(notificationListAdapter);
        notificationListAdapter.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(), getHivMemberObject().getBaseEntityId(), this);

        //Refreshing the hiv Member object with new data just in-case it was updated in the background
        setHivMemberObject(HivDao.getMember(getHivMemberObject().getBaseEntityId()));
        onMemberDetailsReloaded(getHivMemberObject());
        setProfileViewDetails(HivDao.getMember(getHivMemberObject().getBaseEntityId()));

        try {
            CbhsUtils.removeDeceasedClients(getHivMemberObject(), getContext());
        } catch (Exception e) {
            Timber.e(e);
        }

        if (ChwCBHSDao.completedServiceOrNoLongerContinuingWithService(getHivMemberObject().getBaseEntityId())) {
            CbhsUtils.createCloseCbhsEvent(getHivMemberObject());
        }
    }

    @Override
    public void setupViews() {
        super.setupViews();

    }

    @Override
    protected void removeMember() {
        IndividualProfileRemoveActivity.startIndividualProfileActivity((Activity) getContext(), getClientDetailsByBaseEntityID(getHivMemberObject().getBaseEntityId()), getHivMemberObject().getFamilyBaseEntityId(), getHivMemberObject().getFamilyHead(), getHivMemberObject().getPrimaryCareGiver(), FpRegisterActivity.class.getCanonicalName());
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        setHivProfilePresenter(new HivProfilePresenter(this, new CbhsProfileInteractor(this), getHivMemberObject()));
        fetchProfileData();
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(getHivMemberObject().getPhoneNumber()) || StringUtils.isNotBlank(getHivMemberObject().getPrimaryCareGiverPhoneNumber()));

        ((HivFloatingMenu) getHivFloatingMenu()).redraw(phoneNumberAvailable);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.record_hiv_followup_visit) {
            openFollowUpVisitForm(false);
        } else if (id == R.id.rlHivRegistrationDate) {
            startHivRegistrationDetailsActivity();
        }
        handleNotificationRowClick(this, view, notificationListAdapter, getHivMemberObject().getBaseEntityId());
    }

    @Override
    public Context getContext() {
        return HivProfileActivity.this;
    }

    @Override
    public void verifyHasPhone() {
        // Implement
    }

    @Override
    public void notifyHasPhone(boolean b) {
        // Implement
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // recompute schedule
        Runnable runnable = () -> ChwScheduleTaskExecutor.getInstance().execute(getHivMemberObject().getBaseEntityId(), Constants.EventType.FOLLOW_UP_VISIT, new Date());
        org.smartregister.chw.util.Utils.startAsyncTask(new RunnableTask(runnable), null);
        try {
            CbhsUtils.removeDeceasedClients(getHivMemberObject(), getContext());
        } catch (Exception e) {
            Timber.e(e);
        }

        if (ChwCBHSDao.completedServiceOrNoLongerContinuingWithService(getHivMemberObject().getBaseEntityId())) {
            CbhsUtils.createCloseCbhsEvent(getHivMemberObject());
        }

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CoreConstants.ProfileActivityResults.CHANGE_COMPLETED && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, HivRegisterActivity.class);
            intent.putExtras(getIntent().getExtras());
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void openMedicalHistory() {
        CbhsMedicalHistoryActivity.startMe(this, getHivMemberObject());
    }

    @Override
    public void openHivRegistrationForm() {
        try {
            String formName = org.smartregister.chw.util.Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");


            int age = org.smartregister.chw.util.Utils.getAgeFromDate(getHivMemberObject().getAge());
            try {
                updateAgeAndGender(fields, age, getHivMemberObject().getGender());
            } catch (Exception e) {
                Timber.e(e);
            }

            HivRegisterActivity.startHIVFormActivity(this, getHivMemberObject().getBaseEntityId(), formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        }

    }

    @Override
    public void openUpcomingServices() {
        CbhsUpcomingServiceActivity.startMe(this, HivUtil.toMember(getHivMemberObject()));
    }

    @Override
    public void setFamilyStatus(@Nullable AlertStatus status) {
        super.setFamilyStatus(status);
        if (getHivMemberObject().getFamilyMemberEntityType().equals(Constants.FamilyMemberEntityType.EC_INDEPENDENT_CLIENT)) {
            findViewById(R.id.rlFamilyServicesDue).setVisibility(View.GONE);
        }
        //TODO fix editing of CBHS registration then remove the line below
        findViewById(R.id.rlHivRegistrationDate).setVisibility(View.GONE);
    }

    @Override
    public void openFamilyDueServices() {
        Intent intent = new Intent(this, FamilyProfileActivity.class);

        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, getHivMemberObject().getFamilyBaseEntityId());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, getHivMemberObject().getFamilyHead());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, getHivMemberObject().getPrimaryCareGiver());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME, getHivMemberObject().getFamilyName());

        intent.putExtra(CoreConstants.INTENT_KEY.SERVICE_DUE, true);
        startActivity(intent);
    }

    @Override
    public void openFollowUpVisitForm(boolean isEdit) {
        if (!isEdit) {
            try {
                startHivFollowupActivity(this, getHivMemberObject().getBaseEntityId());
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

    private void addHivReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {

            //HIV Testing referrals will only be issued to non positive clients
            if (getHivMemberObject().getCtcNumber().isEmpty()) {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.hts_referral), CoreConstants.JSON_FORM.getHtsReferralForm(), CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST));
            } else { //HIV Treatment and care referrals will be issued to HIV Positive clients
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.hiv_referral), CoreConstants.JSON_FORM.getHivReferralForm(), CoreConstants.TASKS_FOCUS.SICK_HIV));
            }

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_referral), CoreConstants.JSON_FORM.getTbReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_TB));

            if (isClientEligibleForAnc(getHivMemberObject())) {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.anc_danger_signs), org.smartregister.chw.util.Constants.JSON_FORM.getAncUnifiedReferralForm(), CoreConstants.TASKS_FOCUS.ANC_DANGER_SIGNS));
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.pnc_referral), CoreConstants.JSON_FORM.getPncUnifiedReferralForm(), CoreConstants.TASKS_FOCUS.PNC_DANGER_SIGNS));
                if (!AncDao.isANCMember(getHivMemberObject().getBaseEntityId())) {
                    referralTypeModels.add(new ReferralTypeModel(getString(R.string.pregnancy_confirmation), CoreConstants.JSON_FORM.getPregnancyConfirmationReferralForm(), CoreConstants.TASKS_FOCUS.PREGNANCY_CONFIRMATION));
                }
            }
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral), CoreConstants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void initializeCallFAB() {
        setHivFloatingMenu(new HivFloatingMenu(this, getHivMemberObject()));

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.hiv_fab:
                    checkPhoneNumberProvided();
                    ((HivFloatingMenu) getHivFloatingMenu()).animateFAB();
                    break;
                case R.id.call_layout:
                    ((HivFloatingMenu) getHivFloatingMenu()).launchCallWidget();
                    ((HivFloatingMenu) getHivFloatingMenu()).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    ((HivProfilePresenter) getHivProfilePresenter()).referToFacility();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((HivFloatingMenu) getHivFloatingMenu()).setFloatMenuClickListener(onClickFloatingMenu);
        getHivFloatingMenu().setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(getHivFloatingMenu(), linearLayoutParams);
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(org.smartregister.chw.core.R.menu.hiv_profile_menu, menu);
        menu.findItem(R.id.action_anc_registration).setVisible(isClientEligibleForAnc(getHivMemberObject()) && !AncDao.isANCMember(getHivMemberObject().getBaseEntityId()));
        menu.findItem(R.id.action_pregnancy_out_come).setVisible(isClientEligibleForAnc(getHivMemberObject()) && !PNCDao.isPNCMember(getHivMemberObject().getBaseEntityId()));
        menu.findItem(R.id.action_location_info).setVisible(UpdateDetailsUtil.isIndependentClient(getHivMemberObject().getBaseEntityId()));
        if (ChwApplication.getApplicationFlavor().hasHIVST()) {
            String dob = getHivMemberObject().getAge();
            int age = Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_hivst_registration).setVisible(!HivstDao.isRegisteredForHivst(getHivMemberObject().getBaseEntityId()) && age >= 15);
        }
        if (ChwApplication.getApplicationFlavor().hasKvp()) {
            menu.findItem(R.id.action_kvp_prep_registration).setVisible(!KvpDao.isRegisteredForKvpPrEP(getHivMemberObject().getBaseEntityId()));
        }
        //   flavor.updateTbMenuItems(getHivMemberObject().getBaseEntityId(), menu);
        if (ChwApplication.getApplicationFlavor().hasMalaria())
            UtilsFlv.updateMalariaMenuItems(getHivMemberObject().getBaseEntityId(), menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == org.smartregister.chw.core.R.id.action_tb_registration) {
            startTbRegister();
            return true;
        }
        if (itemId == R.id.action_anc_registration) {
            startAncRegister();
            return true;
        } else if (itemId == R.id.action_pregnancy_out_come) {
            PncRegisterActivity.startPncRegistrationActivity((Activity) getContext(), getHivMemberObject().getBaseEntityId(), null, CoreConstants.JSON_FORM.getPregnancyOutcome(), AncLibrary.getInstance().getUniqueIdRepository().getNextUniqueId().getOpenmrsId(), getHivMemberObject().getFamilyBaseEntityId(), getHivMemberObject().getFamilyName(), null);
            return true;
        } else if (itemId == R.id.action_hivst_registration) {
            startHivstRegistration();
            return true;
        } else if (itemId == R.id.action_kvp_prep_registration) {
            startKvpPrepRegistration();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startKvpPrepRegistration() {
        String gender = org.smartregister.chw.util.Utils.getClientGender(getHivMemberObject().getBaseEntityId());
        String dob = getHivMemberObject().getAge();
        int age = Utils.getAgeFromDate(dob);
        KvpPrEPRegisterActivity.startRegistration((Activity) getContext(), getHivMemberObject().getBaseEntityId(), gender, age);
    }


    private void startHivstRegistration() {
        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(getHivMemberObject().getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.GENDER, false);

        HivstRegisterActivity.startHivstRegistrationActivity(this, getHivMemberObject().getBaseEntityId(), gender);
    }

    protected void startTbRegister() {
        try {
            TbRegisterActivity.startTbFormActivity((Activity) getContext(), getHivMemberObject().getBaseEntityId(), CoreConstants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, CoreConstants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    protected void startAncRegister() {
        AncRegisterActivity.startAncRegistrationActivity((Activity) getContext(), Objects.requireNonNull(getHivMemberObject()).getBaseEntityId(), getHivMemberObject().getPhoneNumber(), org.smartregister.chw.util.Constants.JSON_FORM.getAncRegistration(), null, getHivMemberObject().getFamilyBaseEntityId(), getHivMemberObject().getFamilyName());
    }

    /**
     * Pre-populating the registration form before opening it
     */
    public void startHivRegistrationDetailsActivity() {
        Intent intent = new Intent(this, BaseHivFormsActivity.class);
        intent.putExtra(Constants.ActivityPayload.BASE_ENTITY_ID, getHivMemberObject().getBaseEntityId());
        String formName = org.smartregister.chw.util.Constants.JsonForm.getCbhsRegistrationForm();

        JSONObject formJsonObject = null;
        try {
            formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, formName);
        } catch (JSONException e) {
            Timber.e(e);
        }

        try {

            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            int age = org.smartregister.chw.util.Utils.getAgeFromDate(getHivMemberObject().getAge());
            updateAgeAndGender(fields, age, getHivMemberObject().getGender());

            formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, formName);
            formJsonObject.put(ENCOUNTER_TYPE, UPDATE_HIV_REGISTRATION);

            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                if (field.getString(NAME).equals(DBConstants.Key.CBHS_NUMBER)) {
                    field.getJSONObject(PROPERTIES).put(TEXT, getHivMemberObject().getCbhsNumber());
                } else if (field.getString(NAME).equals(DBConstants.Key.CLIENT_HIV_STATUS_DURING_REGISTRATION)) {
                    if (!getHivMemberObject().getCtcNumber().isEmpty())
                        field.getJSONObject(PROPERTIES).put(SELECTION, "1");
                    else field.getJSONObject(PROPERTIES).put(SELECTION, "0");
                } else if (field.getString(NAME).equals(DBConstants.Key.CTC_NUMBER) && !getHivMemberObject().getCtcNumber().isEmpty()) {
                    field.getJSONObject(PROPERTIES).put(TEXT, getHivMemberObject().getCtcNumber());
                } else if (field.getString(NAME).equals(DBConstants.Key.TB_NUMBER) && !getHivMemberObject().getTbNumber().isEmpty()) {
                    field.getJSONObject(PROPERTIES).put(TEXT, getHivMemberObject().getTbNumber());
                } else if (field.getString(NAME).equals(DBConstants.Key.MAT_NUMBER) && !getHivMemberObject().getMatNumber().isEmpty()) {
                    field.getJSONObject(PROPERTIES).put(TEXT, getHivMemberObject().getMatNumber());
                } else if (field.getString(NAME).equals(DBConstants.Key.RCH_NUMBER) && !getHivMemberObject().getRchNumber().isEmpty()) {
                    field.getJSONObject(PROPERTIES).put(TEXT, getHivMemberObject().getRchNumber());
                }
            }

        } catch (Exception e) {
            Timber.e(e);
        }

        intent.putExtra(Constants.ActivityPayload.JSON_FORM, formJsonObject.toString());
        intent.putExtra(Constants.ActivityPayload.ACTION, Constants.ActivityPayloadType.REGISTRATION);
        intent.putExtra(Constants.ActivityPayload.USE_DEFAULT_NEAT_FORM_LAYOUT, false);

        this.startActivityForResult(intent, org.smartregister.chw.anc.util.Constants.REQUEST_CODE_HOME_VISIT);
    }

    protected boolean isClientEligibleForAnc(HivMemberObject hivMemberObject) {
        if (hivMemberObject.getGender().equalsIgnoreCase("Female")) {
            //Obtaining the clients CommonPersonObjectClient used for checking is the client is Of Reproductive Age
            CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

            final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(hivMemberObject.getBaseEntityId());
            final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
            client.setColumnmaps(commonPersonObject.getColumnmaps());

            return org.smartregister.chw.core.utils.Utils.isMemberOfReproductiveAge(client, 15, 49);
        }
        return false;
    }

    @SuppressLint("StringFormatMatches")
    @Override
    public void updateLastVisitRow(@Nullable Date lastVisitDate) {
        Visit lastFollowupVisit = getVisit(org.smartregister.chw.util.Constants.Events.CBHS_FOLLOWUP);
        if (lastFollowupVisit != null) {
            TextView tvLastVisitDay = findViewById(R.id.textview_last_vist_day);
            tvLastVisitDay.setVisibility(View.VISIBLE);

            int numOfDays = Days.daysBetween(new DateTime(lastFollowupVisit.getDate()).toLocalDate(), new DateTime().toLocalDate()).getDays();

            if (numOfDays <= 1) {
                tvLastVisitDay.setText(getString(R.string.cbhs_visit_less_than_twenty_four));
            } else {
                tvLastVisitDay.setText(getString(R.string.cbhs_last_visit_n_days_ago, numOfDays));
            }

            findViewById(R.id.rl_last_visit_layout).setVisibility(View.VISIBLE);
        }
    }

    public @javax.annotation.Nullable Visit getVisit(String eventType) {
        return PmtctLibrary.getInstance().visitRepository().getLatestVisit(getHivMemberObject().getBaseEntityId(), eventType);
    }

    public interface Flavor {
        // void updateTbMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);
    }

    @Override
    public void setUpComingServicesStatus(@Nullable String service, @Nullable AlertStatus status, @Nullable Date date) {
        Date nextAppointmentDate = ChwCBHSDao.getNextVisitDate(getHivMemberObject().getBaseEntityId());
        if (nextAppointmentDate == null) {
            getRlUpcomingServices().setVisibility(View.GONE);
        } else {
            super.setUpComingServicesStatus(service, status, nextAppointmentDate);
        }
    }
}

