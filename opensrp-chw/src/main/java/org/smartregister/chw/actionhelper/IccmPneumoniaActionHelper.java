package org.smartregister.chw.actionhelper;

import static org.smartregister.util.Utils.getAgeFromDate;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.IccmVisitUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmPneumoniaActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper {
    private String jsonPayload;
    private String baseEntityId;
    private Context context;

    private String pneumoniaSigns;

    private boolean isEdit;

    private final HashMap<String, Boolean> checkObject = new HashMap<>();

    public IccmPneumoniaActionHelper(Context context, String baseEntityId, boolean isEdit) {
        this.context = context;
        this.baseEntityId = baseEntityId;
        this.isEdit = isEdit;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);

            IccmMemberObject memberObject = IccmDao.getMember(baseEntityId);
            jsonObject.getJSONObject("global").put("weight", memberObject.getWeight());

            int age = getAgeFromDate(memberObject.getAge());
            if (memberObject.getRespiratoryRate() != null && ((age < 1 && memberObject.getRespiratoryRate() >= 50) || (age >= 1 && age < 6 && memberObject.getRespiratoryRate() >= 40))) {
                JSONArray fields = jsonObject.getJSONObject(Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
                JSONObject pneumoniaSignsField = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "pneumonia_signs");
                JSONArray options = pneumoniaSignsField.getJSONArray("options");
                options.remove(options.length() - 1);
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
            pneumoniaSigns = CoreJsonFormUtils.getValue(jsonObject, "pneumonia_signs");
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
            JSONArray fields = org.smartregister.family.util.JsonFormUtils.fields(jsonObject);

            JSONObject pneumoniaCompletionStatus = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "pneumonia_completion_status");
            assert pneumoniaCompletionStatus != null;
            pneumoniaCompletionStatus.put(JsonFormConstants.VALUE, IccmVisitUtils.getActionStatus(checkObject));

        } catch (Exception e) {
            Timber.e(e);
        }

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
