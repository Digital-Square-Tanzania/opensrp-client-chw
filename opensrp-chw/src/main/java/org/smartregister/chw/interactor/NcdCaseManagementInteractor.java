package org.smartregister.chw.interactor;

import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_CLINICAL_ADHERENCE;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_DANGER_SIGNS;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_LIFESTYLE;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_PSYCHOSOCIAL;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_VITALS_FORM;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.NcdClinicalAdherenceActionHelper;
import org.smartregister.chw.actionhelper.NcdDangerSignsActionHelper;
import org.smartregister.chw.actionhelper.NcdLifestyleActionHelper;
import org.smartregister.chw.actionhelper.NcdPsychosocialActionHelper;
import org.smartregister.chw.actionhelper.NcdVitalsActionHelper;
import org.smartregister.chw.dao.NcdCaseManagementDao;
import org.smartregister.chw.ncd.contract.BaseNcdVisitContract;
import org.smartregister.chw.ncd.interactor.BaseNcdVisitInteractor;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.NcdReferralTaskHelper;

import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

public class NcdCaseManagementInteractor extends BaseNcdVisitInteractor {

    private static final String STEP_ONE = "step1";
    private static final String KEY_IS_RED_ALERT = "is_red_alert";
    private static final String KEY_IS_YELLOW_ALERT = "is_yellow_alert";
    private static final String KEY_IS_SIDE_EFFECTS_ALERT = "is_side_effects_alert";
    private static final String KEY_IS_MISSED_CLINIC_ALERT = "is_missed_clinic_alert";
    private static final String KEY_ALERT_STATUS = "alert_status";

    public static final String ALERT_RED = "red";
    public static final String ALERT_YELLOW = "yellow";
    public static final String ALERT_NONE = "none";

    public NcdCaseManagementInteractor() {
        super(Constants.EncounterType.NCD_MONTHLY_FOLLOWUP);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EncounterType.NCD_MONTHLY_FOLLOWUP;
    }

    @Override
    protected String getTableName() {
        return Constants.TableName.NCD_CASE_MANAGEMENT_FOLLOWUP;
    }

    @Override
    protected void populateActionList(@NonNull BaseNcdVisitContract.InteractorCallBack callback) {
        Runnable task = () -> {
            LinkedHashMap<String, BaseNcdVisitAction> actionMap = new LinkedHashMap<>();

            try {
                boolean unresolvedRed = checkUnresolvedRedAlert();

                NcdDangerSignsActionHelper dangerSignsHelper = new NcdDangerSignsActionHelper();
                if (unresolvedRed) {
                    dangerSignsHelper.setUnresolvedRedAlert(true);
                }

                BaseNcdVisitAction vitals = buildAction(
                        context.getString(R.string.ncd_followup_action_vitals),
                        NCD_VITALS_FORM,
                        new NcdVitalsActionHelper());

                BaseNcdVisitAction clinicalAdherence = buildAction(
                        context.getString(R.string.ncd_followup_action_clinical_adherence),
                        NCD_FOLLOWUP_CLINICAL_ADHERENCE,
                        new NcdClinicalAdherenceActionHelper());

                BaseNcdVisitAction dangerSigns = buildAction(
                        context.getString(R.string.ncd_followup_action_danger_signs),
                        NCD_FOLLOWUP_DANGER_SIGNS,
                        dangerSignsHelper);

                BaseNcdVisitAction lifestyle = buildAction(
                        context.getString(R.string.ncd_followup_action_lifestyle),
                        NCD_FOLLOWUP_LIFESTYLE,
                        new NcdLifestyleActionHelper());

                BaseNcdVisitAction psychosocial = buildAction(
                        context.getString(R.string.ncd_followup_action_psychosocial),
                        NCD_FOLLOWUP_PSYCHOSOCIAL,
                        new NcdPsychosocialActionHelper());

                actionMap.put(context.getString(R.string.ncd_followup_action_vitals), vitals);
                actionMap.put(context.getString(R.string.ncd_followup_action_clinical_adherence), clinicalAdherence);
                actionMap.put(context.getString(R.string.ncd_followup_action_danger_signs), dangerSigns);
                actionMap.put(context.getString(R.string.ncd_followup_action_lifestyle), lifestyle);
                actionMap.put(context.getString(R.string.ncd_followup_action_psychosocial), psychosocial);
            } catch (BaseNcdVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callback.preloadActions(actionMap));
        };

        appExecutors.diskIO().execute(task);
    }

