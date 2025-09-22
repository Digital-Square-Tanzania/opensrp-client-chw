package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;

import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep9FoodBeverageInspectionActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    private String jsonPayload;
    private String submittedPayload;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, java.util.List<VisitDetail>> details) {
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
            String grains = CoreJsonFormUtils.getValue(json, "number_of_inspected_grains");
            String meat = CoreJsonFormUtils.getValue(json, "number_of_inspected_meat");
            String fish = CoreJsonFormUtils.getValue(json, "number_of_inspected_fishing");
            StringBuilder sb = new StringBuilder();
            if (grains != null && !grains.trim().isEmpty()) sb.append("Grains ").append(grains.trim()).append("kg");
            if (meat != null && !meat.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Meat ").append(meat.trim()).append("kg");
            }
            if (fish != null && !fish.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Fish ").append(fish.trim()).append("kg");
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
                    "number_of_inspected_grains",
                    "number_of_inspected_legumes",
                    "number_of_inspected_meat",
                    "number_of_inspected_fishing",
                    "number_of_inspected_alcoholic_beverages",
                    "number_of_inspected_non_alcoholic_beverages",
                    "number_of_grains_discarded",
                    "number_of_legumes_discarded",
                    "number_of_meat_discarded",
                    "number_of_fish_discarded",
                    "number_of_alcoholic_beverages_discarded",
                    "number_of_non_alcoholic_beverages_discarded"
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

