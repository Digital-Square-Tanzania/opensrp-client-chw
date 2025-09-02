package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep4SocialEconomicActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    private String jsonPayload;
    private String submittedPayload;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        this.submittedPayload = jsonPayload;
    }

    @Override
    public BaseHpsVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String s) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        try {
            if (submittedPayload == null) return null;
            JSONObject json = new JSONObject(submittedPayload);
            String tap = CoreJsonFormUtils.getValue(json, "number_of_households_most_commonly_use_tap_as_sources_of_water");
            String elecLight = CoreJsonFormUtils.getValue(json, "number_of_households_using_electricity_as_source_of_energy_for_lighting");
            String gasCook = CoreJsonFormUtils.getValue(json, "number_of_households_using_gas_as_source_of_cooking_energy");
            String mEngaged = CoreJsonFormUtils.getValue(json, "number_of_male_engaged_in_economic_activities");
            String fEngaged = CoreJsonFormUtils.getValue(json, "number_of_female_engaged_in_economic_activities");
            StringBuilder sb = new StringBuilder();
            if (tap != null && !tap.trim().isEmpty()) sb.append("Tap water ").append(tap.trim());
            if (elecLight != null && !elecLight.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Lighting-elec ").append(elecLight.trim());
            }
            if (gasCook != null && !gasCook.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Cooking-gas ").append(gasCook.trim());
            }
            if ((mEngaged != null && !mEngaged.trim().isEmpty()) || (fEngaged != null && !fEngaged.trim().isEmpty())) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Engaged M").append(mEngaged == null ? "" : mEngaged.trim())
                  .append(" F").append(fEngaged == null ? "" : fEngaged.trim());
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @Override
    public BaseHpsVisitAction.Status evaluateStatusOnPayload() {
        try {
            if (submittedPayload == null) return BaseHpsVisitAction.Status.PENDING;
            JSONObject json = new JSONObject(submittedPayload);
            String[] keys = new String[]{
                    // Water sources
                    "number_of_households_most_commonly_use_tap_as_sources_of_water",
                    "number_of_households_most_commonly_use_river_as_sources_of_water",
                    "number_of_households_most_commonly_use_shallow_well_as_sources_of_water",
                    "number_of_households_most_commonly_use_water_pond_as_sources_of_water",
                    "number_of_households_most_commonly_use_small_dam_as_sources_of_water",
                    "number_of_households_most_commonly_use_lake_as_sources_of_water",
                    "number_of_households_most_commonly_use_spring_as_sources_of_water",
                    // Lighting
                    "number_of_households_using_electricity_as_source_of_energy_for_lighting",
                    "number_of_households_using_solar_as_source_of_energy_for_lighting",
                    "number_of_households_using_kerosine_as_source_of_energy_for_lighting",
                    "number_of_households_using_koroboi_as_source_of_energy_for_lighting",
                    "number_of_households_using_other_source_of_energy_for_lighting",
                    // Cooking
                    "number_of_households_using_electricity_as_source_of_cooking_energy",
                    "number_of_households_using_solar_as_source_of_cooking_energy",
                    "number_of_households_using_kerosine_as_source_of_cooking_energy",
                    "number_of_households_using_gas_as_source_of_cooking_energy",
                    "number_of_households_using_charcoal_as_source_of_cooking_energy",
                    "number_of_households_using_firewood_as_source_of_cooking_energy",
                    // Economic activity
                    "number_of_male_capable_of_engaging_in_economic_activities",
                    "number_of_female_capable_of_engaging_in_economic_activities",
                    "number_of_male_engaged_in_economic_activities",
                    "number_of_female_engaged_in_economic_activities"
            };
            for (String k : keys) {
                String v = CoreJsonFormUtils.getValue(json, k);
                if (v != null && !v.trim().isEmpty()) return BaseHpsVisitAction.Status.COMPLETED;
            }
            return BaseHpsVisitAction.Status.PENDING;
        } catch (Exception e) {
            Timber.e(e);
            return BaseHpsVisitAction.Status.PENDING;
        }
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }
}

