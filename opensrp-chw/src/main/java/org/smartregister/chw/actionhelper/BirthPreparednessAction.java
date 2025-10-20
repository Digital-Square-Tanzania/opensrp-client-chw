package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class BirthPreparednessAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

    private Context context;
    private String location_nearest_health_facility = "";
    private String savings_preparedness = "";
    private String birth_companion_preparedness = "";
    private String family_member_individual_stay_home_preparedness = "";
    private String transportation_preparedness = "";

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try{
            JSONObject jsonObject = new JSONObject(jsonPayload);
            location_nearest_health_facility = JsonFormUtils.getValue(jsonObject, "location_nearest_health_facility");
            savings_preparedness = JsonFormUtils.getValue(jsonObject, "savings_preparedness");
            birth_companion_preparedness = JsonFormUtils.getValue(jsonObject, "birth_companion_preparedness");
            family_member_individual_stay_home_preparedness = JsonFormUtils.getValue(jsonObject, "family_member_individual_stay_home_preparedness");
            transportation_preparedness = JsonFormUtils.getValue(jsonObject, "transportation_preparedness");
        }catch (Exception e){
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
    public String postProcess(String jsonPayload) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return MessageFormat.format(
                context.getString(R.string.birth_preparedness_summary),
                location_nearest_health_facility,
                savings_preparedness,
                birth_companion_preparedness,
                family_member_individual_stay_home_preparedness,
                transportation_preparedness
        );
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (location_nearest_health_facility.equalsIgnoreCase("yes") &&
                savings_preparedness.equalsIgnoreCase("yes") &&
                birth_companion_preparedness.equalsIgnoreCase("yes") &&
                family_member_individual_stay_home_preparedness.equalsIgnoreCase("yes") &&
                transportation_preparedness.equalsIgnoreCase("yes")){
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
        else if (location_nearest_health_facility.equalsIgnoreCase("yes") ||
                savings_preparedness.equalsIgnoreCase("yes") ||
                birth_companion_preparedness.equalsIgnoreCase("yes") ||
                family_member_individual_stay_home_preparedness.equalsIgnoreCase("yes") ||
                transportation_preparedness.equalsIgnoreCase("yes")){
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
        else
            return BaseAncHomeVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction ancHomeVisitAction) {

    }
}

