package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.dao.ChwKvpDao;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public abstract class KvpPrEPVisitTypeActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    private String jsonPayload;
    private String visitType;
    private String baseEntityId;
    private boolean hasPreviousVisit;
    private final Map<String, String> visitState;

    public KvpPrEPVisitTypeActionHelper(String baseEntityId, Map<String, String> visitState) {
        this.baseEntityId = baseEntityId;
        this.visitState = visitState;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
        hasPreviousVisit = ChwKvpDao.hasFollowupVisits(baseEntityId);
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);

            JSONArray fields = jsonObject.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            JSONObject visitTypeObject = JsonFormUtils.getFieldJSONObject(fields, "visit_type");

            if (hasPreviousVisit) {
                visitTypeObject.put("value", "followup");
                visitTypeObject.put("read_only", true);
                visitTypeObject.put("editable", false);
                visitState.put("visit_type", "followup");
            } else {
                visitState.put("visit_type", "new_visit");
            }

            return jsonObject.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }

        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            visitType = CoreJsonFormUtils.getValue(jsonObject, "visit_type");

            if (StringUtils.isNotBlank(visitType)) {
                visitState.put("visit_type", visitType);
            }

            processVisitType(visitType);
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    public abstract void processVisitType(String visitType);

    @Override
    public BaseKvpVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String jsonPayload) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseKvpVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(visitType))
            return BaseKvpVisitAction.Status.PENDING;
        else {
            return BaseKvpVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction baseKvpVisitAction) {
        //overridden
    }
}
