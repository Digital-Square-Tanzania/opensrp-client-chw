package org.smartregister.chw.task;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.holders.RegisterViewHolder;
import org.smartregister.chw.core.task.UpdateLastAsyncTask;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.ChwChildDao;
import org.smartregister.commonregistry.CommonRepository;

public class ChwUpdateLastAsyncTask extends UpdateLastAsyncTask {
    public ChwUpdateLastAsyncTask(Context context, CommonRepository commonRepository, RegisterViewHolder viewHolder, String baseEntityId, View.OnClickListener onClickListener) {
        super(context, commonRepository, viewHolder, baseEntityId, onClickListener);
    }

    private void setDueState() {
        if (ChwChildDao.hasDueTodayVaccines(baseEntityId)  || ChwChildDao.hasDueAlerts(baseEntityId)) {
            setVisitButtonDueStatus(context, viewHolder.dueButton);
        } else {
            setVisitButtonNoDueStatus(viewHolder.dueButton);
        }
    }

    @Override
    protected void onPostExecute(Void param) {
        if (!ChwApplication.getApplicationFlavor().showNoDueVaccineView()) {
            super.onPostExecute(param);
        } else {
            if (commonPersonObject != null) {
                viewHolder.dueButton.setVisibility(View.VISIBLE);
                if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.DUE.name())) {
                    setDueState();
                } else if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.OVERDUE.name())) {
                    setVisitButtonOverdueStatus(context, viewHolder.dueButton, childVisit.getNoOfMonthDue());
                } else if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.LESS_TWENTY_FOUR.name())) {
                    setVisitLessTwentyFourView(context, viewHolder.dueButton);
                } else if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.VISIT_THIS_MONTH.name())) {
                    setVisitAboveTwentyFourView(context, viewHolder.dueButton);
                } else if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.NOT_VISIT_THIS_MONTH.name())) {
                    setVisitNotDone(context, viewHolder.dueButton);
                }
            } else {
                viewHolder.dueButton.setVisibility(View.GONE);
            }
        }
    }

    private void setVisitButtonNoDueStatus(Button dueButton) {
        dueButton.setBackgroundResource(R.drawable.transparent_white_button);
        dueButton.setOnClickListener(null);
    }
}
