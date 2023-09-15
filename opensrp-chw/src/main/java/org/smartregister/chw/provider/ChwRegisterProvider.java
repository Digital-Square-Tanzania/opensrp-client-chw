package org.smartregister.chw.provider;

import static org.smartregister.chw.core.utils.Utils.getDuration;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;

import org.jeasy.rules.api.Rules;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.model.ChildVisit;
import org.smartregister.chw.core.provider.CoreRegisterProvider;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.interactor.ChildProfileInteractor;
import org.smartregister.chw.util.ChildUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChwRegisterProvider extends CoreRegisterProvider {

    public ChwRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener);
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        super.getView(cursor, client, viewHolder);
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        String familyBaseEntityId = pc.getCaseId();
        if (updateAsyncTask == null) { //Ensure this task is only called once
            Utils.startAsyncTask(new UpdateAsyncTask(context, viewHolder, familyBaseEntityId), null);
        }
    }

    @Override
    public void updateDueColumn(Context context, RegisterViewHolder viewHolder, ChildVisit childVisit) {
        if (childVisit != null) {
            viewHolder.dueButton.setVisibility(View.VISIBLE);
            if (childVisit.getVisitStatus().equalsIgnoreCase(ChildProfileInteractor.VisitType.DUE.name())) {
                setVisitButtonDueStatus(context, viewHolder.dueButton);
            } else if (childVisit.getVisitStatus().equalsIgnoreCase(ChildProfileInteractor.VisitType.OVERDUE.name())) {
                setVisitButtonOverdueStatus(context, viewHolder.dueButton, childVisit.getNoOfMonthDue());
            } else if (childVisit.getVisitStatus().equalsIgnoreCase(ChildProfileInteractor.VisitType.LESS_TWENTY_FOUR.name())) {
                setVisitLessTwentyFourView(context, viewHolder.dueButton, childVisit.getLastVisitDays());
            } else if (childVisit.getVisitStatus().equalsIgnoreCase(ChildProfileInteractor.VisitType.VISIT_THIS_MONTH.name())) {
                setVisitAboveTwentyFourView(context, viewHolder.dueButton);
            } else if (childVisit.getVisitStatus().equalsIgnoreCase(ChildProfileInteractor.VisitType.NOT_VISIT_THIS_MONTH.name())) {
                setVisitNotDone(context, viewHolder.dueButton);
            }
        } else {
            setVisitButtonDueStatus(context, viewHolder.dueButton);
            viewHolder.dueButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public List<ChildVisit> retrieveChildVisitList(Rules rules, List<Map<String, String>> list) {
        List<ChildVisit> childVisitList = new ArrayList<>();
        for (Map<String, String> map : list) {
            String dobString = getDuration(map.get(DBConstants.KEY.DOB));
            String lastVisitDate = map.get(ChildDBConstants.KEY.LAST_HOME_VISIT);
            String visitNotDone = map.get(ChildDBConstants.KEY.VISIT_NOT_DONE);
            String strDateCreated = map.get(ChildDBConstants.KEY.DATE_CREATED);
            long lastVisit = 0, visitNot = 0, dateCreated = 0;
            if (!TextUtils.isEmpty(lastVisitDate)) {
                lastVisit = Long.valueOf(lastVisitDate);
            }
            if (!TextUtils.isEmpty(visitNotDone)) {
                visitNot = Long.valueOf(visitNotDone);
            }
            if (!TextUtils.isEmpty(strDateCreated)) {
                dateCreated = org.smartregister.family.util.Utils.dobStringToDateTime(strDateCreated).getMillis();
            }
            ChildVisit childVisit = ChildUtils.getChildVisitStatus(context, rules, dobString, lastVisit, visitNot, dateCreated);
            childVisitList.add(childVisit);
        }
        return childVisitList;
    }

    @Override
    public ChildVisit mergeChildVisits(List<ChildVisit> childVisitList) {
        if (childVisitList == null || childVisitList.isEmpty()) {
            return null;
        }

        ChildVisit overdue = null;
        ChildVisit due = null;
        ChildVisit doneLess24 = null;
        ChildVisit done = null;
        ChildVisit notDone = null;

        for (ChildVisit childVisit : childVisitList) {
            String status = childVisit.getVisitStatus();
            if (status.equalsIgnoreCase(ChildProfileInteractor.VisitType.OVERDUE.name())) {
                if (overdue == null || (overdue.getLastVisitTime() > childVisit.getLastVisitTime())) {
                    overdue = childVisit;
                }
            } else if (status.equalsIgnoreCase(ChildProfileInteractor.VisitType.DUE.name())) {
                if (due == null || (due.getLastVisitTime() > childVisit.getLastVisitTime())) {
                    due = childVisit;
                }
            } else if (status.equalsIgnoreCase(ChildProfileInteractor.VisitType.LESS_TWENTY_FOUR.name())) {
                if (doneLess24 == null || (doneLess24.getLastVisitTime() > childVisit.getLastVisitTime())) {
                    doneLess24 = childVisit;
                }
            } else if (status.equalsIgnoreCase(ChildProfileInteractor.VisitType.VISIT_THIS_MONTH.name())) {
                if (done == null || (done.getLastVisitTime() > childVisit.getLastVisitTime())) {
                    done = childVisit;
                }
            } else if (status.equalsIgnoreCase(ChildProfileInteractor.VisitType.NOT_VISIT_THIS_MONTH.name())) {
                if (notDone == null || (notDone.getLastVisitTime() > childVisit.getLastVisitTime())) {
                    notDone = childVisit;
                }
            }
        }

        if (overdue != null) {
            return overdue;
        } else if (due != null) {
            return due;
        } else if (doneLess24 != null) {
            return doneLess24;
        } else if (done != null) {
            return done;
        } else {
            return notDone;
        }
    }
////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final Context context;
        private final RegisterViewHolder viewHolder;
        private final String familyBaseEntityId;

        private final Rules rules;

        private List<Map<String, String>> list;
        private int ancWomanCount;
        private int pncWomanCount;
        private int malariaCount;
        private ChildVisit childVisit;

        private UpdateAsyncTask(Context context, RegisterViewHolder viewHolder, String familyBaseEntityId) {
            this.context = context;
            this.viewHolder = viewHolder;
            this.familyBaseEntityId = familyBaseEntityId;
            this.rules = ChwApplication.getInstance().getRulesEngineHelper().rules(Constants.RULE_FILE.HOME_VISIT);
        }

        @Override
        protected Void doInBackground(Void... params) {
            list = getChildren(familyBaseEntityId);
            ancWomanCount = getAncWomenCount(familyBaseEntityId);
            pncWomanCount = getPncWomenCount(familyBaseEntityId);
            malariaCount = ChwApplication.malariaRegisterRepository().getMalariaCount(familyBaseEntityId, CoreConstants.TABLE_NAME.PNC_MEMBER);
            childVisit = mergeChildVisits(retrieveChildVisitList(rules, list));

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            // Update child Icon
            updateChildIcons(viewHolder, list, ancWomanCount, pncWomanCount);

            // Update due column
            updateDueColumn(context, viewHolder, childVisit);
        }
    }
}