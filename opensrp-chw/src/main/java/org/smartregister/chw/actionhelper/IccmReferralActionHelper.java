package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;

import java.util.List;
import java.util.Map;

public class IccmReferralActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper{
    private String jsonPayload;

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonString;
    }

    @Override
    public String getPreProcessed() {
        return "";
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {

    }

    @Override
    public BaseIccmVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return "";
    }

    @Override
    public String postProcess(String jsonPayload) {
        return "";
    }

    @Override
    public String evaluateSubTitle() {
        return "";
    }

    @Override
    public BaseIccmVisitAction.Status evaluateStatusOnPayload() {
        return BaseIccmVisitAction.Status.COMPLETED;
    }

    @Override
    public void onPayloadReceived(BaseIccmVisitAction ldVisitAction) {

    }
}
