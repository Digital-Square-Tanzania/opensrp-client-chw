package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.COUNT;
import static org.smartregister.chw.core.utils.CoreJsonFormUtils.getEditEvent;
import static org.smartregister.chw.core.utils.CoreJsonFormUtils.getFormWithMetaData;
import static org.smartregister.chw.core.utils.CoreJsonFormUtils.updateValues;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.VISIT_ID;

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

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.presenter.BaseAncMedicalHistoryPresenter;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.ayp.util.VisitUtils;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.chw.interactor.HarmReductionUsedNeedlesAndSyringesCollectionDetailsInteractor;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import timber.log.Timber;

public class HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivity extends CoreAncMedicalHistoryActivity {
    private static final String[] COLLECTION_FIELDS = {
            "date_of_collection",
            "maskani_name",
            "collection_site_gps",
            "number_of_used_needles_and_syringes_collected",
            "issues_challenges_related_to_collection_of_used_needles_and_syringes"
    };

    private final Flavor flavor = new UsedNeedlesAndSyringesCollectionDetailsActivityFlv();

    private ProgressBar progressBar;

    static String formatCollectionVisitTimestamp(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static void startMe(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(
                new HarmReductionUsedNeedlesAndSyringesCollectionDetailsInteractor(),
                this,
                getBaseEntityId()
        );
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(R.string.harm_reduction_back_to_all_used_needles_collection_sessions));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.harm_reduction_used_needles_collection_details));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(R.string.harm_reduction_used_needles_collection);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                String encounterType = form.getString(JsonFormUtils.ENCOUNTER_TYPE);
                if (Constants.EVENT_TYPE.HARM_REDUCTION_USED_NEEDLES_AND_SYRINGES_COLLECTION.equals(encounterType)) {
                    if (form.has(VISIT_ID)) {
                        String deletedVisitId = form.getString(VISIT_ID);
                        form.remove(VISIT_ID);
                        VisitUtils.deleteProcessedVisit(deletedVisitId, getBaseEntityId());
                    }

                    Event baseEvent = org.smartregister.chw.harmreduction.util.JsonFormUtils.processJsonForm(
                            allSharedPreferences,
                            CoreReferralUtils.setEntityId(jsonString, getBaseEntityId()),
                            Constants.TABLES.HARM_REDUCTION_SAFETY_BOX_COLLECTION
                    );
                    org.smartregister.chw.harmreduction.util.JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                    NCUtils.processEvent(
                            baseEvent.getBaseEntityId(),
                            new JSONObject(org.smartregister.chw.harmreduction.util.JsonFormUtils.gson.toJson(baseEvent))
                    );
                    finish();
                }
            } catch (Exception e) {
                Timber.e(e, "HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivity --> onActivityResult");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private String getBaseEntityId() {
        Intent intent = getIntent();
        return intent == null ? null : intent.getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
    }

    private static class UsedNeedlesAndSyringesCollectionDetailsActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
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
                List<LinkedHashMap<String, String>> collectionVisits = new ArrayList<>();

                int x = 0;
                while (x < visits.size()) {
                    LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
                    if (x == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }

                    extractVisitDetails(visits, COLLECTION_FIELDS, visitDetails, x, context);
                    collectionVisits.add(visitDetails);
                    x++;
                }

                processLastVisit(days, context);
                processVisit(collectionVisits, context, visits);
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
            linearLayoutLastVisit.setVisibility(View.GONE);
            if (days < 1) {
                customFontTextViewLastVisit.setText(org.smartregister.chw.core.R.string.less_than_twenty_four);
            } else {
                customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(
                        context.getString(org.smartregister.chw.core.R.string.days_ago),
                        String.valueOf(days)
                )));
            }
        }

        protected void processVisit(List<LinkedHashMap<String, String>> collectionVisits, Context context, List<Visit> visits) {
            if (collectionVisits != null && !collectionVisits.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                int x = 0;
                for (LinkedHashMap<String, String> vals : collectionVisits) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    view.findViewById(R.id.title).setVisibility(View.GONE);
                    TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                    TextView tvEdit = view.findViewById(R.id.textview_edit);
                    Visit visit = visits.get(x);

                    if (x == visits.size() - 1) {
                        tvEdit.setVisibility(View.VISIBLE);
                    } else {
                        tvEdit.setVisibility(View.GONE);
                    }

                    tvEdit.setOnClickListener(view1 -> {
                        if (visit.getBaseEntityId() != null) {
                            startFormForEdit(
                                    R.string.harm_reduction_used_needles_collection,
                                    Constants.FORMS.HARM_REDUCTION_SAFETY_BOX_COLLECTION,
                                    visit.getBaseEntityId(),
                                    visit.getVisitId(),
                                    context
                            );
                        }
                    });

                    tvTypeOfService.setText(context.getString(R.string.harm_reduction_used_needles_collection) + " - " + formatCollectionVisitTimestamp(visit.getDate()));

                    for (Map.Entry<String, String> entry : vals.entrySet()) {
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

                        try {
                            int resource = context.getResources().getIdentifier("harm_reduction_" + entry.getKey(), "string", context.getPackageName());
                            evaluateView(context, visitDetailTv, entry.getValue(), resource);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                    linearLayoutHealthFacilityVisitDetails.addView(view, 0);
                    x++;
                }
            }
        }

        private void evaluateView(Context context, TextView tv, String stringValue, int viewTitleStringResource) {
            if (StringUtils.isBlank(stringValue) || "null".equalsIgnoreCase(stringValue)) {
                tv.setVisibility(View.GONE);
                return;
            }

            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            if (viewTitleStringResource != 0) {
                spannableStringBuilder.append(context.getString(viewTitleStringResource), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");
            }

            if (stringValue.charAt(0) == '[' && stringValue.charAt(stringValue.length() - 1) == ']') {
                String trimmedValue = stringValue.substring(1, stringValue.length() - 1).trim();
                if (trimmedValue.contains(",")) {
                    String[] stringValueArray = trimmedValue.split(",");
                    for (String value : stringValueArray) {
                        spannableStringBuilder.append(getStringResource(context, "harm_reduction_", value.trim()) + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    spannableStringBuilder.append(getStringResource(context, "harm_reduction_", trimmedValue)).append("\n");
                }
            } else {
                spannableStringBuilder.append(stringValue).append("\n");
            }

            tv.setText(spannableStringBuilder);
        }

        private String getStringResource(Context context, String prefix, String resourceName) {
            int resourceId = context.getResources().getIdentifier(prefix + resourceName.trim(), "string", context.getPackageName());
            try {
                return resourceId == 0 ? resourceName : context.getString(resourceId);
            } catch (Exception e) {
                Timber.e(e);
                return resourceName;
            }
        }

        public void startFormForEdit(Integer titleResource, String formName, String baseEntityId, String deletedVisitId, Context context) {
            try {
                Event event = getEditEvent(baseEntityId, Constants.EVENT_TYPE.HARM_REDUCTION_USED_NEEDLES_AND_SYRINGES_COLLECTION);
                if (event == null) {
                    return;
                }

                final List<Obs> observations = event.getObs();
                JSONObject form = getFormWithMetaData(baseEntityId, context, formName, Constants.EVENT_TYPE.HARM_REDUCTION_USED_NEEDLES_AND_SYRINGES_COLLECTION);

                if (form != null) {
                    JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                    JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                    updateValues(jsonArray, observations);

                    if (form.getInt(COUNT) > 1) {
                        for (int i = 2; i <= form.getInt(COUNT); i++) {
                            JSONArray stepFields = form.getJSONObject("step" + i).getJSONArray(JsonFormUtils.FIELDS);
                            updateValues(stepFields, observations);
                        }
                    }

                    form.put(VISIT_ID, deletedVisitId);
                    ((Activity) context).startActivityForResult(
                            getStartEditFormIntent(form, context.getString(titleResource), context),
                            JsonFormUtils.REQUEST_CODE_GET_JSON
                    );
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        public Intent getStartEditFormIntent(JSONObject jsonForm, String title, Context context) {
            Intent intent = FormUtils.getStartFormActivity(jsonForm, null, context);
            intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

            Form form = new Form();
            form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
            form.setName(title);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);

            try {
                form.setWizard(jsonForm.getInt(COUNT) > 1);
            } catch (JSONException e) {
                Timber.e(e);
                form.setWizard(false);
            }
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
            return intent;
        }
    }
}
