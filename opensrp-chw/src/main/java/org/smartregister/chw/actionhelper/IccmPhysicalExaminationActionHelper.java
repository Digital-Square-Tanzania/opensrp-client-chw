package org.smartregister.chw.actionhelper;

import static org.smartregister.util.Utils.getAgeFromDate;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.ld.util.AppExecutors;
import org.smartregister.chw.malaria.contract.BaseIccmVisitContract;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.IccmVisitUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmPhysicalExaminationActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper {
    private String jsonPayload;
    private final String baseEntityId;
    private final Context context;
    private final LinkedHashMap<String, BaseIccmVisitAction> actionList;
    private final BaseIccmVisitContract.InteractorCallBack callBack;
    private final boolean isEdit;
    private final Map<String, List<VisitDetail>> details;
    private final HashMap<String, Boolean> checkObject = new HashMap<>();

    private String isMalariaSuspectString;
    private final String isDiarrheaSuspect;
    private final String isPneumoniaSuspect;

    public IccmPhysicalExaminationActionHelper(Context context, String baseEntityId, LinkedHashMap<String, BaseIccmVisitAction> actionList, Map<String, List<VisitDetail>> details, BaseIccmVisitContract.InteractorCallBack callBack, boolean isEdit, String isMalariaSuspect, String isDiarrheaSuspect, String isPneumoniaSuspect) {
        this.context = context;
        this.baseEntityId = baseEntityId;
        this.actionList = actionList;
        this.isEdit = isEdit;
        this.callBack = callBack;
        this.details = details;
        this.isMalariaSuspectString = isMalariaSuspect;
        this.isDiarrheaSuspect = isDiarrheaSuspect;
        this.isPneumoniaSuspect = isPneumoniaSuspect;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject physicalExaminationActionJsonPayloadObject = new JSONObject(jsonPayload);
            physicalExaminationActionJsonPayloadObject.getJSONObject("global").put("is_malaria_suspect", isMalariaSuspectString);
            return physicalExaminationActionJsonPayloadObject.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }

        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            checkObject.clear();
            JSONObject jsonObject = new JSONObject(jsonPayload);
            String physicalExamination = CoreJsonFormUtils.getValue(jsonObject, "physical_examination");
            checkObject.put("physical_examination", StringUtils.isNotBlank(physicalExamination));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BaseIccmVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String jsonPayload) {
        JSONObject jsonObject = null;
        String isMalariaSuspectAfterPhysicalExamination = "false";
        String clientPastMalariaTreatmentHistory = "";
        try {
            jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = org.smartregister.family.util.JsonFormUtils.fields(jsonObject);

            JSONObject physicalExaminationCompletionStatus = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "physical_examination_completion_status");
            assert physicalExaminationCompletionStatus != null;
            physicalExaminationCompletionStatus.put(JsonFormConstants.VALUE, IccmVisitUtils.getActionStatus(checkObject));

            isMalariaSuspectAfterPhysicalExamination = CoreJsonFormUtils.getValue(jsonObject, "is_malaria_suspect_after_physical_examination");
            clientPastMalariaTreatmentHistory = CoreJsonFormUtils.getValue(jsonObject, "client_past_malaria_treatment_history");
        } catch (JSONException e) {
            Timber.e(e);
        }

        if (isMalariaSuspectString.equalsIgnoreCase("true") || (isMalariaSuspectAfterPhysicalExamination.equalsIgnoreCase("true") && (StringUtils.isBlank(clientPastMalariaTreatmentHistory) || !clientPastMalariaTreatmentHistory.equalsIgnoreCase("yes")))) {
            isMalariaSuspectString = "true";
        }

        if (StringUtils.isBlank(clientPastMalariaTreatmentHistory) || !clientPastMalariaTreatmentHistory.equalsIgnoreCase("yes")) {
            IccmMemberObject memberObject = IccmDao.getMember(baseEntityId);
            int age = getAgeFromDate(memberObject.getAge());
            if (age > 6) {
                String malariaActionTitle = context.getString(R.string.iccm_malaria);
                if (isMalariaSuspectString.equalsIgnoreCase("true")) {
                    try {
                        IccmMalariaActionHelper actionHelper = new IccmMalariaActionHelper(context, baseEntityId, isEdit);
                        BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, malariaActionTitle).withOptional(true).withHelper(actionHelper).withDetails(details).withBaseEntityID(baseEntityId).withFormName(Constants.JsonForm.getIccmMalaria()).build();
                        if (!actionList.containsKey(malariaActionTitle))
                            actionList.put(malariaActionTitle, action);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                } else {
                    //Removing the malaria actions  the client is not a malaria suspect.
                    actionList.remove(context.getString(R.string.iccm_malaria));
                }
            } else {
                if ((memberObject.getRespiratoryRate() != null && ((age < 1 && memberObject.getRespiratoryRate() >= 50) || (age >= 1 && age < 6 && memberObject.getRespiratoryRate() >= 40))) || (isPneumoniaSuspect.equalsIgnoreCase("true") && getAgeFromDate(IccmDao.getMember(baseEntityId).getAge()) < 6)) {
                    try {
                        String title = context.getString(R.string.iccm_pneumonia);
                        IccmPneumoniaActionHelper pneumoniaActionHelper = new IccmPneumoniaActionHelper(context, baseEntityId, actionList, details, callBack, isEdit, isDiarrheaSuspect, isMalariaSuspectString);
                        BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title).withOptional(true).withHelper(pneumoniaActionHelper).withDetails(details).withBaseEntityID(baseEntityId).withFormName(Constants.JsonForm.getIccmPneumonia()).build();
                        actionList.put(title, action);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                } else if (isDiarrheaSuspect.equalsIgnoreCase("true") && getAgeFromDate(IccmDao.getMember(baseEntityId).getAge()) < 6) {
                    actionList.remove(context.getString(R.string.iccm_pneumonia));
                    try {
                        String title = context.getString(R.string.iccm_diarrhea);
                        IccmDiarrheaActionHelper diarrheaActionHelper = new IccmDiarrheaActionHelper(context, baseEntityId, actionList, details, callBack, isEdit, isMalariaSuspectString);
                        BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title).withOptional(true).withHelper(diarrheaActionHelper).withDetails(details).withBaseEntityID(baseEntityId).withFormName(Constants.JsonForm.getIccmDiarrhea()).build();
                        actionList.put(title, action);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                } else if (isMalariaSuspectString.equalsIgnoreCase("true")) {
                    actionList.remove(context.getString(R.string.iccm_pneumonia));
                    actionList.remove(context.getString(R.string.iccm_diarrhea));

                    String malariaActionTitle = context.getString(R.string.iccm_malaria);
                    try {
                        IccmMalariaActionHelper actionHelper = new IccmMalariaActionHelper(context, baseEntityId, isEdit);
                        BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, malariaActionTitle).withOptional(true).withHelper(actionHelper).withDetails(details).withBaseEntityID(baseEntityId).withFormName(Constants.JsonForm.getIccmMalaria()).build();
                        if (!actionList.containsKey(malariaActionTitle))
                            actionList.put(malariaActionTitle, action);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                } else {
                    actionList.remove(context.getString(R.string.iccm_malaria));
                    actionList.remove(context.getString(R.string.iccm_pneumonia));
                    actionList.remove(context.getString(R.string.iccm_diarrhea));
                }
            }
        } else {
            actionList.remove(context.getString(R.string.iccm_pneumonia));
            actionList.remove(context.getString(R.string.iccm_diarrhea));
            actionList.remove(context.getString(R.string.iccm_malaria));
        }

        if (!isMalariaSuspectString.equalsIgnoreCase("true")) {
            actionList.remove(context.getString(R.string.iccm_malaria));
        }

        //Calling the callback method to preload the actions in the actions list.
        new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));

        if (jsonObject != null) {
            return jsonObject.toString();
        }
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseIccmVisitAction.Status evaluateStatusOnPayload() {
        String status = IccmVisitUtils.getActionStatus(checkObject);
        if (status.equalsIgnoreCase(IccmVisitUtils.Complete)) {
            return BaseIccmVisitAction.Status.COMPLETED;
        }
        if (status.equalsIgnoreCase(IccmVisitUtils.Ongoing)) {
            return BaseIccmVisitAction.Status.PARTIALLY_COMPLETED;
        }
        return BaseIccmVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseIccmVisitAction baseIccmVisitAction) {
        //overridden
    }
}
