package org.smartregister.chw.actionhelper;

import static com.vijay.jsonwizard.constants.JsonFormConstants.TYPE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient;
import static org.smartregister.chw.core.utils.Utils.isMemberOfReproductiveAge;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.OPTIONS;
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
import org.smartregister.chw.util.Utils;
import org.smartregister.family.util.DBConstants;
import org.smartregister.util.JsonFormUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmMedicalHistoryActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper {
    private String jsonPayload;

    private final String enrollmentFormSubmissionId;

    private final Context context;

    private final LinkedHashMap<String, BaseIccmVisitAction> actionList;

    private final BaseIccmVisitContract.InteractorCallBack callBack;

    private final Map<String, List<VisitDetail>> details;

    private final HashMap<String, Boolean> checkObject = new HashMap<>();

    private IccmMemberObject memberObject;

    public IccmMedicalHistoryActionHelper(Context context, String enrollmentFormSubmissionId, LinkedHashMap<String, BaseIccmVisitAction> actionList, Map<String, List<VisitDetail>> details, BaseIccmVisitContract.InteractorCallBack callBack) {
        this.context = context;
        this.enrollmentFormSubmissionId = enrollmentFormSubmissionId;
        this.actionList = actionList;
        this.callBack = callBack;
        this.details = details;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
        this.memberObject = IccmDao.getMember(enrollmentFormSubmissionId);
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = jsonObject.getJSONObject(Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);

            JSONObject clientBaseEntityId = JsonFormUtils.getFieldJSONObject(fields, "iccm_enrollment_form_submission_id");
            if (clientBaseEntityId != null) {
                clientBaseEntityId.put(VALUE, memberObject.getIccmEnrollmentFormSubmissionId());
            }

            if (memberObject.getTemperature() > 37.5) {
                JSONObject medicalHistory = JsonFormUtils.getFieldJSONObject(fields, "medical_history");
                medicalHistory.getJSONArray(OPTIONS).getJSONObject(0).put(VALUE, true);
            }

            int age = getAgeFromDate(memberObject.getAge());
            JSONObject medicalHistory = JsonFormUtils.getFieldJSONObject(fields, "is_pneumonia_suspect");
            if (medicalHistory != null) {
                medicalHistory.put(VALUE, memberObject.getRespiratoryRate() != null && ((age < 1 && memberObject.getRespiratoryRate() >= 50) || (age >= 1 && age < 5 && memberObject.getRespiratoryRate() >= 40)));
            }


            boolean isFemaleOfReproductiveAge = isMemberOfReproductiveAge(getCommonPersonObjectClient(memberObject.getBaseEntityId()), 10, 49) && Utils.getValue(getCommonPersonObjectClient(enrollmentFormSubmissionId).getColumnmaps(), DBConstants.KEY.GENDER, false).equalsIgnoreCase("Female");
            if (!isFemaleOfReproductiveAge) {
                JSONObject isTheClientPregnant = JsonFormUtils.getFieldJSONObject(fields, "is_the_client_pregnant");
                if (isTheClientPregnant != null) {
                    isTheClientPregnant.put(TYPE, "hidden");
                }
            }

            if (getAgeFromDate(memberObject.getAge()) > 5) {
                JSONObject promptForDiagnosingDiarrhea = JsonFormUtils.getFieldJSONObject(fields, "prompt_for_diagnosing_diarrhea");
                if (promptForDiagnosingDiarrhea != null) {
                    promptForDiagnosingDiarrhea.put(TYPE, "hidden");
                }

                JSONObject promptForDiagnosingPneumonia = JsonFormUtils.getFieldJSONObject(fields, "prompt_for_diagnosing_pneumonia");
                if (promptForDiagnosingPneumonia != null) {
                    promptForDiagnosingPneumonia.put(TYPE, "hidden");
                }
            }

            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            checkObject.clear();
            JSONObject jsonObject = new JSONObject(jsonPayload);
            String medicalHistory = CoreJsonFormUtils.getValue(jsonObject, "medical_history");
            checkObject.put("medical_history", StringUtils.isNotBlank(medicalHistory));
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
        String isMalariaSuspect = "false";
        String clientPastMalariaTreatmentHistory = "";
        String isDiarrheaSuspect = "false";
        String isPneumoniaSuspect = "false";
        try {
            jsonObject = new JSONObject(jsonPayload);

            isDiarrheaSuspect = CoreJsonFormUtils.getValue(jsonObject, "is_diarrhea_suspect");
            isPneumoniaSuspect = CoreJsonFormUtils.getValue(jsonObject, "is_pneumonia_suspect");

            JSONArray fields = org.smartregister.family.util.JsonFormUtils.fields(jsonObject);
            JSONObject medicalHistoryCompletionStatus = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "medical_history_completion_status");
            assert medicalHistoryCompletionStatus != null;
            medicalHistoryCompletionStatus.put(VALUE, IccmVisitUtils.getActionStatus(checkObject));
            isMalariaSuspect = CoreJsonFormUtils.getValue(jsonObject, "is_malaria_suspect");
            clientPastMalariaTreatmentHistory = CoreJsonFormUtils.getValue(jsonObject, "client_past_malaria_treatment_history");
        } catch (JSONException e) {
            Timber.e(e);
        }

        try {
            if (!clientPastMalariaTreatmentHistory.equalsIgnoreCase("yes")) {
                String title = context.getString(R.string.iccm_physical_examination);
                IccmPhysicalExaminationActionHelper actionHelper = new IccmPhysicalExaminationActionHelper(context, enrollmentFormSubmissionId, actionList, details, callBack, isMalariaSuspect, isDiarrheaSuspect, isPneumoniaSuspect, !CoreJsonFormUtils.getValue(jsonObject, "medical_history").contains("none"));
                BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title).withOptional(true).withHelper(actionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmPhysicalExamination()).build();
                actionList.put(title, action);
            } else {
                actionList.remove(context.getString(R.string.iccm_physical_examination));
                isMalariaSuspect = "false";
                int age = getAgeFromDate(memberObject.getAge());
                if (age < 5) {
                    if (memberObject.getRespiratoryRate() != null && (age < 1 && memberObject.getRespiratoryRate() >= 50 || age >= 1 && memberObject.getRespiratoryRate() >= 40) || isPneumoniaSuspect.equalsIgnoreCase("true")) {
                        processPneumoniaAction(jsonObject, isMalariaSuspect);
                    } else if (isDiarrheaSuspect.equalsIgnoreCase("true")) {
                        actionList.remove(context.getString(R.string.iccm_pneumonia));
                        processDiarrheaAction(isMalariaSuspect);
                    } else {
                        actionList.remove(context.getString(R.string.iccm_pneumonia));
                        actionList.remove(context.getString(R.string.iccm_diarrhea));
                    }
                } else {
                    actionList.remove(context.getString(R.string.iccm_malaria));
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        //Calling the callback method to preload the actions in the actions list.
        new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));

        if (jsonObject != null) {
            return jsonObject.toString();
        }
        return null;
    }

    private void processPneumoniaAction(JSONObject jsonObject, String isMalariaSuspect) {
        try {
            String title = context.getString(R.string.iccm_pneumonia);
            IccmPneumoniaActionHelper pneumoniaActionHelper = new IccmPneumoniaActionHelper(context, memberObject.getIccmEnrollmentFormSubmissionId(), actionList, details, callBack, CoreJsonFormUtils.getValue(jsonObject, "is_diarrhea_suspect"), isMalariaSuspect);
            BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title).withOptional(true).withHelper(pneumoniaActionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmPneumonia()).build();
            if (!actionList.containsKey(context.getString(R.string.iccm_pneumonia)))
                actionList.put(title, action);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void processDiarrheaAction(String isMalariaSuspect) {
        actionList.remove(context.getString(R.string.iccm_pneumonia));
        try {
            String title = context.getString(R.string.iccm_diarrhea);
            IccmDiarrheaActionHelper diarrheaActionHelper = new IccmDiarrheaActionHelper(context, memberObject.getIccmEnrollmentFormSubmissionId(), actionList, details, callBack, isMalariaSuspect);
            BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title).withOptional(true).withHelper(diarrheaActionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmDiarrhea()).build();
            if (!actionList.containsKey(context.getString(R.string.iccm_diarrhea)))
                actionList.put(title, action);
        } catch (Exception e) {
            Timber.e(e);
        }
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
