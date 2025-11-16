package org.smartregister.chw.activity;

import static org.smartregister.chw.tbleprosy.dao.TbLeprosyDao.getTbLeprosyClientStatus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreTbLeprosyProfileActivity;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.custom_view.TbLeprosyFloatingMenu;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.model.ChwAllClientsRegisterModel;
import org.smartregister.chw.presenter.TbLeprosyContactRegisterPresenter;
import org.smartregister.chw.tbleprosy.TbLeprosyLibrary;
import org.smartregister.chw.tbleprosy.dao.TbLeprosyDao;
import org.smartregister.chw.tbleprosy.domain.Visit;
import org.smartregister.chw.tbleprosy.util.Constants;
import org.smartregister.chw.tbleprosy.util.TbLeprosyVisitsUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.domain.FetchStatus;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.pojo.RegisterParams;
import org.smartregister.opd.utils.OpdJsonFormUtils;
import org.smartregister.opd.utils.OpdUtils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.sync.helper.ECSyncHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;


public class TbLeprosyProfileActivity extends CoreTbLeprosyProfileActivity implements TbLeprosyContactRegisterPresenter.ContactRegistrationCallback {

    private static final int REQUEST_CODE_CONTACT_REGISTER = 6700;

    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private TbLeprosyContactRegisterPresenter newClientRegisterPresenter;
    private OpdRegisterActivityContract.View newClientRegisterView;
    @Nullable
    private String pendingContactRegistrationLocationId;

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, TbLeprosyProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.TBLEPROSY_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    public void openClientObservationResults() {
        try {
            startForm("tbleprosy_record_visit");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void observationResults() {
        try {
            startForm("tbleprosy_record_visit");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openTbContactFollowUpVisit() {
        try {
            startForm(Constants.FORMS.TBLEPROSY_FOLLOWUP_VISIT);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openRecordClientVisit() {
        try {
            startForm(Constants.FORMS.RECORD_TBLEPROSY_VISIT);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openObservationResults() {
        String baseEntityId = memberObject.getBaseEntityId();
        try {
            JSONObject form = FormUtils.getFormUtils().getFormJson(Constants.FORMS.OBSERVATION_RESULTS);
            form.put(org.smartregister.util.JsonFormUtils.ENTITY_ID, baseEntityId);

            boolean isTbPresumptive = TbLeprosyDao.isTbPresumptiveClient(baseEntityId);
            boolean isLeprosyPresumptive = TbLeprosyDao.isLeprosyPresumptiveClient(baseEntityId);

            if (isTbPresumptive ^ isLeprosyPresumptive) {
                String hiddenValue = isTbPresumptive ? "tb" : "leprosy";
                applyObservationTypeOverrides(form, hiddenValue);
            }

            startFormActivity(form);
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    @Override
    public void openFollowupVisit() {
        try {
            startForm(Constants.FORMS.TBLEPROSY_FOLLOWUP_VISIT);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openRecordTbContactVisit() {
        TbLeprosyContactVisitActivity.startTbLeprosyVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openTbLeprosyContactRegister() {
        Intent intent = new Intent(this, TbLeprosyContactRegister.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberObject.getBaseEntityId());
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.FAMILY_BASE_ENTITY_ID, memberObject.getFamilyBaseEntityId());
        startActivityForResult(intent, REQUEST_CODE_CONTACT_REGISTER);
    }

    @Override
    protected void setupButtons() {

        String baseEntityId = memberObject.getBaseEntityId();
        boolean isContactClient = getTbLeprosyClientStatus(baseEntityId).equalsIgnoreCase("contact");

        if (!isContactClient) {
            manualProcessVisit.setVisibility(View.GONE);

            boolean isTbPresumptiveClient = TbLeprosyDao.isTbPresumptiveClient(baseEntityId);
            boolean isLeprosyPresumptiveClient = TbLeprosyDao.isLeprosyPresumptiveClient(baseEntityId);
            String latestTbLeprosyVisit = TbLeprosyDao.getTBleprosyVisit(baseEntityId);
            boolean hasTbLeprosyVisit = TbLeprosyDao.hasTbLeprosyVisit(baseEntityId);
            String latestObservationResults = TbLeprosyDao.getTbLeprosyObservationResults(baseEntityId);
            boolean hasPoorQualitySample = StringUtils.isNotBlank(latestObservationResults)
                    && StringUtils.containsIgnoreCase(latestObservationResults, "poor_quality_sample");

            if (isTbPresumptiveClient) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy);
            } else {
                textViewRecordTbLeprosy.setVisibility(View.GONE);
            }

            if (isLeprosyPresumptiveClient) {
                textViewRecordTbLeprosy.setVisibility(View.GONE);
                rlObservationResults.setVisibility(View.VISIBLE);
            }

            if (hasPoorQualitySample && StringUtils.isBlank(latestTbLeprosyVisit) && !hasTbLeprosyVisit) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy);
            } else if (StringUtils.isNotBlank(latestObservationResults)) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy_client_followup_visit);
                textViewRegisterTBLeprosyContact.setVisibility(View.VISIBLE);
                rlObservationResults.setVisibility(View.VISIBLE);
            }

            if (hasTbLeprosyVisit && StringUtils.isBlank(latestObservationResults)) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_observation_results);
            } else if (hasTbLeprosyVisit) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy_client_followup_visit);
                textViewRegisterTBLeprosyContact.setVisibility(View.VISIBLE);
                rlObservationResults.setVisibility(View.VISIBLE);
            }
        }


        if (isContactClient) {

            if (getTbLeprosyContactVisit() == null) {
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy_contact_visit);
            }

            if (getTbLeprosyContactVisit() != null) {


                if (!getTbLeprosyContactVisit().getProcessed()) {
                    textViewRecordTbLeprosy.setVisibility(View.GONE);
                    manualProcessVisit.setVisibility(View.VISIBLE);
                    textViewContinueTbLeprosy.setVisibility(View.VISIBLE);
                    manualProcessVisit.setOnClickListener(view -> {
                        try {
                            TbLeprosyVisitsUtil.manualProcessVisit(getTbLeprosyContactVisit());
                            displayToast(org.smartregister.chw.tbleprosy.R.string.tbleprosy_visit_conducted);
                            setupViews();
                        } catch (Exception e) {
                            Timber.d(e);
                        }
                    });
                } else {
                    manualProcessVisit.setVisibility(View.GONE);
                    textViewContinueTbLeprosy.setVisibility(View.GONE);
                    rlLastVisit.setVisibility(View.VISIBLE);
                    rlObservationResults.setVisibility(View.VISIBLE);
                }
            }

            if (StringUtils.isNotBlank(TbLeprosyDao.getTbLeprosyObservationResults(memberObject.getBaseEntityId()))) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy_contact_visit_followup);
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        delayRefresh();
    }

    private void delayRefresh() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                setupViews();
                fetchProfileData();
                profilePresenter.refreshProfileBottom();
            }, 500);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == org.smartregister.chw.tbleprosy.R.id.rlObservationResults) {
            TbLeprosyObservationResultsActivity.startMe(this, memberObject);
        } else {
            super.onClick(view);
        }
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    @Override
    protected void removeMember() {
        //do nothing
    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {
        //do nothing
    }

    @Override
    public void setProfileDetailThree(String s) {
        //do nothing
    }

    @Override
    public void toggleFamilyHead(boolean b) {
        //do nothing
    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {
        //do nothing
    }


    @Override
    public void startServiceForm() {
//        VmmcServiceActivity.startVmmcVisitActivity(this, baseEntityId, false);
    }

    @Override
    public void continueService() {
//        VmmcServiceActivity.startVmmcVisitActivity(this, baseEntityId, true);
    }


    @Override
    public void continueContactVisit() {
        TbLeprosyContactVisitActivity.startTbLeprosyVisitActivity(this, memberObject.getBaseEntityId(), true);
    }


    @Override
    public void refreshList() {
        //do nothing
    }

    @Override
    public void updateHasPhone(boolean b) {
        //do nothing
    }

    @Override
    public void setFamilyServiceStatus(String s) {
        //do nothing
    }

    @Override
    public void verifyHasPhone() {
        //do nothing
    }

    @Override
    public void notifyHasPhone(boolean b) {
        //do nothing
    }

    private void addReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.hiv_referral), CoreConstants.JSON_FORM.getHivReferralForm(), CoreConstants.TASKS_FOCUS.SICK_HIV));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.hts_referral), CoreConstants.JSON_FORM.getHtsReferralForm(), CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_leprosy_referral), CoreConstants.JSON_FORM.getTbLeprosyReferralForm(), CoreConstants.TASKS_FOCUS.TBLEPROSY));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral), CoreConstants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }


    @Override
    public void initializeFloatingMenu() {

        baseTbLeprosyFloatingMenu = new TbLeprosyFloatingMenu(this, memberObject);

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.vmmc_fab:
                    ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).launchCallWidget();
                    ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    org.smartregister.chw.util.Utils.launchClientReferralActivity(TbLeprosyProfileActivity.this, getReferralTypeModels(), memberObject.getBaseEntityId());
                    ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }
        };


        ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
        baseTbLeprosyFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseTbLeprosyFloatingMenu, linearLayoutParams);
    }

    private void startForm(String jsonForm) {
        JSONObject form = FormUtils.getFormUtils().getFormJson(jsonForm);
        try {
            form.put(org.smartregister.util.JsonFormUtils.ENTITY_ID, memberObject.getBaseEntityId());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        startFormActivity(form);
    }

    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, jsonForm.toString());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    private void applyObservationTypeOverrides(JSONObject form, String hiddenValue) throws JSONException {
        if (form == null) {
            return;
        }

        JSONObject stepOne = form.optJSONObject("step1");
        if (stepOne == null) {
            return;
        }

        JSONArray fields = stepOne.optJSONArray("fields");
        if (fields == null) {
            return;
        }

        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) {
                continue;
            }

            if ("investigation_type".equals(field.optString("key"))) {
                field.put("type", "hidden");
                field.put("value", hiddenValue);
                field.remove("options");
                field.remove("combine_checkbox_option_values");
                field.remove("label");
                field.remove("label_text_style");
                field.remove("text_color");
                field.remove("v_required");
                break;
            }
        }
    }

    protected Visit getTbLeprosyContactVisit() {
        return TbLeprosyLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.TBLEPROSY_CONTACT_VISIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CONTACT_REGISTER && resultCode == Activity.RESULT_OK && data != null) {
            handleNewClientRegistrationResult(data);
        }

        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);

            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        addReferralTypes();
    }

    private void handleNewClientRegistrationResult(@NonNull Intent data) {
        try {
            String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
            if (StringUtils.isBlank(jsonString)) {
                return;
            }

            JSONObject form = new JSONObject(jsonString);
            if (!CoreConstants.EventType.FAMILY_REGISTRATION.equals(form.optString(JsonFormUtils.ENCOUNTER_TYPE))) {
                return;
            }

            JSONObject metadata = form.optJSONObject(CoreJsonFormUtils.METADATA);
            pendingContactRegistrationLocationId = metadata != null ? metadata.optString(JsonFormUtils.ENCOUNTER_LOCATION) : null;

            ensureNewClientRegistrationSupport();
            RegisterParams registerParams = new RegisterParams();
            registerParams.setEditMode(false);
            registerParams.setFormTag(OpdJsonFormUtils.formTag(OpdUtils.context().allSharedPreferences()));
            showProgressDialog(org.smartregister.chw.core.R.string.saving_dialog_title);
            newClientRegisterPresenter.saveForm(jsonString, registerParams);
        } catch (Exception e) {
            pendingContactRegistrationLocationId = null;
            hideProgressDialog();
            Timber.e(e);
            Toast.makeText(this, org.smartregister.chw.core.R.string.error_unable_to_save_form, Toast.LENGTH_LONG).show();
        }
    }

    private void ensureNewClientRegistrationSupport() {
        if (newClientRegisterView == null) {
            newClientRegisterView = new ContactRegistrationView();
            newClientRegisterPresenter = new TbLeprosyContactRegisterPresenter(newClientRegisterView,
                    new ChwAllClientsRegisterModel(this), this);
        }
    }

    private void saveRegisterTbLeprosyContactEvent(String indexClientBaseEntityId, String contactBaseEntityId, String locationId) {
        if (StringUtils.isBlank(indexClientBaseEntityId) || StringUtils.isBlank(contactBaseEntityId)) {
            return;
        }

        try {
            AllSharedPreferences sharedPreferences = ChwApplication.getInstance().getContext().allSharedPreferences();
            if (StringUtils.isBlank(locationId)) {
                locationId = sharedPreferences.fetchDefaultLocalityId(sharedPreferences.fetchRegisteredANM());
            }

            ECSyncHelper syncHelper = FamilyLibrary.getInstance().getEcSyncHelper();
            Event baseEvent = (Event) new Event()
                    .withBaseEntityId(contactBaseEntityId)
                    .withEventDate(new Date())
                    .withEventType(Constants.EVENT_TYPE.TBLEPROSY_CONTACTS)
                    .withFormSubmissionId(org.smartregister.util.JsonFormUtils.generateRandomUUIDString())
                    .withEntityType(Constants.TABLES.TBLEPROSY_CONTACTS)
                    .withProviderId(sharedPreferences.fetchRegisteredANM())
                    .withLocationId(locationId)
                    .withTeamId(sharedPreferences.fetchDefaultTeamId(sharedPreferences.fetchRegisteredANM()))
                    .withTeam(sharedPreferences.fetchDefaultTeam(sharedPreferences.fetchRegisteredANM()))
                    .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION)
                    .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                    .withDateCreated(new Date());

            baseEvent.addObs(new Obs()
                    .withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.INDEX_CLIENT_BASE_ENTITY_ID)
                    .withValue(indexClientBaseEntityId)
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.INDEX_CLIENT_BASE_ENTITY_ID)
                    .withFieldType("formsubmissionField")
                    .withFieldDataType("text")
                    .withParentCode("")
                    .withHumanReadableValues(new ArrayList<>()));

            org.smartregister.chw.util.JsonFormUtils.tagSyncMetadata(sharedPreferences, baseEvent);
            baseEvent.setLocationId(locationId);

            JSONObject eventJson = new JSONObject(org.smartregister.util.JsonFormUtils.gson.toJson(baseEvent));
            syncHelper.addEvent(contactBaseEntityId, eventJson);
            long lastSyncTimeStamp = ChwApplication.getInstance().getContext().allSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            ChwApplication.getClientProcessor(ChwApplication.getInstance().getContext().applicationContext())
                    .processClient(syncHelper.getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
            ChwApplication.getInstance().getContext().allSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Timber.e(e, "TbLeprosyProfileActivity --> saveRegisterTbLeprosyContactEvent");
        }
    }

    private class ContactRegistrationView implements OpdRegisterActivityContract.View {

        @Override
        public Context getContext() {
            return TbLeprosyProfileActivity.this;
        }

        @Override
        public void displaySyncNotification() {
            // no-op
        }

        @Override
        public void displayToast(int resourceId) {
            TbLeprosyProfileActivity.this.displayToast(resourceId);
        }

        @Override
        public void displayToast(String message) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void displayShortToast(int resourceId) {
            Toast.makeText(getContext(), getString(resourceId), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void startFormActivity(JSONObject form) {
            // not used
        }

        @Override
        public void refreshList(FetchStatus fetchStatus) {
            hideProgressDialog();
            Toast.makeText(getContext(), getString(org.smartregister.chw.R.string.tbleprosy_contact_registration_success), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void showProgressDialog(int messageStringIdentifier) {
            TbLeprosyProfileActivity.this.showProgressDialog(messageStringIdentifier);
        }

        @Override
        public void hideProgressDialog() {
            TbLeprosyProfileActivity.this.hideProgressDialog();
        }

        @Override
        public void updateInitialsText(String initials) {
            // no-op
        }

        @Override
        public OpdRegisterActivityContract.Presenter presenter() {
            return newClientRegisterPresenter;
        }

        @Override
        public void startFormActivity(String formName, String entityId, String metaData, HashMap<String, String> injectedFieldValues, String clientTable) {
            // not used
        }

        @Override
        public void startFormActivity(@NonNull JSONObject jsonForm, @Nullable HashMap<String, String> parcelableData) {
            // not used
        }
    }

    @Override
    public void onContactBaseEntityIdGenerated(@Nullable String contactBaseEntityId) {
        String locationId = pendingContactRegistrationLocationId;
        pendingContactRegistrationLocationId = null;

        if (StringUtils.isBlank(contactBaseEntityId)) {
            Timber.w("Contact base entity ID not available after registration save");
            return;
        }

        saveRegisterTbLeprosyContactEvent(memberObject.getBaseEntityId(), contactBaseEntityId, locationId);
    }
}
