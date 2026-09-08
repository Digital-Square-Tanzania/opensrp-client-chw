package org.smartregister.chw.task;

import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.contract.ScheduleTask;
import org.smartregister.chw.core.domain.BaseScheduleTask;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.NcdCaseManagementDao;
import org.smartregister.chw.rule.NcdCaseManagementFollowupRule;
import org.smartregister.chw.util.Constants;

import java.util.Date;
import java.util.List;

/**
 * Scheduler for NCD Case Management monthly follow-up visits.
 * Follows the TB/HIV scheduler pattern: queries DAO for confirmation and last visit dates,
 * applies followup rule to compute due/overdue/expiry, writes schedule to repository.
 */
public class NcdCaseManagementVisitScheduler extends BaseTaskExecutor {

    @Override
    public void resetSchedule(String baseEntityID, String scheduleName) {
        super.resetSchedule(baseEntityID, scheduleName);
        ChwApplication.getInstance().getScheduleRepository().deleteScheduleByGroup(getScheduleGroup(), baseEntityID);
    }

    @Override
    public List<ScheduleTask> generateTasks(String baseEntityID, String eventName, Date eventDate) {
        BaseScheduleTask baseScheduleTask = prepareNewTaskObject(baseEntityID);

        Date confirmationDate = NcdCaseManagementDao.getConfirmationDate(baseEntityID);
        Date lastFollowUpDate = NcdCaseManagementDao.getLastFollowUpDate(baseEntityID);

        NcdCaseManagementFollowupRule followupRule =
                new NcdCaseManagementFollowupRule(confirmationDate, lastFollowUpDate);

        baseScheduleTask.setScheduleDueDate(followupRule.getDueDate());
        baseScheduleTask.setScheduleOverDueDate(followupRule.getOverDueDate());
        baseScheduleTask.setScheduleExpiryDate(followupRule.getExpiryDate());

        return toScheduleList(baseScheduleTask);
    }

    @Override
    public String getScheduleName() {
        return Constants.ScheduleType.NCD_CASE_MANAGEMENT_VISIT;
    }

    @Override
    public String getScheduleGroup() {
        return CoreConstants.SCHEDULE_GROUPS.HOME_VISIT;
    }
}
