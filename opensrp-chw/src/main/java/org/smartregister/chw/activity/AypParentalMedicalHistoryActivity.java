package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONArray;
import org.json.JSONException;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.presenter.BaseAncMedicalHistoryPresenter;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.interactor.AypParentalMedicalHistoryInteractor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class AypParentalMedicalHistoryActivity extends CoreAncMedicalHistoryActivity {

    private static MemberObject memberProfile;

    private final Flavor flavor = new AypParentalMedicalHistoryActivityFlv();

    private ProgressBar progressBar;

    public static void startMe(Activity activity, MemberObject memberObject) {
        Intent intent = new Intent(activity, AypParentalMedicalHistoryActivity.class);
        memberProfile = memberObject;
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        if (memberProfile == null) {
            Timber.w("AypParentalMedicalHistoryActivity launched without a member profile");
            return;
        }
        presenter = new BaseAncMedicalHistoryPresenter(new AypParentalMedicalHistoryInteractor(), this, memberProfile.getBaseEntityId());
    }

    @Override
    public void setUpView() {
        if (memberProfile == null) {
            Timber.w("AypParentalMedicalHistoryActivity launched without a member profile");
            finish();
            return;
        }
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        String displayName = StringUtils.isNotBlank(memberProfile.getFullName()) ? memberProfile.getFullName() : getString(R.string.ayp_client);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, displayName));

        ((TextView) findViewById(R.id.medical_history)).setText(R.string.ayp_visit_history);
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(R.string.ayp_visit);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        memberProfile = null;
    }

    private static class AypParentalMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {

        private static final String[] FIELD_ORDER = new String[]{
                "client_status", "transfer_remarks", "new_address",
                "service_delivery_modality", "group_modality_types",
                "section_selection", "intro_details", "hiv_problem_topics",
                "rh_provided", "rh_sections_provided", "hiv_aids_topics", "stis_rtis_topics",
                "viral_hepatitis_topics", "protection_against_hiv_aids_stis_viral_hepatitis",
                "understanding_youth_challenges", "skills_education_communication_youth",
                "sexual_reproductive_health_youth", "promote_hiv_services_youth",
                "visit_comment", "next_appointment_date", "referral_services", "referral_other_specify"
        };

        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

        @Override
        protected void processAncCard(String has_card, Context context) {
            linearLayoutAncCard.setVisibility(View.GONE);
        }

        @Override
        protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
            // Hide default ANC-specific section
        }

        @Override
        public void processViewData(List<Visit> visits, Context context) {
            if (visits == null || visits.isEmpty()) {
                return;
            }

            int days = 0;
            List<LinkedHashMap<String, List<VisitDetail>>> visitDetailsList = new ArrayList<>();

            for (int index = 0; index < visits.size(); index++) {
                Visit visit = visits.get(index);
                if (index == 0 && visits.get(visits.size() - 1).getDate() != null) {
                    days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                }

                LinkedHashMap<String, List<VisitDetail>> fieldMap = new LinkedHashMap<>();
                extractVisitDetails(visit, fieldMap);
                if (!fieldMap.isEmpty()) {
                    visitDetailsList.add(fieldMap);
                }
            }

//            processLastVisit(days, context);
            processVisit(visitDetailsList, context, visits);
        }

        private void extractVisitDetails(Visit visit, LinkedHashMap<String, List<VisitDetail>> destination) {
            if (visit == null || visit.getVisitDetails() == null) {
                return;
            }

            Map<String, List<VisitDetail>> groupedDetails = visit.getVisitDetails();
            for (String key : FIELD_ORDER) {
                List<VisitDetail> details = groupedDetails.get(key);
                if (details != null && !details.isEmpty()) {
                    destination.put(key, new ArrayList<>(details));
                }
            }
        }

        protected void processVisit(List<LinkedHashMap<String, List<VisitDetail>>> visitsData, Context context, List<Visit> visits) {
            if (visitsData == null || visitsData.isEmpty()) {
                return;
            }

            linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

            for (int index = 0; index < visitsData.size(); index++) {
                LinkedHashMap<String, List<VisitDetail>> values = visitsData.get(index);
                View view = inflater.inflate(R.layout.medical_history_visit, null);
                view.findViewById(R.id.title).setVisibility(View.GONE);

                TextView typeOfService = view.findViewById(R.id.type_of_service);
                LinearLayout detailsLayout = view.findViewById(R.id.visit_details_layout);
                TextView editView = view.findViewById(R.id.textview_edit);
                editView.setVisibility(View.GONE);

                Visit visit = visits.get(index);
                typeOfService.setText(buildVisitHeader(context, visit));

                populateVisitDetails(context, detailsLayout, values);

                linearLayoutHealthFacilityVisitDetails.addView(view, 0);
            }
        }

        private String buildVisitHeader(Context context, Visit visit) {
            String visitType = visit != null ? visit.getVisitType() : null;
            String visitDate = context.getString(R.string.ayp_visit_history_unknown_date);
            if (visit != null) {
                if (visit.getDate() != null) {
                    visitDate = dateFormat.format(visit.getDate());
                } else if (visit.getUpdatedAt() != null) {
                    visitDate = dateFormat.format(visit.getUpdatedAt());
                }
            }

            String label = resolveVisitTypeLabel(context, visitType);
            return String.format(Locale.getDefault(), "%s - %s", label, visitDate);
        }

        private String resolveVisitTypeLabel(Context context, String visitType) {
            if (StringUtils.equalsIgnoreCase(visitType, Constants.EVENT_TYPE.AYP_PARENTAL_SERVICES)) {
                return context.getString(R.string.ayp_parental_services_visit_type);
            }
            return StringUtils.isNotBlank(visitType) ? visitType : context.getString(R.string.ayp_visit);
        }

        private void populateVisitDetails(Context context, LinearLayout container, LinkedHashMap<String, List<VisitDetail>> values) {
            container.removeAllViews();
            for (Map.Entry<String, List<VisitDetail>> entry : values.entrySet()) {
                TextView detailView = new TextView(context);
                detailView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                detailView.setTextColor(ContextCompat.getColor(context, R.color.medical_sub_text_inner));
                detailView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                float scale = context.getResources().getDisplayMetrics().density;
                int padding = (int) (10 * scale + 0.5f);
                detailView.setPadding(padding, 0, 0, 0);

                evaluateView(context, values, detailView, entry.getKey());

                if (detailView.getVisibility() == View.VISIBLE) {
                    container.addView(detailView);
                }
            }
        }

        private void evaluateView(Context context, Map<String, List<VisitDetail>> values, TextView tv, String key) {
            List<VisitDetail> details = values.get(key);
            if (details == null || details.isEmpty()) {
                tv.setVisibility(View.GONE);
                return;
            }

            List<String> answers = deriveAnswerValues(context, key, details);
            if (answers.isEmpty()) {
                tv.setVisibility(View.GONE);
                return;
            }

            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(resolveQuestionLabel(context, key), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

            for (String answer : answers) {
                builder.append(answer, new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");
            }

            tv.setText(builder);
        }

        private String resolveQuestionLabel(Context context, String key) {
            int resId = context.getResources().getIdentifier("ayp_parenting_services_field_" + key, "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
            return StringUtils.capitalize(key.replace('_', ' '));
        }

        private List<String> deriveAnswerValues(Context context, String key, List<VisitDetail> details) {
            List<String> answers = new ArrayList<>();
            for (VisitDetail detail : details) {
                answers.addAll(resolveDetailAnswers(context, key, detail));
            }
            return answers;
        }

        private List<String> resolveDetailAnswers(Context context, String key, VisitDetail detail) {
            List<String> resolved = new ArrayList<>();
            if (detail == null) {
                return resolved;
            }

            List<String> rawValues = parseValues(detail.getDetails());
            if (rawValues.isEmpty()) {
                rawValues = parseValues(detail.getHumanReadable());
            }

            if (rawValues.isEmpty()) {
                if (StringUtils.isNotBlank(detail.getHumanReadable())) {
                    resolved.add(detail.getHumanReadable().trim());
                } else if (StringUtils.isNotBlank(detail.getDetails())) {
                    resolved.add(detail.getDetails().trim());
                }
                return resolved;
            }

            List<String> humanValues = parseValues(detail.getHumanReadable());
            for (int i = 0; i < rawValues.size(); i++) {
                String raw = rawValues.get(i);
                String mapped = mapValueToResource(context, raw);
                if (mapped == null && humanValues.size() == rawValues.size()) {
                    mapped = humanValues.get(i);
                }
                if (mapped == null && StringUtils.isNotBlank(detail.getHumanReadable()) && rawValues.size() == 1) {
                    mapped = detail.getHumanReadable().trim();
                }
                if (mapped == null) {
                    mapped = raw;
                }
                resolved.add(mapped);
            }

            return resolved;
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
                        String value = array.optString(i);
                        if (StringUtils.isNotBlank(value)) {
                            results.add(value.trim());
                        }
                    }
                    return results;
                } catch (JSONException e) {
                    Timber.e(e);
                }
            }

            if (trimmed.contains(",")) {
                String[] parts = trimmed.split(",");
                for (String part : parts) {
                    String candidate = part.trim();
                    if (candidate.startsWith("[") && candidate.endsWith("]")) {
                        candidate = candidate.substring(1, candidate.length() - 1).trim();
                    }
                    if (candidate.startsWith("\"") && candidate.endsWith("\"")) {
                        candidate = candidate.substring(1, candidate.length() - 1);
                    }
                    if (StringUtils.isNotBlank(candidate)) {
                        results.add(candidate);
                    }
                }
                if (!results.isEmpty()) {
                    return results;
                }
            }

            String single = trimmed;
            if (single.startsWith("\"") && single.endsWith("\"")) {
                single = single.substring(1, single.length() - 1);
            }
            results.add(single);
            return results;
        }

        private String mapValueToResource(Context context, String rawValue) {
            if (StringUtils.isBlank(rawValue)) {
                return null;
            }
            String normalized = rawValue.trim();
            if (normalized.startsWith("\"") && normalized.endsWith("\"")) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            String resourceName = "ayp_parenting_services_option_" + normalized
                    .replaceAll("[^A-Za-z0-9_]+", "_")
                    .replaceAll("_{2,}", "_")
                    .toLowerCase(Locale.US);
            int resId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
            return null;
        }
    }
}
