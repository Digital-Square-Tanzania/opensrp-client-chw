package org.smartregister.chw.actionhelper;

import android.content.Context;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.ncd.domain.VisitDetail;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * Helper that drives the client education action by tracking whether counselling was provided and
 * surfacing the selected topics as a subtitle.
 */
public class NcdClientEducationActionHelper implements BaseNcdVisitAction.NcdVisitActionHelper {

    private static final String STEP_ONE = "step1";
    private static final String KEY_COUNSELLING_PROVIDED = "provided_counselling";
    private static final String KEY_COUNSELLING_TOPICS = "counselling_topics";

    private static final Map<String, Integer> TOPIC_LABEL_RES_IDS;
    private static final Map<String, String> TOPIC_LABEL_FALLBACKS;

    static {
        Map<String, Integer> labelResIds = new HashMap<>();
        labelResIds.put("chk_diet", R.string.ncd_visit_topic_diet);
        labelResIds.put("chk_physical_activity", R.string.ncd_visit_topic_physical_activity);
        labelResIds.put("chk_medication_adherence", R.string.ncd_visit_topic_medication_adherence);
        labelResIds.put("chk_follow_up", R.string.ncd_visit_topic_follow_up);
        labelResIds.put("chk_warning_signs", R.string.ncd_visit_topic_warning_signs);
        labelResIds.put("chk_none", R.string.ncd_visit_topic_none);
        labelResIds.put("chk_other", R.string.ncd_visit_topic_other);
        TOPIC_LABEL_RES_IDS = Collections.unmodifiableMap(labelResIds);

        Map<String, String> fallbacks = new HashMap<>();
        fallbacks.put("chk_diet", "Healthy diet and nutrition");
        fallbacks.put("chk_physical_activity", "Physical activity and exercise");
        fallbacks.put("chk_medication_adherence", "Medication adherence");
        fallbacks.put("chk_follow_up", "Follow-up and referral plans");
        fallbacks.put("chk_warning_signs", "Warning signs and when to seek care");
        fallbacks.put("chk_none", "None");
        fallbacks.put("chk_other", "Other");
        TOPIC_LABEL_FALLBACKS = Collections.unmodifiableMap(fallbacks);
    }

    private final Map<String, String> cachedValues = new HashMap<>();

    private Context context;
    private Map<String, List<VisitDetail>> details;
    private String jsonPayload;

    @Override
    public void onJsonFormLoaded(String json, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
        this.details = details;
        clearCachedValues();

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
        clearCachedValues();
    }

    @Override
    public BaseNcdVisitAction.ScheduleStatus getPreProcessedStatus() {
        return counsellingProvided() ? BaseNcdVisitAction.ScheduleStatus.DUE : BaseNcdVisitAction.ScheduleStatus.OVERDUE;
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
        String counselling = getCounsellingResponse();
        if (StringUtils.isBlank(counselling)) {
            return context != null
                    ? context.getString(R.string.ncd_visit_client_education_missing)
                    : "Counselling status not recorded";
        }

        if ("yes".equalsIgnoreCase(counselling)) {
            String topics = getTopicsSummary();
            if (StringUtils.isNotBlank(topics)) {
                return context != null
                        ? context.getString(R.string.ncd_visit_client_education_topics, topics)
                        : String.format(Locale.US, "Topics: %s", topics);
            }
            return context != null
                    ? context.getString(R.string.ncd_visit_client_education_done)
                    : "Counselling provided";
        }

        if ("no".equalsIgnoreCase(counselling)) {
            return context != null
                    ? context.getString(R.string.ncd_visit_client_education_not_done)
                    : "Counselling not provided";
        }

        return context != null
                ? context.getString(R.string.ncd_visit_client_education_missing)
                : "Counselling status not recorded";
    }

