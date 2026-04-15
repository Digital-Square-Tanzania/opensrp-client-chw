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
 public class CounsellingStatusAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
    private Context context;
    private String counselling_given;

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
            counselling_given = JsonFormUtils.getValue(jsonObject, "counselling_given").toLowerCase();
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
        String subTitle = (!counselling_given.contains("chk_none") ? context.getString(R.string.done).toLowerCase() : context.getString(R.string.not_done).toLowerCase());
        return MessageFormat.format("{0} {1}", context.getString(R.string.counselling), subTitle);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(counselling_given)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        } else if (counselling_given.contains("chk_none")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }

        return BaseAncHomeVisitAction.Status.COMPLETED;
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
        Timber.v("onPayloadReceived");
    }
}
