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
import android.view.Menu;
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
import org.smartregister.chw.interactor.TbLeprosyMobilizationSessionDetailsInteractor;
import org.smartregister.chw.tbleprosy.util.Constants;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class TbLeprosyMobilizationDetailsActivity extends CoreAncMedicalHistoryActivity {

    private static String baseEntityId;

    private final Flavor flavor = new TbLeprosyMobilizationDetailsActivityFlv();
    private ProgressBar progressBar;

    public static void startMe(Activity activity, String baseEntityId) {
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, TbLeprosyMobilizationDetailsActivity.class);
        activity.startActivity(intent);
        TbLeprosyMobilizationDetailsActivity.baseEntityId = baseEntityId;
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(new TbLeprosyMobilizationSessionDetailsInteractor(), this, baseEntityId);
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(R.string.back_to_all_mobilization_sessions));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.tbleprosy_session));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(R.string.tbleprosy_mobilization);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private static class TbLeprosyMobilizationDetailsActivityFlv extends DefaultAncMedicalHistoryActivityFlv {

        private static final String[] DETAILS_KEYS = new String[]{
                "mobilization_date",
                "mobilization_area",
                "other_mobilization_area",
                "female_clients_reached",
                "male_clients_reached",
                "population_type",
                "other_population_type",
                "number_of_IEC_materials_provided"
        };

        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

        @Override
        protected void processAncCard(String has_card, Context context) {
            linearLayoutAncCard.setVisibility(View.GONE);
        }

        @Override
        protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
            // Not used for TB/Leprosy mobilization details.
        }

        @Override
        public void processViewData(List<Visit> visits, Context context) {
            if (visits == null || visits.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.GONE);
                return;
            }

            int days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
            List<LinkedHashMap<String, String>> visitGroups = new ArrayList<>();

            for (int i = 0; i < visits.size(); i++) {
                visitGroups.add(extractVisitDetails(visits, DETAILS_KEYS, i, context));
            }

            processLastVisit(days, context);
            processVisit(visitGroups, context, visits);
        }

        private LinkedHashMap<String, String> extractVisitDetails(List<Visit> sourceVisits, String[] params, int index, Context context) {
            LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
            Map<String, List<VisitDetail>> groupedDetails = sourceVisits.get(index).getVisitDetails();
            if (groupedDetails == null) {
                return visitDetails;
            }

            for (String param : params) {
                try {
                    List<VisitDetail> details = groupedDetails.get(param);
                    visitDetails.put(param, getTexts(context, details));
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            return visitDetails;
        }

        private void processLastVisit(int days, Context context) {
            linearLayoutLastVisit.setVisibility(View.GONE);
            if (days < 1) {
                customFontTextViewLastVisit.setText(org.smartregister.chw.core.R.string.less_than_twenty_four);
            } else {
                customFontTextViewLastVisit.setText(StringUtils.capitalize(
                        MessageFormat.format(context.getString(org.smartregister.chw.core.R.string.days_ago), String.valueOf(days))
                ));
            }
        }

        private void processVisit(List<LinkedHashMap<String, String>> visitsData, Context context, List<Visit> visits) {
            if (visitsData == null || visitsData.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.GONE);
                return;
            }

            linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

            for (int i = 0; i < visitsData.size(); i++) {
                LinkedHashMap<String, String> values = visitsData.get(i);
                View view = inflater.inflate(R.layout.medical_history_visit, null);
                view.findViewById(R.id.title).setVisibility(View.GONE);

                TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
                TextView tvEdit = view.findViewById(R.id.textview_edit);
                LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                tvEdit.setVisibility(View.GONE);

                String visitType = visits.get(i).getVisitType();
                if (StringUtils.equalsIgnoreCase(visitType, Constants.EVENT_TYPE.TB_LEPROSY_MOBILIZATION)) {
                    visitType = context.getString(R.string.tbleprosy_mobilization);
                }
                tvTypeOfService.setText(visitType + " - " + simpleDateFormat.format(visits.get(i).getDate()));

                for (Map.Entry<String, String> entry : values.entrySet()) {
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

                    evaluateView(context, values, visitDetailTv, entry.getKey());
                }

                linearLayoutHealthFacilityVisitDetails.addView(view, 0);
            }
        }

        private void evaluateView(Context context, Map<String, String> values, TextView textView, String key) {
            String value = getMapValue(values, key);
            if (StringUtils.isBlank(value) || StringUtils.equalsIgnoreCase(value, "null")) {
                textView.setVisibility(View.GONE);
                return;
            }

            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(getLabel(context, key), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

            if (value.contains(",")) {
                String[] valueArray = value.split(",");
                for (String item : valueArray) {
                    String cleanValue = normalizeValue(item);
                    builder.append(getValueText(context, cleanValue) + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                builder.append(getValueText(context, normalizeValue(value))).append("\n");
            }

            textView.setText(builder);
        }

        private String getLabel(Context context, String key) {
            int labelResource = context.getResources().getIdentifier(
                    "tbleprosy_mobilization_session_" + key,
                    "string",
                    context.getPackageName()
            );
            if (labelResource == 0) {
                labelResource = context.getResources().getIdentifier(key, "string", context.getPackageName());
            }
            if (labelResource != 0) {
                return context.getString(labelResource);
            }
            return humanizeValue(key);
        }

        private String getValueText(Context context, String valueKey) {
            int valueResource = context.getResources().getIdentifier(valueKey, "string", context.getPackageName());
            if (valueResource == 0) {
                valueResource = context.getResources().getIdentifier("tbleprosy_" + valueKey, "string", context.getPackageName());
            }

            if (valueResource != 0) {
                try {
                    return context.getString(valueResource);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            return humanizeValue(valueKey);
        }

        private String normalizeValue(String value) {
            return value == null ? "" : value.trim().replace("[", "").replace("]", "");
        }

        private String humanizeValue(String key) {
            if (StringUtils.isBlank(key)) {
                return "";
            }

            String withSpaces = key.trim().replaceAll("_+", " ");
            return StringUtils.capitalize(withSpaces);
        }

        private String getMapValue(Map<String, String> map, String key) {
            return map != null && map.containsKey(key) ? map.get(key) : "";
        }
    }
}
