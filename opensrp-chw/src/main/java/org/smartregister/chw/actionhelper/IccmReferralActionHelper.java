package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.chw.util.IccmVisitUtils;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.chw.util.JsonFormUtilsFlv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmReferralActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper{
    private String jsonPayload;
    private Map<String, List<VisitDetail>> details;
    private final HashMap<String, Boolean> checkObject = new HashMap<>();

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
        try {
            checkObject.clear();
            JSONObject jsonObject = new JSONObject(jsonPayload);

            String problem = CoreJsonFormUtils.getValue(jsonObject, "problem");
            String problemOther = CoreJsonFormUtils.getValue(jsonObject, "problem_other");
            String serviceBeforeReferral = CoreJsonFormUtils.getValue(jsonObject, "service_before_referral");
            String referralFacility = CoreJsonFormUtils.getValue(jsonObject, "chw_referral_hf");
            String referralAppointmentDate = CoreJsonFormUtils.getValue(jsonObject, "referral_appointment_date");

            checkObject.put("problem", hasValue(problem));
            checkObject.put("service_before_referral", hasValue(serviceBeforeReferral));
            checkObject.put("chw_referral_hf", hasValue(referralFacility));
            checkObject.put("referral_appointment_date", hasValue(referralAppointmentDate));

            if (containsOption(problem, "other_reasons")) {
                checkObject.put("problem_other", hasValue(problemOther));
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
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
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return "";
    }

    @Override
    public BaseIccmVisitAction.Status evaluateStatusOnPayload() {
        String status = IccmVisitUtils.getActionStatus(checkObject);
        if (status.equalsIgnoreCase(IccmVisitUtils.Complete)) {
            return BaseIccmVisitAction.Status.COMPLETED;
        }
        if (status.equalsIgnoreCase(IccmVisitUtils.Ongoing)) {
            return BaseIccmVisitAction.Status.PARTIALLY_COMPLETED;
        }
        return BaseIccmVisitAction.Status.PENDING;
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

        JSONArray jsonArray = JsonFormUtils.fields(jsonForm);
        JSONObject referralFacilityField = JsonFormUtils.getFieldJSONObject(jsonArray, "chw_referral_hf");

        if (referralFacilityField != null) {
            referralFacilityField.put("value", selectedFacility);
        }
    }

    private boolean hasValue(String value) {
        String normalizedValue = StringUtils.trimToEmpty(value);
        return StringUtils.isNotBlank(normalizedValue)
                && !"[]".equals(normalizedValue)
                && !"{}".equals(normalizedValue)
                && !"null".equalsIgnoreCase(normalizedValue);
    }

    private boolean containsOption(String value, String optionKey) {
        return hasValue(value) && StringUtils.containsIgnoreCase(value, optionKey);
    }
}
