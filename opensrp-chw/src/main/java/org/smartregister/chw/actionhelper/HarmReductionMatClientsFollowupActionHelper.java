package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.harmreduction.domain.VisitDetail;
import org.smartregister.chw.harmreduction.model.BaseHarmReductionVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HarmReductionMatClientsFollowupActionHelper implements BaseHarmReductionVisitAction.HarmReductionVisitActionHelper {
    private static final String METHADONE_TREATMENT_STATUS_KEY = "methadone_treatment_status";

    private String methadoneTreatmentStatus;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        // no-op
    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            methadoneTreatmentStatus = JsonFormUtils.getValue(jsonObject, METHADONE_TREATMENT_STATUS_KEY);
            if (StringUtils.isBlank(methadoneTreatmentStatus)) {
                methadoneTreatmentStatus = getFieldValue(jsonObject, METHADONE_TREATMENT_STATUS_KEY);
            }
            if (StringUtils.isBlank(methadoneTreatmentStatus)) {
                methadoneTreatmentStatus = JsonFormUtils.getCheckBoxValue(jsonObject, METHADONE_TREATMENT_STATUS_KEY);
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public BaseHarmReductionVisitAction.ScheduleStatus getPreProcessedStatus() {
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
    public BaseHarmReductionVisitAction.Status evaluateStatusOnPayload() {
        return StringUtils.isNotBlank(methadoneTreatmentStatus)
                ? BaseHarmReductionVisitAction.Status.COMPLETED
                : BaseHarmReductionVisitAction.Status.PENDING;
    }

    private String getFieldValue(JSONObject jsonObject, String key) {
        try {
            JSONObject step = jsonObject.optJSONObject("step1");
            if (step == null) {
                return "";
            }

            JSONArray fields = step.optJSONArray("fields");
            if (fields == null) {
                return "";
            }

            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.optJSONObject(i);
                if (field != null && StringUtils.equalsIgnoreCase(key, field.optString("key"))) {
                    return field.optString("value");
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return "";
    }

    @Override
    public void onPayloadReceived(BaseHarmReductionVisitAction baseVisitAction) {
        // no-op
    }
}
