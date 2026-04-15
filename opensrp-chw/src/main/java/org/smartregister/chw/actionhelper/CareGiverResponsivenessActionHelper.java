package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.domain.Alert;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class CareGiverResponsivenessActionHelper extends HomeVisitActionHelper {
    private String caregiver_interacts_with_child;
    private String caregiver_comfort_child;
    private String caregiver_response_cue;
    private Alert alert;

    public CareGiverResponsivenessActionHelper(Alert alert) {
        this.alert = alert;
    }

    public CareGiverResponsivenessActionHelper() {}

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        super.onJsonFormLoaded(jsonString, context, details);
    }

    @Override
    public void onPayloadReceived(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray fields = JsonFormUtils.fields(jsonObject);

            caregiver_interacts_with_child = JsonFormUtils.getValue(jsonObject, "caregiver_interacts_with_child");
            caregiver_comfort_child = JsonFormUtils.getValue(jsonObject, "caregiver_comfort_child");
            caregiver_response_cue = JsonFormUtils.getValue(jsonObject, "caregiver_response_cue");

        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(caregiver_comfort_child) || StringUtils.isBlank(caregiver_interacts_with_child) || StringUtils.isBlank(caregiver_response_cue)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        } else if (caregiver_interacts_with_child.equalsIgnoreCase("responds_to_child") && "Yes".equalsIgnoreCase(caregiver_comfort_child) && (caregiver_response_cue.equalsIgnoreCase("respond_to_child_cues_by_looking_into_child_eyes") || caregiver_response_cue.equalsIgnoreCase("respond_to_child_cues_by_responding_child_action"))) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }

    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
        if (alert != null) {
            return isOverDue() ? BaseAncHomeVisitAction.ScheduleStatus.OVERDUE : BaseAncHomeVisitAction.ScheduleStatus.DUE;
        } else {
            return null;
        }
    }

    private boolean isOverDue() {
        return new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
    }
}