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

public class HpsAnnualCensusStep2NutritionSourcesActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    private final String householdMax;
    private String jsonPayload;
    private String submittedPayload;

    public HpsAnnualCensusStep2NutritionSourcesActionHelper(String householdMax) {
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
            String veg = CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_vegetable");
            String fruit = CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_fruit_trees");
            String animal = CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_domestic_animal");
            veg = veg == null ? "" : veg.trim();
            fruit = fruit == null ? "" : fruit.trim();
            animal = animal == null ? "" : animal.trim();
            if (veg.isEmpty() && fruit.isEmpty() && animal.isEmpty()) return null;
            return String.format("Veg:%s  Fruit:%s  Animal:%s", veg, fruit, animal);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public BaseHpsVisitAction.Status evaluateStatusOnPayload() {
        try {
            if (submittedPayload == null) return BaseHpsVisitAction.Status.PENDING;
            JSONObject json = new JSONObject(submittedPayload);
            String veg = CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_vegetable");
            String fruit = CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_fruit_trees");
            String animal = CoreJsonFormUtils.getValue(json, "number_of_house_hold_with_basic_nutrition_source_domestic_animal");
            boolean any = (veg != null && !veg.trim().isEmpty()) || (fruit != null && !fruit.trim().isEmpty()) || (animal != null && !animal.trim().isEmpty());
            return any ? BaseHpsVisitAction.Status.COMPLETED : BaseHpsVisitAction.Status.PENDING;
        } catch (Exception e) {
            return BaseHpsVisitAction.Status.PENDING;
        }
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }
}
