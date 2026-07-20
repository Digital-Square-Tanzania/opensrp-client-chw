package org.smartregister.chw.actionhelper;

import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.util.JsonFormUtils.STEP1;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.dao.ChwKvpDao;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.domain.VisitDetail;

import java.util.List;
import java.util.Map;

public class KvpPrEPPreventiveServicesActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    private static final String[] HIV_PREP_SUPPRESSED_FIELDS = {
            "hiv_tested_within_last_3_months",
            "hiv_result_recent",
            "ctc_number_a",
            "on_prep",
            "prep_facility_a",
            "linked_to_prep_recent",
            "linked_to_prep_recent_prompt",
            "referred_for_hiv_test",
            "referred_for_hiv_prompt",
            "tested_for_hiv",
            "testing_location",
            "facility_name",
            "test_date",
            "hiv_result",
            "ctc_number_b",
            "prep_follow_up",
            "prep_facility_b",
            "linked_to_prep",
            "linked_to_prep_prompt",
            "received_prep_last_facility",
            "kits_distributed",
            "prompt_for_hivst"
    };

    private static final String[] BRANCH_A_FIELDS = {
            "hiv_result_recent",
            "ctc_number_a",
            "on_prep",
            "prep_facility_a",
            "linked_to_prep_recent"
    };

    private static final String[] BRANCH_B_FIELDS = {
            "referred_for_hiv_test",
            "tested_for_hiv",
            "testing_location",
            "facility_name",
            "test_date",
            "hiv_result",
            "ctc_number_b",
            "prep_follow_up",
            "prep_facility_b",
            "linked_to_prep"
    };

    private static final String[] REFERRED_FOR_HIV_DOWNSTREAM_FIELDS = {
            "tested_for_hiv",
            "testing_location",
            "facility_name",
            "test_date",
            "hiv_result",
            "ctc_number_b",
            "prep_follow_up",
            "prep_facility_b",
            "linked_to_prep"
    };

    private static final String[] TESTED_FOR_HIV_DOWNSTREAM_FIELDS = {
            "testing_location",
            "facility_name",
            "test_date",
            "hiv_result",
            "ctc_number_b",
            "prep_follow_up",
            "prep_facility_b",
            "linked_to_prep"
    };

    private String condoms_given;
    private String jsonPayload;
    private String baseEntityId;
    private final Map<String, String> visitState;

    public KvpPrEPPreventiveServicesActionHelper(String baseEntityId, Map<String, String> visitState) {
        this.baseEntityId = baseEntityId;
        this.visitState = visitState;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            if (!ChwKvpDao.getDominantKVPGroup(baseEntityId).equalsIgnoreCase("pwud") &&
                    !ChwKvpDao.getDominantKVPGroup(baseEntityId).equalsIgnoreCase("pwid")) {
                getFieldJSONObject(fields(jsonObject, STEP1), "number_of_needles_and_syringes_distributed").put("type", "hidden");
                getFieldJSONObject(fields(jsonObject, STEP1), "number_of_sterile_water_for_injection_distributed").put("type", "hidden");
                getFieldJSONObject(fields(jsonObject, STEP1), "number_of_alcohol_swabs_distributed").put("type", "hidden");
                getFieldJSONObject(fields(jsonObject, STEP1), "number_of_disposable_safety_boxes_distributed").put("type", "hidden");
                getFieldJSONObject(fields(jsonObject, STEP1), "number_of_plasters_distributed").put("type", "hidden");
                getFieldJSONObject(fields(jsonObject, STEP1), "protective_items_for_PWID_label").put("type", "hidden");
            }

            boolean clientHivPositive = ChwKvpDao.isClientHivPositive(baseEntityId);
            if (clientHivPositive || !ChwKvpDao.isHivRetestDue(baseEntityId)) {
                suppressHivPrepFields(jsonObject);
            }

            JSONObject global = jsonObject.optJSONObject("global");
            if (global != null) {
                String visitType = StringUtils.defaultIfBlank(visitState.get("visit_type"), ChwKvpDao.getLatestVisitType(baseEntityId));
                boolean hasFollowupVisits = ChwKvpDao.hasFollowupVisits(baseEntityId);
                String visitNumber = hasFollowupVisits ? "2" : "1";
                if (StringUtils.equalsIgnoreCase(visitType, "followup")) {
                    visitNumber = "2";
                }
                String hivStatus = clientHivPositive ? "positive" : StringUtils.defaultIfBlank(visitState.get("client_hiv_status"), ChwKvpDao.getLatestClientHivStatus(baseEntityId));

                if (StringUtils.isNotBlank(visitType)) {
                    global.put("visit_type", visitType);
                }
                if (StringUtils.isNotBlank(visitNumber)) {
                    global.put("visit_number", visitNumber);
                }
                if (StringUtils.isNotBlank(hivStatus)) {
                    global.put("client_hiv_status", hivStatus);
                }
            }
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void suppressHivPrepFields(JSONObject jsonObject) throws JSONException {
        JSONArray formFields = fields(jsonObject, STEP1);
        for (String fieldKey : HIV_PREP_SUPPRESSED_FIELDS) {
            JSONObject field = getField(formFields, fieldKey);
            if (field != null) {
                field.put("type", "hidden");
                field.remove("relevance");
            }
        }
    }

    private JSONObject getField(JSONArray formFields, String fieldKey) {
        if (formFields == null || StringUtils.isBlank(fieldKey)) {
            return null;
        }

        for (int i = 0; i < formFields.length(); i++) {
            JSONObject field = formFields.optJSONObject(i);
            if (field != null && StringUtils.equals(field.optString("key"), fieldKey)) {
                return field;
            }
        }

        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            condoms_given = CoreJsonFormUtils.getValue(jsonObject, "condoms_given");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BaseKvpVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONArray formFields = fields(jsonObject, STEP1);

            clearInactiveBranchValues(formFields);
            normalizePersistedHivState(formFields);

            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonPayload;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseKvpVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(condoms_given))
            return BaseKvpVisitAction.Status.PENDING;
        else {
            return BaseKvpVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction baseKvpVisitAction) {
        //overridden
    }

    private void clearInactiveBranchValues(JSONArray formFields) throws JSONException {
        String tested3months = getValue(formFields, "hiv_tested_within_last_3_months");

        if (!StringUtils.equalsIgnoreCase(tested3months, "yes")) {
            clearValues(formFields, BRANCH_A_FIELDS);
        }

        if (!StringUtils.equalsIgnoreCase(tested3months, "no")) {
            clearValues(formFields, BRANCH_B_FIELDS);
        }

        if (StringUtils.equalsIgnoreCase(tested3months, "yes")) {
            clearBranchAValues(formFields);
        } else if (StringUtils.equalsIgnoreCase(tested3months, "no")) {
            clearBranchBValues(formFields);
        }
    }

    private void clearBranchAValues(JSONArray formFields) throws JSONException {
        String hivResultRecent = getValue(formFields, "hiv_result_recent");
        if (StringUtils.equalsIgnoreCase(hivResultRecent, "positive")) {
            clearValues(formFields, "on_prep", "prep_facility_a", "linked_to_prep_recent");
        } else if (StringUtils.equalsIgnoreCase(hivResultRecent, "negative")) {
            clearValues(formFields, "ctc_number_a");
            clearPrEPBranchAValues(formFields);
        } else {
            clearValues(formFields, "ctc_number_a", "on_prep", "prep_facility_a", "linked_to_prep_recent");
        }
    }

    private void clearPrEPBranchAValues(JSONArray formFields) throws JSONException {
        String onPrEP = getValue(formFields, "on_prep");
        if (StringUtils.equalsIgnoreCase(onPrEP, "yes")) {
            clearValues(formFields, "linked_to_prep_recent");
        } else if (StringUtils.equalsIgnoreCase(onPrEP, "no")) {
            clearValues(formFields, "prep_facility_a");
        } else {
            clearValues(formFields, "prep_facility_a", "linked_to_prep_recent");
        }
    }

    private void clearBranchBValues(JSONArray formFields) throws JSONException {
        String referredForHiv = getValue(formFields, "referred_for_hiv_test");
        if (!StringUtils.equalsIgnoreCase(referredForHiv, "yes")) {
            clearValues(formFields, REFERRED_FOR_HIV_DOWNSTREAM_FIELDS);
            return;
        }

        String testedForHiv = getValue(formFields, "tested_for_hiv");
        if (!StringUtils.equalsIgnoreCase(testedForHiv, "yes")) {
            clearValues(formFields, TESTED_FOR_HIV_DOWNSTREAM_FIELDS);
            return;
        }

        String hivResult = getValue(formFields, "hiv_result");
        if (StringUtils.equalsIgnoreCase(hivResult, "positive")) {
            clearValues(formFields, "prep_follow_up", "prep_facility_b", "linked_to_prep");
        } else if (StringUtils.equalsIgnoreCase(hivResult, "negative")) {
            clearValues(formFields, "ctc_number_b");
            clearPrEPBranchBValues(formFields);
        } else {
            clearValues(formFields, "ctc_number_b", "prep_follow_up", "prep_facility_b", "linked_to_prep");
        }
    }

    private void clearPrEPBranchBValues(JSONArray formFields) throws JSONException {
        String prepFollowUp = getValue(formFields, "prep_follow_up");
        if (StringUtils.equalsIgnoreCase(prepFollowUp, "yes")) {
            clearValues(formFields, "linked_to_prep");
        } else if (StringUtils.equalsIgnoreCase(prepFollowUp, "no")) {
            clearValues(formFields, "prep_facility_b");
        } else {
            clearValues(formFields, "prep_facility_b", "linked_to_prep");
        }
    }

    private void normalizePersistedHivState(JSONArray formFields) throws JSONException {
        String tested3months = getValue(formFields, "hiv_tested_within_last_3_months");
        String hivResultRecent = getValue(formFields, "hiv_result_recent");
        String referredForHiv = getValue(formFields, "referred_for_hiv_test");
        String testedForHiv = getValue(formFields, "tested_for_hiv");
        String testingLocation = getValue(formFields, "testing_location");
        String hivResult = getValue(formFields, "hiv_result");
        String ctcNumberA = getValue(formFields, "ctc_number_a");
        String ctcNumberB = getValue(formFields, "ctc_number_b");

        boolean positive = ChwKvpDao.isClientHivPositive(baseEntityId) ||
                StringUtils.equalsIgnoreCase(hivResultRecent, "positive") ||
                StringUtils.equalsIgnoreCase(hivResult, "positive");
        boolean negative = !positive && (StringUtils.equalsIgnoreCase(hivResultRecent, "negative") ||
                StringUtils.equalsIgnoreCase(hivResult, "negative"));

        setValue(formFields, "client_hiv_status", positive ? "positive" : negative ? "negative" : "");
        setValue(formFields, "hiv_positive", Boolean.toString(positive));
        setValue(formFields, "ctc_number", positive ? StringUtils.defaultIfBlank(ctcNumberA, ctcNumberB) : "");
        setValue(formFields, "hiv_test_conducted", getCompatHivTestConducted(tested3months, hivResultRecent, referredForHiv, testedForHiv));
        setValue(formFields, "hiv_test_location", StringUtils.equalsIgnoreCase(testedForHiv, "yes") ? testingLocation : "");

        if (positive) {
            visitState.put("client_hiv_status", "positive");
        }
    }

    private String getCompatHivTestConducted(String tested3months, String hivResultRecent, String referredForHiv, String testedForHiv) {
        if (StringUtils.equalsIgnoreCase(tested3months, "yes") && StringUtils.isNotBlank(hivResultRecent)) {
            return "yes";
        }

        if (StringUtils.equalsIgnoreCase(tested3months, "no")) {
            if (StringUtils.equalsIgnoreCase(testedForHiv, "yes")) {
                return "yes";
            }

            if (StringUtils.equalsIgnoreCase(referredForHiv, "no") || StringUtils.equalsIgnoreCase(testedForHiv, "no")) {
                return "no";
            }
        }

        return "";
    }

    private void clearValues(JSONArray formFields, String... fieldKeys) throws JSONException {
        for (String fieldKey : fieldKeys) {
            setValue(formFields, fieldKey, "");
        }
    }

    private String getValue(JSONArray formFields, String fieldKey) {
        JSONObject field = getField(formFields, fieldKey);
        return field == null ? null : field.optString("value", null);
    }

    private void setValue(JSONArray formFields, String fieldKey, String value) throws JSONException {
        JSONObject field = getField(formFields, fieldKey);
        if (field != null) {
            field.put("value", StringUtils.defaultString(value));
        }
    }
}