    @Override
    public BaseNcdVisitAction.Status evaluateStatusOnPayload() {
        return counsellingProvided() ? BaseNcdVisitAction.Status.COMPLETED : BaseNcdVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseNcdVisitAction action) {
        jsonPayload = action.getJsonPayload();
        clearCachedValues();

        action.setSubTitle(evaluateSubTitle());
        action.setScheduleStatus(getPreProcessedStatus());
        action.setActionStatus(evaluateStatusOnPayload());
    }

    private boolean counsellingProvided() {
        return "yes".equalsIgnoreCase(getCounsellingResponse());
    }

    private String getCounsellingResponse() {
        if (cachedValues.containsKey(KEY_COUNSELLING_PROVIDED)) {
            return cachedValues.get(KEY_COUNSELLING_PROVIDED);
        }

        String value = extractFieldValue(KEY_COUNSELLING_PROVIDED);
        cachedValues.put(KEY_COUNSELLING_PROVIDED, value);
        return value;
    }

    private String getTopicsSummary() {
        if (cachedValues.containsKey(KEY_COUNSELLING_TOPICS)) {
            return cachedValues.get(KEY_COUNSELLING_TOPICS);
        }

        List<String> topics = extractMultipleValues(KEY_COUNSELLING_TOPICS);
        String summary = null;
        if (!topics.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < topics.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(formatValue(topics.get(i)));
            }
            summary = builder.toString();
        }

        cachedValues.put(KEY_COUNSELLING_TOPICS, summary);
        return summary;
    }

    private String extractFieldValue(String fieldKey) {
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
                                String value = field.optString("value");
                                if (StringUtils.isNotBlank(value)) {
                                    return value;
                                }
                                JSONArray array = field.optJSONArray("value");
                                if (array != null && array.length() > 0) {
                                    return array.join(", ");
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

        return fetchSingleStoredValue(fieldKey);
    }

    private List<String> extractMultipleValues(String fieldKey) {
        List<String> values = new ArrayList<>();

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
                                JSONArray array = field.optJSONArray("value");
                                if (array != null) {
                                    for (int j = 0; j < array.length(); j++) {
                                        values.add(array.optString(j));
                                    }
                                }
                                String singleValue = field.optString("value");
                                if (values.isEmpty() && StringUtils.isNotBlank(singleValue)) {
                                    values.add(singleValue);
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

        if (!values.isEmpty()) {
            return values;
        }

        List<VisitDetail> visitDetails = getVisitDetails(fieldKey);
        if (visitDetails != null) {
            for (int i = visitDetails.size() - 1; i >= 0; i--) {
                VisitDetail detail = visitDetails.get(i);
                if (detail != null && StringUtils.isNotBlank(detail.getHumanReadable())) {
                    String[] parts = detail.getHumanReadable().split(",");
                    for (String part : parts) {
                        String trimmed = part.trim();
                        if (StringUtils.isNotBlank(trimmed)) {
                            values.add(trimmed);
                        }
                    }
                    if (!values.isEmpty()) {
                        break;
                    }
                }
            }
        }
        return values;
    }

    private String fetchStoredPayload() {
        List<VisitDetail> visitDetails = getVisitDetails(KEY_COUNSELLING_PROVIDED);
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

    private String fetchSingleStoredValue(String key) {
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

        String key = normalized.toLowerCase(Locale.US);

        if (key.startsWith("chk_")) {
            Integer resId = TOPIC_LABEL_RES_IDS.get(key);
            if (resId != null && context != null) {
                return context.getString(resId);
            }

            String fallback = TOPIC_LABEL_FALLBACKS.get(key);
            if (fallback != null) {
                return fallback;
            }

            return toTitleCase(key.substring(4).replace('_', ' '));
        }

        if (normalized.contains("_")) {
            return toTitleCase(normalized.replace('_', ' '));
        }

        return normalized;
    }

    private void clearCachedValues() {
        cachedValues.clear();
    }

    private String toTitleCase(String phrase) {
        if (StringUtils.isBlank(phrase)) {
            return "";
        }

        String[] words = phrase.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (StringUtils.isBlank(word)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.US));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase(Locale.US));
            }
        }
        return builder.toString();
    }
}
