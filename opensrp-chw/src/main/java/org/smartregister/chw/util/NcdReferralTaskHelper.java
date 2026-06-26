package org.smartregister.chw.util;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.NcdCaseManagementDao;
import org.smartregister.chw.model.NcdReferralInputs;
import org.smartregister.chw.referral.util.DBConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.Task;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.util.JsonFormUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

/**
 * Creates NCD referral tasks and their backing Referral Registration events.
 *
 * The canonical CHW referral workflow requires BOTH:
 *   1. A "Referral Registration" event in the events table (with its own formSubmissionId)
 *   2. A Task in ec_tasks whose reasonReference points to that event's formSubmissionId
 *
 * Phase 1 originally only created the task and passed the NCD case-management visit's
 * formSubmissionId as reasonReference — which left the referral orphaned from any
 * Referral Registration event. This helper now creates the event first, persists it,
 * and uses its formSubmissionId to link the task.
 *
 * Deduplication: NcdCaseManagementDao.hasOpenReferral() prevents creating a new referral
 * while a previous ncd_urgent_referral or ncd_non_emergency_referral task is still open.
 */
public class NcdReferralTaskHelper {

    private NcdReferralTaskHelper() {
        // utility class
    }

    /**
     * Backward-compatible overload that creates a referral with no emergency/treatment-supporter
     * details captured. Equivalent to passing {@code null} inputs.
     */
    public static boolean createReferralIfNeeded(String baseEntityId, String triggeringFormSubmissionId,
                                                  String alertStatus, String description,
                                                  List<String> problemKeys, List<String> problemHumanReadableValues) {
        return createReferralIfNeeded(baseEntityId, triggeringFormSubmissionId, alertStatus,
                description, problemKeys, problemHumanReadableValues, null);
    }

    /**
     * Creates a Referral Registration event and a linked task if no open referral exists.
     *
     * @param baseEntityId             client ID
     * @param triggeringFormSubmissionId form submission of the case-management visit that triggered this referral (optional context, not used for reasonReference)
     * @param alertStatus              "red" or "yellow"
     * @param description              human-readable description of the referral reason (kept on the task)
     * @param problemKeys              coded concept keys for each problem; persisted as the "problem" obs values
     * @param problemHumanReadableValues human-readable label per problem; persisted as the obs humanReadableValues (parallel to problemKeys)
     * @param inputs                   emergency-case + treatment-supporter values captured on the prompt; may be {@code null}
     * @return true if a referral was created, false if skipped
     */
    public static boolean createReferralIfNeeded(String baseEntityId, String triggeringFormSubmissionId,
                                                  String alertStatus, String description,
                                                  List<String> problemKeys, List<String> problemHumanReadableValues,
                                                  NcdReferralInputs inputs) {
        if (!"red".equals(alertStatus) && !"yellow".equals(alertStatus)) {
            return false;
        }

        if (NcdCaseManagementDao.hasOpenReferral(baseEntityId)) {
            Timber.d("NCD referral skipped — open referral already exists for %s", baseEntityId);
            return false;
        }

        String taskCode;
        String focus;
        int priority;
        if ("red".equals(alertStatus)) {
            taskCode = Constants.NcdReferral.URGENT_REFERRAL_CODE;
            focus = Constants.NcdReferral.FOCUS_NCD_DANGER_SIGNS;
            priority = 1;
        } else {
            taskCode = Constants.NcdReferral.NON_EMERGENCY_REFERRAL_CODE;
            focus = Constants.NcdReferral.FOCUS_NCD_CLINICAL_CONCERN;
            priority = 3;
        }

        // Build parallel lists of coded problem keys + human-readable labels. When no specific
        // reason was captured, fall back to a single generic key paired with the description so
        // the obs still carries both a value (key) and a humanReadableValue.
        List<String> problemValues = new ArrayList<>();
        List<String> problemReadable = new ArrayList<>();
        if (problemKeys != null && !problemKeys.isEmpty()
                && problemHumanReadableValues != null
                && problemHumanReadableValues.size() == problemKeys.size()) {
            problemValues.addAll(problemKeys);
            problemReadable.addAll(problemHumanReadableValues);
        } else {
            problemValues.add("red".equals(alertStatus)
                    ? Constants.NcdReferral.PROBLEM_KEY_DANGER_SIGNS
                    : Constants.NcdReferral.PROBLEM_KEY_CLINICAL_CONCERN);
            problemReadable.add(description);
        }

        Event referralEvent = buildAndPersistReferralEvent(baseEntityId, focus, problemValues, problemReadable, inputs);
        if (referralEvent == null) {
            Timber.e("NCD referral event could not be persisted for %s — skipping task creation", baseEntityId);
            return false;
        }

        createTask(baseEntityId, referralEvent.getFormSubmissionId(), taskCode, focus, description, priority);
        return true;
    }

    /**
     * Builds a "Referral Registration" event with the minimal obs set expected by the
     * referral client processor (problem, referral_service, referral_status, referral_type,
     * referral_date, referral_time, referral_hf), tags it with sync metadata, and persists
     * it via NCUtils.processEvent. Returns the persisted event so callers can use its
     * formSubmissionId as the task's reasonReference.
     */
    private static Event buildAndPersistReferralEvent(String baseEntityId, String referralService,
                                                      List<String> problemValues, List<String> problemHumanReadableValues,
                                                      NcdReferralInputs inputs) {
        AllSharedPreferences sharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
        String providerId = sharedPreferences.fetchRegisteredANM();
        String locationId = sharedPreferences.fetchDefaultLocalityId(providerId);
        String teamId = sharedPreferences.fetchDefaultTeamId(providerId);
        String team = sharedPreferences.fetchDefaultTeam(providerId);

        Event event = (Event) new Event()
                .withBaseEntityId(baseEntityId)
                .withEventDate(new Date())
                .withEventType(org.smartregister.chw.referral.util.Constants.EventType.REGISTRATION)
                .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                .withEntityType(org.smartregister.chw.referral.util.Constants.Tables.REFERRAL)
                .withProviderId(providerId)
                .withLocationId(locationId)
                .withTeamId(teamId)
                .withTeam(team)
                .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION)
                .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                .withDateCreated(new Date());

