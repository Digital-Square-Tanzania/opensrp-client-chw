package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class CommunityHealthWorkerObservationsAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
    private String value = "";
    private String anyone_presence = "";
    private Context context;

    @Override
    public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            value = JsonFormUtils.getCheckBoxValue(jsonObject, "anyone_else_present_during_visit");
            anyone_presence = JsonFormUtils.getValue(jsonObject, "anyone_else_present_during_visit");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
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
        if (!value.isEmpty()){
            return MessageFormat.format(context.getString(R.string.community_health_worker_observations_evaluate_sub_title), value);
        } else{
            return value;
        }
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (!anyone_presence.isEmpty()){
            if (anyone_presence.contains("anyone_present_yes")){
                return BaseAncHomeVisitAction.Status.COMPLETED;
            } else{
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            }
        }
        else
            return BaseAncHomeVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
        Timber.v("onPayloadReceived");
    }
}
