package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.client.utils.constants.JsonFormConstants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Summarizes committee attendance and traditional medicine provider counts.
 */
public class HpsAnnualCensusStep5CommitteesTraditionalMedicineActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    private final String householdMax;
    private Context context;
    private String jsonPayload;
    private String submittedPayload;

    public HpsAnnualCensusStep5CommitteesTraditionalMedicineActionHelper(String householdMax) {
        this.householdMax = householdMax;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonPayload;
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        try {
            if (jsonPayload == null) return null;
            JSONObject json = new JSONObject(jsonPayload);
            if (householdMax != null && !householdMax.trim().isEmpty()) {
                json.getJSONObject(JsonFormConstants.JSON_FORM_KEY.GLOBAL).put("household_max", householdMax);
            }
            return json.toString();
        } catch (Exception e) {
            Timber.e(e);
            return jsonPayload;
        }
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        this.submittedPayload = jsonPayload;
    }

    @Override
    public BaseHpsVisitAction.ScheduleStatus getPreProcessedStatus() { return null; }

    @Override
    public String getPreProcessedSubTitle() { return null; }

    @Override
    public String postProcess(String s) { return null; }

    @Override
    public String evaluateSubTitle() {
        try {
            if (submittedPayload == null) return null;
            JSONObject json = new JSONObject(submittedPayload);
            String committeeMembers = CoreJsonFormUtils.getValue(json, "number_of_health_committee_members_for_effective_committee_meetings");
            String q1 = CoreJsonFormUtils.getValue(json, "number_of_committee_members_attended_first_quarter");
            String q2 = CoreJsonFormUtils.getValue(json, "number_of_committee_members_attended_second_quarter");
            String q3 = CoreJsonFormUtils.getValue(json, "number_of_committee_members_attended_third_quarter");
            String q4 = CoreJsonFormUtils.getValue(json, "number_of_committee_members_attended_fourth_quarter");
            String altReg = CoreJsonFormUtils.getValue(json, "number_of_registered_alternative_medicine_service_providers");
            String tradReg = CoreJsonFormUtils.getValue(json, "number_of_registered_traditional_medicine_service_providers");
            String altUnreg = CoreJsonFormUtils.getValue(json, "number_of_unregistered_alternative_medicine_service_providers");
            String tradUnreg = CoreJsonFormUtils.getValue(json, "number_of_unregistered_traditional_medicine_service_providers");
            committeeMembers = committeeMembers == null ? "" : committeeMembers.trim();
            q1 = q1 == null ? "" : q1.trim();
            q2 = q2 == null ? "" : q2.trim();
            q3 = q3 == null ? "" : q3.trim();
            q4 = q4 == null ? "" : q4.trim();
            altReg = altReg == null ? "" : altReg.trim();
            tradReg = tradReg == null ? "" : tradReg.trim();
            altUnreg = altUnreg == null ? "" : altUnreg.trim();
            tradUnreg = tradUnreg == null ? "" : tradUnreg.trim();
            if (committeeMembers.isEmpty() && q1.isEmpty() && q2.isEmpty() && q3.isEmpty() && q4.isEmpty()
                    && altReg.isEmpty() && tradReg.isEmpty() && altUnreg.isEmpty() && tradUnreg.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            if (!committeeMembers.isEmpty()) {
                appendSegment(sb, context != null
                        ? context.getString(R.string.hps_annual_census_committees_traditional_subtitle_committee, committeeMembers)
                        : "Committee " + committeeMembers);
            }
            if (!q1.isEmpty() || !q2.isEmpty() || !q3.isEmpty() || !q4.isEmpty() || !altReg.isEmpty() || !tradReg.isEmpty()) {
                appendSegment(sb, context != null
                        ? context.getString(
                        R.string.hps_annual_census_committees_traditional_subtitle,
                        q1, q2, q3, q4, altReg, tradReg)
                        : String.format("Attendance Q1:%s Q2:%s Q3:%s Q4:%s | Registered Alt:%s Trad:%s", q1, q2, q3, q4, altReg, tradReg));
            }
            if (!altUnreg.isEmpty() || !tradUnreg.isEmpty()) {
                appendSegment(sb, context != null
                        ? context.getString(
                        R.string.hps_annual_census_committees_traditional_subtitle_unregistered,
                        altUnreg,
                        tradUnreg)
                        : String.format("Unregistered Alt:%s Trad:%s", altUnreg, tradUnreg));
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public BaseHpsVisitAction.Status evaluateStatusOnPayload() {
        try {
            if (submittedPayload == null) return BaseHpsVisitAction.Status.PENDING;
            JSONObject json = new JSONObject(submittedPayload);
            String[] keys = new String[]{
                    "number_of_health_committee_members_for_effective_committee_meetings",
                    "number_of_committee_members_attended_first_quarter",
                    "number_of_committee_members_attended_second_quarter",
                    "number_of_committee_members_attended_third_quarter",
                    "number_of_committee_members_attended_fourth_quarter",
                    "number_of_registered_alternative_medicine_service_providers",
                    "number_of_registered_traditional_medicine_service_providers",
                    "number_of_unregistered_alternative_medicine_service_providers",
                    "number_of_unregistered_traditional_medicine_service_providers"
            };
            for (String k : keys) {
                String v = CoreJsonFormUtils.getValue(json, k);
                if (v != null && !v.trim().isEmpty()) return BaseHpsVisitAction.Status.COMPLETED;
            }
            return BaseHpsVisitAction.Status.PENDING;
        } catch (Exception e) {
            Timber.e(e);
            return BaseHpsVisitAction.Status.PENDING;
        }
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }

    private void appendSegment(StringBuilder sb, String segment) {
        if (segment == null || segment.trim().isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(segment);
    }
}
