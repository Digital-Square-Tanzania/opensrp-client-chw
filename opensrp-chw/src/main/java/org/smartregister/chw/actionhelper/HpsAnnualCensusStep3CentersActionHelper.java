package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep3CentersActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    private String jsonPayload;
    private String submittedPayload;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        // No preprocessing or global injection needed for this step
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        this.submittedPayload = jsonPayload;
    }

    @Override
    public BaseHpsVisitAction.ScheduleStatus getPreProcessedStatus() {
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
        try {
            if (submittedPayload == null) return null;
            JSONObject json = new JSONObject(submittedPayload);
            // Summarize selected categories
            String categories = JsonFormUtils.getCheckBoxValue(json, "select_centers_category");
            if (categories != null && !categories.trim().isEmpty()) {
                return "Categories: " + categories.replaceAll(
                        "(?i)faith_based_organisation", "FBO");
            }
            // Fallback to a couple of numeric highlights if present
            String hospitalsGov = CoreJsonFormUtils.getValue(json, "number_of_hospital_government");
            String schoolsGov = CoreJsonFormUtils.getValue(json, "number_of_primary_schools_government");
            if ((hospitalsGov != null && !hospitalsGov.trim().isEmpty()) || (schoolsGov != null && !schoolsGov.trim().isEmpty())) {
                hospitalsGov = hospitalsGov == null ? "" : hospitalsGov.trim();
                schoolsGov = schoolsGov == null ? "" : schoolsGov.trim();
                return String.format("Gov: Hospitals %s, Primary schools %s", hospitalsGov, schoolsGov);
            }
            return null;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @Override
    public BaseHpsVisitAction.Status evaluateStatusOnPayload() {
        try {
            if (submittedPayload == null) return BaseHpsVisitAction.Status.PENDING;
            JSONObject json = new JSONObject(submittedPayload);
            String categories = JsonFormUtils.getCheckBoxValue(json, "select_centers_category");
            if (categories != null && !categories.trim().isEmpty()) {
                return BaseHpsVisitAction.Status.COMPLETED;
            }
            // If any numeric field is filled, also treat as completed
            String[] keys = new String[]{
                    "number_of_pre_schools_government", "number_of_primary_schools_government", "number_of_secondary_schools_government",
                    "number_of_universities_government", "number_of_special_needs_schools_government", "number_of_dispensary_government",
                    "number_of_health_centers_government", "number_of_hospital_government", "number_of_special_clinics_government",
                    "number_of_laboratory_government", "number_of_pharmacy_government", "number_of_ADDO_government",
                    "number_of_maternity_home_government", "number_of_orphan_care_centers_government",
                    "number_of_centers_for_children_with_disabilities_government", "number_of_cbecdc_rehabilitation_centre_government",
                    "number_of_day_care_centers_government", "number_of_elderly_care_centers_government",

                    "number_of_pre_schools_faith_based_organisation", "number_of_primary_schools_faith_based_organisation",
                    "number_of_secondary_schools_faith_based_organisation", "number_of_universities_faith_based_organisation",
                    "number_of_special_needs_schools_faith_based_organisation", "number_of_dispensary_faith_based_organisation",
                    "number_of_health_centers_faith_based_organisation", "number_of_hospital_faith_based_organisation",
                    "number_of_special_clinics_faith_based_organisation", "number_of_laboratory_faith_based_organisation",
                    "number_of_pharmacy_faith_based_organisation", "number_of_ADDO_faith_based_organisation",
                    "number_of_maternity_home_faith_based_organisation", "number_of_orphan_care_centers_faith_based_organisation",
                    "number_of_centers_for_children_with_disabilities_faith_based_organisation",
                    "number_of_cbecdc_rehabilitation_centre_faith_based_organisation",
                    "number_of_day_care_centers_faith_based_organisation", "number_of_elderly_care_centers_faith_based_organisation",

                    "number_of_pre_schools_public", "number_of_primary_schools_public", "number_of_secondary_schools_public",
                    "number_of_universities_public", "number_of_special_needs_schools_public", "number_of_dispensary_public",
                    "number_of_health_centers_public", "number_of_hospital_public", "number_of_special_clinics_public",
                    "number_of_laboratory_public", "number_of_pharmacy_public", "number_of_ADDO_public",
                    "number_of_maternity_home_public", "number_of_orphan_care_centers_public",
                    "number_of_centers_for_children_with_disabilities_public", "number_of_cbecdc_rehabilitation_centre_public",
                    "number_of_day_care_centers_public", "number_of_elderly_care_centers_public",

                    "number_of_pre_schools_private", "number_of_primary_schools_private", "number_of_secondary_schools_private",
                    "number_of_universities_private", "number_of_special_needs_schools_private", "number_of_dispensary_private",
                    "number_of_health_centers_private", "number_of_hospital_private", "number_of_special_clinics_private",
                    "number_of_laboratory_private", "number_of_pharmacy_private", "number_of_ADDO_private",
                    "number_of_maternity_home_private", "number_of_orphan_care_centers_private",
                    "number_of_centers_for_children_with_disabilities_private", "number_of_cbecdc_rehabilitation_centre_private",
                    "number_of_day_care_centers_private", "number_of_elderly_care_centers_private"
            };
            for (String k : keys) {
                String v = CoreJsonFormUtils.getValue(json, k);
                if (v != null && !v.trim().isEmpty()) {
                    return BaseHpsVisitAction.Status.COMPLETED;
                }
            }
            return BaseHpsVisitAction.Status.PENDING;
        } catch (Exception e) {
            Timber.e(e);
            return BaseHpsVisitAction.Status.PENDING;
        }
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }
}

