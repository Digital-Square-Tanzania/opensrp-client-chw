package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.core.activity.DefaultChildMedicalHistoryActivityFlv;
import org.smartregister.chw.core.domain.MedicalHistory;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.MedicalHistoryViewBuilder;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.Vaccine;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class ChildMedicalHistoryActivityFlv extends DefaultChildMedicalHistoryActivityFlv {

    private static final String[] ORDERED_SUMMARY_KEYS = {
            "gps",
            "toddler_danger_signs_present", "toddler_visited_health_facility",
            "referral_facility", "toddler_referral_health_facility",
            "service_before_referral", "chw_referral_hf", "referral_appointment_date",
            "fam_llin_1m5yr", "llin_2days_1m5yr", "llin_condition_1m5yr",
            "couselling_pnc", "pnc_counselling",
            "nutrition_status_1m5yr",
            "date_of_illness", "illness_description", "action_taken_1m5yr", "other_treat_1m5yr",
            "comp_feed_counselling_status",
            "child_growth_booklet_present", "child_growth_monitoring", "child_growth_graph",
            "child_growth_muac", "palm_pallor",
            "child_playing_challenge",
            "child_minor_ailment", "other_specify",
            "spend_time_with", "spend_time_with_other", "is_male_caregiver_present",
            "male_caregiver_present", "child_attend_daycare", "practice_play",
            "prepare_play_materials", "lesson_learned_previously", "play_with_child",
            "demonstrate_play_child", "challenges_child_communication",
            "child_safety_counselled",
            "breastfeed_current", "other_food_child_feeds", "times_child_breastfeeds",
            "breastfeeding_position_counselling", "child_suckling_well", "exclusive_breast_feeding",
            "communication_with_child", "child_communication_observation",
            "child_development_issues",
            "caregiver_interacts_with_child", "caregiver_comfort_child", "caregiver_response_cue",
            "caregiver_child_correction", "child_treated_harshly", "abuse_child_experienced",
            "comfortable_disclosing_hiv_status", "hiv_status", "test_hiv_past_three_months",
            "already_taking_art", "comfortable_sharing_child_hiv_status", "childs_hiv_status",
            "skin_to_skin_counselling"
    };

    private static final Set<String> UPPERCASE_TOKENS = new HashSet<>(Arrays.asList(
            "art", "chw", "ctc", "gps", "hiv", "llin", "muac", "ors", "pmtct", "pnc"
    ));

    private final StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
    private final SimpleDateFormat titleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private LinearLayout parentView;

    @Override
    public View bindViews(Activity activity) {
        View view = super.bindViews(activity);
        if (view instanceof LinearLayout) {
            parentView = (LinearLayout) view;
        }
        return view;
    }

    @Override
    public void processViewData(List<Visit> visits, Map<String, List<Vaccine>> vaccineMap, List<ServiceRecord> serviceTypeListMap, Context context) {
        this.visits = visits == null ? new ArrayList<>() : visits;
        this.vaccineMap = vaccineMap;
        visitMap.clear();

        for (Visit v : this.visits) {
            List<Visit> type_visits = visitMap.get(v.getVisitType());
            if (type_visits == null) type_visits = new ArrayList<>();

            type_visits.add(v);
            visitMap.put(v.getVisitType(), type_visits);
        }

        evaluateLastVisitDate();
        evaluateImmunizations();
        evaluateGrowthAndNutrition();
        evaluateECD();
        evaluateLLITN();
        evaluateChildHomeVisitHistory(context);
    }

    private void evaluateChildHomeVisitHistory(Context context) {
        if (parentView == null || visits == null || visits.isEmpty()) {
            return;
        }

        List<MedicalHistory> medicalHistories = new ArrayList<>();
        int visitNumber = 1;
        for (Visit visit : visits) {
            if (!CoreConstants.EventType.CHILD_HOME_VISIT.equals(visit.getVisitType())) {
                continue;
            }

            LinkedHashMap<String, List<String>> summary = extractVisitDetails(visit);
            if (summary.isEmpty()) {
                continue;
            }

            MedicalHistory history = new MedicalHistory();
            history.setTitle(MessageFormat.format(
                    context.getString(R.string.child_visit_date),
                    visitNumber,
                    titleDateFormat.format(visit.getDate())
            ));
            history.setSpannableStringBuilders(buildVisitDetails(context, summary));
            medicalHistories.add(history);
            visitNumber++;
        }

        if (!medicalHistories.isEmpty()) {
            View view = new MedicalHistoryViewBuilder(inflater, context)
                    .withTitle(context.getString(R.string.child_home_visit_history))
                    .withHistory(medicalHistories)
                    .withSeparator(false)
                    .build();

            parentView.addView(view);
        }
    }

    private LinkedHashMap<String, List<String>> extractVisitDetails(Visit visit) {
        LinkedHashMap<String, List<String>> summary = new LinkedHashMap<>();
        Map<String, List<VisitDetail>> visitDetails = visit.getVisitDetails();
        if (visitDetails == null || visitDetails.isEmpty()) {
            return summary;
        }

        for (String key : ORDERED_SUMMARY_KEYS) {
            try {
                List<String> values = extractVisitValues(visitDetails.get(key));
                if (!values.isEmpty()) {
                    summary.put(key, values);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        return summary;
    }

    private List<String> extractVisitValues(List<VisitDetail> visitDetails) {
        List<String> values = new ArrayList<>();
        if (visitDetails == null) {
            return values;
        }

        for (VisitDetail visitDetail : visitDetails) {
            if (visitDetail == null) {
                continue;
            }

            List<String> rawValues = parseValues(visitDetail.getDetails());
            List<String> humanReadableValues = parseValues(visitDetail.getHumanReadable());

            if (!humanReadableValues.isEmpty()
                    && (rawValues.isEmpty() || humanReadableValues.size() == rawValues.size())) {
                values.addAll(filterDisplayValues(humanReadableValues));
            } else if (StringUtils.isNotBlank(visitDetail.getHumanReadable()) && rawValues.size() <= 1) {
                addIfDisplayable(values, visitDetail.getHumanReadable());
            } else {
                values.addAll(filterDisplayValues(rawValues));
            }
        }

        return values;
    }

    private List<String> parseValues(String raw) {
        List<String> results = new ArrayList<>();
        if (StringUtils.isBlank(raw)) {
            return results;
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                JSONArray array = new JSONArray(trimmed);
                for (int i = 0; i < array.length(); i++) {
                    addIfDisplayable(results, array.optString(i));
                }
                return results;
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        if (trimmed.contains(",")) {
            for (String part : trimmed.split(",")) {
                addIfDisplayable(results, stripQuotes(part));
            }
            if (!results.isEmpty()) {
                return results;
            }
        }

        addIfDisplayable(results, stripQuotes(trimmed));
        return results;
    }

    private List<String> filterDisplayValues(List<String> rawValues) {
        List<String> values = new ArrayList<>();
        for (String value : rawValues) {
            addIfDisplayable(values, value);
        }
        return values;
    }

    private void addIfDisplayable(List<String> values, String value) {
        String cleanedValue = StringUtils.trimToEmpty(value);
        if (shouldDisplayValue(cleanedValue)) {
            values.add(cleanedValue);
        }
    }

    private String stripQuotes(String raw) {
        String value = StringUtils.trimToEmpty(raw);
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private List<SpannableStringBuilder> buildVisitDetails(Context context, LinkedHashMap<String, List<String>> summary) {
        List<SpannableStringBuilder> details = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : summary.entrySet()) {
            List<String> displayValues = new ArrayList<>();
            for (String value : entry.getValue()) {
                String resolvedValue = resolveDisplayToken(context, value);
                if (shouldDisplayValue(resolvedValue)) {
                    displayValues.add(resolvedValue);
                }
            }

            if (displayValues.isEmpty()) {
                continue;
            }

            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(resolveFieldLabel(context, entry.getKey()), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    .append("\n");

            if (displayValues.size() > 1) {
                for (String value : displayValues) {
                    builder.append(value + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                builder.append(displayValues.get(0)).append("\n");
            }

            details.add(builder);
        }
        return details;
    }

    private boolean shouldDisplayValue(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        String trimmedValue = value.trim();
        return !"false".equalsIgnoreCase(trimmedValue)
                && !"null".equalsIgnoreCase(trimmedValue)
                && !"[]".equals(trimmedValue)
                && !"{}".equals(trimmedValue);
    }

    private String resolveFieldLabel(Context context, String key) {
        if ("gps".equals(key)) {
            return context.getString(R.string.pnc_hv_location);
        }

        String prefixedKey = key.startsWith("child_") ? key : "child_" + key;
        String resourceValue = findStringResource(context, prefixedKey);
        if (StringUtils.isNotBlank(resourceValue)) {
            return resourceValue;
        }

        resourceValue = findStringResource(context, key);
        return StringUtils.isNotBlank(resourceValue) ? resourceValue : humanizeText(key);
    }

    private String resolveDisplayToken(Context context, String rawToken) {
        String token = StringUtils.trimToEmpty(rawToken);
        if (StringUtils.isBlank(token)) {
            return "";
        }

        String normalizedToken = normalizeResourceName(token);
        if ("yes".equals(normalizedToken) || normalizedToken.endsWith("_yes")) {
            return context.getString(R.string.yes);
        }
        if ("no".equals(normalizedToken) || normalizedToken.endsWith("_no")) {
            return context.getString(R.string.no);
        }
        if ("none".equals(normalizedToken) || "chk_none".equals(normalizedToken)) {
            return context.getString(R.string.none);
        }
        if (normalizedToken.endsWith("_positive")) {
            return context.getString(R.string.positive);
        }
        if (normalizedToken.endsWith("_negative")) {
            return context.getString(R.string.negative);
        }

        String resourceValue = findStringResource(context, token);
        if (StringUtils.isNotBlank(resourceValue)) {
            return resourceValue;
        }

        resourceValue = findStringResource(context, normalizedToken);
        if (StringUtils.isNotBlank(resourceValue)) {
            return resourceValue;
        }

        resourceValue = findStringResource(context, "child_option_" + normalizedToken);
        if (StringUtils.isNotBlank(resourceValue)) {
            return resourceValue;
        }

        String strippedToken = normalizedToken.startsWith("chk_")
                ? normalizedToken.substring(4)
                : normalizedToken;
        resourceValue = findStringResource(context, "child_option_" + strippedToken);
        if (StringUtils.isNotBlank(resourceValue)) {
            return resourceValue;
        }

        return humanizeText(strippedToken);
    }

    private String findStringResource(Context context, String resourceName) {
        if (StringUtils.isBlank(resourceName)) {
            return null;
        }

        int resourceId = context.getResources().getIdentifier(
                resourceName.trim(),
                "string",
                context.getPackageName()
        );

        if (resourceId == 0) {
            return null;
        }

        try {
            return context.getString(resourceId);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    private String humanizeText(String rawText) {
        String trimmedText = StringUtils.trimToEmpty(rawText);
        if (StringUtils.isBlank(trimmedText)) {
            return "";
        }

        if (!trimmedText.contains("_")) {
            if (!trimmedText.matches("[A-Za-z0-9]+")) {
                return trimmedText;
            }
            return formatToken(trimmedText);
        }

        List<String> parts = new ArrayList<>();
        for (String part : trimmedText.split("_")) {
            if (StringUtils.isBlank(part) || "chk".equalsIgnoreCase(part)) {
                continue;
            }
            parts.add(formatToken(part));
        }

        return StringUtils.join(parts, " ");
    }

    private String formatToken(String rawToken) {
        String token = StringUtils.trimToEmpty(rawToken);
        if (StringUtils.isBlank(token)) {
            return "";
        }

        String normalizedToken = token.toLowerCase(Locale.getDefault());
        if (UPPERCASE_TOKENS.contains(normalizedToken)) {
            return normalizedToken.toUpperCase(Locale.getDefault());
        }

        return StringUtils.capitalize(normalizedToken);
    }

    private String normalizeResourceName(String rawText) {
        return StringUtils.trimToEmpty(rawText)
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase(Locale.US);
    }

}
