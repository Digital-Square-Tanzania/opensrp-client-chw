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
    private boolean previousHivPositive;
    private boolean hasCtcNumber;
    private boolean hivRetestDue;
    private final Map<String, String> visitState;

    public KvpPrEPVisitTypeActionHelper(String baseEntityId, Map<String, String> visitState) {
        this.baseEntityId = baseEntityId;
        this.visitState = visitState;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
        hasPreviousVisit = ChwKvpDao.hasFollowupVisits(baseEntityId);
        previousHivPositive = ChwKvpDao.isClientHivPositive(baseEntityId);
        hasCtcNumber = ChwKvpDao.hasCtcNumber(baseEntityId);
        hivRetestDue = ChwKvpDao.isHivRetestDue(baseEntityId);
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);

            JSONArray fields = jsonObject.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            JSONObject visitTypeObject = JsonFormUtils.getFieldJSONObject(fields, "visit_type");
            JSONObject hivTestConducted = JsonFormUtils.getFieldJSONObject(fields, "hiv_test_conducted");
            JSONObject hivTestLocation = JsonFormUtils.getFieldJSONObject(fields, "hiv_test_location");
            JSONObject hivStatusObject = JsonFormUtils.getFieldJSONObject(fields, "client_hiv_status");
            JSONObject ctcNumberObject = JsonFormUtils.getFieldJSONObject(fields, "ctc_number");

            makeFieldOptional(ctcNumberObject);

            if (hasPreviousVisit) {
                visitTypeObject.put("value", "followup");
                visitTypeObject.put("read_only", true);
                visitTypeObject.put("editable", false);
                visitState.put("visit_type", "followup");
            } else {
                visitState.put("visit_type", "new_visit");
            }

            if (hasCtcNumber) {
                hideField(ctcNumberObject);
            }

            if (previousHivPositive) {
                hideField(hivTestConducted);
                hideField(hivTestLocation);

                if (hivStatusObject != null) {
                    hivStatusObject.put("type", "hidden");
                    hivStatusObject.put("value", "positive");
                    hivStatusObject.remove("relevance");
                    visitState.put("client_hiv_status", "positive");
                }

                if (ctcNumberObject != null && !hasCtcNumber) {
                    ctcNumberObject.remove("relevance");
                }

                return jsonObject.toString();
            }

            if (hivRetestDue) {
                if (hivTestConducted != null) {
                    hivTestConducted.remove("relevance");
                }
                applyYesRelevance(hivTestLocation, "hiv_test_conducted");
                applyYesRelevance(hivStatusObject, "hiv_test_conducted");
            } else {
                hideField(hivTestConducted);
                hideField(hivTestLocation);
                hideField(hivStatusObject);
                hideField(ctcNumberObject);
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
            String hivStatus = CoreJsonFormUtils.getValue(jsonObject, "client_hiv_status");

            if (StringUtils.isNotBlank(visitType)) {
                visitState.put("visit_type", visitType);
            }
            if (StringUtils.isNotBlank(hivStatus)) {
                visitState.put("client_hiv_status", hivStatus);
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

    private void hideField(JSONObject field) throws JSONException {
        if (field != null) {
            field.put("type", "hidden");
            field.remove("relevance");
        }
    }

    private void applyYesRelevance(JSONObject field, String sourceKey) throws JSONException {
        if (field == null || StringUtils.isBlank(sourceKey)) {
            return;
        }

        JSONObject relevance = new JSONObject();
        JSONObject condition = new JSONObject();
        condition.put("type", "string");
        condition.put("ex", "equalTo(., \"yes\")");
        relevance.put(JsonFormConstants.STEP1 + ":" + sourceKey, condition);
        field.put("relevance", relevance);
    }

    private void makeFieldOptional(JSONObject field) throws JSONException {
        if (field == null) {
            return;
        }

        JSONObject optional = new JSONObject();
        optional.put("value", false);
        field.put("v_required", optional);
    }
}
