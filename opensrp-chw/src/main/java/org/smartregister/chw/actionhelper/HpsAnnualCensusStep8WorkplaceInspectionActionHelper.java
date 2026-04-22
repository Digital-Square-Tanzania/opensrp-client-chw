package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep8WorkplaceInspectionActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

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
            String agriculture = CoreJsonFormUtils.getValue(json, "number_of_inspected_agriculture_areas");
            String industries = CoreJsonFormUtils.getValue(json, "number_of_inspected_industries_areas");
            String offices = CoreJsonFormUtils.getValue(json, "number_of_inspected_offices_areas");
            StringBuilder sb = new StringBuilder();
            if (agriculture != null && !agriculture.trim().isEmpty()) sb.append("Agriculture ").append(agriculture.trim());
            if (industries != null && !industries.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Industries ").append(industries.trim());
            }
            if (offices != null && !offices.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Offices ").append(offices.trim());
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
                    "number_of_other_areas_inspected_with_risk_indicators"
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

