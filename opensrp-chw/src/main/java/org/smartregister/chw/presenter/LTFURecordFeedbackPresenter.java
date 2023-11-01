package org.smartregister.chw.presenter;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.nerdstone.neatformcore.domain.model.NFormViewData;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.model.LTFURecordFeedbackModel;
import org.smartregister.chw.referral.contract.BaseIssueReferralContract;
import org.smartregister.chw.referral.model.AbstractIssueReferralModel;
import org.smartregister.chw.referral.presenter.BaseIssueReferralPresenter;
import org.smartregister.chw.referral.util.DBConstants;
import org.smartregister.chw.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.LocationRepository;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;


public class LTFURecordFeedbackPresenter extends BaseIssueReferralPresenter {
    private final String referralHf;
    private final String taskId;
    private final String baseEntityId;

    public LTFURecordFeedbackPresenter(@NonNull String baseEntityID, String taskId, String referralHf, @NonNull BaseIssueReferralContract.View view, @NonNull Class<? extends AbstractIssueReferralModel> viewModelClass, @NonNull BaseIssueReferralContract.Interactor interactor) {
        super(baseEntityID, view, viewModelClass, interactor);
        this.referralHf = referralHf;
        this.baseEntityId = baseEntityID;
        this.taskId = taskId;
    }


    @Override
    public Class<? extends AbstractIssueReferralModel> getViewModel() {
        return LTFURecordFeedbackModel.class;
    }

    @NonNull
    @Override
    public String getMainCondition() {
        return " " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.Key.BASE_ENTITY_ID + " = '" + baseEntityId + "'";
    }

    @NotNull
    @Override
    public String getMainTable() {
        return Constants.TABLE_NAME.FAMILY_MEMBER;
    }

    @Override
    public void onRegistrationSaved(boolean saveSuccessful) {
        NavigationMenu navigationMenu = NavigationMenu.getInstance((Activity) getView(),
                null, null);
        if (navigationMenu != null) {
            navigationMenu.refreshCount();
        }
    }

    @Override
    public void saveForm(@NonNull HashMap<String, NFormViewData> valuesHashMap, @NonNull JSONObject jsonObject) {
        //first close the referral task
        //if from the valuesHasMap followupStatus value is client_found_ready_to_return then call save form super
        //else create an event that just sends the feedback to the server
        if (StringUtils.containsIgnoreCase(String.valueOf(valuesHashMap.get("followup_status").getValue()), "client_found_ready_to_return")) {
            tagWithReferralDetails(valuesHashMap);
            super.saveForm(valuesHashMap, jsonObject);
        }
        try {
            createFeedbackEvent(valuesHashMap);
        } catch (Exception e) {
            Timber.e(e);
        }
        saveCloseReferralEvent();
        completeTask();

    }

    private void tagWithReferralDetails(HashMap<String, NFormViewData> valuesHashMap) {

        LocationRepository locationRepository = new LocationRepository();
        Location location = locationRepository.getLocationById(referralHf);

        valuesHashMap.put("problem", generateProblem());
        valuesHashMap.put("chw_referral_hf", generateChwReferralHf(referralHf, location.getProperties().getName()));
    }

    private NFormViewData generateProblem() {
        NFormViewData problem = new NFormViewData();
        NFormViewData problemValue = new NFormViewData();
        HashMap<String, NFormViewData> problemValueHash = new HashMap<>();


        HashMap<String, String> problemMetaData = new HashMap<>();
        problemMetaData.put("openmrs_entity", "concept");
        problemMetaData.put("openmrs_entity_id", "problem");

        HashMap<String, String> problemValueMetaData = new HashMap<>();
        problemValueMetaData.put("openmrs_entity", "concept");
        problemValueMetaData.put("openmrs_entity_id", "client_returning_to_services");

        problemValue.setMetadata(problemValueMetaData);
        problemValue.setValue("LTF Client Returning to Service");

        problemValueHash.put("client_returning_to_services", problemValue);

        problem.setMetadata(problemMetaData);
        problem.setValue(problemValueHash);
        problem.setVisible(true);
        problem.setType("Calculation");

        return problem;
    }

