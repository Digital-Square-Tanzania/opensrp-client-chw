package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.client.utils.constants.JsonFormConstants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep6EnvironmentSanitationActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {
    private final String householdMax;
    private String jsonPayload;
    private String submittedPayload;

    public HpsAnnualCensusStep6EnvironmentSanitationActionHelper(String householdMax) {
        this.householdMax = householdMax;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            if (jsonPayload == null) return null;
            JSONObject json = new JSONObject(jsonPayload);
            if (householdMax != null && !householdMax.trim().isEmpty()) {
                json.getJSONObject(JsonFormConstants.JSON_FORM_KEY.GLOBAL)
                        .put("household_max", householdMax);
            }
            return json.toString();
        } catch (Exception e) {
            Timber.e(e);
            return jsonPayload;
        }
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
            String inspected = CoreJsonFormUtils.getValue(json, "number_of_households_inspected");
            String goodLatrine = CoreJsonFormUtils.getValue(json, "number_of_households_with_good_latrine");
            String noLatrine = CoreJsonFormUtils.getValue(json, "number_of_households_with_no_latrine");
            StringBuilder sb = new StringBuilder();
            if (inspected != null && !inspected.trim().isEmpty()) sb.append("Inspected ").append(inspected.trim());
            if (goodLatrine != null && !goodLatrine.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Good latrine ").append(goodLatrine.trim());
            }
            if (noLatrine != null && !noLatrine.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("No latrine ").append(noLatrine.trim());
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
                    "number_of_households_that_have_been_sprayed_with_insecticide"
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
