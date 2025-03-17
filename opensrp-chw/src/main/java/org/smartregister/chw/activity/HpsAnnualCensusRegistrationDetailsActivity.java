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
import org.smartregister.chw.cecap.util.VisitUtils;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hps.util.Constants;
import org.smartregister.chw.interactor.HpsAnnualCensusDetailsInteractor;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusRegistrationDetailsActivity extends CoreAncMedicalHistoryActivity {
    private static String baseEntityId;

    private final Flavor flavor = new AnnualCensusRegisterDetailsActivityFlv();

    private ProgressBar progressBar;

    public static void startMe(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, HpsAnnualCensusRegistrationDetailsActivity.class);
        activity.startActivity(intent);
        HpsAnnualCensusRegistrationDetailsActivity.baseEntityId = baseEntityId;
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(new HpsAnnualCensusDetailsInteractor(), this, baseEntityId);
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(R.string.hps_back_to_all_annual_census_register));

        findViewById(R.id.medical_history).setVisibility(View.GONE);
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(R.string.hps_annual_census_register_details);
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
                if (encounterType.equals(Constants.EVENT_TYPE.HPS_ANNUAL_CENSUS)) {
                    if (form.has(VISIT_ID)) {
                        String deletedVisitId = form.getString(VISIT_ID);
                        form.remove(VISIT_ID);
                        VisitUtils.deleteProcessedVisit(deletedVisitId, baseEntityId);
                    }

                    Event baseEvent = org.smartregister.chw.anc.util.JsonFormUtils.processJsonForm(allSharedPreferences, CoreReferralUtils.setEntityId(jsonString, baseEntityId), Constants.TABLES.HPS_ANNUAL_CENSUS_REGISTER);
                    org.smartregister.chw.anc.util.JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                    NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(baseEvent)));
                    finish();
                }
            } catch (Exception e) {
                Timber.e(e, "HpsAnnualCensusRegistrationDetailsActivity -- > onActivityResult");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private static class AnnualCensusRegisterDetailsActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
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

            if (!visits.isEmpty()) {
                int days = 0;
                List<LinkedHashMap<String, String>> hf_visits = new ArrayList<>();

                int x = 0;
                while (x < visits.size()) {
                    LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
                    // the first object in this list is the days difference
                    if (x == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }

                    String[] fields = {
                            "select_age_group",
                            "number_of_male_by_age_group_<1",
                            "number_of_female_by_age_group_<1",
                            "number_of_male_by_age_group_1_4",
                            "number_of_female_by_age_group_1_4",
                            "number_of_male_by_age_group_5_14",
                            "number_of_female_by_age_group_5_14",
                            "number_of_male_by_age_group_15_49",
                            "number_of_female_by_age_group_15_49",
                            "number_of_male_by_age_group_50_59",
                            "number_of_female_by_age_group_50_59",
                            "number_of_male_by_age_group_60_+",
                            "number_of_female_by_age_group_60_+",
                            "number_of_house_hold",
                            "number_of_house_hold_with_road_access",
                            "number_of_house_holds_with_at_least_one_landline_or_mobile_phone",
                            "number_of_house_hold_with_basic_nutrition_source_vegetable",
                            "number_of_house_hold_with_basic_nutrition_source_fruit_trees",
                            "number_of_house_hold_with_basic_nutrition_source_domestic_animal",
                            "select_centers_category",
                            "number_of_pre_schools_government",
                            "number_of_primary_schools_government",
                            "number_of_secondary_schools_government",
                            "number_of_universities_government",
                            "number_of_special_needs_schools_government",
                            "number_of_dispensary_government",
                            "number_of_health_centers_government",
                            "number_of_hospital_government",
                            "number_of_special_clinics_government",
                            "number_of_laboratory_government",
                            "number_of_pharmacy_government",
                            "number_of_ADDO_government",
                            "number_of_maternity_home_government",
                            "number_of_orphan_care_centers_government",
                            "number_of_centers_for_children_with_disabilities_government",
                            "number_of_cbecdc_rehabilitation_centre_government",
                            "number_of_day_care_centers_government",
                            "number_of_elderly_care_centers_government",
                            "number_of_pre_schools_faith_based_organisation",
                            "number_of_primary_schools_faith_based_organisation",
                            "number_of_secondary_schools_faith_based_organisation",
                            "number_of_universities_faith_based_organisation",
                            "number_of_special_needs_schools_faith_based_organisation",
                            "number_of_dispensary_faith_based_organisation",
                            "number_of_health_centers_faith_based_organisation",
                            "number_of_hospital_faith_based_organisation",
                            "number_of_special_clinics_faith_based_organisation",
                            "number_of_laboratory_faith_based_organisation",
                            "number_of_pharmacy_faith_based_organisation",
                            "number_of_ADDO_faith_based_organisation",
                            "number_of_maternity_home_faith_based_organisation",
                            "number_of_orphan_care_centers_faith_based_organisation",
                            "number_of_centers_for_children_with_disabilities_faith_based_organisation",
                            "number_of_cbecdc_rehabilitation_centre_faith_based_organisation",
                            "number_of_day_care_centers_faith_based_organisation",
                            "number_of_elderly_care_centers_faith_based_organisation",
                            "number_of_pre_schools_public",
                            "number_of_primary_schools_public",
                            "number_of_secondary_schools_public",
                            "number_of_universities_public",
                            "number_of_special_needs_schools_public",
                            "number_of_dispensary_public",
                            "number_of_health_centers_public",
                            "number_of_hospital_public",
                            "number_of_special_clinics_public",
                            "number_of_laboratory_public",
                            "number_of_pharmacy_public",
                            "number_of_ADDO_public",
                            "number_of_maternity_home_public",
                            "number_of_orphan_care_centers_public",
                            "number_of_centers_for_children_with_disabilities_public",
                            "number_of_cbecdc_rehabilitation_centre_public",
                            "number_of_day_care_centers_public",
                            "number_of_elderly_care_centers_public",
                            "number_of_pre_schools_private",
                            "number_of_primary_schools_private",
                            "number_of_secondary_schools_private",
                            "number_of_universities_private",
                            "number_of_special_needs_schools_private",
                            "number_of_dispensary_private",
                            "number_of_health_centers_private",
                            "number_of_hospital_private",
                            "number_of_special_clinics_private",
                            "number_of_laboratory_private",
                            "number_of_pharmacy_private",
                            "number_of_ADDO_private",
                            "number_of_maternity_home_private",
                            "number_of_orphan_care_centers_private",
                            "number_of_centers_for_children_with_disabilities_private",
                            "number_of_cbecdc_rehabilitation_centre_private",
                            "number_of_day_care_centers_private",
                            "number_of_elderly_care_centers_private",
                            "number_of_households_most_commonly_use_tap_as_sources_of_water",
                            "number_of_households_most_commonly_use_river_as_sources_of_water",
                            "number_of_households_most_commonly_use_shallow_well_as_sources_of_water",
                            "number_of_households_most_commonly_use_water_pond_as_sources_of_water",
                            "number_of_households_most_commonly_use_small_dam_as_sources_of_water",
                            "number_of_households_most_commonly_use_lake_as_sources_of_water",
                            "number_of_households_most_commonly_use_spring_as_sources_of_water",
                            "number_of_households_using_electricity_as_source_of_energy_for_lighting",
                            "number_of_households_using_solar_as_source_of_energy_for_lighting",
                            "number_of_households_using_kerosine_as_source_of_energy_for_lighting",
                            "number_of_households_using_koroboi_as_source_of_energy_for_lighting",
                            "number_of_households_using_other_source_of_energy_for_lighting",
                            "number_of_households_using_electricity_as_source_of_cooking_energy",
                            "number_of_households_using_solar_as_source_of_cooking_energy",
                            "number_of_households_using_kerosine_as_source_of_cooking_energy",
                            "number_of_households_using_gas_as_source_of_cooking_energy",
                            "number_of_households_using_charcoal_as_source_of_cooking_energy",
                            "number_of_households_using_firewood_as_source_of_cooking_energy",
                            "number_of_male_capable_of_engaging_in_economic_activities",
                            "number_of_female_capable_of_engaging_in_economic_activities",
                            "number_of_male_engaged_in_economic_activities",
                            "number_of_female_engaged_in_economic_activities",
                            "number_of_health_committee_members_for_effective_committee_meetings",
                            "number_of_committee_members_attended_fisrt_quarter",
                            "number_of_committee_members_attended_second_quarter",
                            "number_of_committee_members_attended_third_quarter",
                            "number_of_committee_members_attended_fourth_quarter",
                            "number_of_registered_alternative_medicine_service_providers",
                            "number_of_registered_traditional_medicine_service_providers",
                            "number_of_unregistered_alternative_medicine_service_providers",
                            "number_of_unregistered_traditional_medicine_service_providers",
                            "number_of_households_inspected",
                            "number_of_households_with_good_latrine",
                            "number_of_households_without_good_latrine",
                            "number_of_households_with_no_latrine",
                            "number_of_households_with_waste_disposal_pits",
                            "number_of_households_with_dish_racks_for_drying_utensils",
                            "number_of_households_near_clean_water_sources",
                            "number_of_households_with_economic_problems",
                            "number_of_households_with_social_problems",
                            "number_of_households_with_handwashing_facilities_after_using_the_toilet",
                            "number_of_households_that_have_been_sprayed_with_insecticide",
                            "number_of_food_shop_visited",
                            "number_of_restaurants_visited",
                            "number_of_butcheries_visited",
                            "number_of_bar_and_clubs_visited",
                            "number_of_guest_house_visited",
                            "number_of_loca_food_vendors_visited",
                            "number_of_markets_visited",
                            "number_of_public_toilets_visited",
                            "number_of_bus_stations_visited",
                            "number_of_primary_schools_visited",
                            "number_of_secondary_schools_visited",
                            "number_of_hospital_visited",
                            "number_of_health_centers_visited",
                            "number_of_dispensaries_visited",
                            "number_of_offices_visited",
                            "number_of_universities_college_visited",
                            "number_of_food_shop_that_Met_the_Standards",
                            "number_of_restaurants_that_Met_the_Standards",
                            "number_of_butcheries_that_Met_the_Standards",
                            "number_of_bar_and_clubs_that_Met_the_Standards",
                            "number_of_guest_house_that_Met_the_Standards",
                            "number_of_loca_food_vendors_that_Met_the_Standards",
                            "number_of_markets_that_Met_the_Standards",
                            "number_of_public_toilets_that_Met_the_Standards",
                            "number_of_bus_stations_that_Met_the_Standards",
                            "number_of_primary_schools_that_Met_the_Standards",
                            "number_of_secondary_schools_that_Met_the_Standards",
                            "number_of_hospital_that_Met_the_Standards",
                            "number_of_health_centers_that_Met_the_Standards",
                            "number_of_dispensaries_that_Met_the_Standards",
                            "number_of_offices_that_Met_the_Standards",
                            "number_of_universities_college_that_Met_the_Standards",
                            "number_of_inspected_agriculture_areas",
                            "number_of_inspected_livestock_keeping_areas",
                            "number_of_inspected_fishing_areas",
                            "number_of_inspected_industries_areas",
                            "number_of_inspected_offices_areas",
                            "number_of_inspected_transportation_areas",
                            "number_of_other_inspected_areas",
                            "number_of_agriculture_areas_inspected_with_risk_indicators",
                            "number_of_livestock_keeping_areas_inspected_with_risk_indicators",
                            "number_of_fishing_areas_inspected_with_risk_indicators",
                            "number_of_industries_areas_inspected_with_risk_indicators",
                            "number_of_offices_areas_inspected_with_risk_indicators",
                            "number_of_transportation_areas_inspected_with_risk_indicators",
                            "number_of_other_areas_inspected_with_risk_indicators",
                            "number_of_inspected_grains",
                            "number_of_inspected_legumes",
                            "number_of_inspected_meat",
                            "number_of_inspected_fishing",
                            "number_of_inspected_alcoholic_beverages",
                            "number_of_inspected_non_alcoholic_beverages",
                            "number_of_grains_discarded",
                            "number_of_legumes_discarded",
                            "number_of_meat_discarded",
                            "number_of_fishing_discarded",
                            "number_of_alcoholic_beverage_discarded",
                            "number_of_non_alcoholic_beverage_discarded",
                            "health_reports_affecting_people_in_workplaces_respiratory_diseases",
                            "health_reports_affecting_people_in_workplaces_toxic_chemicals",
                            "health_reports_affecting_people_in_workplaces_burns",
                            "health_reports_affecting_people_in_workplaces_hearing_loss",
                            "health_reports_affecting_people_in_workplaces_eye_problems",
                            "health_reports_affecting_people_in_workplaces_other_effects",
                            "amount_of_solid_waste_generated_annually_tons",
                            "amount_of_solid_waste_disposed_at_a_designated_site_annually_tons",
                            "number_of_waste_collection_equipment_vehicles",
                            "number_of_waste_collection_equipment_tractors",
                            "number_of_waste_collection_equipment_carts",
                            "number_of_waste_collection_equipment_wheelbarrows",
                            "number_of_waste_collection_equipment_others",
                            "number_of_areas_sprayed_with_pesticides_ponds",
                            "number_of_areas_sprayed_with_pesticides_cans",
                            "number_of_areas_sprayed_with_pesticides_drums",
                            "number_of_areas_sprayed_with_pesticides_barrels",
                            "number_of_areas_sprayed_with_pesticides_coconut_shells",
                            "number_of_times_spraying_was_done_ponds",
                            "number_of_times_spraying_was_done_cans",
                            "number_of_times_spraying_was_done_drums",
                            "number_of_times_spraying_was_done_barrels",
                            "number_of_times_spraying_was_done_coconut_shells",
                            "types_of_pesticides_used_ponds",
                            "types_of_pesticides_used_cans",
                            "types_of_pesticides_used_drums",
                            "types_of_pesticides_used_barrels",
                            "types_of_pesticides_used_coconut_shells",
                            "amount_of_pesticide_used_ponds",
                            "amount_of_pesticide_used_cans",
                            "amount_of_pesticide_used_drums",
                            "amount_of_pesticide_used_barrels",
                            "amount_of_pesticide_used_coconut_shells"
                    };

                    extractVisitDetails(visits, fields, visitDetails, x, context);


                    hf_visits.add(visitDetails);

                    x++;
                }

                processLastVisit(days, context);
                processVisit(hf_visits, context, visits);
            }
        }

        private void extractVisitDetails(List<Visit> sourceVisits, String[] hf_params, LinkedHashMap<String, String> visitDetailsMap, int iteration, Context context) {
            // get the hf details
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for (String param : hf_params) {
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
                customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(context.getString(org.smartregister.chw.core.R.string.days_ago), String.valueOf(days))));
            }
        }


        protected void processVisit(List<LinkedHashMap<String, String>> community_visits, Context context, List<Visit> visits) {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            if (community_visits != null && !community_visits.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                int x = 0;
                for (LinkedHashMap<String, String> vals : community_visits) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    view.findViewById(R.id.title).setVisibility(View.GONE);
                    TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                    TextView tvEdit = view.findViewById(R.id.textview_edit);

                    // Updating visibility of EDIT button if the visit is the last visit
                    if ((x == visits.size() - 1)) tvEdit.setVisibility(View.VISIBLE);
                    else tvEdit.setVisibility(View.GONE);

                    tvEdit.setOnClickListener(view1 -> {
                        Visit visit = visits.get(0);

                        if (visit.getBaseEntityId() != null) {
                            startFormForEdit(R.string.hps_annual_census_register_title, Constants.FORMS.HPS_ANNUAL_CENSUS, visit.getBaseEntityId(), visit.getVisitId(), context);

                        }
                    });

                    String visitType = visits.get(x).getVisitType();
                    ;


                    tvTypeOfService.setText(visitType + " - " + simpleDateFormat.format(visits.get(x).getDate()));


                    for (LinkedHashMap.Entry<String, String> entry : vals.entrySet()) {
                        TextView visitDetailTv = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                        visitDetailTv.setLayoutParams(params);
                        float scale = context.getResources().getDisplayMetrics().density;
                        int dpAsPixels = (int) (10 * scale + 0.5f);
                        visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                        visitDetailsLayout.addView(visitDetailTv);


                        try {
                            int resource = context.getResources().getIdentifier("hps_" + entry.getKey(), "string", context.getPackageName());
                            evaluateView(context, vals, visitDetailTv, entry.getKey(), resource, "hps_");
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                    linearLayoutHealthFacilityVisitDetails.addView(view, 0);

                    x++;
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
                } else if (stringValue.charAt(0) == '[' && stringValue.charAt(stringValue.length() - 1) == ']') {
                    spannableStringBuilder.append(getStringResource(context, valuePrefixInStringResources, stringValue.substring(1, stringValue.length() - 1))).append("\n");
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
            int resourceId = context.getResources().getIdentifier(prefix + resourceName.trim(), "string", context.getPackageName());
            try {
                return context.getString(resourceId);
            } catch (Exception e) {
                Timber.e(e);
                return resourceName;
            }
        }

        public void startFormForEdit(Integer title_resource, String formName, String baseEntityId, String deletedVisitId, Context context) {
            try {

                Event event = getEditEvent(baseEntityId, Constants.EVENT_TYPE.HPS_ANNUAL_CENSUS);

                final List<Obs> observations = event.getObs();
                JSONObject form = getFormWithMetaData(baseEntityId, context, formName, Constants.FORMS.HPS_ANNUAL_CENSUS);

                if (form != null) {
                    JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                    JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                    updateValues(jsonArray, observations);

                    //Checking if the form has multiple steps and prefiling them if they exist
                    if (form.getInt("count") > 1) {
                        for (int i = 2; i <= form.getInt("count"); i++) {
                            JSONArray stepFields = form.getJSONObject("step" + i).getJSONArray(JsonFormUtils.FIELDS);
                            updateValues(stepFields, observations);
                        }
                    }
                }

                form.put(VISIT_ID, deletedVisitId);

                ((Activity) context).startActivityForResult(getStartEditFormIntent(form, context.getString(title_resource), context), JsonFormUtils.REQUEST_CODE_GET_JSON);
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
