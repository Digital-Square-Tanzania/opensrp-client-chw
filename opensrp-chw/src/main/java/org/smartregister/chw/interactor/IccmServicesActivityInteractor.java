package org.smartregister.chw.interactor;

import static org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.ICCM_SERVICES_VISIT;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.IccmMedicalHistoryActionHelper;
import org.smartregister.chw.malaria.MalariaLibrary;
import org.smartregister.chw.malaria.contract.BaseIccmVisitContract;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.domain.Visit;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.interactor.BaseIccmVisitInteractor;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.IccmVisitUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by Ilakoze Jumanne on 2023-04-20
 */
public class IccmServicesActivityInteractor extends BaseIccmVisitInteractor {

    private static IccmMemberObject memberObject;

    final LinkedHashMap<String, BaseIccmVisitAction> actionList = new LinkedHashMap<>();

    protected Context context;

    Map<String, List<VisitDetail>> details = null;


    @Override
    public IccmMemberObject getMemberClient(String memberID) {

        return IccmDao.getMember(memberID);
    }

    @Override
    public void calculateActions(BaseIccmVisitContract.View view, IccmMemberObject memberObject, BaseIccmVisitContract.InteractorCallBack callBack) {
        context = view.getContext();
        IccmServicesActivityInteractor.memberObject = memberObject;

        if (view.getEditMode()) {
            Visit lastVisit = MalariaLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), ICCM_SERVICES_VISIT);

            if (lastVisit != null) {
                details = IccmVisitUtils.getVisitGroups(MalariaLibrary.getInstance().visitDetailsRepository().getVisits(lastVisit.getVisitId()));
            }
        }

        final Runnable runnable = () -> {
            // update the local database incase of manual date adjustment
            try {
                IccmVisitUtils.processVisits(memberObject.getBaseEntityId());
            } catch (Exception e) {
                Timber.e(e);
            }

            try {
                evaluateMedicalHistory(callBack);
            } catch (BaseIccmVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateMedicalHistory(BaseIccmVisitContract.InteractorCallBack callBack) throws BaseIccmVisitAction.ValidationException {
        String title = context.getString(R.string.iccm_medical_history);
        IccmMedicalHistoryActionHelper actionHelper = new IccmMedicalHistoryActionHelper(context, memberObject.getIccmEnrollmentFormSubmissionId(), actionList, details, callBack);
        BaseIccmVisitAction action = getBuilder(title).withOptional(false).withHelper(actionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmMedicalHistory()).build();
        actionList.put(title, action);
    }


    private BaseIccmVisitAction.Builder getBuilder(String title) {
        return new BaseIccmVisitAction.Builder(context, title);
    }

    @Override
    protected String getEncounterType() {
        return ICCM_SERVICES_VISIT;
    }

    @Override
    protected void processExternalVisits(Visit visit, Map<String, BaseIccmVisitAction> externalVisits, String memberID) throws Exception {
        //super.processExternalVisits(visit, externalVisits, memberID);
        if (visit != null && !externalVisits.isEmpty()) {
            for (Map.Entry<String, BaseIccmVisitAction> entry : externalVisits.entrySet()) {
                Map<String, BaseIccmVisitAction> subEvent = new HashMap<>();
                subEvent.put(entry.getKey(), entry.getValue());

                String subMemberID = entry.getValue().getBaseEntityID();
                if (StringUtils.isBlank(subMemberID)) subMemberID = memberID;

                submitVisit(false, subMemberID, subEvent, visit.getVisitType());
            }
        }
        try {

            boolean visitCompleted = true;
            for (Map.Entry<String, BaseIccmVisitAction> entry : actionList.entrySet()) {
                String actionStatus = entry.getValue().getActionStatus().toString();
                if (actionStatus.equalsIgnoreCase("PARTIALLY_COMPLETED")) {
                    visitCompleted = false;

                }
            }

            if (visitCompleted) {
                IccmVisitUtils.processVisits(memberObject.getBaseEntityId());
            }
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    public AllSharedPreferences getAllSharedPreferences() {
        return Utils.context().allSharedPreferences();
    }


}
