package org.smartregister.chw.domain.hps_reports;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class HpsAnnualReportObject extends ReportObject {
    public static String[] selectorsForSimpleKeys = {
            "number_of_male_by_age_group_under1",
            "number_of_female_by_age_group_under1",
            "number_of_male_by_age_group_1_4",
            "number_of_female_by_age_group_1_4",
            "number_of_male_by_age_group_5_14",
            "number_of_female_by_age_group_5_14",
            "number_of_male_by_age_group_15_49",
            "number_of_female_by_age_group_15_49",
            "number_of_male_by_age_group_50_59",
            "number_of_female_by_age_group_50_59",
            "number_of_male_by_age_group_60_plus",
            "number_of_female_by_age_group_60_plus",
            "number_of_house_hold",
            "number_of_house_hold_with_road_access",
            "number_of_house_holds_with_at_least_one_landline_or_mobile_phone",
            "number_of_house_hold_with_basic_nutrition_source_vegetable",
            "number_of_house_hold_with_basic_nutrition_source_fruit_trees",
            "number_of_house_hold_with_basic_nutrition_source_domestic_animal",

//          ******************************** BOX HERE (Down) ****************************     //

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
            "number_of_male_engaged_in_economic_activities",
            "number_of_female_capable_of_engaging_in_economic_activities",
            "number_of_female_engaged_in_economic_activities",
            "number_of_health_committee_members_for_effective_committee_meetings",
            "number_of_committee_members_attended_fisrt_quarter",
            "number_of_registered_alternative_medicine_service_providers",
            "number_of_registered_traditional_medicine_service_providers",
            "number_of_unregistered_alternative_medicine_service_providers",
            "number_of_unregistered_traditional_medicine_service_providers"
    };
    public static String[] selectorsForComplexKeys = {
            "number_of_pre_schools_government",
            "number_of_pre_schools_faith_based_organisation",
            "number_of_pre_schools_public",
            "number_of_pre_schools_private",
            "number_of_primary_schools_government",
            "number_of_primary_schools_faith_based_organisation",
            "number_of_primary_schools_public",
            "number_of_primary_schools_private",
            "number_of_secondary_schools_government",
            "number_of_secondary_schools_faith_based_organisation",
            "number_of_secondary_schools_public",
            "number_of_secondary_schools_private",
            "number_of_universities_government",
            "number_of_universities_faith_based_organisation",
            "number_of_universities_public",
            "number_of_universities_private",
            "number_of_special_needs_schools_government",
            "number_of_special_needs_schools_faith_based_organisation",
            "number_of_special_needs_schools_public",
            "number_of_special_needs_schools_private",
            "number_of_dispensary_government",
            "number_of_dispensary_faith_based_organisation",
            "number_of_dispensary_public",
            "number_of_dispensary_private",
            "number_of_health_centers_government",
            "number_of_health_centers_faith_based_organisation",
            "number_of_health_centers_public",
            "number_of_health_centers_private",
            "number_of_hospital_government",
            "number_of_hospital_faith_based_organisation",
            "number_of_hospital_public",
            "number_of_hospital_private",
            "number_of_special_clinics_government",
            "number_of_special_clinics_faith_based_organisation",
            "number_of_special_clinics_public",
            "number_of_special_clinics_private",
            "number_of_laboratory_government",
            "number_of_laboratory_faith_based_organisation",
            "number_of_laboratory_public",
            "number_of_laboratory_private",
            "number_of_pharmacy_government",
            "number_of_pharmacy_faith_based_organisation",
            "number_of_pharmacy_public",
            "number_of_pharmacy_private",
            "number_of_ADDO_government",
            "number_of_ADDO_faith_based_organisation",
            "number_of_ADDO_public",
            "number_of_ADDO_private",
            "number_of_maternity_home_government",
            "number_of_maternity_home_faith_based_organisation",
            "number_of_maternity_home_public",
            "number_of_maternity_home_private",
            "number_of_orphan_care_centers_government",
            "number_of_orphan_care_centers_faith_based_organisation",
            "number_of_orphan_care_centers_public",
            "number_of_orphan_care_centers_private",
            "number_of_centers_for_children_with_disabilities_government",
            "number_of_centers_for_children_with_disabilities_faith_based_organisation",
            "number_of_centers_for_children_with_disabilities_public",
            "number_of_centers_for_children_with_disabilities_private",
            "number_of_cbecdc_rehabilitation_centre_government",
            "number_of_cbecdc_rehabilitation_centre_faith_based_organisation",
            "number_of_cbecdc_rehabilitation_centre_public",
            "number_of_cbecdc_rehabilitation_centre_private",
            "number_of_day_care_centers_government",
            "number_of_day_care_centers_faith_based_organisation",
            "number_of_day_care_centers_public",
            "number_of_day_care_centers_private",
            "number_of_elderly_care_centers_government",
            "number_of_elderly_care_centers_faith_based_organisation",
            "number_of_elderly_care_centers_public",
            "number_of_elderly_care_centers_private",
    };
    private final String[] simpleKeys = new String[]{
            "hps-M-<1", "hps-F-<1", "hps-M-1-4", "hps-F-1-4", "hps-M-5-14", "hps-F-5-14", "hps-M-15-49", "hps-F-15-49", "hps-M-50-59", "hps-F-50-59", "hps-M-60-+", "hps-F-60-+", "hps-total-households",
            "hps-a-1", "hps-a-2", "hps-b-1", "hps-b-2", "hps-b-3", "hps-u-1", "hps-u-2", "hps-u-3", "hps-u-4", "hps-u-5", "hps-u-6", "hps-u-7", "hps-u-8", "hps-u-9", "hps-u-10", "hps-u-11",
            "hps-u-12", "hps-u-13", "hps-u-14", "hps-u-15", "hps-u-16", "hps-u-17", "hps-u-18", "hps-v-1", "hps-vv-1", "hps-v-2", "hps-vv-2", "hps-x-1", "hps-x-2", "hps-y-1", "hps-y-2", "hps-z-1", "hps-z-2",
    };
    private final String[] complexKeys = new String[]{
            "hps-c", "hps-d", "hps-e", "hps-f", "hps-g", "hps-h", "hps-i", "hps-j", "hps-k", "hps-l", "hps-m", "hps-n", "hps-o", "hps-p", "hps-q", "hps-r", "hps-s", "hps-t",
    };
    private final String[] hpsQuestionsGroups = new String[]{"1", "2", "3", "4"};
    private final String[] whereClauses = new String[]{"government", "faith_based_organisation", "public", "private"};

    private final Date reportDate;
    JSONObject jsonObject = new JSONObject();
    JSONArray dataArray = new JSONArray();
    private int totalOfMale = 0;
    private int totalOfFemale = 0;
    private int totalOfBothMaleAndFemale = 0;

    private int totalOfhpsc = 0;
    private int totalOfhpsd = 0;
    private int totalOfhpse = 0;
    private int totalOfhpsf = 0;
    private int totalOfhpsg = 0;
    private int totalOfhpsh = 0;
    private int totalOfhpsi = 0;
    private int totalOfhpsj = 0;
    private int totalOfhpsk = 0;
    private int totalOfhpsl = 0;
    private int totalOfhpsm = 0;
    private int totalOfhpsn = 0;
    private int totalOfhpso = 0;
    private int totalOfhpsp = 0;
    private int totalOfhpsq = 0;
    private int totalOfhpsr = 0;
    private int totalOfhpss = 0;
    private int totalOfhpst = 0;
    private int totalOfhpsv = 0;
    private int totalOfhpsvv = 0;



    public HpsAnnualReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;

    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        // Request values from the Dyanamic Tables
        List<Map<String, String>> getHpsDynamicTablesList = ReportDao.getHpsAnnualDynamicTablesreports(reportDate, simpleKeys, selectorsForSimpleKeys);
        List<Map<String, String>> getHpsDynamicTablesListWithClause = ReportDao.getHpsAnnualDynamicTablesreports(reportDate, generateCombinedArrayKeys(), selectorsForComplexKeys);  // ****** BOX kuu ******
        // get values from the Dyanamic Tables
        excuteDynamicTables(getHpsDynamicTablesList);
        excuteDynamicTables(getHpsDynamicTablesListWithClause);
        return jsonObject;
    }

    private void excuteDynamicTables(List<Map<String, String>> getHpsDynamicTablesList) {
        // To keep track of already printed tables
        Set<Map<String, String>> printedTables = new HashSet<>();
        for (Map<String, String> getHpsDynamicTables : getHpsDynamicTablesList) {
            // Skip empty maps
            if (getHpsDynamicTables.isEmpty()) {
                continue;
            }
            // Print and process only unprinted tables
            if (!printedTables.contains(getHpsDynamicTables)) {
                for (String tableKey : getHpsDynamicTables.keySet()) {
                    try {
                        jsonObject.put(tableKey, getHpsDynamicTable(getHpsDynamicTables, tableKey));
                        getTotalForAgeAndOtherBandsQuestions(tableKey, getHpsDynamicTables);
                    } catch (JSONException e) {
                        Timber.tag("Hps Annual report error").d(e);
                    }
                }
                // Mark this table as printed
                printedTables.add(getHpsDynamicTables);
            }
        }
    }

    private void getTotalForAgeAndOtherBandsQuestions(String tableKey, Map<String, String> getHpsDynamicTables) throws JSONException {
        if (tableKey.contains("hps-M")) {
            totalOfMale += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            totalOfBothMaleAndFemale += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-M-total", totalOfMale);
        }
        if (tableKey.contains("hps-F")) {
            totalOfFemale += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            totalOfBothMaleAndFemale += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-F-total", totalOfFemale);
        }
        jsonObject.put("hps-MF-grand-total", totalOfBothMaleAndFemale);

        if (tableKey.contains("hps-c")) {
            totalOfhpsc += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-c-total", totalOfhpsc);
        }
        if (tableKey.contains("hps-d")) {
            totalOfhpsd += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-d-total", totalOfhpsd);
        }
        if (tableKey.contains("hps-e")) {
            totalOfhpse += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-e-total", totalOfhpse);
        }
        if (tableKey.contains("hps-f")) {
            totalOfhpsf += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-f-total", totalOfhpsf);
        }
        if (tableKey.contains("hps-g")) {
            totalOfhpsg += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-g-total", totalOfhpsg);
        }
        if (tableKey.contains("hps-h")) {
            totalOfhpsh += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-h-total", totalOfhpsh);
        }
        if (tableKey.contains("hps-i")) {
            totalOfhpsi += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-i-total", totalOfhpsi);
        }
        if (tableKey.contains("hps-j")) {
            totalOfhpsj += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-j-total", totalOfhpsj);
        }
        if (tableKey.contains("hps-k")) {
            totalOfhpsk += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-k-total", totalOfhpsk);
        }
        if (tableKey.contains("hps-l")) {
            totalOfhpsl += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-l-total", totalOfhpsl);
        }
        if (tableKey.contains("hps-m")) {
            totalOfhpsm += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-m-total", totalOfhpsm);
        }
        if (tableKey.contains("hps-n")) {
            totalOfhpsn += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-n-total", totalOfhpsn);
        }
        if (tableKey.contains("hps-o")) {
            totalOfhpso += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-o-total", totalOfhpso);
        }
        if (tableKey.contains("hps-p")) {
            totalOfhpsp += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-p-total", totalOfhpsp);
        }
        if (tableKey.contains("hps-q")) {
            totalOfhpsq += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-q-total", totalOfhpsq);
        }
        if (tableKey.contains("hps-r")) {
            totalOfhpsr += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-r-total", totalOfhpsr);
        }
        if (tableKey.contains("hps-s")) {
            totalOfhpss += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-s-total", totalOfhpss);
        }
        if (tableKey.contains("hps-t")) {
            totalOfhpst += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-t-total", totalOfhpst);
        }
        if (tableKey.contains("hps-v")) {
            totalOfhpsv += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-v-total", totalOfhpsv);
        }
        if (tableKey.contains("hps-vv")) {
            totalOfhpsvv += Integer.parseInt(getHpsDynamicTable(getHpsDynamicTables, tableKey));
            jsonObject.put("hps-vv-total", totalOfhpsvv);
        }

    }

    private String getHpsDynamicTable(Map<String, String> chwRegistrationFollowupClient, String key) {
        String details = chwRegistrationFollowupClient.get(key);
        if (details.equals("null")) {
            return "0";
        }
        if (StringUtils.isNotBlank(details)) {
            return details;
        }
        return "0";
    }

    public String[] generateCombinedArrayKeys() {
        List<String> combinedList = new ArrayList<>();
        for (String indicatorTableCode : complexKeys) {
            for (String questionGroup : hpsQuestionsGroups) {
                combinedList.add(indicatorTableCode + "-" + questionGroup);
            }
        }
        return combinedList.toArray(new String[0]); // Convert to array
    }
}
