package org.smartregister.chw.actionhelper;

import android.content.Context;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.ncd.domain.VisitDetail;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class NcdFollowUpStatusActionHelper implements BaseNcdVisitAction.NcdVisitActionHelper {

    public static final String KEY_STATUS = "client_status";
    public static final String KEY_DATE_OF_DEATH = "date_of_death";

    public static final String STATUS_CONTINUING = "continuing";
    public static final String STATUS_DEAD = "dead";
    public static final String STATUS_TRANSFERRED = "transferred";

    private static final String STEP_ONE = "step1";

    private Context context;
    private Map<String, List<VisitDetail>> details;
    private String jsonPayload;

    @Override
    public void onJsonFormLoaded(String json, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
        this.details = details;
        if (TextUtils.isEmpty(jsonPayload)) {
            jsonPayload = fetchStoredPayload();
        }
    }

    @Override
    public String getPreProcessed() {
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String payload) {
        jsonPayload = payload;
    }

    @Override
    public BaseNcdVisitAction.ScheduleStatus getPreProcessedStatus() {
        return isComplete()
                ? BaseNcdVisitAction.ScheduleStatus.DUE
                : BaseNcdVisitAction.ScheduleStatus.OVERDUE;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return evaluateSubTitle();
    }

    @Override
    public String postProcess(String payload) {
        return payload;
    }

    @Override
    public String evaluateSubTitle() {
        String status = getStatus();
        if (STATUS_CONTINUING.equals(status)) {
            return getString(R.string.ncd_followup_status_continuing, "Continuing with Services");
        }
        if (STATUS_DEAD.equals(status)) {
            return getString(R.string.ncd_followup_status_dead, "Dead");
        }
        if (STATUS_TRANSFERRED.equals(status)) {
            return getString(R.string.ncd_followup_status_transferred, "Transferred");
        }
        return getString(R.string.ncd_followup_status_pending, "Not yet completed");
    }

    @Override
    public BaseNcdVisitAction.Status evaluateStatusOnPayload() {
        return isComplete()
                ? BaseNcdVisitAction.Status.COMPLETED
                : BaseNcdVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseNcdVisitAction action) {
        jsonPayload = action.getJsonPayload();
        action.setSubTitle(evaluateSubTitle());
        action.setScheduleStatus(getPreProcessedStatus());
        action.setActionStatus(evaluateStatusOnPayload());
    }

    public String getStatus() {
        return extractValue(jsonPayload, KEY_STATUS);
    }

    public boolean isActive() {
        return STATUS_CONTINUING.equals(getStatus());
    }

    public boolean isDeceased() {
        return STATUS_DEAD.equals(getStatus());
    }

    private boolean isComplete() {
        String status = getStatus();
        if (STATUS_CONTINUING.equals(status) || STATUS_TRANSFERRED.equals(status)) {
            return true;
        }
        if (STATUS_DEAD.equals(status)) {
            return StringUtils.isNotBlank(extractValue(jsonPayload, KEY_DATE_OF_DEATH));
        }
        return false;
    }

    private String fetchStoredPayload() {
        if (details == null) {
            return null;
        }
        List<VisitDetail> storedDetails = details.get(KEY_STATUS);
        if (storedDetails == null) {
            return null;
        }
        for (int i = storedDetails.size() - 1; i >= 0; i--) {
            VisitDetail detail = storedDetails.get(i);
            if (detail != null && StringUtils.isNotBlank(detail.getJsonDetails())) {
                return detail.getJsonDetails();
            }
        }
        return null;
    }

    public static String extractValue(String payload, String key) {
        if (StringUtils.isBlank(payload)) {
            return null;
        }
        try {
            JSONObject form = new JSONObject(payload);
            JSONObject step = form.optJSONObject(STEP_ONE);
            JSONArray fields = step != null ? step.optJSONArray("fields") : null;
            if (fields == null) {
                return null;
            }
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.optJSONObject(i);
                if (field != null && key.equals(field.optString("key"))) {
                    return field.optString("value", null);
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to parse NCD follow-up status payload");
        }
        return null;
    }

    public static String clearIrrelevantValues(String payload) {
        if (StringUtils.isBlank(payload)) {
            return payload;
        }
        try {
            JSONObject form = new JSONObject(payload);
            JSONObject step = form.optJSONObject(STEP_ONE);
            JSONArray fields = step != null ? step.optJSONArray("fields") : null;
            if (fields == null) {
                return payload;
            }

            String status = findValue(fields, KEY_STATUS);
            if (!STATUS_DEAD.equals(status)) {
                setValue(fields, KEY_DATE_OF_DEATH, "");
            }
            return form.toString();
        } catch (Exception e) {
            Timber.e(e, "Unable to sanitize NCD follow-up status payload");
            return payload;
        }
    }

    private static String findValue(JSONArray fields, String key) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString("key"))) {
                return field.optString("value", null);
            }
        }
        return null;
    }

    private static void setValue(JSONArray fields, String key, String value) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString("key"))) {
                field.put("value", value);
                return;
            }
        }
    }

    private String getString(int resourceId, String fallback) {
        return context != null ? context.getString(resourceId) : fallback;
    }
}
