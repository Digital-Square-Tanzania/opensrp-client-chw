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
import org.smartregister.chw.harmreduction.domain.MemberObject;
import org.smartregister.chw.interactor.HarmReductionMatClientsVisitHistoryInteractor;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class HarmReductionMatClientsVisitHistoryActivity extends CoreAncMedicalHistoryActivity {
    private static final String MAT_CLIENTS_FOLLOWUP_EVENT = "Harm Reduction MAT Clients Followup";
    private static MemberObject harmReductionMemberObject;

    private final Flavor flavor = new HarmReductionMatClientsHistoryActivityFlv();
    private ProgressBar progressBar;

    public static void startMe(Activity activity, MemberObject memberObject) {
        Intent intent = new Intent(activity, HarmReductionMatClientsVisitHistoryActivity.class);
        harmReductionMemberObject = memberObject;
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(
                new HarmReductionMatClientsVisitHistoryInteractor(),
                this,
                harmReductionMemberObject.getBaseEntityId()
        );
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, harmReductionMemberObject.getFullName()));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.visits_history));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(R.string.harm_reduction_mat_clients_followup_visit);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    private static class HarmReductionMatClientsHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
        private static final String[] VISIT_PARAMS = {
                "health_education_provided",
                "education_delivery_method"
        };

        private static final String[] LABEL_PREFIXES = {
                "harm_reduction_mat_clients_",
                "harm_reduction_"
        };

        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

        @Override
        protected void processAncCard(String has_card, Context context) {
            linearLayoutAncCard.setVisibility(View.GONE);
        }

        @Override
        protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
            // no-op
        }

        @Override
        public void processViewData(List<Visit> visits, Context context) {
            if (!visits.isEmpty()) {
                int days = 0;
                List<LinkedHashMap<String, String>> matClientVisits = new ArrayList<>();

                int x = 0;
                while (x < visits.size()) {
                    LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
                    if (x == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }

                    extractVisitDetails(visits, VISIT_PARAMS, visitDetails, x, context);
                    matClientVisits.add(visitDetails);
                    x++;
                }

                processLastVisit(days, context);
                processVisit(matClientVisits, context, visits);
            }
        }

        private void extractVisitDetails(List<Visit> sourceVisits, String[] params, LinkedHashMap<String, String> visitDetailsMap, int iteration, Context context) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for (String param : params) {
                try {
                    List<VisitDetail> details = sourceVisits.get(iteration).getVisitDetails().get(param);
                    map.put(param, getTexts(context, details));
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            visitDetailsMap.putAll(map);
        }

        private void processLastVisit(int days, Context context) {
            linearLayoutLastVisit.setVisibility(View.VISIBLE);
            if (days < 1) {
                customFontTextViewLastVisit.setText(org.smartregister.chw.core.R.string.less_than_twenty_four);
            } else {
                customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(context.getString(org.smartregister.chw.core.R.string.days_ago), String.valueOf(days))));
            }
        }

        protected void processVisit(List<LinkedHashMap<String, String>> matClientVisits, Context context, List<Visit> visits) {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            if (matClientVisits != null && !matClientVisits.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                int x = 0;
                for (LinkedHashMap<String, String> vals : matClientVisits) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    view.findViewById(R.id.title).setVisibility(View.GONE);
                    TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                    TextView tvEdit = view.findViewById(R.id.textview_edit);

                    if (x == visits.size() - 1) {
                        tvEdit.setVisibility(View.VISIBLE);
                    } else {
                        tvEdit.setVisibility(View.GONE);
                    }

                    tvEdit.setOnClickListener(view1 -> {
                        Visit visit = visits.get(0);
                        if (visit.getBaseEntityId() != null) {
                            ((Activity) context).finish();
                            HarmReductionMatClientsVisitActivity.startHarmReductionMatClientsVisitActivity((Activity) context, visit.getBaseEntityId(), true);
                        }
                    });

                    String visitType = visits.get(x).getVisitType();
                    if (MAT_CLIENTS_FOLLOWUP_EVENT.equals(visitType)) {
                        visitType = context.getString(R.string.harm_reduction_mat_clients_followup_visit);
                    }
                    tvTypeOfService.setText(String.format("%s - %s", visitType, simpleDateFormat.format(visits.get(x).getDate())));

                    for (Map.Entry<String, String> entry : vals.entrySet()) {
                        TextView visitDetailTv = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                                (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                        visitDetailTv.setLayoutParams(params);
                        float scale = context.getResources().getDisplayMetrics().density;
                        int dpAsPixels = (int) (10 * scale + 0.5f);
                        visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                        visitDetailsLayout.addView(visitDetailTv);

                        try {
                            int resource = resolveFieldLabelResource(context, entry.getKey());
                            evaluateView(context, vals, visitDetailTv, entry.getKey(), resource);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                    linearLayoutHealthFacilityVisitDetails.addView(view, 0);
                    x++;
                }
            }
        }

        private int resolveFieldLabelResource(Context context, String key) {
            for (String prefix : LABEL_PREFIXES) {
                int resource = context.getResources().getIdentifier(prefix + key, "string", context.getPackageName());
                if (resource != 0) {
                    return resource;
                }
            }
            return 0;
        }

        private void evaluateView(Context context, Map<String, String> vals, TextView tv, String valueKey, int viewTitleStringResource) {
            if (StringUtils.isNotBlank(getMapValue(vals, valueKey))) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                String fieldLabel = viewTitleStringResource != 0 ? context.getString(viewTitleStringResource) : StringUtils.capitalize(valueKey.replace("_", " "));
                spannableStringBuilder.append(fieldLabel, boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

                String stringValue = getMapValue(vals, valueKey);
                if (stringValue.contains(",")) {
                    String[] stringValueArray = stringValue.split(",");
                    for (String value : stringValueArray) {
                        String mValue = value.trim().replaceAll("^\\[|]$", "");
                        spannableStringBuilder.append(getStringResource(context, mValue)).append("\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    spannableStringBuilder.append(getStringResource(context, stringValue)).append("\n");
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

        private String getStringResource(Context context, String resourceName) {
            for (String prefix : LABEL_PREFIXES) {
                int resourceId = context.getResources()
                        .getIdentifier(prefix + resourceName.trim(), "string", context.getPackageName());
                if (resourceId != 0) {
                    try {
                        return context.getString(resourceId);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }
            return resourceName;
        }
    }
}
