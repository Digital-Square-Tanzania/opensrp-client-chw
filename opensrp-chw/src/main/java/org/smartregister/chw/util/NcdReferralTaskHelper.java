package org.smartregister.chw.util;

import org.joda.time.DateTime;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.NcdCaseManagementDao;
import org.smartregister.domain.Task;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;

import java.util.UUID;

import timber.log.Timber;

/**
 * Creates NCD referral tasks in the local task store following the pattern
 * from ReferralUtils.createReferralTask(). Handles deduplication via
 * NcdCaseManagementDao.hasOpenReferral().
 */
public class NcdReferralTaskHelper {

    private NcdReferralTaskHelper() {
        // utility class
    }

    /**
     * Creates a referral task for the given alert status if no open referral exists.
     *
     * @param baseEntityId     client ID
     * @param formSubmissionId form submission that triggered this referral
     * @param alertStatus      "red" or "yellow"
     * @param description      human-readable description of the referral reason
     * @return true if a task was created, false if skipped (open referral exists or alert is none)
     */
    public static boolean createReferralIfNeeded(String baseEntityId, String formSubmissionId,
                                                  String alertStatus, String description) {
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

        createTask(baseEntityId, formSubmissionId, taskCode, focus, description, priority);
        return true;
    }

    private static void createTask(String baseEntityId, String formSubmissionId,
                                   String taskCode, String focus, String description, int priority) {
        AllSharedPreferences sharedPreferences = Utils.getAllSharedPreferences();

        Task task = new Task();
        task.setIdentifier(UUID.randomUUID().toString());
        task.setPlanIdentifier(CoreConstants.REFERRAL_PLAN_ID);
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
        task.setReasonReference(formSubmissionId);
        task.setRequester(
                sharedPreferences.getANMPreferredName(sharedPreferences.fetchRegisteredANM()));
        task.setLocation(
                sharedPreferences.fetchUserLocalityId(sharedPreferences.fetchRegisteredANM()));

        CoreChwApplication.getInstance().getTaskRepository().addOrUpdate(task);
        Timber.d("NCD referral task created: code=%s, entity=%s", taskCode, baseEntityId);
    }
}
