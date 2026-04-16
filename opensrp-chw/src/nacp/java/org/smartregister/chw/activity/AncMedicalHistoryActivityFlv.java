package org.smartregister.chw.activity;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;

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

public class AncMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {

    private static final String[] ORDERED_SUMMARY_KEYS = {
            "gps",
            "danger_signs_present", "danger_signs_counseling",
            "service_before_referral", "chw_referral_hf", "referral_appointment_date",
            "problem", "problem_other",
            "first_clinic_visit", "anc_hf_visit", "anc_hf_visit_date", "anc_clinic_visit_partner",
            "reason_for_missed_visit", "other_reason_missed_visit", "last_clinic_visit",
            "services_pregnant_woman_anc_visit", "anc_hf_next_visit_date",
            "foods_available", "foods_consumed_last_week", "nutrition_restrictions",
            "nutrition_restrictions_other", "folic_acid_tablets", "anaemia_tablets", "meals_per_day",
            "location_nearest_health_facility", "savings_preparedness",
            "birth_companion_preparedness", "family_member_individual_stay_home_preparedness",
            "transportation_preparedness",
            "hiv_test", "disclose_status", "hiv_status", "taking_art",
            "malaria_protective_measures", "malaria_protective_measure_other", "fam_llin",
            "llin_2days", "llin_condition", "malaria_sp", "reason_for_missed_sp",
            "other_reason_missed_sp_dosage",
            "preg_woman_other_children", "preg_woman_breastfeed", "preg_woman_challenges_breastfeed",
            "fam_planning",
            "partner_head_of_household", "partner_head_of_household_stay_visit",
            "partner_head_of_households_interaction",
            "anyone_else_present_during_visit", "who_present_during_visit",
            "counselling_given",
            "date_of_illness", "illness_description", "action_taken",
            "chw_comment_anc"
    };

    private static final Set<String> MULTI_SELECT_KEYS = new HashSet<>(Arrays.asList(
            "danger_signs_present",
            "service_before_referral",
            "problem",
            "reason_for_missed_visit",
            "services_pregnant_woman_anc_visit",
            "foods_available",
            "foods_consumed_last_week",
            "nutrition_restrictions",
            "malaria_protective_measures",
            "partner_head_of_households_interaction",
            "who_present_during_visit",
            "counselling_given",
            "action_taken"
    ));

    private static final Set<String> UPPERCASE_TOKENS = new HashSet<>(Arrays.asList(
            "anc", "art", "chw", "gps", "hf", "hiv", "ifa", "lam", "llin",
            "ors", "pmtct", "pnc", "sp", "tt"
    ));

    private final StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
    private final SimpleDateFormat serviceDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat titleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    @Override
    protected void processAncCard(String has_card, Context context) {
        linearLayoutAncCard.setVisibility(View.GONE);
    }

