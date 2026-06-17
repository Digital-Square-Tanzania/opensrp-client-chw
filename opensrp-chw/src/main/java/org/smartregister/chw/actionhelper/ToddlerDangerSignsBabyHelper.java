package org.smartregister.chw.actionhelper;

import static org.smartregister.chw.util.JsonFormUtils.getCheckBoxValue;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;

import timber.log.Timber;

public class ToddlerDangerSignsBabyHelper extends HomeVisitActionHelper {
    private static final String NONE = "(?i)hakuna|none";
    private static final String YES_OR_EMPTY = "(?i)yes|ndio|ndiyo|";
    private final Context context;
    private String danger_signs_present_child;
    private ToddlerDangerSignsConsumer dangerSignConsumer;

    public ToddlerDangerSignsBabyHelper(Context context) {
        this.context = context;
    }

    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
        return BaseAncHomeVisitAction.ScheduleStatus.DUE;
    }

    @Override
    public void onPayloadReceived(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);
            danger_signs_present_child = getCheckBoxValue(jsonObject, "toddler_danger_signs_present");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        return MessageFormat.format("{0}: {1}", context.getString(R.string.child_danger_signs_baby_task), danger_signs_present_child);
    }


    @Override
    public String postProcess(String jsonPayload) {
        try {
            if (dangerSignConsumer == null) {
                return super.postProcess(jsonPayload);
            }
            JSONObject form = new JSONObject(jsonPayload);
            boolean noDangerSigns = danger_signs_present_child.matches(NONE);
            boolean goFacility = !noDangerSigns && JsonFormUtils.getValue(form, "toddler_referral_health_facility").matches(YES_OR_EMPTY);
            dangerSignConsumer.take(form, danger_signs_present_child, goFacility);
        } catch (Exception e) {
            Timber.e(e);
        }
        return super.postProcess(jsonPayload);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(danger_signs_present_child)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }

        if (StringUtils.isNotBlank(danger_signs_present_child)) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }

    public void setDangerSignsResultsListener(ToddlerDangerSignsConsumer c) {
        dangerSignConsumer = c;
    }

    public interface ToddlerDangerSignsConsumer {
        void take(JSONObject dangerSignPayload, String dangerSigns, boolean goFacility);
    }
}