    private NFormViewData generateChwReferralHf(String referralHfCode, String referralHfName) {
        NFormViewData chwReferralHf = new NFormViewData();
        HashMap<String, String> chwReferralHfMetaData = new HashMap<>();

        NFormViewData chwReferralHfValue = new NFormViewData();
        HashMap<String, String> chwReferralHfValueMetaData = new HashMap<>();

        chwReferralHfValueMetaData.put("openmrs_entity", "concept");
        chwReferralHfValueMetaData.put("openmrs_entity_id", referralHfCode);

        chwReferralHfValue.setMetadata(chwReferralHfValueMetaData);

        chwReferralHfValue.setValue(referralHfName);

        chwReferralHfMetaData.put("openmrs_entity", "concept");
        chwReferralHfMetaData.put("openmrs_entity_id", "chw_referral_hf");

        chwReferralHf.setMetadata(chwReferralHfMetaData);
        chwReferralHf.setValue(chwReferralHfValue);
        chwReferralHf.setVisible(true);
        chwReferralHf.setType("Calculation");

        return chwReferralHf;
    }

    private void saveCloseReferralEvent() {
        try {
            AllSharedPreferences sharedPreferences = Utils.getAllSharedPreferences();
            ECSyncHelper syncHelper = FamilyLibrary.getInstance().getEcSyncHelper();
            Event baseEvent = (Event) new Event()
                    .withBaseEntityId(baseEntityId)
                    .withEventDate(new Date())
                    .withEventType(CoreConstants.EventType.CLOSE_REFERRAL)
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withEntityType(CoreConstants.TABLE_NAME.CLOSE_REFERRAL)
                    .withProviderId(sharedPreferences.fetchRegisteredANM())
                    .withLocationId(getTask().getLocation())
                    .withTeamId(sharedPreferences.fetchDefaultTeamId(sharedPreferences.fetchRegisteredANM()))
                    .withTeam(sharedPreferences.fetchDefaultTeam(sharedPreferences.fetchRegisteredANM()))
                    .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION)
                    .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                    .withDateCreated(new Date());

            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK).withValue(getTask().getIdentifier())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_STATUS).withValue(getTask().getStatus())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_STATUS).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_BUSINESS_STATUS).withValue(getTask().getBusinessStatus())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_BUSINESS_STATUS).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            org.smartregister.chw.util.JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), baseEvent);// tag docs

            //setting the location uuid of the referral initiator so that to allow the event to sync back to the chw app since it sync data by location.
            baseEvent.setLocationId(getTask().getLocation());

            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(baseEvent));
            syncHelper.addEvent(baseEntityId, eventJson);
            long lastSyncTimeStamp = ChwApplication.getInstance().getContext().allSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            ChwApplication.getClientProcessor(ChwApplication.getInstance().getContext().applicationContext()).processClient(syncHelper.getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
            ChwApplication.getInstance().getContext().allSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Timber.e(e, "LTFURecordFeedbackPresenter --> saveCloseReferralEvent");
        }

    }

    private void createFeedbackEvent(HashMap<String, NFormViewData> valuesHashMap) throws Exception {
        List<Obs> obs = org.smartregister.chw.util.JsonFormUtils.getObsForNeatForm(valuesHashMap);
        if (obs.size() > 0) {
            Event baseEvent = (Event) new Event()
                    .withBaseEntityId(baseEntityId)
                    .withEventDate(new Date())
                    .withEventType("LTFU Feedback")
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withEntityType(CoreConstants.TABLE_NAME.REFERRAL)
                    .withProviderId(Utils.getAllSharedPreferences().fetchRegisteredANM())
                    .withLocationId(referralHf)
                    .withTeamId(Utils.getAllSharedPreferences().fetchDefaultTeamId(Utils.getAllSharedPreferences().fetchRegisteredANM()))
                    .withTeam(Utils.getAllSharedPreferences().fetchDefaultTeam(Utils.getAllSharedPreferences().fetchRegisteredANM()))
                    .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION)
                    .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                    .withDateCreated(new Date());

            for (Obs ob : obs) {
                baseEvent.addObs(ob);
            }
            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK).withValue(getTask().getIdentifier())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(baseEvent));
            FamilyLibrary.getInstance().getEcSyncHelper().addEvent(baseEntityId, eventJson);
            long lastSyncTimeStamp = ChwApplication.getInstance().getContext().allSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            ChwApplication.getClientProcessor(ChwApplication.getInstance().getContext().applicationContext()).processClient(FamilyLibrary.getInstance().getEcSyncHelper().getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
            ChwApplication.getInstance().getContext().allSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        }
    }

    private Task getTask() {
        return ChwApplication.getInstance().getTaskRepository().getTaskByIdentifier(taskId);
    }

    private void completeTask() {
        Task currentTask = getTask();
        currentTask.setForEntity(baseEntityId);
        currentTask.setStatus(Task.TaskStatus.IN_PROGRESS);
        CoreReferralUtils.completeTask(currentTask, false);
    }
}
