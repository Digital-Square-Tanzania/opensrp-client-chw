package org.smartregister.chw.activity;

import static org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle;

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

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.tbleprosy_observation_results_history));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(R.string.tbleprosy_observation_results_visit_title);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    private static class TbLeprosyObservationResultsActivityFlv extends DefaultAncMedicalHistoryActivityFlv {

        private static final String RESOURCE_PREFIX = "tbleprosy_observation_results_";

        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

        @Override
        protected void processAncCard(String has_card, Context context) {
            linearLayoutAncCard.setVisibility(View.GONE);
        }

        @Override
        protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
            // not used for TB/Leprosy observation results
        }

        @Override
        public void processViewData(List<Visit> visits, Context context) {
            if (!visits.isEmpty()) {
                int days = 0;
                List<LinkedHashMap<String, String>> observationVisits = new ArrayList<>();

                int index = 0;
                while (index < visits.size()) {
                    if (index == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }

                    String[] visitParams = {"investigation_type", "tb_preliminary_investigation_tests", "tb_preliminary_investigation_results",
                            "tb_diagnostic_test_type", "tb_sample_test_results", "clinical_decision", "leprosy_diagnostic_method",
                            "leprosy_investigation_results", "tb_treatment_initiated", "leprosy_treatment_initiated", "eligible_for_tpt",
                            "tpt_initiation_date", "tb_treatment_start_date", "leprosy_treatment_start_date", "hiv_tested",
                            "poor_sample_quality_prompt"};
                    observationVisits.add(extractVisitDetails(visits, visitParams, index, context));

                    index++;
                }

                processLastVisit(days, context);
                processVisit(observationVisits, context, visits);
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

        private void processVisit(List<LinkedHashMap<String, String>> observationVisits, Context context, List<Visit> visits) {
            if (observationVisits != null && !observationVisits.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                int index = 0;
                for (LinkedHashMap<String, String> visitDetails : observationVisits) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    view.findViewById(R.id.title).setVisibility(View.GONE);
                    TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                    TextView tvEdit = view.findViewById(R.id.textview_edit);
                    tvEdit.setVisibility(View.GONE);

                    String visitType = context.getString(R.string.tbleprosy_observation_results_visit_title);
                    tvTypeOfService.setText(MessageFormat.format("{0} - {1}", visitType, simpleDateFormat.format(visits.get(index).getDate())));

                    for (Map.Entry<String, String> entry : visitDetails.entrySet()) {
                        TextView visitDetailTv = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        visitDetailTv.setLayoutParams(params);
                        float scale = context.getResources().getDisplayMetrics().density;
                        int dpAsPixels = (int) (10 * scale + 0.5f);
                        visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                        visitDetailsLayout.addView(visitDetailTv);

                        int resource = context.getResources().getIdentifier(RESOURCE_PREFIX + entry.getKey(),
                                "string", context.getPackageName());
                        evaluateView(context, visitDetails, visitDetailTv, entry.getKey(), resource);
                    }

                    linearLayoutHealthFacilityVisitDetails.addView(view, 0);
                    index++;
                }
            }
        }

        private void evaluateView(Context context, Map<String, String> values, TextView textView, String key, int titleRes) {
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
                        String displayValue = getValueString(context, cleanValue);
                        builder.append(displayValue + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    builder.append(getValueString(context, value)).append("\n");
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

        private String getValueString(Context context, String valueKey) {
            int resourceId = context.getResources().getIdentifier(RESOURCE_PREFIX + valueKey.trim(), "string", context.getPackageName());
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
    }
}
