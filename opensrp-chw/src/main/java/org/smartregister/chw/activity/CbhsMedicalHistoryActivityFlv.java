package org.smartregister.chw.activity;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class CbhsMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
    private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

    @Override
    protected void processAncCard(String has_card, Context context) {
        // super.processAncCard(has_card, context);
        linearLayoutAncCard.setVisibility(View.GONE);
    }

    @Override
    protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
        //super.processHealthFacilityVisit(hf_visits, context);
    }

    @Override
    public void processViewData(List<Visit> visits, Context context) {

        if (visits.size() > 0) {
            int days = 0;
            List<Map<String, String>> hf_visits = new ArrayList<>();

            int x = 0;
            while (x < visits.size()) {

                // the first object in this list is the days difference
                if (x == 0) {
                    days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                }


                String[] visitParams = {"followup_visit_date", "registration_or_followup_status", "client_condition", "health_problem", "social_problem", "supplies_provided", "medicine_provided", "hiv_services_provided", "referrals_issued_to_other_services", "referrals_to_other_services_completed", "state_of_therapy", "client_moved_location"};
                extractVisitDetails(visits, visitParams, hf_visits, x, context);

                x++;
            }

            processLastVisit(days, context);
            processVisit(hf_visits, context);
        }
    }

    private void extractVisitDetails(List<Visit> sourceVisits, String[] hf_params, List<Map<String, String>> hf_visits, int iteration, Context context) {
        // get the hf details
        Map<String, String> map = new HashMap<>();
        for (String param : hf_params) {
            try {
                List<VisitDetail> details = sourceVisits.get(iteration).getVisitDetails().get(param);
                map.put(param, getTexts(context, details));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        hf_visits.add(map);
    }

    @Override
    public String getTexts(Context context, List<VisitDetail> visitDetails) {
        if (visitDetails == null)
            return "";

        List<String> texts = new ArrayList<>();
        for (VisitDetail vd : visitDetails) {
            String val = getText(vd);
            if (StringUtils.isNotBlank(val))
                texts.add(val);
        }
        return NCUtils.toCSV(texts);
    }

    /**
     * Extract value from VisitDetail
     *
     * @return
     */
    @NotNull
    public static String getText(@Nullable VisitDetail visitDetail) {
        if (visitDetail == null)
            return "";
        return (StringUtils.isNotBlank(visitDetail.getDetails())) ? visitDetail.getDetails().trim() : "";
    }


    private void processLastVisit(int days, Context context) {
        linearLayoutLastVisit.setVisibility(View.VISIBLE);
        if (days < 1) {
            customFontTextViewLastVisit.setText(org.smartregister.chw.core.R.string.less_than_twenty_four);
        } else {
            customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(context.getString(org.smartregister.chw.core.R.string.days_ago), String.valueOf(days))));
        }
    }


    protected void processVisit(List<Map<String, String>> hf_visits, Context context) {
        if (hf_visits != null && hf_visits.size() > 0) {
            linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

            int x = 0;
            for (Map<String, String> vals : hf_visits) {
                View view = inflater.inflate(R.layout.medical_history_cbhs_visit, null);
                TextView tvTitle = view.findViewById(R.id.title);
                evaluateTitle(context, x, vals, tvTitle);

                evaluateView(context, vals, view.findViewById(R.id.registration_or_followup_status), "registration_or_followup_status", R.string.cbhs_registration_or_followup_status, "cbhs_");
                evaluateView(context, vals, view.findViewById(R.id.health_problem), "health_problem", R.string.cbhs_health_problem, "cbhs_");
                evaluateView(context, vals, view.findViewById(R.id.social_problem), "social_problem", R.string.cbhs_social_problem, "cbhs_");
                evaluateView(context, vals, view.findViewById(R.id.medicine_provided), "medicine_provided", R.string.cbhs_medicine_provided, "cbhs_");
                evaluateView(context, vals, view.findViewById(R.id.hiv_services_provided), "hiv_services_provided", R.string.cbhs_hiv_services_provided, "cbhs_");
                evaluateView(context, vals, view.findViewById(R.id.referrals_issued_other_services), "referrals_issued_to_other_services", R.string.cbhs_referrals_issued_to_other_services, "cbhs_");
                evaluateView(context, vals, view.findViewById(R.id.referrals_to_other_services_completed), "referrals_to_other_services_completed", R.string.cbhs_referrals_to_other_services_completed, "cbhs_");
                evaluateView(context, vals, view.findViewById(R.id.state_of_therapy), "state_of_therapy", R.string.cbhs_state_of_therapy, "cbhs_");

                linearLayoutHealthFacilityVisitDetails.addView(view, 0);

                x++;
            }
        }
    }


    private void evaluateTitle(Context context, int x, Map<String, String> vals, TextView tvTitle) {
        String visitDate = vals.get("followup_visit_date");
        if (StringUtils.isBlank(visitDate)) {
            tvTitle.setVisibility(View.GONE);
        } else {
            try {
                tvTitle.setText(MessageFormat.format(context.getString(R.string.mother_champion_visit_title), x + 1, visitDate));
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private void evaluateView(Context context, Map<String, String> vals, TextView tv, String valueKey, int viewTitleStringResource, String valuePrefixInStringResources) {
        if (StringUtils.isNotBlank(getMapValue(vals, valueKey))) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder.append(context.getString(viewTitleStringResource), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

            String stringValue = getMapValue(vals, valueKey);
            String[] stringValueArray;
            if (stringValue.contains(",")) {
                stringValueArray = stringValue.split(",");
                for (String value : stringValueArray) {
                    spannableStringBuilder.append(getStringResource(context, valuePrefixInStringResources, value.trim()) + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                spannableStringBuilder.append(getStringResource(context, valuePrefixInStringResources, stringValue)).append("\n");
            }
            tv.setText(spannableStringBuilder);
        } else {
            tv.setVisibility(View.GONE);
        }
    }


    private String getMapValue(Map<String, String> map, String key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return "";
    }

    private String getStringResource(Context context, String prefix, String resourceName) {
        int resourceId = context.getResources().
                getIdentifier(prefix + resourceName.trim(), "string", context.getPackageName());
        try {
            return context.getString(resourceId);
        } catch (Exception e) {
            Timber.e(e);
            return prefix + resourceName;
        }
    }
}
