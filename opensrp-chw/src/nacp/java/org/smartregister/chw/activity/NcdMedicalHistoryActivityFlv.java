package org.smartregister.chw.activity;

import android.content.Context;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders the list of past NCD Monthly Follow-Up visits. For each visit we show:
 *  - Title: visit type + date
 *  - Alert chip: red / yellow / none (from injected alert_status field)
 *  - Vitals row: systolic/diastolic BP and blood glucose
 *  - Adherence row: clinic attendance and medication adherence
 *  - Danger signs row: any yes flags among non-healing wounds, neuropathy, vision changes, chest pain
 */
public class NcdMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.US);
    private final StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);

    @Override
    protected void processAncCard(String hasCard, Context context) {
        linearLayoutAncCard.setVisibility(View.GONE);
    }

    @Override
    protected void processHealthFacilityVisit(List<Map<String, String>> hfVisits, Context context) {
        // no-op: NCD has no "ANC HF visit" concept; rendering happens in processViewData
    }

    @Override
    public void processViewData(List<Visit> visits, Context context) {
        if (visits == null || visits.isEmpty()) {
            linearLayoutLastVisit.setVisibility(View.GONE);
            return;
        }

        int days = Days.daysBetween(
                new DateTime(visits.get(visits.size() - 1).getDate()),
                new DateTime()).getDays();
        processLastVisit(days, context);

        linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

        for (int i = 0; i < visits.size(); i++) {
            Visit visit = visits.get(i);
            View row = inflater.inflate(R.layout.medical_history_visit, null);

            TextView tvTitle = row.findViewById(R.id.title);
            row.findViewById(R.id.textview_edit).setVisibility(View.GONE);
            TextView tvTypeOfService = row.findViewById(R.id.type_of_service);
            LinearLayout visitDetailsLayout = row.findViewById(R.id.visit_details_layout);

            tvTitle.setText(MessageFormat.format("{0} - {1}",
                    context.getString(R.string.ncd_case_management_visit_title),
                    DATE_FORMAT.format(visit.getDate())));

            String alertStatus = firstValue(visit, "alert_status");
            tvTypeOfService.setText(formatAlertChip(context, alertStatus));

            appendRow(context, visitDetailsLayout,
                    R.string.ncd_med_history_bp_label,
                    formatBloodPressure(visit));
            appendRow(context, visitDetailsLayout,
                    R.string.ncd_med_history_glucose_label,
                    firstText(context, visit, "diabetes_test_result"));
            appendRow(context, visitDetailsLayout,
                    R.string.ncd_med_history_clinic_label,
                    firstText(context, visit, "clinic_attendance"));
            appendRow(context, visitDetailsLayout,
                    R.string.ncd_med_history_adherence_label,
                    firstText(context, visit, "medication_adherence"));
            appendRow(context, visitDetailsLayout,
                    R.string.ncd_med_history_danger_signs_label,
                    formatDangerSigns(context, visit));

            linearLayoutHealthFacilityVisitDetails.addView(row, 0);
        }
    }

    private void processLastVisit(int days, Context context) {
        linearLayoutLastVisit.setVisibility(View.VISIBLE);
        if (days < 1) {
            customFontTextViewLastVisit.setText(
                    org.smartregister.chw.core.R.string.less_than_twenty_four);
        } else {
            customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(
                    context.getString(org.smartregister.chw.core.R.string.days_ago),
                    String.valueOf(days))));
        }
    }

    private void appendRow(Context context, LinearLayout parent, int labelResId, String value) {
        if (StringUtils.isBlank(value)) return;
        TextView tv = new TextView(context);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        float scale = context.getResources().getDisplayMetrics().density;
        int padding = (int) (10 * scale + 0.5f);
        tv.setPadding(padding, 0, 0, 0);

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(context.getString(labelResId), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(" ").append(value);
        tv.setText(sb);

        parent.addView(tv);
    }

    private String formatAlertChip(Context context, String alertStatus) {
        if (StringUtils.isBlank(alertStatus)) {
            return context.getString(R.string.ncd_med_history_alert_none);
        }
        switch (alertStatus.toLowerCase(Locale.US)) {
            case "red":
                return context.getString(R.string.ncd_med_history_alert_red);
            case "yellow":
                return context.getString(R.string.ncd_med_history_alert_yellow);
            default:
                return context.getString(R.string.ncd_med_history_alert_none);
        }
    }

    private String formatBloodPressure(Visit visit) {
        String systolic = firstValue(visit, "blood_pressure_systolic");
        String diastolic = firstValue(visit, "blood_pressure_diastolic");
        if (StringUtils.isBlank(systolic) && StringUtils.isBlank(diastolic)) return null;
        return MessageFormat.format("{0}/{1}",
                StringUtils.defaultIfBlank(systolic, "-"),
                StringUtils.defaultIfBlank(diastolic, "-"));
    }

    private String formatDangerSigns(Context context, Visit visit) {
        StringBuilder sb = new StringBuilder();
        appendIfYes(context, sb, visit, "non_healing_wounds",
                R.string.ncd_referral_reason_non_healing_wounds);
        appendIfYes(context, sb, visit, "neuropathy",
                R.string.ncd_referral_reason_neuropathy);
        appendIfYes(context, sb, visit, "vision_changes",
                R.string.ncd_referral_reason_vision_changes);
        appendIfYes(context, sb, visit, "chest_pain",
                R.string.ncd_referral_reason_chest_pain);
        if (sb.length() == 0) {
            return context.getString(R.string.ncd_med_history_danger_signs_none);
        }
        return sb.toString();
    }

    private void appendIfYes(Context context, StringBuilder sb, Visit visit,
                              String key, int labelResId) {
        String value = firstValue(visit, key);
        if ("yes".equalsIgnoreCase(value)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(context.getString(labelResId));
        }
    }

    private String firstValue(Visit visit, String key) {
        List<VisitDetail> details = visit.getVisitDetails() != null
                ? visit.getVisitDetails().get(key) : null;
        if (details == null || details.isEmpty()) return null;
        VisitDetail detail = details.get(0);
        String human = detail.getHumanReadable();
        return StringUtils.isNotBlank(human) ? human : detail.getDetails();
    }

    private String firstText(Context context, Visit visit, String key) {
        return getTexts(context, visit.getVisitDetails() != null
                ? visit.getVisitDetails().get(key) : null);
    }
}
