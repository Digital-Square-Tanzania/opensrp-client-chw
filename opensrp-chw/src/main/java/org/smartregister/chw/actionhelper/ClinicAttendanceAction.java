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

public class ClinicAttendanceAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

    private Context context;
    private String clinic_attendance;

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
            clinic_attendance = JsonFormUtils.getValue(jsonObject, "attend_anc_clinic_visit");
        }catch (JSONException e){
            e.printStackTrace();
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
        if (clinic_attendance.equalsIgnoreCase("Yes")) {
            return MessageFormat.format("{0}: {1}", context.getString(
                    R.string.anc_hv_clinic_attendance_sub_title), context.getString(R.string.yes));
        } else {
            return MessageFormat.format("{0}: {1}", context.getString(
                    R.string.anc_hv_clinic_attendance_sub_title), context.getString(R.string.no));
        }
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (clinic_attendance.equalsIgnoreCase("No")){
            return BaseAncHomeVisitAction.Status.PENDING;
        }else{
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {

    }
}