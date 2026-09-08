package org.smartregister.chw.actionhelper;

import android.content.Context;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.ncd.domain.VisitDetail;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * ActionHelper for Section B: Danger Signs.
 * Extracts danger sign responses and counts positives for subtitle display.
 */
public class NcdDangerSignsActionHelper implements BaseNcdVisitAction.NcdVisitActionHelper {

    private static final String STEP_ONE = "step1";
    private static final String KEY_NON_HEALING_WOUNDS = "non_healing_wounds";
    private static final String KEY_NEUROPATHY = "neuropathy";
    private static final String KEY_VISION_CHANGES = "vision_changes";
    private static final String KEY_CHEST_PAIN = "chest_pain";

    private static final String[] DANGER_SIGN_KEYS = {
            KEY_NON_HEALING_WOUNDS, KEY_NEUROPATHY, KEY_VISION_CHANGES, KEY_CHEST_PAIN
    };

    private final Map<String, String> cachedResults = new HashMap<>();

    private boolean unresolvedRedAlert;

    private Context context;
    private Map<String, List<VisitDetail>> details;
    private String jsonPayload;

    /**
     * Sets the unresolved RED alert flag. When true, the form will display
     * a warning banner about an unresolved urgent referral from the previous visit.
     */
    public void setUnresolvedRedAlert(boolean unresolvedRedAlert) {
        this.unresolvedRedAlert = unresolvedRedAlert;
    }

    public boolean hasUnresolvedRedAlert() {
        return unresolvedRedAlert;
    }

    @Override
    public void onJsonFormLoaded(String json, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
        this.details = details;
        cachedResults.clear();

        if (TextUtils.isEmpty(jsonPayload)) {
            jsonPayload = fetchStoredPayload();
        }

        if (unresolvedRedAlert && StringUtils.isNotBlank(jsonPayload)) {
            try {
                JSONObject form = new JSONObject(jsonPayload);
                JSONObject step = form.optJSONObject(STEP_ONE);
                if (step != null) {
                    JSONArray fields = step.optJSONArray("fields");
                    if (fields != null) {
                        injectUnresolvedRedAlertField(fields);
                        jsonPayload = form.toString();
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "Failed to inject unresolved RED alert field");
            }
        }
    }

    private void injectUnresolvedRedAlertField(JSONArray fields) throws Exception {
        // Set the hidden flag
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && "unresolved_red_alert".equals(field.optString("key"))) {
                field.put("value", "true");
                return;
            }
        }
        // If hidden field doesn't exist in form, add it
        JSONObject hiddenField = new JSONObject();
        hiddenField.put("key", "unresolved_red_alert");
        hiddenField.put("type", "hidden");
        hiddenField.put("value", "true");
        hiddenField.put("openmrs_entity", "concept");
        hiddenField.put("openmrs_entity_id", "unresolved_red_alert");
        hiddenField.put("openmrs_entity_parent", "");
        fields.put(hiddenField);
    }

    @Override
    public String getPreProcessed() {
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String payload) {
        jsonPayload = payload;
        cachedResults.clear();
    }

    @Override
    public BaseNcdVisitAction.ScheduleStatus getPreProcessedStatus() {
        return allAnswered() ? BaseNcdVisitAction.ScheduleStatus.DUE : BaseNcdVisitAction.ScheduleStatus.OVERDUE;
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
        if (!allAnswered()) {
            return context != null
                    ? context.getString(R.string.ncd_followup_danger_signs_pending)
                    : "Not yet completed";
        }

        int count = countDangerSigns();
        if (count == 0) {
            return context != null
                    ? context.getString(R.string.ncd_followup_danger_signs_none)
                    : "No danger signs";
        }

        return context != null
                ? context.getString(R.string.ncd_followup_danger_signs_detected, count)
                : String.format(Locale.US, "%d danger sign(s) detected", count);
    }

    @Override
    public BaseNcdVisitAction.Status evaluateStatusOnPayload() {
        return allAnswered() ? BaseNcdVisitAction.Status.COMPLETED : BaseNcdVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseNcdVisitAction action) {
        jsonPayload = action.getJsonPayload();
        cachedResults.clear();

        action.setSubTitle(evaluateSubTitle());
        action.setScheduleStatus(getPreProcessedStatus());
        action.setActionStatus(evaluateStatusOnPayload());
    }

    private boolean allAnswered() {
        for (String key : DANGER_SIGN_KEYS) {
            if (StringUtils.isBlank(extractResult(key))) {
                return false;
            }
        }
        return true;
    }

    private int countDangerSigns() {
        int count = 0;
        for (String key : DANGER_SIGN_KEYS) {
            if ("yes".equalsIgnoreCase(extractResult(key))) {
                count++;
            }
        }
        return count;
    }

    private String extractResult(String fieldKey) {
        if (cachedResults.containsKey(fieldKey)) {
            return cachedResults.get(fieldKey);
        }

        String value = null;

        if (StringUtils.isNotBlank(jsonPayload)) {
            try {
                JSONObject form = new JSONObject(jsonPayload);
                JSONObject step = form.optJSONObject(STEP_ONE);
                if (step != null) {
                    JSONArray fields = step.optJSONArray("fields");
                    if (fields != null) {
                        for (int i = 0; i < fields.length(); i++) {
                            JSONObject field = fields.optJSONObject(i);
                            if (field != null && fieldKey.equals(field.optString("key"))) {
                                value = field.optString("value");
                                if (StringUtils.isBlank(value)) {
                                    JSONArray array = field.optJSONArray("value");
                                    if (array != null && array.length() > 0) {
                                        value = array.join(", ");
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        if (StringUtils.isBlank(value)) {
            value = fetchStoredValue(fieldKey);
        }

        cachedResults.put(fieldKey, value);
        return value;
    }

    private String fetchStoredPayload() {
        List<VisitDetail> visitDetails = getVisitDetails(KEY_NON_HEALING_WOUNDS);
        if (visitDetails != null) {
            for (int i = visitDetails.size() - 1; i >= 0; i--) {
                VisitDetail detail = visitDetails.get(i);
                if (detail != null && StringUtils.isNotBlank(detail.getJsonDetails())) {
                    return detail.getJsonDetails();
                }
            }
        }
        return null;
    }

    private String fetchStoredValue(String key) {
        List<VisitDetail> visitDetails = getVisitDetails(key);
        if (visitDetails != null) {
            for (int i = visitDetails.size() - 1; i >= 0; i--) {
                VisitDetail detail = visitDetails.get(i);
                if (detail != null) {
                    if (StringUtils.isNotBlank(detail.getDetails())) {
                        return detail.getDetails();
                    }
                    if (StringUtils.isNotBlank(detail.getHumanReadable())) {
                        return detail.getHumanReadable();
                    }
                }
            }
        }
        return null;
    }

    private List<VisitDetail> getVisitDetails(String key) {
        if (details != null && details.containsKey(key)) {
            return details.get(key);
        }
        return null;
    }
}
