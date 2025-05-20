package org.smartregister.chw.actionhelper;

import static org.smartregister.chw.anc.model.BaseAncHomeVisitAction.Status.COMPLETED;
import static org.smartregister.chw.anc.model.BaseAncHomeVisitAction.Status.PENDING;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ChildMinorAilmentsActionHelper implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

    private String minor_ailments;
    private MemberObject memberObject;
    private Context context;

    public ChildMinorAilmentsActionHelper(Context context, MemberObject memberObject){
        this.context = context;
        this.memberObject = memberObject;
    }

    @Override
    public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {

    }

    @Override
    public String getPreProcessed() {
        return "";
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            minor_ailments = JsonFormUtils.getCheckBoxValue(jsonObject, "child_minor_ailment").toLowerCase();
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
        return "";
    }

    @Override
    public String postProcess(String s) {
        return s;
    }

    @Override
    public String evaluateSubTitle() {
        return MessageFormat.format("{0}: {1}", (MessageFormat.format(context.getString(R.string.child_minor_illness), memberObject.getFullName())), minor_ailments);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(minor_ailments))
            return PENDING;
        else
            return COMPLETED;
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {

    }


}