    /**
     * Computes alert status from action payloads and injects it before the combined event is saved.
     * With COMBINED processing mode, all fields land in one event/row.
     */
    @Override
    protected String submitVisit(boolean editMode, String memberID,
                                 Map<String, BaseNcdVisitAction> map,
                                 String parentEventType) throws Exception {
        // Compute and inject alert status before forms are combined into one event
        computeAndInjectAlertStatus(map);

        String result = super.submitVisit(editMode, memberID, map, parentEventType);

        // Create referral task if needed (after visit is saved)
        if (!ALERT_NONE.equals(lastComputedAlertStatus) && StringUtils.isBlank(parentEventType)) {
            String formSubmissionId = extractFormSubmissionId(result);
            String description = buildReferralDescription(lastComputedAlertStatus,
                    lastHasSideEffects, lastHasMissedClinic);
            NcdReferralTaskHelper.createReferralIfNeeded(
                    memberID,
                    formSubmissionId,
                    lastComputedAlertStatus,
                    description);
        }

        return result;
    }

    private String extractFormSubmissionId(String visitJson) {
        if (StringUtils.isBlank(visitJson)) return null;
        try {
            return new JSONObject(visitJson).optString("formSubmissionId", null);
        } catch (Exception e) {
            Timber.e(e, "Failed to extract formSubmissionId from visit JSON");
            return null;
        }
    }

    private String lastComputedAlertStatus = ALERT_NONE;
    private boolean lastHasSideEffects = false;
    private boolean lastHasMissedClinic = false;

    private void computeAndInjectAlertStatus(Map<String, BaseNcdVisitAction> map) {
        BaseNcdVisitAction dangerSignsAction = findActionByFormName(map, NCD_FOLLOWUP_DANGER_SIGNS);
        BaseNcdVisitAction clinicalAction = findActionByFormName(map, NCD_FOLLOWUP_CLINICAL_ADHERENCE);

        boolean isRedAlert = false;
        if (dangerSignsAction != null) {
            isRedAlert = "true".equalsIgnoreCase(
                    extractFieldValue(dangerSignsAction.getJsonPayload(), KEY_IS_RED_ALERT));
        }

        boolean isYellowAlert = false;
        lastHasSideEffects = false;
        lastHasMissedClinic = false;
        if (clinicalAction != null) {
            JSONArray clinicalFields = getFieldsArray(clinicalAction.getJsonPayload());
            if (clinicalFields != null) {
                isYellowAlert = "true".equalsIgnoreCase(findFieldValue(clinicalFields, KEY_IS_YELLOW_ALERT));
                lastHasSideEffects = "true".equalsIgnoreCase(findFieldValue(clinicalFields, KEY_IS_SIDE_EFFECTS_ALERT));
                lastHasMissedClinic = "true".equalsIgnoreCase(findFieldValue(clinicalFields, KEY_IS_MISSED_CLINIC_ALERT));
            }
        }

        if (isRedAlert) {
            lastComputedAlertStatus = ALERT_RED;
        } else if (isYellowAlert) {
            lastComputedAlertStatus = ALERT_YELLOW;
        } else {
            lastComputedAlertStatus = ALERT_NONE;
        }

        injectAlertStatusIntoPayload(dangerSignsAction, lastComputedAlertStatus,
                lastHasSideEffects, lastHasMissedClinic);
    }

    /**
     * Injects alert_status into the Danger Signs sub-event payload.
     * When RED, also copies YELLOW context flags so the facility clinician sees the full picture.
     */
    private void injectAlertStatusIntoPayload(BaseNcdVisitAction dangerSignsAction,
                                               String alertStatus,
                                               boolean hasSideEffects,
                                               boolean hasMissedClinic) {
        if (dangerSignsAction == null || StringUtils.isBlank(dangerSignsAction.getJsonPayload())) {
            return;
        }

        try {
            JSONObject form = new JSONObject(dangerSignsAction.getJsonPayload());
            JSONObject step = form.optJSONObject(STEP_ONE);
            if (step == null) return;

            JSONArray fields = step.optJSONArray("fields");
            if (fields == null) return;

            setOrAddHiddenField(fields, KEY_ALERT_STATUS, alertStatus);

            if (ALERT_RED.equals(alertStatus)) {
                setOrAddHiddenField(fields, KEY_IS_SIDE_EFFECTS_ALERT,
                        hasSideEffects ? "true" : "false");
                setOrAddHiddenField(fields, KEY_IS_MISSED_CLINIC_ALERT,
                        hasMissedClinic ? "true" : "false");
            }

            dangerSignsAction.setJsonPayload(form.toString());
        } catch (Exception e) {
            Timber.e(e, "Failed to inject alert_status into Danger Signs payload");
        }
    }

