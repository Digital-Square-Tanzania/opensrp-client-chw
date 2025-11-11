package org.smartregister.chw.util;

import static org.smartregister.chw.ncd.util.Constants.TABLES.DIABETES_HYPERTENSION_CONFIRMATION;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.dao.NcdDao;
import org.smartregister.domain.db.EventClient;
import org.smartregister.repository.AllSharedPreferences;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

public final class NcdAutoConfirmationHelper {

    public static final String SCREENING_EVENT_TYPE = "Diabetes and Hypertension Screening";
    private static final String CONFIRMATION_EVENT_TYPE = "Diabetes and Hypertension Screening Confirmation";
    private static final String FIELD_DIAGNOSED = "diagnosed_diabetes";
    private static final String FIELD_MEDICINES_DIABETES = "medicines_diabetes";
    private static final String FIELD_MEDICINES_HYPERTENSION = "medicines_hypertension";
    private static final String DIABETES_RESULT_FIELD = "diabetes_result";
    private static final String HYPERTENSION_RESULT_FIELD = "hypertension_result";
    private static final String POSITIVE_VALUE = "Positive";

    private NcdAutoConfirmationHelper() {
        // no-op
    }

    public static void maybeAutoConfirmDiabetesHypertension(EventClient eventClient) {
        try {
            if (eventClient == null || eventClient.getEvent() == null) {
                return;
            }

            org.smartregister.domain.Event screeningEvent = eventClient.getEvent();
            if (!SCREENING_EVENT_TYPE.equalsIgnoreCase(screeningEvent.getEventType())) {
                return;
            }

            String baseEntityId = screeningEvent.getBaseEntityId();
            if (StringUtils.isBlank(baseEntityId)) {
                return;
            }

            boolean diabetesPositive = isAffirmative(extractValue(screeningEvent, FIELD_DIAGNOSED))
                    || isAffirmative(extractValue(screeningEvent, FIELD_MEDICINES_DIABETES));
            boolean hypertensionPositive = isAffirmative(extractValue(screeningEvent, FIELD_MEDICINES_HYPERTENSION));

            if (!diabetesPositive && !hypertensionPositive) {
                return;
            }

            NcdDao.ConfirmationStatus currentStatus = NcdDao.getLatestConfirmationStatus(baseEntityId);
            boolean needsDiabetesConfirmation = diabetesPositive && (currentStatus == null || !currentStatus.isDiabetesPositive());
            boolean needsHypertensionConfirmation = hypertensionPositive && (currentStatus == null || !currentStatus.isHypertensionPositive());

            if (!needsDiabetesConfirmation && !needsHypertensionConfirmation) {
                return;
            }

            queueConfirmationEvent(baseEntityId, screeningEvent.getDetails(), needsDiabetesConfirmation, needsHypertensionConfirmation);
        } catch (Exception e) {
            Timber.e(e, "Error auto-confirming diabetes/hypertension results");
        }
    }

    private static void queueConfirmationEvent(String baseEntityId, Map<String, String> details,
                                               boolean confirmDiabetes, boolean confirmHypertension) {
        try {
            AllSharedPreferences allSharedPreferences = ChwApplication.getInstance().getContext().allSharedPreferences();
            String providerId = allSharedPreferences.fetchRegisteredANM();
            String teamId = allSharedPreferences.fetchDefaultTeamId(providerId);
            String team = allSharedPreferences.fetchDefaultTeam(providerId);
            String locationId = JsonFormUtils.locationId(allSharedPreferences);

            org.smartregister.clientandeventmodel.Event confirmationEvent =
                    (org.smartregister.clientandeventmodel.Event) new org.smartregister.clientandeventmodel.Event()
                            .withBaseEntityId(baseEntityId)
                            .withEventDate(new Date())
                            .withEventType(CONFIRMATION_EVENT_TYPE)
                            .withEntityType(DIABETES_HYPERTENSION_CONFIRMATION)
                            .withProviderId(providerId)
                            .withTeamId(teamId)
                            .withTeam(team)
                            .withLocationId(locationId)
                            .withFormSubmissionId(UUID.randomUUID().toString())
                            .withDateCreated(new Date())
                            .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                            .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION);

            if (details != null) {
                confirmationEvent.setDetails(details);
            }

            if (confirmDiabetes) {
                confirmationEvent.addObs(buildPositiveObs(DIABETES_RESULT_FIELD));
            }

            if (confirmHypertension) {
                confirmationEvent.addObs(buildPositiveObs(HYPERTENSION_RESULT_FIELD));
            }

            NCUtils.addEvent(allSharedPreferences, confirmationEvent);
            NCUtils.startClientProcessing();
        } catch (Exception e) {
            Timber.e(e, "Unable to enqueue diabetes/hypertension confirmation event");
        }
    }

    private static org.smartregister.clientandeventmodel.Obs buildPositiveObs(String field) {
        return (org.smartregister.clientandeventmodel.Obs) new org.smartregister.clientandeventmodel.Obs()
                .withFormSubmissionField(field)
                .withFieldCode(field)
                .withFieldType("formsubmissionField")
                .withFieldDataType("text")
                .withValue(POSITIVE_VALUE)
                .withHumanReadableValues(Collections.singletonList("Positive"));
    }

    private static String extractValue(org.smartregister.domain.Event event, String field) {
        if (event == null || event.getObs() == null) {
            return null;
        }
        for (org.smartregister.domain.Obs obs : event.getObs()) {
            if (field.equals(obs.getFormSubmissionField())) {
                if (obs.getHumanReadableValues() != null && !obs.getHumanReadableValues().isEmpty()) {
                    return obs.getHumanReadableValues().get(0).toString();
                }
                if (obs.getValue() != null) {
                    return obs.getValue().toString();
                }
            }
        }
        return null;
    }

    private static boolean isAffirmative(String answer) {
        if (StringUtils.isBlank(answer)) {
            return false;
        }
        String normalized = answer.trim().toLowerCase(Locale.US);
        return normalized.equals("yes") || normalized.equals("y") || normalized.equals("true")
                || normalized.equals("1") || normalized.equals("ndio") || normalized.equals("ndiyo");
    }
}
