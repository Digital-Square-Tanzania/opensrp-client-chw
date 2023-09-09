package org.smartregister.chw.task;

import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.contract.ScheduleTask;
import org.smartregister.chw.core.domain.BaseScheduleTask;
import org.smartregister.chw.core.rule.TbFollowupRule;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.HomeVisitUtil;
import org.smartregister.chw.tb.dao.TbDao;
import org.smartregister.chw.tb.domain.TbMemberObject;
import org.smartregister.chw.tb.util.Constants;

import java.util.Date;
import java.util.List;

public class TbVisitScheduler extends BaseTaskExecutor {

    @Override
    public void resetSchedule(String baseEntityID, String scheduleName) {
        super.resetSchedule(baseEntityID, scheduleName);
        ChwApplication.getInstance().getScheduleRepository().deleteScheduleByGroup(getScheduleGroup(), baseEntityID);
    }

    @Override
    public List<ScheduleTask> generateTasks(String baseEntityID, String eventName, Date eventDate) {

        BaseScheduleTask baseScheduleTask = prepareNewTaskObject(baseEntityID);
        TbMemberObject tbMemberObject = TbDao.getMember(baseEntityID);
        Visit lastVisit = TbDao.getLatestVisit(baseEntityID, Constants.EventType.FOLLOW_UP_VISIT);
        Date lastVisitDate = lastVisit != null ? lastVisit.getDate() : null;
        TbFollowupRule tbFollowupRule = HomeVisitUtil.getTbVisitStatus(lastVisitDate, tbMemberObject.getTbRegistrationDate());

        baseScheduleTask.setScheduleDueDate(tbFollowupRule.getDueDate());
        baseScheduleTask.setScheduleExpiryDate(tbFollowupRule.getExpiryDate());
        baseScheduleTask.setScheduleOverDueDate(tbFollowupRule.getOverDueDate());

        return toScheduleList(baseScheduleTask);
    }

    @Override
    public String getScheduleName() {
        return CoreConstants.SCHEDULE_TYPES.TB_VISIT;
    }

    @Override
    public String getScheduleGroup() {
        return CoreConstants.SCHEDULE_GROUPS.HOME_VISIT;
    }
}
