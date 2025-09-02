package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep10WorkplaceHealthReportsActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

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
            String respiratory = CoreJsonFormUtils.getValue(json, "health_reports_affecting_people_in_workplaces_respiratory_diseases");
            String toxic = CoreJsonFormUtils.getValue(json, "health_reports_affecting_people_in_workplaces_toxic_chemicals");
            if ((respiratory == null || respiratory.trim().isEmpty()) && (toxic == null || toxic.trim().isEmpty())) return null;
            StringBuilder sb = new StringBuilder();
            if (respiratory != null && !respiratory.trim().isEmpty()) sb.append("Respiratory ").append(respiratory.trim());
            if (toxic != null && !toxic.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Toxic chemicals ").append(toxic.trim());
            }
            return sb.toString();
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
                    "health_reports_affecting_people_in_workplaces_respiratory_diseases",
                    "health_reports_affecting_people_in_workplaces_toxic_chemicals",
                    "health_reports_affecting_people_in_workplaces_burns",
                    "health_reports_affecting_people_in_workplaces_hearing_loss",
                    "health_reports_affecting_people_in_workplaces_eye_problems",
                    "health_reports_affecting_people_in_workplaces_other_effects"
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

