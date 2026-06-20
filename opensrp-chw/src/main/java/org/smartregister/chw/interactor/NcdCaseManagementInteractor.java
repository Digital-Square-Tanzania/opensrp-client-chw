package org.smartregister.chw.interactor;

import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_CLINICAL_ADHERENCE;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_DANGER_SIGNS;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_LIFESTYLE;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_PSYCHOSOCIAL;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_STATUS;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_VITALS_FORM;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.NcdClinicalAdherenceActionHelper;
import org.smartregister.chw.actionhelper.NcdDangerSignsActionHelper;
import org.smartregister.chw.actionhelper.NcdFollowUpStatusActionHelper;
import org.smartregister.chw.actionhelper.NcdLifestyleActionHelper;
import org.smartregister.chw.actionhelper.NcdPsychosocialActionHelper;
import org.smartregister.chw.actionhelper.NcdVitalsActionHelper;
import org.smartregister.chw.dao.NcdCaseManagementDao;
import org.smartregister.chw.ncd.contract.BaseNcdVisitContract;
import org.smartregister.chw.ncd.interactor.BaseNcdVisitInteractor;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;
import org.smartregister.chw.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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

                BaseNcdVisitAction followUpStatus = buildAction(
                        context.getString(R.string.ncd_followup_action_status),
                        NCD_FOLLOWUP_STATUS,
                        new NcdFollowUpStatusActionHelper());

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

                actionMap.put(context.getString(R.string.ncd_followup_action_status), followUpStatus);
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
     * Phase 2: referral creation is no longer invoked inline. Instead, when the visit raises a
     * non-NONE alert, a {@link PendingNcdReferral} is staged on this interactor and the
     * VisitActivity reads it after submission to prompt the CHW for confirmation. Only the
     * Confirm path calls {@link org.smartregister.chw.util.NcdReferralTaskHelper#createReferralIfNeeded}.
     */
    @Override
    protected String submitVisit(boolean editMode, String memberID,
                                 Map<String, BaseNcdVisitAction> map,
                                 String parentEventType) throws Exception {
        BaseNcdVisitAction followUpStatusAction = findActionByFormName(map, NCD_FOLLOWUP_STATUS);
        if (followUpStatusAction != null) {
            followUpStatusAction.setJsonPayload(
                    NcdFollowUpStatusActionHelper.clearIrrelevantValues(
                            followUpStatusAction.getJsonPayload()));
        }
        lastSubmittedFollowUpStatus = followUpStatusAction == null ? null
                : NcdFollowUpStatusActionHelper.extractValue(
                        followUpStatusAction.getJsonPayload(), NcdFollowUpStatusActionHelper.KEY_STATUS);
        lastSubmittedInactiveReason = followUpStatusAction == null ? null
                : NcdFollowUpStatusActionHelper.extractValue(
                        followUpStatusAction.getJsonPayload(), NcdFollowUpStatusActionHelper.KEY_REASON);
        lastSubmittedDateOfDeath = followUpStatusAction == null ? null
                : NcdFollowUpStatusActionHelper.extractValue(
                        followUpStatusAction.getJsonPayload(), NcdFollowUpStatusActionHelper.KEY_DATE_OF_DEATH);

        // Compute alert status and stage pending referral before forms are combined into one event
        computeAndInjectAlertStatus(map);

        if (!ALERT_NONE.equals(lastComputedAlertStatus) && StringUtils.isBlank(parentEventType)) {
            String description = buildReferralDescription(lastComputedAlertStatus,
                    lastHasSideEffects, lastHasMissedClinic);
            pendingReferral = new PendingNcdReferral(
                    memberID,
                    lastComputedAlertStatus,
                    description,
                    Collections.unmodifiableList(new ArrayList<>(lastReferralReasons)));
        } else {
            pendingReferral = null;
        }

        return super.submitVisit(editMode, memberID, map, parentEventType);
    }

    public PendingNcdReferral getPendingReferral() {
        return pendingReferral;
    }

    public void clearPendingReferral() {
        pendingReferral = null;
    }

    private PendingNcdReferral pendingReferral = null;

    private String lastSubmittedFollowUpStatus;
    private String lastSubmittedInactiveReason;
    private String lastSubmittedDateOfDeath;

    private String lastComputedAlertStatus = ALERT_NONE;
    private boolean lastHasSideEffects = false;
    private boolean lastHasMissedClinic = false;
    private String lastVitalsAlertReason = null;
    private final List<String> lastReferralReasons = new ArrayList<>();

    public String getLastSubmittedFollowUpStatus() {
        return lastSubmittedFollowUpStatus;
    }

    public String getLastSubmittedInactiveReason() {
        return lastSubmittedInactiveReason;
    }

    public String getLastSubmittedDateOfDeath() {
        return lastSubmittedDateOfDeath;
    }

    private void computeAndInjectAlertStatus(Map<String, BaseNcdVisitAction> map) {
        BaseNcdVisitAction dangerSignsAction = findActionByFormName(map, NCD_FOLLOWUP_DANGER_SIGNS);
        BaseNcdVisitAction clinicalAction    = findActionByFormName(map, NCD_FOLLOWUP_CLINICAL_ADHERENCE);
        BaseNcdVisitAction vitalsAction      = findActionByFormName(map, NCD_VITALS_FORM);

        lastReferralReasons.clear();

        boolean isRedAlert = false;
        if (dangerSignsAction != null) {
            String payload = dangerSignsAction.getJsonPayload();
            isRedAlert = "true".equalsIgnoreCase(extractFieldValue(payload, KEY_IS_RED_ALERT));
            collectDangerSignReasons(payload);
        }

        // Vitals threshold breach escalates to RED regardless of danger signs result
        lastVitalsAlertReason = null;
        if (vitalsAction != null) {
            String vitalsAlert = extractFieldValue(vitalsAction.getJsonPayload(), "is_vitals_alert");
            if ("true".equalsIgnoreCase(vitalsAlert)) {
                isRedAlert = true;
                lastVitalsAlertReason = extractFieldValue(vitalsAction.getJsonPayload(), "vitals_alert_reason");
                addVitalsReason(lastVitalsAlertReason);
            }
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
                if (lastHasSideEffects) {
                    addReasonString(R.string.ncd_referral_reason_side_effects);
                }
                if (lastHasMissedClinic) {
                    addReasonString(R.string.ncd_referral_reason_missed_clinic);
                }
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

    private void collectDangerSignReasons(String payload) {
        if (StringUtils.isBlank(payload)) return;
        if ("yes".equalsIgnoreCase(extractFieldValue(payload, "non_healing_wounds"))) {
            addReasonString(R.string.ncd_referral_reason_non_healing_wounds);
        }
        if ("yes".equalsIgnoreCase(extractFieldValue(payload, "neuropathy"))) {
            addReasonString(R.string.ncd_referral_reason_neuropathy);
        }
        if ("yes".equalsIgnoreCase(extractFieldValue(payload, "vision_changes"))) {
            addReasonString(R.string.ncd_referral_reason_vision_changes);
        }
        if ("yes".equalsIgnoreCase(extractFieldValue(payload, "chest_pain"))) {
            addReasonString(R.string.ncd_referral_reason_chest_pain);
        }
    }

    private void addVitalsReason(String reasonCode) {
        if (StringUtils.isBlank(reasonCode)) return;
        switch (reasonCode) {
            case "high_bp":
                addReasonString(R.string.ncd_referral_reason_high_bp);
                break;
            case "high_glucose":
                addReasonString(R.string.ncd_referral_reason_high_glucose);
                break;
            case "high_bp_and_glucose":
                addReasonString(R.string.ncd_referral_reason_high_bp_and_glucose);
                break;
            default:
                break;
        }
    }

    private void addReasonString(int stringResId) {
        if (context == null) return;
        String text = context.getString(stringResId);
        if (StringUtils.isNotBlank(text) && !lastReferralReasons.contains(text)) {
            lastReferralReasons.add(text);
        }
    }

    /**
     * DTO staged by the interactor when a visit produces an alert-worthy condition.
     * The VisitActivity reads this after submission to prompt the CHW for confirmation
     * before any referral event/task is written.
     */
    public static class PendingNcdReferral {
        public final String baseEntityId;
        public final String alertLevel; // ALERT_RED or ALERT_YELLOW
        public final String description;
        public final List<String> reasons;

        public PendingNcdReferral(String baseEntityId, String alertLevel,
                                  String description, List<String> reasons) {
            this.baseEntityId = baseEntityId;
            this.alertLevel = alertLevel;
            this.description = description;
            this.reasons = reasons;
        }
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

        if (StringUtils.isNotBlank(lastVitalsAlertReason)) {
            switch (lastVitalsAlertReason) {
                case "high_bp":             desc.append(" Elevated blood pressure."); break;
                case "high_glucose":        desc.append(" Elevated blood glucose.");  break;
                case "high_bp_and_glucose": desc.append(" Elevated BP and glucose."); break;
                default:                    break;
            }
        }

        return desc.toString().trim();
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
