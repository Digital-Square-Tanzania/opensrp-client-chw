package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.chw.hps.domain.VisitDetail;

import java.util.List;
import java.util.Map;

/**
 * Generic helper that treats any submitted payload as COMPLETED.
 */
public class HpsSimpleFormActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    private boolean submitted;
    private String jsonPayload;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        this.submitted = true;
    }

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
        return submitted ? BaseHpsVisitAction.Status.COMPLETED : BaseHpsVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }
}

