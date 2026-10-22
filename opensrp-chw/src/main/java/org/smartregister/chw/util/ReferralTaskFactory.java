package org.smartregister.chw.util;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

/**
 * Creates community-to-facility referrals: a "Referral Registration" event plus the task that
 * points back at it.
 *
 * <p>The canonical CHW referral workflow requires BOTH:
 * <ol>
 *   <li>A "Referral Registration" event in the events table (with its own formSubmissionId)</li>
 *   <li>A Task in ec_tasks whose reasonReference points to that event's formSubmissionId</li>
 * </ol>
 *
 * <p>This was originally written inside {@code NcdReferralTaskHelper}; it lives here because
 * nothing about building a referral is NCD-specific. {@code NcdReferralTaskHelper} keeps the
 * NCD-specific alert gating and deduplication and delegates the construction here, and the
 * referral follow-up flow calls it directly when a client's condition is still unresolved.
 */
public final class ReferralTaskFactory {

    private ReferralTaskFactory() {
        // utility class
    }

    /**
     * Builds and persists the referral event, then the linked task. Callers own any gating or
     * deduplication; by the time this runs the decision to refer has already been made.
     *
     * @param baseEntityId   client ID
     * @param focus          task focus, also emitted as the {@code referral_service} obs
     * @param taskCode       task code, e.g. {@code "Referral"}
     * @param priority       task priority; 1 is urgent
     * @param description    human-readable referral reason kept on the task
     * @param problemKeys    coded concept keys persisted as the "problem" obs values
     * @param problemLabels  labels persisted as matching obs humanReadableValues
     * @param inputs         facility / emergency / treatment-supporter values; may be {@code null}
     * @return true when both the event and the task were written
     */
    public static boolean createReferral(String baseEntityId, String focus, String taskCode,
                                         int priority, String description,
                                         List<String> problemKeys, List<String> problemLabels,
                                         NcdReferralInputs inputs) {
        Event referralEvent = buildAndPersistReferralEvent(baseEntityId, focus, problemKeys,
                problemLabels, inputs);
        if (referralEvent == null) {
            Timber.e("Referral event could not be persisted for %s; skipping task creation",
                    baseEntityId);
            return false;
        }

        String referralFacilityId = inputs != null ? inputs.getReferralFacilityId() : null;
        createTask(baseEntityId, referralEvent.getFormSubmissionId(), taskCode, focus, description,
                priority, referralFacilityId);
        return true;
    }

    /**
     * Builds a "Referral Registration" event with the minimal obs set expected by the
     * referral client processor (problem, referral_service, referral_status, referral_type,
     * referral_date, referral_time, referral_hf), tags it with sync metadata, and persists
     * it via NCUtils.processEvent. Returns the persisted event so callers can use its
     * formSubmissionId as the task's reasonReference.
     */
    static Event buildAndPersistReferralEvent(String baseEntityId, String referralService,
                                              List<String> problemValues,
                                              List<String> problemHumanReadableValues,
                                              NcdReferralInputs inputs) {
        AllSharedPreferences sharedPreferences =
                org.smartregister.util.Utils.getAllSharedPreferences();
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
                .withValue(org.smartregister.chw.referral.util.Constants.ReferralType
                        .COMMUNITY_TO_FACILITY_REFERRAL));

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

        event.addObs(buildReferralHfObs(inputs, locationId));

        addReferralInputObs(event, inputs);

        try {
            org.smartregister.chw.util.JsonFormUtils.tagSyncMetadata(sharedPreferences, event);
            NCUtils.processEvent(event.getBaseEntityId(),
                    new JSONObject(
                            org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(event)));
            return event;
        } catch (Exception e) {
            Timber.e(e, "Failed to persist Referral Registration event for %s", baseEntityId);
            return null;
        }
    }

    /**
     * Appends the emergency-case and treatment-supporter obs captured on the referral prompt.
     * {@code is_emergency_case} and {@code has_treatment_supporter} are always written when
     * present; the name/phone/relationship details are written only when the supporter gate is
     * "Yes" and the value is non-blank, mirroring the manual referral form's behaviour. No-op
     * when {@code inputs} is null.
     */
    static void addReferralInputObs(Event event, NcdReferralInputs inputs) {
        if (inputs == null) {
            return;
        }
        addConceptObs(event, Constants.NcdReferral.IS_EMERGENCY_CASE, inputs.getIsEmergencyCase());
        addConceptObs(event, Constants.NcdReferral.HAS_TREATMENT_SUPPORTER,
                inputs.getHasTreatmentSupporter());
        if (inputs.isTreatmentSupporterGateYes()) {
            addConceptObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_NAME,
                    inputs.getSupporterName());
            addConceptObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_PHONE,
                    inputs.getSupporterPhone());
            addConceptObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_RELATIONSHIP,
                    inputs.getSupporterRelationship());
        }
    }

    /**
     * Builds the {@code chw_referral_hf} obs. The value is the CHW-selected referral facility's
     * location id (falling back to the CHW's locality when none was captured), and the facility
     * name is attached as the humanReadableValue so register/detail views can display it without a
     * second lookup.
     */
    static Obs buildReferralHfObs(NcdReferralInputs inputs, String fallbackLocationId) {
        Obs obs = new Obs()
                .withFormSubmissionField(DBConstants.Key.REFERRAL_HF)
                .withFieldType(JsonFormUtils.CONCEPT)
                .withFieldCode(DBConstants.Key.REFERRAL_HF)
                .withValue(referralFacilityId(inputs, fallbackLocationId));
        if (inputs != null && isNotBlank(inputs.getReferralFacilityName())) {
            obs.withHumanReadableValues(new ArrayList<Object>(
                    Collections.singletonList(inputs.getReferralFacilityName().trim())));
        }
        return obs;
    }

    /**
     * Resolves the referral facility's location id from the captured inputs, falling back to the
     * CHW's locality id when no facility was selected (preserving the pre-facility behaviour).
     */
    private static String referralFacilityId(NcdReferralInputs inputs, String fallbackLocationId) {
        if (inputs != null && isNotBlank(inputs.getReferralFacilityId())) {
            return inputs.getReferralFacilityId().trim();
        }
        return fallbackLocationId;
    }

    /**
     * The task groupIdentifier: the selected facility's location id, or the CHW's locality id when
     * no facility was captured.
     */
    static String resolveGroupIdentifier(String referralFacilityId, String fallbackLocalityId) {
        if (isNotBlank(referralFacilityId)) {
            return referralFacilityId.trim();
        }
        return fallbackLocalityId;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
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

    static void createTask(String baseEntityId, String referralEventFormSubmissionId,
                           String taskCode, String focus, String description, int priority,
                           String referralFacilityId) {
        AllSharedPreferences sharedPreferences =
                org.smartregister.util.Utils.getAllSharedPreferences();

        Task task = new Task();
        task.setIdentifier(UUID.randomUUID().toString());
        task.setPlanIdentifier(CoreConstants.REFERRAL_PLAN_ID);
        // Group the task by the CHW-selected referral facility (its location id), falling back to
        // the CHW's own locality when no facility was captured.
        task.setGroupIdentifier(resolveGroupIdentifier(referralFacilityId,
                sharedPreferences.fetchUserLocalityId(sharedPreferences.fetchRegisteredANM())));
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
        Timber.d("Referral task created: code=%s, focus=%s, entity=%s, reasonReference=%s",
                taskCode, focus, baseEntityId, referralEventFormSubmissionId);
    }
}
