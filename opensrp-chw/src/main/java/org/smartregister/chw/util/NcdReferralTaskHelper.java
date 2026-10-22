package org.smartregister.chw.util;

import org.smartregister.chw.dao.NcdCaseManagementDao;
import org.smartregister.chw.model.NcdReferralInputs;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Decides whether an NCD case-management visit should raise a referral, and delegates the actual
 * event + task construction to {@link ReferralTaskFactory}.
 *
 * <p>What is NCD-specific and stays here: the red/yellow alert gate, the urgent vs. non-emergency
 * task code and focus, the generic problem-key fallback, and the
 * {@link NcdCaseManagementDao#hasOpenReferral} deduplication that stops a new referral being
 * created while a previous one is still open.
 */
public class NcdReferralTaskHelper {

    private NcdReferralTaskHelper() {
        // utility class
    }

    /**
     * Backward-compatible overload that creates a referral with no emergency/treatment-supporter
     * details captured. Equivalent to passing {@code null} inputs.
     */
    public static boolean createReferralIfNeeded(String baseEntityId,
                                                  String triggeringFormSubmissionId,
                                                  String alertStatus, String description,
                                                  List<String> problemKeys,
                                                  List<String> problemHumanReadableValues) {
        return createReferralIfNeeded(baseEntityId, triggeringFormSubmissionId, alertStatus,
                description, problemKeys, problemHumanReadableValues, null);
    }

    /**
     * Creates a Referral Registration event and a linked task if no open referral exists.
     *
     * @param baseEntityId client ID
     * @param triggeringFormSubmissionId case-management visit form submission; optional context
     * @param alertStatus "red" or "yellow"
     * @param description human-readable referral reason kept on the task
     * @param problemKeys coded concept keys persisted as the "problem" obs values
     * @param problemHumanReadableValues labels persisted as matching obs humanReadableValues
     * @param inputs emergency-case and treatment-supporter prompt values; may be {@code null}
     * @return true if a referral was created, false if skipped
     */
    public static boolean createReferralIfNeeded(String baseEntityId,
                                                  String triggeringFormSubmissionId,
                                                  String alertStatus, String description,
                                                  List<String> problemKeys,
                                                  List<String> problemHumanReadableValues,
                                                  NcdReferralInputs inputs) {
        if (!"red".equals(alertStatus) && !"yellow".equals(alertStatus)) {
            return false;
        }

        if (NcdCaseManagementDao.hasOpenReferral(baseEntityId)) {
            Timber.d("NCD referral skipped; open referral already exists for %s", baseEntityId);
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

        return ReferralTaskFactory.createReferral(baseEntityId, focus, taskCode, priority,
                description, problemValues, problemReadable, inputs);
    }

    @androidx.annotation.VisibleForTesting
    static void addReferralInputObs(Event event, NcdReferralInputs inputs) {
        ReferralTaskFactory.addReferralInputObs(event, inputs);
    }

    @androidx.annotation.VisibleForTesting
    static Obs buildReferralHfObs(NcdReferralInputs inputs, String fallbackLocationId) {
        return ReferralTaskFactory.buildReferralHfObs(inputs, fallbackLocationId);
    }

    @androidx.annotation.VisibleForTesting
    static String resolveGroupIdentifier(String referralFacilityId, String fallbackLocalityId) {
        return ReferralTaskFactory.resolveGroupIdentifier(referralFacilityId, fallbackLocalityId);
    }
}
