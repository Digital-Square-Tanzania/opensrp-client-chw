package org.smartregister.chw.task;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.contract.ScheduleTask;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.VisitDao;
import org.smartregister.chw.core.domain.BaseScheduleTask;
import org.smartregister.chw.core.rule.AncVisitAlertRule;
import org.smartregister.chw.core.utils.CoreConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ANCVisitScheduler extends BaseTaskExecutor {
    @Override
    public void resetSchedule(String baseEntityID, String scheduleName) {
        super.resetSchedule(baseEntityID, scheduleName);
        ChwApplication.getInstance().getScheduleRepository().deleteScheduleByGroup(getScheduleGroup(), baseEntityID);
    }

    @Override
    public List<ScheduleTask> generateTasks(String baseEntityID, String eventName, Date eventDate) {
        BaseScheduleTask baseScheduleTask = prepareNewTaskObject(baseEntityID);

        // clean the visit db . delete all do and undo events that happened the same day
        List<String> strings = VisitDao.getVisitsToDelete();
        for (String visitID : strings) {
            AncLibrary.getInstance().visitRepository().deleteVisit(visitID);
        }

        Visit lastNotDoneVisit = AncLibrary.getInstance().visitRepository().getLatestVisit(baseEntityID, Constants.EVENT_TYPE.ANC_HOME_VISIT_NOT_DONE);
        if (lastNotDoneVisit != null) {
            Visit lastNotDoneVisitUndo = AncLibrary.getInstance().visitRepository().getLatestVisit(baseEntityID, Constants.EVENT_TYPE.ANC_HOME_VISIT_NOT_DONE_UNDO);
            if (lastNotDoneVisitUndo != null
                    && lastNotDoneVisitUndo.getDate().after(lastNotDoneVisit.getDate())) {
                lastNotDoneVisit = null;
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        Visit lastVisit = AncLibrary.getInstance().visitRepository().getLatestVisit(baseEntityID, Constants.EVENT_TYPE.ANC_HOME_VISIT);
        String visitDate = lastVisit != null ? sdf.format(lastVisit.getDate()) : null;
        String lastVisitNotDone = lastNotDoneVisit != null ? sdf.format(lastNotDoneVisit.getDate()) : null;

        String create_date = AncDao.getAncDateCreated(baseEntityID);
        LocalDate dateCreated = StringUtils.isNotBlank(create_date) ? (new DateTime(create_date)).toLocalDate() : new LocalDate(eventDate);

        AncVisitAlertRule alertRule = new AncVisitAlertRule(
                ChwApplication.getInstance().getApplicationContext()
                , DateTimeFormat.forPattern("dd-MM-yyyy").print(dateCreated),
                visitDate,
                lastVisitNotDone,
                dateCreated
        );


        baseScheduleTask.setScheduleDueDate(alertRule.getDueDate());
        baseScheduleTask.setScheduleNotDoneDate(alertRule.getNotDoneDate());
        baseScheduleTask.setScheduleExpiryDate(alertRule.getExpiryDate());
        baseScheduleTask.setScheduleCompletionDate(alertRule.getCompletionDate());
        baseScheduleTask.setScheduleOverDueDate(alertRule.getOverDueDate());

        return toScheduleList(baseScheduleTask);
    }

    @Override
    public String getScheduleName() {
        return CoreConstants.SCHEDULE_TYPES.ANC_VISIT;
    }

    @Override
    public String getScheduleGroup() {
        return CoreConstants.SCHEDULE_GROUPS.HOME_VISIT;
    }
}
