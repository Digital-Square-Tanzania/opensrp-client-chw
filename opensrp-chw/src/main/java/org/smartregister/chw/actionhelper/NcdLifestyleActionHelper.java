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
 * ActionHelper for Section C: Lifestyle Modifications.
 * Extracts salt, sugar, and physical activity target responses.
 */
public class NcdLifestyleActionHelper implements BaseNcdVisitAction.NcdVisitActionHelper {

    private static final String STEP_ONE = "step1";
    private static final String KEY_SALT_INTAKE = "salt_intake_target_met";
    private static final String KEY_SUGAR_INTAKE = "sugar_intake_target_met";
    private static final String KEY_PHYSICAL_ACTIVITY = "physical_activity_target_met";

    private static final String[] TARGET_KEYS = {
            KEY_SALT_INTAKE, KEY_SUGAR_INTAKE, KEY_PHYSICAL_ACTIVITY
    };

    private final Map<String, String> cachedResults = new HashMap<>();

    private Context context;
    private Map<String, List<VisitDetail>> details;
    private String jsonPayload;

    @Override
    public void onJsonFormLoaded(String json, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
        this.details = details;
        cachedResults.clear();

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
                    ? context.getString(R.string.ncd_followup_lifestyle_pending)
                    : "Not yet completed";
        }

        int met = countTargetsMet();
        return context != null
                ? context.getString(R.string.ncd_followup_lifestyle_targets_met, met)
                : String.format(Locale.US, "%d/3 targets met", met);
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
        for (String key : TARGET_KEYS) {
            if (StringUtils.isBlank(extractResult(key))) {
                return false;
            }
        }
        return true;
    }

    private int countTargetsMet() {
        int count = 0;
        for (String key : TARGET_KEYS) {
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
        List<VisitDetail> visitDetails = getVisitDetails(KEY_SALT_INTAKE);
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
