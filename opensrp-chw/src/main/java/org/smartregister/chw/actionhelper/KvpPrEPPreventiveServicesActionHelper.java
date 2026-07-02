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
            "tested3months",
            "hivResult_recent",
            "onPrEP",
            "prepFacilityA",
            "linkedToPrEP_recent",
            "referredForHiv",
            "testedForHiv",
            "testingLocation",
            "facilityName",
            "testDate",
            "hivResult",
            "prepFollowUp",
            "prepFacilityB",
            "linkedToPrEP",
            "received_prep_last_facility",
            "kits_distributed",
            "prompt_for_hivst"
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
            if (clientHivPositive) {
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
        return null;
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
}
