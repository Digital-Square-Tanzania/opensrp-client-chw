package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import org.jeasy.rules.api.Rules;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.model.ChildVisit;
import org.smartregister.chw.core.provider.CoreRegisterProvider;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FamilyRegisterProvider extends CoreRegisterProvider {
    protected final Context context;
    private final View.OnClickListener onClickListener;

    public FamilyRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener);
        this.context = context;
        this.onClickListener = onClickListener;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        super.getView(cursor, client, viewHolder);
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        updateHouseholdRowTitle(pc, viewHolder);
        viewHolder.dueButton.setVisibility(View.GONE);
        String familyBaseEntityId = pc.getCaseId();
        if (updateAsyncTask == null) { //Ensure this task is only called once
            Utils.startAsyncTask(new UpdateAsyncTask(context, viewHolder, familyBaseEntityId), null);
        }
    }

    @Override
    public void updateDueColumn(Context context, RegisterViewHolder viewHolder, ChildVisit childVisit) {

    }

    @Override
    public List<ChildVisit> retrieveChildVisitList(Rules rules, List<Map<String, String>> list) {
        return null;
    }

    protected void setTasksDoneStatus(Context context, Button dueButton) {
        dueButton.setTextColor(context.getResources().getColor(R.color.alert_complete_green));
        dueButton.setText(context.getString(R.string.tasks_done));
        dueButton.setBackgroundColor(context.getResources().getColor(R.color.transparent));
        dueButton.setOnClickListener(null);
    }

    private void setTasksOverdueStatus(Context context, Button dueButton, Integer count) {
        dueButton.setTextColor(context.getResources().getColor(R.color.white));
        dueButton.setText(MessageFormat.format(context.getString(R.string.tasks_status), count));
        dueButton.setBackgroundResource(R.drawable.overdue_red_btn_selector);
        dueButton.setOnClickListener(onClickListener);
    }

    private void setTasksDueStatus(Context context, Button dueButton, Integer count) {
        dueButton.setTextColor(context.getResources().getColor(R.color.alert_in_progress_blue));
        dueButton.setText(MessageFormat.format(context.getString(R.string.tasks_status), count));
        dueButton.setBackgroundResource(R.drawable.blue_btn_selector);
        dueButton.setOnClickListener(onClickListener);
    }

    private void setTaskNotDone(Context context, Button dueButton) {
        dueButton.setTextColor(context.getResources().getColor(R.color.progress_orange));
        dueButton.setText(context.getString(R.string.tasks_not_done));
        dueButton.setBackgroundColor(context.getResources().getColor(R.color.transparent));
        dueButton.setOnClickListener(onClickListener);
    }

    private void updateHouseholdRowTitle(CommonPersonObjectClient pc, RegisterViewHolder viewHolder) {
        if (pc == null || viewHolder == null || viewHolder.patientName == null) {
            return;
        }

        String householdName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
        if (TextUtils.isEmpty(householdName)) {
            return;
        }

        String householdLabel = context.getString(R.string.household_register_row_title, householdName);
        viewHolder.patientName.setText(householdLabel);
    }

    @Override
    public ChildVisit mergeChildVisits(List<ChildVisit> childVisitList) {
        return null;
    }

    @Override
    public String getChildAgeLimitFilter(){
        if(ChwApplication.getApplicationFlavor().showIconsForChildrenUnderTwoAndGirlsAgeNineToEleven()){
          return org.smartregister.chw.util.ChildDBConstants.childDueVaccinesFilterForChildrenBelowTwoAndGirlsAgeNineToEleven();
        }
        else {
            return ChildDBConstants.childAgeLimitFilter();
        }
    }

    private void updateButtonState(Context context, RegisterViewHolder viewHolder, Map<String, Integer> services) {
        if (services != null && !services.isEmpty()) {
            viewHolder.dueButton.setVisibility(View.VISIBLE);
            Integer visits_done = services.get(CoreConstants.VisitType.DONE.name());
            Integer visits_due = services.get(CoreConstants.VisitType.DUE.name());
            Integer visits_not_done = services.get(CoreConstants.VisitType.NOT_VISIT_THIS_MONTH.name());
            Integer visits_over_due = services.get(CoreConstants.VisitType.OVERDUE.name());

            int due = visits_due == null ? 0 : visits_due;
            int over_due = visits_over_due == null ? 0 : visits_over_due;
            if (due > 0 && over_due > 0) {
                over_due = over_due + due;
            }

            //over_due = over_due + due;
            if (over_due > 0) {
                setTasksOverdueStatus(context, viewHolder.dueButton, over_due);
            } else if (due > 0) {
                setTasksDueStatus(context, viewHolder.dueButton, due);
            } else if (visits_done != null && visits_done > 0) {
                setTasksDoneStatus(context, viewHolder.dueButton);
            } else if (visits_not_done != null && visits_not_done > 0) {
                setTaskNotDone(context, viewHolder.dueButton);
            }

        } else {
            viewHolder.dueButton.setVisibility(View.GONE);
        }
    }

    private class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final Context context;
        private final RegisterViewHolder viewHolder;
        private final String familyBaseEntityId;

        private List<Map<String, String>> list;
        private int ancWomanCount;
        private int pncWomanCount;
        private int malariaCount;
        private int fpCount;
        private Map<String, Integer> services;

        private UpdateAsyncTask(Context context, RegisterViewHolder viewHolder, String familyBaseEntityId) {
            this.context = context;
            this.viewHolder = viewHolder;
            this.familyBaseEntityId = familyBaseEntityId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            list = getChildren(familyBaseEntityId);

            if (ChwApplication.getApplicationFlavor().hasANC())
                ancWomanCount = getAncWomenCount(familyBaseEntityId);

            if (ChwApplication.getApplicationFlavor().hasPNC())
                pncWomanCount = getPncWomenCount(familyBaseEntityId);

            if (ChwApplication.getApplicationFlavor().hasMalaria())
                malariaCount = MalariaDao.getMalariaFamilyMembersCount(familyBaseEntityId);

            if (ChwApplication.getApplicationFlavor().hasFamilyPlanning())
                fpCount = FpDao.getFpWomenCount(familyBaseEntityId) != null ? FpDao.getFpWomenCount(familyBaseEntityId) : 0;

            services = getFamilyDueState(familyBaseEntityId);
            return null;
        }

        private Map<String, Integer>  getFamilyDueState(String familyBaseEntityId) {
            if (ChwApplication.getApplicationFlavor().showFamilyServicesScheduleWithChildrenAboveTwo()) {
                return FamilyDao.getFamilyServiceSchedule(familyBaseEntityId);
            } else {
                return FamilyDao.getFamilyServiceScheduleWithChildrenOnlyUnderTwo(familyBaseEntityId);
            }
        }

        @Override
        protected void onPostExecute(Void param) {
            // Update child Icon
            updateChildIcons(viewHolder, list, ancWomanCount, pncWomanCount);
            updateMalariaIcons(viewHolder, malariaCount);
            updateButtonState(context, viewHolder, services);
            updateFpIcons(viewHolder, fpCount);
        }
    }

}
