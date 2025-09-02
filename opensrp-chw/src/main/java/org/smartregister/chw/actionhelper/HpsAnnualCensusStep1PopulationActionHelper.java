package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.chw.hps.domain.VisitDetail;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep1PopulationActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    public interface Callback {
        void onHouseholdCountCaptured(String households);
    }

    private final Callback callback;
    private String jsonPayload;
    private String householdCountValue;

    public HpsAnnualCensusStep1PopulationActionHelper(Callback callback) {
        this.callback = callback;
    }

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
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            householdCountValue = org.smartregister.chw.core.utils.CoreJsonFormUtils.getValue(jsonObject, "number_of_house_hold");
            if (callback != null) callback.onHouseholdCountCaptured(householdCountValue);
        } catch (Exception e) {
            Timber.e(e);
        }
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
        if (householdCountValue == null || householdCountValue.trim().isEmpty())
            return BaseHpsVisitAction.Status.PENDING;
        return BaseHpsVisitAction.Status.COMPLETED;
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }

}
