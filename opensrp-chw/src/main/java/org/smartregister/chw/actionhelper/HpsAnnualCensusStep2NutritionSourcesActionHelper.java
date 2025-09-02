package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.chw.kvp.domain.VisitDetail;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep2NutritionSourcesActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    private String jsonPayload;
    private final String householdMax;

    public HpsAnnualCensusStep2NutritionSourcesActionHelper(String householdMax) {
        this.householdMax = householdMax;
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<org.smartregister.chw.hps.domain.VisitDetail>> details) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            if (jsonPayload == null) return null;
            JSONObject json = new JSONObject(jsonPayload);
            json.getJSONObject(org.smartregister.client.utils.constants.JsonFormConstants.JSON_FORM_KEY.GLOBAL)
                    .put("household_max", householdMax == null ? "" : householdMax);
            return json.toString();
        } catch (Exception e) {
            Timber.e(e);
            return jsonPayload;
        }
    }

    @Override
    public void onPayloadReceived(String jsonPayload) { /* no-op */ }

    @Override
    public BaseHpsVisitAction.ScheduleStatus getPreProcessedStatus() { return null; }

    @Override
    public String getPreProcessedSubTitle() { return null; }

    @Override
    public String postProcess(String s) { return null; }

    @Override
    public String evaluateSubTitle() { return null; }

    @Override
    public BaseHpsVisitAction.Status evaluateStatusOnPayload() {
        try {
            JSONObject json = new JSONObject(jsonPayload);
            String veg = org.smartregister.chw.core.utils.CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_vegetable");
            String fruit = org.smartregister.chw.core.utils.CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_fruit_trees");
            String animal = org.smartregister.chw.core.utils.CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_domestic_animal");
            boolean any = (veg != null && !veg.trim().isEmpty()) || (fruit != null && !fruit.trim().isEmpty()) || (animal != null && !animal.trim().isEmpty());
            return any ? BaseHpsVisitAction.Status.COMPLETED : BaseHpsVisitAction.Status.PENDING;
        } catch (Exception e) {
            return BaseHpsVisitAction.Status.PENDING;
        }
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }
}