    private String buildReferralDescription(String alertStatus,
                                            boolean hasSideEffects, boolean hasMissedClinic) {
        StringBuilder desc = new StringBuilder(
                ALERT_RED.equals(alertStatus) ? "NCD Danger Signs detected" : "NCD Clinical Concern:");

        if (hasMissedClinic || hasSideEffects) {
            if (ALERT_RED.equals(alertStatus)) desc.append(" + ");
            else desc.append(" ");
            if (hasMissedClinic) desc.append("missed clinic");
            if (hasSideEffects && hasMissedClinic) desc.append(", ");
            if (hasSideEffects) desc.append("medication side effects");
        }
        return desc.toString();
    }

    /**
     * Checks if the previous visit had a RED alert with an open referral still unresolved.
     */
    private boolean checkUnresolvedRedAlert() {
        if (memberObject == null || StringUtils.isBlank(memberObject.getBaseEntityId())) {
            return false;
        }

        Map<String, String> lastEvent = NcdCaseManagementDao.getLastFollowUpEvent(
                memberObject.getBaseEntityId());
        if (lastEvent == null) {
            return false;
        }

        String previousAlertStatus = lastEvent.get(KEY_ALERT_STATUS);
        if (!ALERT_RED.equals(previousAlertStatus)) {
            return false;
        }

        return NcdCaseManagementDao.hasOpenReferral(memberObject.getBaseEntityId());
    }

    private BaseNcdVisitAction findActionByFormName(Map<String, BaseNcdVisitAction> actionMap,
                                                     String formName) {
        if (actionMap == null) return null;
        for (BaseNcdVisitAction action : actionMap.values()) {
            if (formName.equals(action.getFormName())) {
                return action;
            }
        }
        return null;
    }

    private JSONArray getFieldsArray(String jsonPayload) {
        if (StringUtils.isBlank(jsonPayload)) return null;
        try {
            JSONObject form = new JSONObject(jsonPayload);
            JSONObject step = form.optJSONObject(STEP_ONE);
            if (step == null) return null;
            return step.optJSONArray("fields");
        } catch (Exception e) {
            Timber.e(e, "Failed to parse form payload");
        }
        return null;
    }

    private String findFieldValue(JSONArray fields, String fieldKey) {
        if (fields == null) return null;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && fieldKey.equals(field.optString("key"))) {
                return field.optString("value", null);
            }
        }
        return null;
    }

    private String extractFieldValue(String jsonPayload, String fieldKey) {
        return findFieldValue(getFieldsArray(jsonPayload), fieldKey);
    }

    private void setOrAddHiddenField(JSONArray fields, String key, String value) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString("key"))) {
                field.put("value", value);
                return;
            }
        }
        JSONObject hiddenField = new JSONObject();
        hiddenField.put("key", key);
        hiddenField.put("type", "hidden");
        hiddenField.put("value", value);
        hiddenField.put("openmrs_entity", "concept");
        hiddenField.put("openmrs_entity_id", key);
        hiddenField.put("openmrs_entity_parent", "");
        fields.put(hiddenField);
    }

    private BaseNcdVisitAction buildAction(String title, String formName,
                                           BaseNcdVisitAction.NcdVisitActionHelper helper)
            throws BaseNcdVisitAction.ValidationException {

        BaseNcdVisitAction.Builder builder = getBuilder(title)
                .withOptional(false)
                .withScheduleStatus(BaseNcdVisitAction.ScheduleStatus.DUE)
                .withFormName(formName)
                .withDetails(details)
                .withHelper(helper)
                .withProcessingMode(BaseNcdVisitAction.ProcessingMode.COMBINED);

        if (memberObject != null && StringUtils.isNotBlank(memberObject.getBaseEntityId())) {
            builder = builder.withBaseEntityID(memberObject.getBaseEntityId());
        }

        return builder.build();
    }
}
