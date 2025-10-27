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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.presenter.BaseAncMedicalHistoryPresenter;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.interactor.TbLeprosyObservationResultsInteractor;
import org.smartregister.chw.tbleprosy.domain.MemberObject;
import org.smartregister.chw.tbleprosy.util.Constants;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class TbLeprosyObservationResultsActivity extends CoreAncMedicalHistoryActivity {

    private static MemberObject memberObject;

    private final Flavor flavor = new TbLeprosyObservationResultsActivityFlv();

    private ProgressBar progressBar;

    public static void startMe(Activity activity, MemberObject member) {
        Intent intent = new Intent(activity, TbLeprosyObservationResultsActivity.class);
        memberObject = member;
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(new TbLeprosyObservationResultsInteractor(), this, memberObject.getBaseEntityId());
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, memberObject.getFullName()));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.tbleprosy_visit_history_title));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(R.string.tbleprosy_visit_history_title);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    private static class TbLeprosyObservationResultsActivityFlv extends DefaultAncMedicalHistoryActivityFlv {

        private static final String OBSERVATION_PREFIX = "tbleprosy_observation_results_";
        private static final String RECORD_VISIT_PREFIX = "tbleprosy_record_visit_";
        private static final String FOLLOW_UP_PREFIX = "tbleprosy_followup_visit_";

        private static final String[] OBSERVATION_PARAMS = {"investigation_type", "tb_preliminary_investigation_tests", "tb_preliminary_investigation_results",
                "tb_diagnostic_test_type", "tb_sample_test_results", "clinical_decision", "leprosy_diagnostic_method",
                "leprosy_investigation_results", "tb_treatment_initiated", "leprosy_treatment_initiated", "eligible_for_tpt",
                "tpt_initiation_date", "tb_treatment_start_date", "leprosy_treatment_start_date", "hiv_tested",
                "poor_sample_quality_prompt"};

        private static final String[] RECORD_VISIT_PARAMS = {"has_sample_been_collected", "sample_collection_date", "sputum_container_provided",
                "extra_sputum_container_required"};

        private static final String[] FOLLOW_UP_PARAMS = {"follow_up_reason", "follow_up_outcome", "reason_client_not_found",
                "returned_to_treatment", "service_access_challenges", "reasons_for_not_returning_to_services_while_not_facing_challenges",
                "client_challenge_types", "health_facility_challenges_detail", "return_to_treatment_prompt"};

        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

        @Override
        protected void processAncCard(String has_card, Context context) {
            linearLayoutAncCard.setVisibility(View.GONE);
        }

        @Override
        protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
            // not used for TB/Leprosy history
        }

        @Override
        public void processViewData(List<Visit> visits, Context context) {
            if (!visits.isEmpty()) {
                int days = 0;
                List<VisitDisplayItem> visitItems = new ArrayList<>();

                int index = 0;
                while (index < visits.size()) {
                    Visit visit = visits.get(index);

                    if (index == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }

                    if (StringUtils.equalsIgnoreCase(visit.getVisitType(), Constants.EVENT_TYPE.TB_LEPROSY_CLIENT_OBSERVATION)) {
                        LinkedHashMap<String, String> details = extractVisitDetails(visits, OBSERVATION_PARAMS, index, context);
                        visitItems.add(new VisitDisplayItem(details, OBSERVATION_PREFIX, R.string.tbleprosy_observation_results_visit_title, visit));
                    } else if (StringUtils.equalsIgnoreCase(visit.getVisitType(), Constants.EVENT_TYPE.TB_LEPROSY_RECORD_VISIT)) {
                        LinkedHashMap<String, String> details = extractVisitDetails(visits, RECORD_VISIT_PARAMS, index, context);
                        visitItems.add(new VisitDisplayItem(details, RECORD_VISIT_PREFIX, R.string.tbleprosy_record_visit_visit_title, visit));
                    } else if (StringUtils.equalsIgnoreCase(visit.getVisitType(), Constants.EVENT_TYPE.TB_LEPROSY_FOLLOW_UP_VISIT)) {
                        LinkedHashMap<String, String> details = extractVisitDetails(visits, FOLLOW_UP_PARAMS, index, context);
                        visitItems.add(new VisitDisplayItem(details, FOLLOW_UP_PREFIX, R.string.tbleprosy_followup_visit_title, visit));
                    }

                    index++;
                }

                processLastVisit(days, context);
                processVisit(visitItems, context);
            }
        }

        private void processLastVisit(int days, Context context) {
            linearLayoutLastVisit.setVisibility(View.VISIBLE);
            if (days < 1) {
                customFontTextViewLastVisit.setText(org.smartregister.chw.core.R.string.less_than_twenty_four);
            } else {
                customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(context.getString(org.smartregister.chw.core.R.string.days_ago), String.valueOf(days))));
            }
        }

        private LinkedHashMap<String, String> extractVisitDetails(List<Visit> sourceVisits, String[] params, int iteration, Context context) {
            LinkedHashMap<String, String> visitDetailsMap = new LinkedHashMap<>();
            for (String param : params) {
                try {
                    List<VisitDetail> details = sourceVisits.get(iteration).getVisitDetails().get(param);
                    visitDetailsMap.put(param, getTexts(context, details));
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            return visitDetailsMap;
        }

        private void processVisit(List<VisitDisplayItem> visitItems, Context context) {
            if (visitItems != null && !visitItems.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                for (VisitDisplayItem item : visitItems) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    view.findViewById(R.id.title).setVisibility(View.GONE);
                    TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                    TextView tvEdit = view.findViewById(R.id.textview_edit);
                    tvEdit.setVisibility(View.GONE);

                    String visitType = context.getString(item.titleRes);
                    tvTypeOfService.setText(MessageFormat.format("{0} - {1}", visitType, simpleDateFormat.format(item.visit.getDate())));

                    for (Map.Entry<String, String> entry : item.values.entrySet()) {
                        TextView visitDetailTv = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        visitDetailTv.setLayoutParams(params);
                        float scale = context.getResources().getDisplayMetrics().density;
                        int dpAsPixels = (int) (10 * scale + 0.5f);
                        visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                        visitDetailsLayout.addView(visitDetailTv);

                        int resource = context.getResources().getIdentifier(item.prefix + entry.getKey(),
                                "string", context.getPackageName());
                        evaluateView(context, item.values, visitDetailTv, entry.getKey(), resource, item.prefix);
                    }

                    linearLayoutHealthFacilityVisitDetails.addView(view, 0);
                }
            }
        }

        private void evaluateView(Context context, Map<String, String> values, TextView textView, String key, int titleRes, String valuePrefix) {
            if (StringUtils.isNotBlank(getMapValue(values, key))) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                if (titleRes != 0) {
                    builder.append(context.getString(titleRes), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");
                } else {
                    builder.append(key, boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");
                }

                String value = getMapValue(values, key);
                if (value.contains(",")) {
                    for (String item : value.split(",")) {
                        String cleanValue = item.trim().replaceAll("^\\[|\\]$", "");
                        String displayValue = getValueString(context, valuePrefix, cleanValue);
                        builder.append(displayValue + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    builder.append(getValueString(context, valuePrefix, value)).append("\n");
                }

                textView.setText(builder);
            } else {
                textView.setVisibility(View.GONE);
            }
        }

        private String getMapValue(Map<String, String> map, String key) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            return "";
        }

        private String getValueString(Context context, String prefix, String valueKey) {
            String normalizedKey = normalizeResourceName(valueKey);
            int resourceId = context.getResources().getIdentifier(prefix + normalizedKey, "string", context.getPackageName());
            if (resourceId == 0) {
                resourceId = context.getResources().getIdentifier(prefix + valueKey.trim(), "string", context.getPackageName());
            }
            if (resourceId == 0) {
                return valueKey.trim();
            }
            try {
                return context.getString(resourceId);
            } catch (Exception e) {
                Timber.e(e);
                return valueKey.trim();
            }
        }

        private String normalizeResourceName(String value) {
            String normalized = value.trim().toLowerCase(Locale.getDefault()).replaceAll("[^a-z0-9_]", "_");
            return normalized.replaceAll("_+", "_");
        }

        private static class VisitDisplayItem {
            private final LinkedHashMap<String, String> values;
            private final String prefix;
            private final int titleRes;
            private final Visit visit;

            VisitDisplayItem(LinkedHashMap<String, String> values, String prefix, int titleRes, Visit visit) {
                this.values = values;
                this.prefix = prefix;
                this.titleRes = titleRes;
                this.visit = visit;
            }
        }
    }
}
