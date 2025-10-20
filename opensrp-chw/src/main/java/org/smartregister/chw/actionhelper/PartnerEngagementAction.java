package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
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

public class PartnerEngagementAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
    private String partner_presence;
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
            partner_presence = JsonFormUtils.getCheckBoxValue(jsonObject, "partner_head_of_household");
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
        return MessageFormat.format(context.getString(R.string.partner_engagement_evaluate_sub_title), partner_presence);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (!StringUtils.isBlank(partner_presence))
            if(partner_presence.equalsIgnoreCase("yes") || partner_presence.equalsIgnoreCase("ndio")){
                return BaseAncHomeVisitAction.Status.COMPLETED;
            }else{
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            }
        else
            return BaseAncHomeVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
        Timber.v("onPayloadReceived");
    }
}
