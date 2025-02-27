package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MalnutritionScreeningActionHelper extends HomeVisitActionHelper {
    String growthMonitoringSelectedKey = "";

    String growthMonitoringSelectedValue = "";

    String palmPallorValue = "";

    String palmPallorKey= "";

    String childGrowthMuacKey = "";

    String childGrowthMuacValue = "";

    String childGrowthBookletPresent = "";

    String jsonString = "";

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonString = jsonString;
        this.context = context;
    }

    @Override
    public void onPayloadReceived(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            growthMonitoringSelectedKey = JsonFormUtils.getValue(jsonObject, "child_growth_monitoring");
            growthMonitoringSelectedValue = JsonFormUtils.getCheckBoxValue(jsonObject, "child_growth_monitoring");
            palmPallorKey = JsonFormUtils.getValue(jsonObject, "palm_pallor");
            childGrowthMuacKey = JsonFormUtils.getValue(jsonObject, "child_growth_muac");
            childGrowthBookletPresent = JsonFormUtils.getValue(jsonObject, "child_growth_booklet_present");
        }catch (JSONException e){
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        if (growthMonitoringSelectedKey.isEmpty()) return "";

        if (palmPallorKey.equalsIgnoreCase("no") || palmPallorKey.equalsIgnoreCase("hapana")) {
            palmPallorValue = context.getString(R.string.no);
        }

        if (palmPallorKey.equalsIgnoreCase("yes") || palmPallorKey.equalsIgnoreCase("ndio")) {
            palmPallorValue = context.getString(R.string.yes);
        }

        if (childGrowthMuacKey.equalsIgnoreCase("Red")) {
            childGrowthMuacValue = context.getString(R.string.palm_pallor_red);
        }

        if (childGrowthMuacKey.equalsIgnoreCase("Green")) {
            childGrowthMuacValue = context.getString(R.string.palm_pallor_green);
        }

        if (childGrowthMuacKey.equalsIgnoreCase("Yellow")) {
            childGrowthMuacValue = context.getString(R.string.palm_pallor_yellow);
        }

        return getContext().getString(R.string.malnutrition_screening_subtitle, childGrowthMuacValue, palmPallorValue);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(childGrowthBookletPresent) || StringUtils.isBlank(palmPallorKey)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }

        if (childGrowthMuacKey.equalsIgnoreCase("Green") && palmPallorKey.equalsIgnoreCase("no")) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }
}