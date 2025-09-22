package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep12InsectBreedingControlActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

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
            String ponds = CoreJsonFormUtils.getValue(json, "number_of_areas_sprayed_with_pesticides_ponds");
            String cans = CoreJsonFormUtils.getValue(json, "number_of_areas_sprayed_with_pesticides_cans");
            StringBuilder sb = new StringBuilder();
            if (ponds != null && !ponds.trim().isEmpty()) sb.append("Ponds ").append(ponds.trim());
            if (cans != null && !cans.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Cans ").append(cans.trim());
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
                    "number_of_areas_sprayed_with_pesticides_ponds",
                    "number_of_areas_sprayed_with_pesticides_cans",
                    "number_of_areas_sprayed_with_pesticides_drums",
                    "number_of_areas_sprayed_with_pesticides_barrels",
                    "number_of_areas_sprayed_with_pesticides_coconut_shells",
                    "number_of_times_spraying_was_done_ponds",
                    "number_of_times_spraying_was_done_cans",
                    "number_of_times_spraying_was_done_drums",
                    "number_of_times_spraying_was_done_barrels",
                    "number_of_times_spraying_was_done_coconut_shells"
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

