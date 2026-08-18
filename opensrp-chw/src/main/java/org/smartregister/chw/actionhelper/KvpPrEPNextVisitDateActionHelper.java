package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpPrEPNextVisitDateActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {
    private String jsonPayload;
    private String nextVisitDate;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context,
                                 Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            nextVisitDate = CoreJsonFormUtils.getValue(new JSONObject(jsonPayload), "next_visit_date");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public BaseKvpVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String jsonPayload) {
        return jsonPayload;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseKvpVisitAction.Status evaluateStatusOnPayload() {
        return StringUtils.isBlank(nextVisitDate)
                ? BaseKvpVisitAction.Status.PENDING
                : BaseKvpVisitAction.Status.COMPLETED;
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction kvpVisitAction) {
        // No additional action state is required.
    }
}
