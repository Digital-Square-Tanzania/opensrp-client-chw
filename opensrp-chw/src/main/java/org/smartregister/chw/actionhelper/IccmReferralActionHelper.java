package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.chw.util.JsonFormUtilsFlv;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmReferralActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper{
    private String jsonPayload;
    private Map<String, List<VisitDetail>> details;

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonString;
        this.details = details;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonForm = new JSONObject(jsonPayload);
            Map<String, String> facilityOptions = LocationUtils.INSTANCE.getFacilitiesKeyAndName();
            JsonFormUtilsFlv.overwriteQuestionOptions("chw_referral_hf", facilityOptions, jsonForm);
            populateSelectedFacility(jsonForm);
            return jsonForm.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {

    }

    @Override
    public BaseIccmVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return "";
    }

    @Override
    public String postProcess(String jsonPayload) {
        return "";
    }

    @Override
    public String evaluateSubTitle() {
        return "";
    }

    @Override
    public BaseIccmVisitAction.Status evaluateStatusOnPayload() {
        return BaseIccmVisitAction.Status.COMPLETED;
    }

    @Override
    public void onPayloadReceived(BaseIccmVisitAction ldVisitAction) {

    }

    private void populateSelectedFacility(JSONObject jsonForm) throws JSONException {
        if (details == null || details.isEmpty()) {
            return;
        }

        List<VisitDetail> visitDetails = details.get("chw_referral_hf");
        if (visitDetails == null || visitDetails.isEmpty() || visitDetails.get(0) == null) {
            return;
        }

        String selectedFacility = visitDetails.get(0).getDetails();
        if (selectedFacility == null || selectedFacility.trim().isEmpty()) {
            selectedFacility = visitDetails.get(0).getHumanReadable();
        }

        if (selectedFacility == null || selectedFacility.trim().isEmpty()) {
            return;
        }

        JSONObject referralFacilityField = findFieldByKey(jsonForm, "chw_referral_hf");
        if (referralFacilityField != null) {
            referralFacilityField.put("value", selectedFacility);
        }
    }

    private JSONObject findFieldByKey(Object node, String fieldKey) {
        if (node instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) node;
            String key = jsonObject.optString("key", jsonObject.optString("name"));
            if (fieldKey.equals(key)) {
                return jsonObject;
            }

            JSONArray names = jsonObject.names();
            if (names != null) {
                for (int index = 0; index < names.length(); index++) {
                    String name = names.optString(index);
                    JSONObject match = findFieldByKey(jsonObject.opt(name), fieldKey);
                    if (match != null) {
                        return match;
                    }
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) node;
            for (int index = 0; index < jsonArray.length(); index++) {
                JSONObject match = findFieldByKey(jsonArray.opt(index), fieldKey);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }
}
