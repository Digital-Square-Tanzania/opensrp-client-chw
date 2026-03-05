package org.smartregister.chw.interactor;

import static org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.ICCM_SERVICES_VISIT;

import android.content.Context;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.IccmMedicalHistoryActionHelper;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
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
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.chw.util.ReferralUtils;
import org.smartregister.repository.AllSharedPreferences;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    protected void submitVisit(boolean editMode, String memberID, Map<String, BaseIccmVisitAction> map, String parentEventType) throws Exception {
        try {
            sendIccmReferral(editMode, memberID, map);
        } catch (Exception e) {
            Timber.e(e);
        }
        super.submitVisit(editMode, memberID, map, parentEventType);
    }

    protected void sendIccmReferral(boolean editMode, String memberID, Map<String, BaseIccmVisitAction> map) throws Exception {
        if (editMode || map == null || context == null) {
            return;
        }

        BaseIccmVisitAction referralAction = map.get(context.getString(R.string.iccm_referral));
        if (referralAction == null) {
            return;
        }

        String referralFormPayload = referralAction.getJsonPayload();
        if (StringUtils.isBlank(referralFormPayload)) {
            return;
        }

        JSONObject referralFormJson = new JSONObject(referralFormPayload);

        JSONObject appointmentDateJsonObject = CoreJsonFormUtils.getJsonField(referralFormJson, "step1", "referral_appointment_date");
        String appointmentDateFieldValue = CoreJsonFormUtils.getValue(referralFormJson, "referral_appointment_date");
        String normalizedDateValue = appointmentDateFieldValue != null ? appointmentDateFieldValue.trim() : "";
        Long epochMillis = normalizedDateValue.isEmpty() ? null : parseDateToEpochMillis(normalizedDateValue);
        if (appointmentDateJsonObject != null && epochMillis != null){
            appointmentDateJsonObject.put("value", String.valueOf(epochMillis));
        }

        String referralProblems = JsonFormUtils.getCheckBoxValue(referralFormJson, "problem");

        ReferralUtils.processReferral(referralFormJson.toString(), memberID, CoreConstants.TASKS_FOCUS.ICCM_REFERRAL, referralProblems);
        notifyReferralSubmitted();
    }

    private Long parseDateToEpochMillis(String dateValue) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            format.setLenient(false);
            java.util.Date date = format.parse(dateValue);
            return date != null ? date.getTime() : null;
        } catch (Exception e) {
            return null;
        }
    }

    protected void notifyReferralSubmitted() {
        if (context == null) {
            return;
        }
        appExecutors.mainThread().execute(() ->
                Toast.makeText(context, R.string.referral_submitted, Toast.LENGTH_LONG).show());
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
        return org.smartregister.family.util.Utils.context().allSharedPreferences();
    }

}
