package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class BreastFeedingActionHelper implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

    private String preg_woman_other_children;
    private String preg_woman_breastfeed;


    @Override
    public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {

    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String s) {

        try {
            JSONObject jsonObject = new JSONObject(s);
            preg_woman_other_children = JsonFormUtils.getValue(jsonObject, "preg_woman_other_children");
            preg_woman_breastfeed = JsonFormUtils.getValue(jsonObject, "preg_woman_breastfeed");
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
        return null;
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(preg_woman_other_children) || StringUtils.isBlank(preg_woman_breastfeed)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        } else if (preg_woman_breastfeed.contains("chk_no")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {

    }
}