    @Override
    protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
        // Rendered dynamically from all ANC visit details in processViewData().
    }

    @Override
    public void processViewData(List<Visit> visits, Context context) {
        super.processViewData(visits, context);

        linearLayoutHealthFacilityVisitDetails.removeAllViews();

        if (visits == null || visits.isEmpty()) {
            linearLayoutHealthFacilityVisit.setVisibility(View.GONE);
            return;
        }

        List<VisitSummary> summaries = new ArrayList<>();
        for (Visit visit : visits) {
            LinkedHashMap<String, String> details = extractVisitDetails(visit);
            if (!details.isEmpty()) {
                summaries.add(new VisitSummary(visit, details));
            }
        }

        processVisit(summaries, context);
    }

    private LinkedHashMap<String, String> extractVisitDetails(Visit visit) {
        LinkedHashMap<String, String> summary = new LinkedHashMap<>();

        for (String key : ORDERED_SUMMARY_KEYS) {
            try {
                String value = extractVisitValue(visit.getVisitDetails().get(key));
                if (shouldDisplayValue(value)) {
                    summary.put(key, value);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        return summary;
    }

    private String extractVisitValue(List<VisitDetail> visitDetails) {
        if (visitDetails == null) {
            return "";
        }

        List<String> values = new ArrayList<>();
        for (VisitDetail visitDetail : visitDetails) {
            String value = extractText(visitDetail);
            if (shouldDisplayValue(value)) {
                values.add(value);
            }
        }

        return StringUtils.join(values, ", ");
    }

    private String extractText(VisitDetail visitDetail) {
        if (visitDetail == null) {
            return "";
        }

        if (StringUtils.isNotBlank(visitDetail.getHumanReadable())) {
            return visitDetail.getHumanReadable().trim();
        }

        return StringUtils.defaultString(visitDetail.getDetails()).trim();
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

    private void processVisit(List<VisitSummary> summaries, Context context) {
        if (summaries.isEmpty()) {
            linearLayoutHealthFacilityVisit.setVisibility(View.GONE);
            return;
        }

        linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

        int visitNumber = 1;
        for (VisitSummary summary : summaries) {
            View view = inflater.inflate(R.layout.medical_history_visit, null);
            TextView tvTitle = view.findViewById(R.id.title);
            TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
            TextView tvEdit = view.findViewById(R.id.textview_edit);
            LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);

            tvEdit.setVisibility(View.GONE);
            tvTitle.setText(MessageFormat.format(
                    context.getString(R.string.anc_visit_date),
                    visitNumber,
                    resolveVisitTitleDate(summary)
            ));
            tvTypeOfService.setText(summary.visit.getVisitType() + " - " + serviceDateFormat.format(summary.visit.getDate()));

            for (Map.Entry<String, String> entry : summary.details.entrySet()) {
                TextView visitDetailTv = new TextView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                visitDetailTv.setLayoutParams(params);
                float scale = context.getResources().getDisplayMetrics().density;
                int dpAsPixels = (int) (10 * scale + 0.5f);
                visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                visitDetailsLayout.addView(visitDetailTv);

                evaluateView(context, visitDetailTv, entry.getKey(), entry.getValue());
            }

            linearLayoutHealthFacilityVisitDetails.addView(view, 0);
            visitNumber++;
        }
    }

    private String resolveVisitTitleDate(VisitSummary summary) {
        String visitDate = summary.details.get("anc_hf_visit_date");
        if (StringUtils.isNotBlank(visitDate)) {
            return visitDate;
        }

        return titleDateFormat.format(summary.visit.getDate());
    }

    private void evaluateView(Context context, TextView tv, String valueKey, String rawValue) {
        if (!shouldDisplayValue(rawValue)) {
            tv.setVisibility(View.GONE);
            return;
        }

        List<String> displayValues = new ArrayList<>();
        if (MULTI_SELECT_KEYS.contains(valueKey) && rawValue.contains(",")) {
            String[] values = rawValue.split(",");
            for (String value : values) {
                String resolvedValue = resolveDisplayToken(context, value);
                if (shouldDisplayValue(resolvedValue)) {
                    displayValues.add(resolvedValue);
                }
            }
        } else {
            String resolvedValue = resolveDisplayToken(context, rawValue);
            if (shouldDisplayValue(resolvedValue)) {
                displayValues.add(resolvedValue);
            }
        }

        if (displayValues.isEmpty()) {
            tv.setVisibility(View.GONE);
            return;
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder
                .append(resolveFieldLabel(context, valueKey), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append("\n");

        if (displayValues.size() > 1) {
            for (String value : displayValues) {
                spannableStringBuilder.append(
                        value + "\n",
                        new BulletSpan(10),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        } else {
            spannableStringBuilder.append(displayValues.get(0)).append("\n");
        }

        tv.setText(spannableStringBuilder);
    }

    private String resolveFieldLabel(Context context, String key) {
        if ("gps".equals(key)) {
            return context.getString(R.string.pnc_hv_location);
        }

        String prefixedKey = key.startsWith("anc_") ? key : "anc_" + key;
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

        String normalizedToken = token.toLowerCase(Locale.getDefault());
        if ("yes".equals(normalizedToken)) {
            return context.getString(R.string.yes);
        }
        if ("no".equals(normalizedToken)) {
            return context.getString(R.string.no);
        }
        if ("none".equals(normalizedToken) || "chk_none".equals(normalizedToken)) {
            return context.getString(R.string.none);
        }
        if ("once_in_a_while".equals(normalizedToken)) {
            return humanizeText(normalizedToken);
        }
        if (normalizedToken.endsWith("_yes")) {
            return context.getString(R.string.yes);
        }
        if (normalizedToken.endsWith("_no")) {
            return context.getString(R.string.no);
        }
        if (normalizedToken.endsWith("_positive")) {
            return humanizeText("positive");
        }
        if (normalizedToken.endsWith("_negative")) {
            return humanizeText("negative");
        }

        String resourceValue = findStringResource(context, token);
        if (StringUtils.isNotBlank(resourceValue)) {
            return resourceValue;
        }

        String normalizedResourceValue = findStringResource(context, normalizedToken);
        if (StringUtils.isNotBlank(normalizedResourceValue)) {
            return normalizedResourceValue;
        }

        String strippedToken = token.startsWith("chk_") ? token.substring(4) : token;
        if (StringUtils.equalsAnyIgnoreCase(strippedToken, "yes", "no", "none")) {
            return resolveDisplayToken(context, strippedToken);
        }

        String strippedResourceValue = findStringResource(context, strippedToken);
        if (StringUtils.isNotBlank(strippedResourceValue)) {
            return strippedResourceValue;
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

    private static class VisitSummary {
        private final Visit visit;
        private final LinkedHashMap<String, String> details;

        private VisitSummary(Visit visit, LinkedHashMap<String, String> details) {
            this.visit = visit;
            this.details = details;
        }
    }
}
