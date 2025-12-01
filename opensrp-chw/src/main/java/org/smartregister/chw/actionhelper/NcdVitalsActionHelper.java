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
 * Helper that extracts blood sugar and blood pressure results from the follow-up form and
 * updates the visit action subtitle and status accordingly.
 */
public class NcdVitalsActionHelper implements BaseNcdVisitAction.NcdVisitActionHelper {

    private static final String STEP_ONE = "step1";
    private static final String KEY_BLOOD_SUGAR_RESULT = "diabetes_test_result";
    private static final String KEY_BLOOD_PRESSURE_SYSTOLIC = "blood_pressure_systolic";
    private static final String KEY_BLOOD_PRESSURE_DIASTOLIC = "blood_pressure_diastolic";

    private final Map<String, String> cachedResults = new HashMap<>();

    private Context context;
    private Map<String, List<VisitDetail>> details;
    private String jsonPayload;

    @Override
    public void onJsonFormLoaded(String json, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
        this.details = details;
        clearCachedResults();

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
        clearCachedResults();
    }

    @Override
    public BaseNcdVisitAction.ScheduleStatus getPreProcessedStatus() {
        return hasCompleteVitals() ? BaseNcdVisitAction.ScheduleStatus.DUE : BaseNcdVisitAction.ScheduleStatus.OVERDUE;
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
        String bloodSugar = extractResult(KEY_BLOOD_SUGAR_RESULT);
        String systolic = extractResult(KEY_BLOOD_PRESSURE_SYSTOLIC);
        String diastolic = extractResult(KEY_BLOOD_PRESSURE_DIASTOLIC);

        boolean hasBloodSugar = StringUtils.isNotBlank(bloodSugar);
        boolean hasBloodPressure = StringUtils.isNotBlank(systolic) && StringUtils.isNotBlank(diastolic);

        if (!hasBloodSugar && !hasBloodPressure) {
            return context != null
                    ? context.getString(R.string.ncd_visit_vitals_missing_all)
                    : "No vitals recorded yet";
        }

        String sugarDisplay = hasBloodSugar ? formatValue(bloodSugar) : getMissingPlaceholder();
        String pressureDisplay = hasBloodPressure ? formatBloodPressure(systolic, diastolic) : getMissingPlaceholder();

        if (context != null) {
            return context.getString(R.string.ncd_visit_vitals_summary, sugarDisplay, pressureDisplay);
        }
        return String.format(Locale.US, "Blood sugar: %s | Blood pressure: %s", sugarDisplay, pressureDisplay);
    }

    @Override
    public BaseNcdVisitAction.Status evaluateStatusOnPayload() {
        return hasCompleteVitals() ? BaseNcdVisitAction.Status.COMPLETED : BaseNcdVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseNcdVisitAction action) {
        jsonPayload = action.getJsonPayload();
        clearCachedResults();

        action.setSubTitle(evaluateSubTitle());
        action.setScheduleStatus(getPreProcessedStatus());
        action.setActionStatus(evaluateStatusOnPayload());
    }

    private boolean hasCompleteVitals() {
        return StringUtils.isNotBlank(extractResult(KEY_BLOOD_SUGAR_RESULT))
                && StringUtils.isNotBlank(extractResult(KEY_BLOOD_PRESSURE_SYSTOLIC))
                && StringUtils.isNotBlank(extractResult(KEY_BLOOD_PRESSURE_DIASTOLIC));
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
        String payload = fetchStoredPayloadForKey(KEY_BLOOD_SUGAR_RESULT);
        if (StringUtils.isBlank(payload)) {
            payload = fetchStoredPayloadForKey(KEY_BLOOD_PRESSURE_SYSTOLIC);
        }
        if (StringUtils.isBlank(payload)) {
            payload = fetchStoredPayloadForKey(KEY_BLOOD_PRESSURE_DIASTOLIC);
        }
        return payload;
    }

    private String fetchStoredPayloadForKey(String key) {
        List<VisitDetail> visitDetails = getVisitDetails(key);
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

    private String formatValue(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        return normalized.substring(0, 1).toUpperCase(Locale.US) + normalized.substring(1).toLowerCase(Locale.US);
    }

    private String getMissingPlaceholder() {
        return context != null
                ? context.getString(R.string.ncd_visit_vitals_missing_placeholder)
                : "Not recorded";
    }

    private void clearCachedResults() {
        cachedResults.clear();
    }

    private String formatBloodPressure(String systolic, String diastolic) {
        String normalizedSystolic = formatValue(systolic);
        String normalizedDiastolic = formatValue(diastolic);
        return String.format(Locale.US, "%s/%s mmHg", normalizedSystolic, normalizedDiastolic);
    }
}
