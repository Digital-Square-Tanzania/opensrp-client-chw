package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import org.smartregister.chw.interactor.IccmMedicalHistoryInteractor;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.util.Constants;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class IccmMedicalHistoryActivity extends CoreAncMedicalHistoryActivity {
    private static IccmMemberObject iccmMemberObject;

    private final Flavor flavor = new IccmMedicalHistoryActivityFlv();

    private ProgressBar progressBar;

    public static void startMe(Activity activity, IccmMemberObject memberObject) {
        Intent intent = new Intent(activity, IccmMedicalHistoryActivity.class);
        iccmMemberObject = memberObject;
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(new IccmMedicalHistoryInteractor(), this, iccmMemberObject.getBaseEntityId());
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, iccmMemberObject.getFullName()));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.visits_history));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView agywVisitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        agywVisitTitle.setText(R.string.iccm_visit);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    class IccmMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
        private final StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);

        private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

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
                List<LinkedHashMap<String, String>> hf_visits = new ArrayList<>();

                int x = 0;
                while (x < visits.size()) {

                    // the first object in this list is the days difference
                    if (x == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }
                    String[] visitParams = {"is_the_client_pregnant", "medical_history", "other_medical_history", "physical_examination", "interpretation_for_mrdt", "interpretation_for_mrdt_two", "mrdt_results", "dispensed_alu_category", "number_of_alu_tablets_dispensed", "diarrhea_signs", "diarrhea_medication_dispensed", "diarrhea_medication_dispensed_for_referred_clients", "number_of_ors_sachets_dispensed", "number_of_zinc_tablets_dispensed", "number_of_zinc_ors_co_packs_dispensed", "reasons_for_not_dispensing_diarrhea_medication", "other_reasons_for_not_dispensing_diarrhea_medication"};
                    extractVisitDetails(visits, visitParams, hf_visits, x, context);

                    x++;
                }

                processLastVisit(days, context);
                processVisit(hf_visits, context, visits);
            }
        }

        private void extractVisitDetails(List<Visit> sourceVisits, String[] hf_params, List<LinkedHashMap<String, String>> hf_visits, int iteration, Context context) {
            // get the hf details
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
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


        private void processLastVisit(int days, Context context) {
            linearLayoutLastVisit.setVisibility(View.VISIBLE);
            if (days < 1) {
                customFontTextViewLastVisit.setText(org.smartregister.chw.core.R.string.less_than_twenty_four);
            } else {
                customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(context.getString(org.smartregister.chw.core.R.string.days_ago), String.valueOf(days))));
            }
        }


        protected void processVisit(List<LinkedHashMap<String, String>> community_visits, Context context, List<Visit> visits) {
            if (community_visits != null && community_visits.size() > 0) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                int x = 0;
                for (Map<String, String> vals : community_visits) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    TextView tvTitle = view.findViewById(R.id.title);
                    TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                    TextView tvEdit = view.findViewById(R.id.textview_edit);

                    evaluateTitle(context, x, vals, tvTitle);

                    String visitType = visits.get(x).getVisitType();

                    tvTypeOfService.setText(visitType + " - " + simpleDateFormat.format(visits.get(x).getDate()));

                    // Updating visibility of EDIT button if the visit is the last visit
                    if (x == visits.size() - 1) tvEdit.setVisibility(View.VISIBLE);
                    else tvEdit.setVisibility(View.GONE);

                    tvEdit.setOnClickListener(view1 -> {
                        ((Activity) context).finish();
                        Visit visit = visits.get(0);
                        if (visit != null && visit.getVisitType().equalsIgnoreCase(Constants.EVENT_TYPE.ICCM_SERVICES_VISIT) && visit.getBaseEntityId() != null)
                            //TODO fix this iccm Issue
                            IccmServicesActivity.startIccmServicesActivity((Activity) context, iccmMemberObject.getIccmEnrollmentFormSubmissionId(), true);
                    });


                    for (Map.Entry<String, String> entry : vals.entrySet()) {
                        TextView visitDetailTv = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) LinearLayout.LayoutParams.MATCH_PARENT, (int) LinearLayout.LayoutParams.WRAP_CONTENT);

                        visitDetailTv.setLayoutParams(params);
                        float scale = context.getResources().getDisplayMetrics().density;
                        int dpAsPixels = (int) (10 * scale + 0.5f);
                        visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                        visitDetailsLayout.addView(visitDetailTv);


                        try {
                            int resource = context.getResources().getIdentifier("iccm_" + entry.getKey(), "string", context.getPackageName());
                            evaluateView(context, vals, visitDetailTv, entry.getKey(), resource, "");
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
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
            try {
                int resourceId = context.getResources().getIdentifier(prefix + resourceName.trim(), "string", context.getPackageName());
                return context.getString(resourceId);
            } catch (Exception e) {
                return prefix + resourceName;
            }
        }
    }
}
