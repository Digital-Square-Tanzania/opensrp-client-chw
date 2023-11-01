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
import org.smartregister.family.util.JsonFormUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmPhysicalExaminationActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper {
    private String jsonPayload;

    private final Context context;

    private final LinkedHashMap<String, BaseIccmVisitAction> actionList;

    private final BaseIccmVisitContract.InteractorCallBack callBack;

    private final Map<String, List<VisitDetail>> details;

    private final HashMap<String, Boolean> checkObject = new HashMap<>();

    private String isMalariaSuspectString;

    private final String isDiarrheaSuspect;

    private final String isPneumoniaSuspect;

    private final boolean hasAnySymptom;

    private final IccmMemberObject memberObject;

    public IccmPhysicalExaminationActionHelper(Context context, String enrollmentFormSubmissionId, LinkedHashMap<String, BaseIccmVisitAction> actionList, Map<String, List<VisitDetail>> details, BaseIccmVisitContract.InteractorCallBack callBack, String isMalariaSuspect, String isDiarrheaSuspect, String isPneumoniaSuspect, boolean hasAnySymptom) {
        this.context = context;
        this.actionList = actionList;
        this.callBack = callBack;
        this.details = details;
        this.isMalariaSuspectString = isMalariaSuspect;
        this.isDiarrheaSuspect = isDiarrheaSuspect;
        this.isPneumoniaSuspect = isPneumoniaSuspect;
        this.hasAnySymptom = hasAnySymptom;

        memberObject = IccmDao.getMember(enrollmentFormSubmissionId);
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
            physicalExaminationActionJsonPayloadObject.getJSONObject("global").put("is_diarrhea_suspect", isDiarrheaSuspect);
            physicalExaminationActionJsonPayloadObject.getJSONObject("global").put("is_pneumonia_suspect", isPneumoniaSuspect);
            physicalExaminationActionJsonPayloadObject.getJSONObject("global").put("has_any_symptom", hasAnySymptom);
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
            JSONArray fields = JsonFormUtils.fields(jsonObject);

            JSONObject physicalExaminationCompletionStatus = JsonFormUtils.getFieldJSONObject(fields, "physical_examination_completion_status");
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

        int age = getAgeFromDate(memberObject.getAge());
        if (age > 6) {
            String malariaActionTitle = context.getString(R.string.iccm_malaria);
            if (isMalariaSuspectString.equalsIgnoreCase("true")) {
                try {
                    IccmMalariaActionHelper actionHelper = new IccmMalariaActionHelper(memberObject.getIccmEnrollmentFormSubmissionId());
                    BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, malariaActionTitle).withOptional(true).withHelper(actionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmMalaria()).build();
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
            if ((memberObject.getRespiratoryRate() != null && ((age < 1 && memberObject.getRespiratoryRate() >= 50) || (age >= 1 && age < 5 && memberObject.getRespiratoryRate() >= 40))) || (isPneumoniaSuspect.equalsIgnoreCase("true") && getAgeFromDate(memberObject.getAge()) < 6)) {
                try {
                    String title = context.getString(R.string.iccm_pneumonia);
                    IccmPneumoniaActionHelper pneumoniaActionHelper = new IccmPneumoniaActionHelper(context, memberObject.getIccmEnrollmentFormSubmissionId(), actionList, details, callBack, isDiarrheaSuspect, isMalariaSuspectString);
                    BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title).withOptional(true).withHelper(pneumoniaActionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmPneumonia()).build();
                    actionList.put(title, action);
                } catch (Exception e) {
                    Timber.e(e);
                }
            } else if (isDiarrheaSuspect.equalsIgnoreCase("true") && getAgeFromDate(memberObject.getAge()) < 5) {
                actionList.remove(context.getString(R.string.iccm_pneumonia));
                try {
                    String title = context.getString(R.string.iccm_diarrhea);
                    IccmDiarrheaActionHelper diarrheaActionHelper = new IccmDiarrheaActionHelper(context, memberObject.getIccmEnrollmentFormSubmissionId(), actionList, details, callBack, isMalariaSuspectString);
                    BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title).withOptional(true).withHelper(diarrheaActionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmDiarrhea()).build();
                    actionList.put(title, action);
                } catch (Exception e) {
                    Timber.e(e);
                }
            } else if (isMalariaSuspectString.equalsIgnoreCase("true")) {
                actionList.remove(context.getString(R.string.iccm_pneumonia));
                actionList.remove(context.getString(R.string.iccm_diarrhea));
                String malariaActionTitle = context.getString(R.string.iccm_malaria);
                try {
                    IccmMalariaActionHelper actionHelper = new IccmMalariaActionHelper(memberObject.getIccmEnrollmentFormSubmissionId());
                    BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, malariaActionTitle).withOptional(true).withHelper(actionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmMalaria()).build();
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
