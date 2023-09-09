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

public class IccmPneumoniaActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper {
    private final Context context;

    private final String isDiarrheaSuspect;

    private final String isMalariaSuspect;

    private final HashMap<String, Boolean> checkObject = new HashMap<>();

    private final LinkedHashMap<String, BaseIccmVisitAction> actionList;

    private final BaseIccmVisitContract.InteractorCallBack callBack;

    private final Map<String, List<VisitDetail>> details;

    private final IccmMemberObject memberObject;

    private String jsonPayload;

    public IccmPneumoniaActionHelper(Context context, String iccmEnromentFormSubmissionId, LinkedHashMap<String, BaseIccmVisitAction> actionList, Map<String, List<VisitDetail>> details, BaseIccmVisitContract.InteractorCallBack callBack, String isDiarrheaSuspect, String isMalariaSuspect) {
        this.context = context;
        this.actionList = actionList;
        this.details = details;
        this.callBack = callBack;
        this.isDiarrheaSuspect = isDiarrheaSuspect;
        this.isMalariaSuspect = isMalariaSuspect;
        memberObject = IccmDao.getMember(iccmEnromentFormSubmissionId);
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            jsonObject.getJSONObject("global").put("weight", memberObject.getWeight());

            int age = getAgeFromDate(memberObject.getAge());
            if (memberObject.getRespiratoryRate() != null && ((age < 1 && memberObject.getRespiratoryRate() >= 50) || (age >= 1 && age < 5 && memberObject.getRespiratoryRate() >= 40))) {
                JSONArray fields = jsonObject.getJSONObject(Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
                JSONObject pneumoniaSignsField = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "pneumonia_signs");
                JSONArray options;
                if (pneumoniaSignsField != null) {
                    options = pneumoniaSignsField.getJSONArray("options");
                    options.remove(options.length() - 1);
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
            String pneumoniaSigns = CoreJsonFormUtils.getValue(jsonObject, "pneumonia_signs");
            checkObject.put("pneumonia_signs", StringUtils.isNotBlank(pneumoniaSigns));
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
        try {
            jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = JsonFormUtils.fields(jsonObject);

            JSONObject pneumoniaCompletionStatus = JsonFormUtils.getFieldJSONObject(fields, "pneumonia_completion_status");
            assert pneumoniaCompletionStatus != null;
            pneumoniaCompletionStatus.put(JsonFormConstants.VALUE, IccmVisitUtils.getActionStatus(checkObject));

        } catch (Exception e) {
            Timber.e(e);
        }


        if (IccmVisitUtils.getActionStatus(checkObject).equalsIgnoreCase(IccmVisitUtils.Complete)) {
            if (isDiarrheaSuspect.equalsIgnoreCase("true") && getAgeFromDate(memberObject.getAge()) < 5) {
                try {
                    String title = context.getString(R.string.iccm_diarrhea);
                    IccmDiarrheaActionHelper diarrheaActionHelper = new IccmDiarrheaActionHelper(context, memberObject.getIccmEnrollmentFormSubmissionId(), actionList, details, callBack, isMalariaSuspect);
                    BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title).withOptional(true).withHelper(diarrheaActionHelper).withDetails(details).withBaseEntityID(memberObject.getBaseEntityId()).withFormName(Constants.JsonForm.getIccmDiarrhea()).build();
                    actionList.put(title, action);
                } catch (Exception e) {
                    Timber.e(e);
                }
            } else if (isMalariaSuspect.equalsIgnoreCase("true")) {
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
                actionList.remove(context.getString(R.string.iccm_diarrhea));
            }
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
