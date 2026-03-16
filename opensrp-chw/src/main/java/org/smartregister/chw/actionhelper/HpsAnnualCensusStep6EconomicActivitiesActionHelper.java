package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep6EconomicActivitiesActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

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
            String menWithCapacity = CoreJsonFormUtils.getValue(json, "no_of_men_with_capacity_to_engage_in_economic_activities");
            String womenWithCapacity = CoreJsonFormUtils.getValue(json, "no_of_women_with_capacity_to_engage_in_economic_activities");
            String menEngaged = CoreJsonFormUtils.getValue(json, "no_of_men_engaged_in_economic_activities");
            StringBuilder sb = new StringBuilder();
            if (menWithCapacity != null && !menWithCapacity.trim().isEmpty()) sb.append("Capacity Men ").append(menWithCapacity.trim());
            if (womenWithCapacity != null && !womenWithCapacity.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Capacity Women ").append(womenWithCapacity.trim());
            }
            if (menEngaged != null && !menEngaged.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Men Engaged ").append(menEngaged.trim());
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
                    "no_of_men_with_capacity_to_engage_in_economic_activities",
                    "no_of_women_with_capacity_to_engage_in_economic_activities",
                    "no_of_men_engaged_in_economic_activities",
                    "no_of_women_engaged_in_economic_activities"
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
