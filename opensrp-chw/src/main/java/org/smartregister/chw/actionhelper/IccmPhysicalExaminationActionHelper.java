package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.ld.util.AppExecutors;
import org.smartregister.chw.malaria.contract.BaseIccmVisitContract;
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

    private String physicalExamination;

    private final boolean isEdit;
    private final Map<String, List<VisitDetail>> details;
    private final HashMap<String, Boolean> checkObject = new HashMap<>();

    public IccmPhysicalExaminationActionHelper(Context context, String baseEntityId, LinkedHashMap<String, BaseIccmVisitAction> actionList, Map<String, List<VisitDetail>> details, BaseIccmVisitContract.InteractorCallBack callBack, boolean isEdit) {
        this.context = context;
        this.baseEntityId = baseEntityId;
        this.actionList = actionList;
        this.isEdit = isEdit;
        this.callBack = callBack;
        this.details = details;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
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
            physicalExamination = CoreJsonFormUtils.getValue(jsonObject, "physical_examination");
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
        String isMalariaSuspect = "false";
        String clientPastMalariaTreatmentHistory = "";
        try {
            jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = org.smartregister.family.util.JsonFormUtils.fields(jsonObject);

            JSONObject physicalExaminationCompletionStatus = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "physical_examination_completion_status");
            assert physicalExaminationCompletionStatus != null;
            physicalExaminationCompletionStatus.put(JsonFormConstants.VALUE, IccmVisitUtils.getActionStatus(checkObject));

            isMalariaSuspect = CoreJsonFormUtils.getValue(jsonObject, "is_malaria_suspect_after_physical_examination");
            clientPastMalariaTreatmentHistory = CoreJsonFormUtils.getValue(jsonObject, "client_past_malaria_treatment_history");
        } catch (JSONException e) {
            Timber.e(e);
        }

        String malariaActionTitle = context.getString(R.string.iccm_malaria);
        if (isMalariaSuspect.equalsIgnoreCase("true") && (StringUtils.isBlank(clientPastMalariaTreatmentHistory) || !clientPastMalariaTreatmentHistory.equalsIgnoreCase("yes"))) {
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
            if (actionList.containsKey(context.getString(R.string.iccm_malaria))) {
                actionList.remove(context.getString(R.string.iccm_malaria));
            }
        }

        if ((!StringUtils.isBlank(clientPastMalariaTreatmentHistory) && clientPastMalariaTreatmentHistory.equalsIgnoreCase("yes"))) {
            actionList.remove(context.getString(R.string.iccm_malaria));
            actionList.remove(context.getString(R.string.iccm_diarrhea));
            actionList.remove(context.getString(R.string.iccm_pneumonia));
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