        // Mirror the structure produced by the screening referral form: coded keys in `values`
        // and the matching display text in `humanReadableValues`, with fieldCode "concept" and
        // parentCode "problem".
        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.PROBLEM)
                .withFieldType("")
                .withFieldCode(JsonFormUtils.CONCEPT)
                .withParentCode(DBConstants.Key.PROBLEM)
                .withValues(new ArrayList<Object>(problemValues))
                .withHumanReadableValues(new ArrayList<Object>(problemHumanReadableValues)));

        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.SERVICE_BEFORE_REFERRAL)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.SERVICE_BEFORE_REFERRAL)
                .withValue("None"));

        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.REFERRAL_SERVICE)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.REFERRAL_SERVICE)
                .withValue(referralService));

        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.REFERRAL_STATUS)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.REFERRAL_STATUS)
                .withValue("PENDING"));

        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.REFERRAL_TYPE)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.REFERRAL_TYPE)
                .withValue(org.smartregister.chw.referral.util.Constants.ReferralType.COMMUNITY_TO_FACILITY_REFERRAL));

        Date now = new Date();
        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.REFERRAL_DATE)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.REFERRAL_DATE)
                .withValue(now.getTime()));

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss.SSS", Locale.getDefault());
        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.REFERRAL_TIME)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.REFERRAL_TIME)
                .withValue(timeFormat.format(now)));

        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.REFERRAL_APPOINTMENT_DATE)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.REFERRAL_APPOINTMENT_DATE)
                .withValue(now.getTime()));

        event.addObs(new Obs()
                .withFormSubmissionField(DBConstants.Key.REFERRAL_HF)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.REFERRAL_HF)
                .withValue(locationId));

        addReferralInputObs(event, inputs);

        try {
            org.smartregister.chw.util.JsonFormUtils.tagSyncMetadata(sharedPreferences, event);
            NCUtils.processEvent(event.getBaseEntityId(),
                    new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(event)));
            return event;
        } catch (Exception e) {
            Timber.e(e, "Failed to persist NCD Referral Registration event for %s", baseEntityId);
            return null;
        }
    }

    /**
     * Appends the emergency-case and treatment-supporter obs captured on the post-visit prompt.
     * {@code is_emergency_case} and {@code has_treatment_supporter} are always written when
     * present; the name/phone/relationship details are written only when the supporter gate is
     * "Yes" and the value is non-blank — mirroring the manual referral form's behaviour. No-op
     * when {@code inputs} is null.
     */
    @androidx.annotation.VisibleForTesting
    static void addReferralInputObs(Event event, NcdReferralInputs inputs) {
        if (inputs == null) {
            return;
        }
        addConceptObs(event, Constants.NcdReferral.IS_EMERGENCY_CASE, inputs.getIsEmergencyCase());
        addConceptObs(event, Constants.NcdReferral.HAS_TREATMENT_SUPPORTER, inputs.getHasTreatmentSupporter());
        if (inputs.isTreatmentSupporterGateYes()) {
            addConceptObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_NAME, inputs.getSupporterName());
            addConceptObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_PHONE, inputs.getSupporterPhone());
            addConceptObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_RELATIONSHIP, inputs.getSupporterRelationship());
        }
    }

    private static void addConceptObs(Event event, String conceptKey, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        event.addObs(new Obs()
                .withFormSubmissionField(conceptKey)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(conceptKey)
                .withValue(value.trim()));
    }

    private static void createTask(String baseEntityId, String referralEventFormSubmissionId,
                                   String taskCode, String focus, String description, int priority) {
        AllSharedPreferences sharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();

        Task task = new Task();
        task.setIdentifier(UUID.randomUUID().toString());
        task.setPlanIdentifier(CoreConstants.REFERRAL_PLAN_ID);
        // change this to selected facility we might need to include facility selection on the new dialog
        task.setGroupIdentifier(
                sharedPreferences.fetchUserLocalityId(sharedPreferences.fetchRegisteredANM()));
        task.setStatus(Task.TaskStatus.READY);
        task.setBusinessStatus(CoreConstants.BUSINESS_STATUS.REFERRED);
        task.setPriority(priority);
        task.setCode(taskCode);
        task.setDescription(description);
        task.setFocus(focus);
        task.setForEntity(baseEntityId);

        DateTime now = new DateTime();
        task.setExecutionStartDate(now);
        task.setAuthoredOn(now);
        task.setLastModified(now);
        task.setOwner(sharedPreferences.fetchRegisteredANM());
        task.setSyncStatus(BaseRepository.TYPE_Created);
        task.setReasonReference(referralEventFormSubmissionId);
        task.setRequester(
                sharedPreferences.getANMPreferredName(sharedPreferences.fetchRegisteredANM()));
        task.setLocation(
                sharedPreferences.fetchUserLocalityId(sharedPreferences.fetchRegisteredANM()));

        CoreChwApplication.getInstance().getTaskRepository().addOrUpdate(task);
        Timber.d("NCD referral task created: code=%s, entity=%s, reasonReference=%s",
                taskCode, baseEntityId, referralEventFormSubmissionId);
    }
}
