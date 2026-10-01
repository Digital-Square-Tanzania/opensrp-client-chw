package org.smartregister.chw.actionhelper;

import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.util.JsonFormUtils.STEP1;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.ayp.actionhelper.aypOutOfSchool.AypOutSchoolMedicalServiceActionHelper;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.dao.AypOutSchoolDao;

public class AypOutSchoolMedicalServicesActionHelper extends AypOutSchoolMedicalServiceActionHelper {

    private static final String[] HIV_PREP_FIELDS = {
            "hiv_tested_within_last_3_months",
            "hiv_result_recent",
            "ctc_number_a",
            "on_prep",
            "prep_facility_a",
            "linked_to_prep_recent",
            "linked_to_prep_recent_prompt",
            "referred_for_hiv_test",
            "referred_for_hiv_test_prompt",
            "tested_for_hiv",
            "testing_location",
            "facility_name",
            "test_date",
            "hiv_result",
            "ctc_number_b",
            "prep_follow_up",
            "prep_facility_b",
            "linked_to_prep",
            "linked_to_prep_prompt"
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

    private final String baseEntityId;

    public AypOutSchoolMedicalServicesActionHelper(Context context, MemberObject memberObject) {
        super(context, memberObject);
        baseEntityId = memberObject.getBaseEntityId();
    }

    @Override
    public String getPreProcessed() {
        String preProcessedPayload = super.getPreProcessed();
        if (StringUtils.isBlank(preProcessedPayload)) {
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(preProcessedPayload);
            if (isClientHivPositive()) {
                suppressHivPrepFields(jsonObject);
            }
            return jsonObject.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    protected boolean isClientHivPositive() {
        return AypOutSchoolDao.isClientHivPositive(baseEntityId);
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
            return jsonPayload;
        }
    }

    private void suppressHivPrepFields(JSONObject jsonObject) throws JSONException {
        JSONArray formFields = fields(jsonObject, STEP1);
        for (String fieldKey : HIV_PREP_FIELDS) {
            JSONObject field = getField(formFields, fieldKey);
            if (field != null) {
                field.put("type", "hidden");
                field.remove("relevance");
            }
        }
    }

    private void clearInactiveBranchValues(JSONArray formFields) throws JSONException {
        String testedRecently = getValue(formFields, "hiv_tested_within_last_3_months");
        if (!StringUtils.equalsIgnoreCase(testedRecently, "yes")) {
            clearValues(formFields, BRANCH_A_FIELDS);
        }
        if (!StringUtils.equalsIgnoreCase(testedRecently, "no")) {
            clearValues(formFields, BRANCH_B_FIELDS);
        }

        if (StringUtils.equalsIgnoreCase(testedRecently, "yes")) {
            clearBranchAValues(formFields);
        } else if (StringUtils.equalsIgnoreCase(testedRecently, "no")) {
            clearBranchBValues(formFields);
        }
    }

    private void clearBranchAValues(JSONArray formFields) throws JSONException {
        String result = getValue(formFields, "hiv_result_recent");
        if (StringUtils.equalsIgnoreCase(result, "positive")) {
            clearValues(formFields, "on_prep", "prep_facility_a", "linked_to_prep_recent");
        } else if (StringUtils.equalsIgnoreCase(result, "negative")) {
            clearValues(formFields, "ctc_number_a");
            clearPrepBranchValues(formFields, "on_prep", "prep_facility_a", "linked_to_prep_recent");
        } else {
            clearValues(formFields, "ctc_number_a", "on_prep", "prep_facility_a", "linked_to_prep_recent");
        }
    }

    private void clearBranchBValues(JSONArray formFields) throws JSONException {
        if (!StringUtils.equalsIgnoreCase(getValue(formFields, "referred_for_hiv_test"), "yes")) {
            clearValues(formFields, REFERRED_FOR_HIV_DOWNSTREAM_FIELDS);
            return;
        }
        if (!StringUtils.equalsIgnoreCase(getValue(formFields, "tested_for_hiv"), "yes")) {
            clearValues(formFields, TESTED_FOR_HIV_DOWNSTREAM_FIELDS);
            return;
        }

        String result = getValue(formFields, "hiv_result");
        if (StringUtils.equalsIgnoreCase(result, "positive")) {
            clearValues(formFields, "prep_follow_up", "prep_facility_b", "linked_to_prep");
        } else if (StringUtils.equalsIgnoreCase(result, "negative")) {
            clearValues(formFields, "ctc_number_b");
            clearPrepBranchValues(formFields, "prep_follow_up", "prep_facility_b", "linked_to_prep");
        } else {
            clearValues(formFields, "ctc_number_b", "prep_follow_up", "prep_facility_b", "linked_to_prep");
        }
    }

    private void clearPrepBranchValues(JSONArray formFields, String prepField, String facilityField,
                                       String linkageField) throws JSONException {
        String prepValue = getValue(formFields, prepField);
        if (StringUtils.equalsIgnoreCase(prepValue, "yes")) {
            clearValues(formFields, linkageField);
        } else if (StringUtils.equalsIgnoreCase(prepValue, "no")) {
            clearValues(formFields, facilityField);
        } else {
            clearValues(formFields, facilityField, linkageField);
        }
    }

    private void normalizePersistedHivState(JSONArray formFields) throws JSONException {
        String testedRecently = getValue(formFields, "hiv_tested_within_last_3_months");
        String recentResult = getValue(formFields, "hiv_result_recent");
        String referred = getValue(formFields, "referred_for_hiv_test");
        String tested = getValue(formFields, "tested_for_hiv");
        String testingLocation = getValue(formFields, "testing_location");
        String result = getValue(formFields, "hiv_result");

        boolean positive = isClientHivPositive()
                || StringUtils.equalsIgnoreCase(recentResult, "positive")
                || StringUtils.equalsIgnoreCase(result, "positive");
        boolean negative = !positive && (StringUtils.equalsIgnoreCase(recentResult, "negative")
                || StringUtils.equalsIgnoreCase(result, "negative"));

        setValue(formFields, "client_hiv_status", positive ? "positive" : negative ? "negative" : "");
        setValue(formFields, "hiv_positive", Boolean.toString(positive));
        setValue(formFields, "ctc_number", positive
                ? StringUtils.defaultIfBlank(getValue(formFields, "ctc_number_a"), getValue(formFields, "ctc_number_b"))
                : "");
        setValue(formFields, "tested_hiv", getCompatHivTested(testedRecently, recentResult, referred, tested));
        setValue(formFields, "loc_test_conducted",
                StringUtils.equalsIgnoreCase(tested, "yes") ? testingLocation : "");
        setValue(formFields, "linked_to_prep_services",
                StringUtils.defaultIfBlank(getValue(formFields, "linked_to_prep_recent"),
                        getValue(formFields, "linked_to_prep")));
    }

    private String getCompatHivTested(String testedRecently, String recentResult, String referred,
                                      String tested) {
        if (StringUtils.equalsIgnoreCase(testedRecently, "yes") && StringUtils.isNotBlank(recentResult)) {
            return "yes";
        }
        if (StringUtils.equalsIgnoreCase(testedRecently, "no")) {
            if (StringUtils.equalsIgnoreCase(tested, "yes")) {
                return "yes";
            }
            if (StringUtils.equalsIgnoreCase(referred, "no")
                    || StringUtils.equalsIgnoreCase(tested, "no")) {
                return "no";
            }
        }
        return "";
    }

    private JSONObject getField(JSONArray formFields, String fieldKey) {
        if (formFields == null || StringUtils.isBlank(fieldKey)) {
            return null;
        }
        return getFieldJSONObject(formFields, fieldKey);
    }

    private String getValue(JSONArray formFields, String fieldKey) {
        JSONObject field = getField(formFields, fieldKey);
        return field == null ? null : field.optString("value", null);
    }

    private void clearValues(JSONArray formFields, String... fieldKeys) throws JSONException {
        for (String fieldKey : fieldKeys) {
            setValue(formFields, fieldKey, "");
        }
    }

    private void setValue(JSONArray formFields, String fieldKey, String value) throws JSONException {
        JSONObject field = getField(formFields, fieldKey);
        if (field != null) {
            field.put("value", StringUtils.defaultString(value));
        }
    }
}
