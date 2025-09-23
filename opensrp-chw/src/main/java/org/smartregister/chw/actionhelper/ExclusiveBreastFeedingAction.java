package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.domain.Alert;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ExclusiveBreastFeedingAction extends HomeVisitActionHelper {
    private Context context;
    private String exclusive_breast_feeding;
    private Alert alert;

    private String visitID = "";

    private String jsonString;
    public ExclusiveBreastFeedingAction(Context context, Alert alert) {
        this.context = context;
        this.alert = alert;
    }

    public ExclusiveBreastFeedingAction(Context context, Alert alert, String serviceIteration) {
        this.context = context;
        this.alert = alert;
        this.visitID = serviceIteration;
    }

    public  ExclusiveBreastFeedingAction(Context context, String visitId){
        this.context = context;
        this.visitID = visitId;
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonString = jsonString;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray fields = JsonFormUtils.fields(jsonObject);
            JSONObject visit_1_visit_8 = JsonFormUtils.getFieldJSONObject(fields, "visit_1_visit_8");
            if ((visit_1_visit_8 != null) && (this.visitID.equalsIgnoreCase("1") || this.visitID.equalsIgnoreCase("3") || this.visitID.equalsIgnoreCase("5") || this.visitID.equalsIgnoreCase("7") || this.visitID.equalsIgnoreCase("8") || this.visitID.equalsIgnoreCase("9"))) {
                    visit_1_visit_8.put("value", "true");
            }
            return jsonObject.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }
        return super.getPreProcessed();
    }

    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
        return isOverDue() ? BaseAncHomeVisitAction.ScheduleStatus.OVERDUE : BaseAncHomeVisitAction.ScheduleStatus.DUE;
    }

    private boolean isOverDue() {
        return new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            exclusive_breast_feeding = JsonFormUtils.getValue(jsonObject, "exclusive_breast_feeding");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        if (StringUtils.isBlank(exclusive_breast_feeding))
            return "";

        return "No".equalsIgnoreCase(exclusive_breast_feeding) ? context.getString(R.string.yes) : context.getString(R.string.no);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(exclusive_breast_feeding))
            return BaseAncHomeVisitAction.Status.PENDING;

        if (exclusive_breast_feeding.equalsIgnoreCase("Yes")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else if (exclusive_breast_feeding.equalsIgnoreCase("No")) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PENDING;
        }
    }
}