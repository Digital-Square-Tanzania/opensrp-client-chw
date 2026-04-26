package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.IccmReferralFormUtils;
import org.smartregister.chw.util.IccmVisitUtils;
import org.smartregister.chw.util.JsonFormUtilsFlv;
import org.smartregister.family.util.JsonFormUtils;


import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class IccmReferralActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper{
    private static final String STEP1 = "step1";
    private static final String FIELDS = "fields";
    private static final String OPTIONS = "options";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String NONE = "none";

    private static final String YES = "yes";

    private String jsonPayload;
    private final HashMap<String, Boolean> checkObject = new HashMap<>();
    private final String enrollmentFormSubmissionId;
    private final Map<String, BaseIccmVisitAction> actionList;
    private final int age;
    private final String gender;

    public IccmReferralActionHelper() {
        this(null, null, 0, null);
    }

    public IccmReferralActionHelper(String enrollmentFormSubmissionId, Map<String, BaseIccmVisitAction> actionList) {
        this(enrollmentFormSubmissionId, actionList, 0, null);
    }

    public IccmReferralActionHelper(String enrollmentFormSubmissionId,
                                    Map<String, BaseIccmVisitAction> actionList,
                                    int age,
                                    String gender) {
        this.enrollmentFormSubmissionId = enrollmentFormSubmissionId;
        this.actionList = actionList;
        this.age = age;
        this.gender = gender;
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonString;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonForm = new JSONObject(jsonPayload);
            Map<String, String> facilityOptions = LocationUtils.INSTANCE.getFacilitiesKeyAndName();
            JsonFormUtilsFlv.overwriteQuestionOptions("chw_referral_hf", facilityOptions, jsonForm);
            updateReferralFields(jsonForm);
            prepopulateTreatmentsBeforeReferral(jsonForm);
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

    private void prepopulateTreatmentsBeforeReferral(JSONObject jsonForm) {
        try {
            Set<String> treatments = collectTreatmentsForPrepopulation();
            if (treatments.isEmpty()) {
                return;
            }

            JSONArray fields = jsonForm.getJSONObject(STEP1).getJSONArray(FIELDS);
            JSONObject serviceBeforeReferral = JsonFormUtils
                    .getFieldJSONObject(fields, Constants.iCCMTreatment.FIELD_SERVICE_BEFORE_REFERRAL);
            if (serviceBeforeReferral == null) {
                return;
            }

            JSONArray options = serviceBeforeReferral.optJSONArray(OPTIONS);
            if (options == null) {
                return;
            }

            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                String optionKey = option.optString(KEY);
                if (StringUtils.isBlank(optionKey)) {
                    continue;
                }

                if (NONE.equalsIgnoreCase(optionKey)) {
                    option.put(VALUE, false);
                    continue;
                }

                if (treatments.contains(optionKey)) {
                    option.put(VALUE, true);
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void updateReferralFields(JSONObject jsonForm) {
        try {
            JSONArray fields = jsonForm.getJSONObject(STEP1).getJSONArray(FIELDS);
            boolean isChild = age < 10;
            boolean isFemaleOfReproductiveAge = age >= 10
                    && age <= 49
                    && StringUtils.equalsIgnoreCase(gender, "female");
            IccmReferralFormUtils.updateNativeReferralFields(fields, isChild, isFemaleOfReproductiveAge);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private Set<String> collectTreatmentsForPrepopulation() {
        Set<String> treatments = new LinkedHashSet<>();
        if (isAntiPyreticDispensedDuringEnrollment()) {
            treatments.add(Constants.iCCMTreatment.TREATMENT_ANTI_PYRETIC);
        }
        treatments.addAll(getDiarrheaTreatmentsFromActionList());
        return treatments;
    }

    private boolean isAntiPyreticDispensedDuringEnrollment() {
        if (StringUtils.isBlank(enrollmentFormSubmissionId)) {
            return false;
        }

        try {
            Event enrollmentEvent = IccmDao.getEventByFormSubmissionId(enrollmentFormSubmissionId);
            if (enrollmentEvent == null || enrollmentEvent.getObs() == null) {
                return false;
            }

            for (Obs obs : enrollmentEvent.getObs()) {
                String fieldName = StringUtils.defaultIfBlank(obs.getFormSubmissionField(), obs.getFieldCode());
                if (!Constants.iCCMTreatment.FIELD_DISPENSED_ANTI_PYRETIC.equalsIgnoreCase(fieldName)) {
                    continue;
                }

                String value = observationValue(obs);
                return containsOption(value, YES);
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return false;
    }

    private String observationValue(Obs obs) {
        if (obs == null) {
            return "";
        }

        try {
            List<Object> values = obs.getValues();
            if (values != null && !values.isEmpty()) {
                return StringUtils.join(values, ",");
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            Object value = obs.getValue();
            return value != null ? String.valueOf(value) : "";
        } catch (Exception e) {
            Timber.e(e);
        }

        return "";
    }

    private Set<String> getDiarrheaTreatmentsFromActionList() {
        Set<String> treatments = new LinkedHashSet<>();
        if (actionList == null || actionList.isEmpty()) {
            return treatments;
        }

        try {
            for (BaseIccmVisitAction action : actionList.values()) {
                if (action == null || !Constants.JsonForm.getIccmDiarrhea().equalsIgnoreCase(action.getFormName())) {
                    continue;
                }

                String diarrheaPayload = action.getJsonPayload();
                if (StringUtils.isBlank(diarrheaPayload)) {
                    continue;
                }

                JSONObject jsonObject = new JSONObject(diarrheaPayload);
                addDiarrheaTreatments(jsonObject, Constants.iCCMTreatment.FIELD_DIARRHEA_MEDICATION_DISPENSED, treatments);
                addDiarrheaTreatments(jsonObject, Constants.iCCMTreatment.FIELD_DIARRHEA_MEDICATION_REFERRED_CLIENT, treatments);
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return treatments;
    }

    private void addDiarrheaTreatments(JSONObject jsonObject, String fieldName, Set<String> treatments) {
        Set<String> selectedOptions = getSelectedOptions(jsonObject, fieldName);
        if (selectedOptions.contains(Constants.iCCMTreatment.TREATMENT_ORS)) {
            treatments.add(Constants.iCCMTreatment.TREATMENT_ORS);
        }
        if (selectedOptions.contains(Constants.iCCMTreatment.TREATMENT_ZINC_SOURCE)
                || selectedOptions.contains(Constants.iCCMTreatment.TREATMENT_ZINC_ORS_CO_PACK_SOURCE)
                || selectedOptions.contains(Constants.iCCMTreatment.TREATMENT_ORS_ZINC_CO_PACK)) {
            treatments.add(Constants.iCCMTreatment.TREATMENT_ORS_ZINC_CO_PACK);
        }
    }

    private Set<String> getSelectedOptions(JSONObject jsonObject, String fieldName) {
        Set<String> selectedOptions = new HashSet<>();
        if (jsonObject == null || StringUtils.isBlank(fieldName)) {
            return selectedOptions;
        }

        selectedOptions.addAll(parseDelimitedValues(CoreJsonFormUtils.getValue(jsonObject, fieldName)));

        try {
            JSONArray fields = jsonObject.getJSONObject(STEP1).optJSONArray(FIELDS);
            if (fields == null) {
                return selectedOptions;
            }

            JSONObject fieldObject = JsonFormUtils.getFieldJSONObject(fields, fieldName);
            if (fieldObject == null) {
                return selectedOptions;
            }

            JSONArray valueArray = fieldObject.optJSONArray(VALUE);
            if (valueArray != null) {
                for (int i = 0; i < valueArray.length(); i++) {
                    String value = valueArray.optString(i);
                    if (StringUtils.isNotBlank(value)) {
                        selectedOptions.add(value.trim().toLowerCase());
                    }
                }
            }

            JSONArray options = fieldObject.optJSONArray(OPTIONS);
            if (options == null) {
                return selectedOptions;
            }

            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                if (!option.optBoolean(VALUE, false)) {
                    continue;
                }
                String key = option.optString(KEY);
                if (StringUtils.isNotBlank(key)) {
                    selectedOptions.add(key.trim().toLowerCase());
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return selectedOptions;
    }

    private Set<String> parseDelimitedValues(String value) {
        Set<String> parsedValues = new HashSet<>();
        if (!hasValue(value)) {
            return parsedValues;
        }

        String normalized = value
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("'", "");

        String[] splitValues = normalized.split(",");
        for (String splitValue : splitValues) {
            String cleaned = StringUtils.trimToEmpty(splitValue).toLowerCase();
            if (StringUtils.isNotBlank(cleaned)) {
                parsedValues.add(cleaned);
            }
        }
        return parsedValues;
    }
}
