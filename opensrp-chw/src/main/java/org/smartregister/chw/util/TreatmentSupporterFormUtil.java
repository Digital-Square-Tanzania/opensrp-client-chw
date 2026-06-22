package org.smartregister.chw.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.nerdstone.neatformcore.domain.model.NFormViewData;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.dao.TreatmentSupporterDao;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Pre-populates the Treatment Supporter / Caregiver fields on a NeatForm referral
 * form from the data captured at client registration.
 *
 * <p>The fields ({@code has_treatment_supporter}, {@code treatment_supporter_name},
 * {@code treatment_supporter_phone}) only exist on the NACP referral forms, so this
 * utility is a no-op for any form that does not contain them. It is also fully
 * guarded so that a missing DB column on another flavor cannot break the referral
 * flow.
 */
public class TreatmentSupporterFormUtil {

    @VisibleForTesting
    static final String FIELD_GATE = "has_treatment_supporter";
    @VisibleForTesting
    static final String FIELD_NAME = "treatment_supporter_name";
    @VisibleForTesting
    static final String FIELD_PHONE = "treatment_supporter_phone";
    @VisibleForTesting
    static final String FIELD_RELATIONSHIP = "treatment_supporter_relationship";

    private static final String TYPE_SPINNER = "spinner";
    private static final String TYPE_EDIT_TEXT = "text_input_edit_text";

    private TreatmentSupporterFormUtil() {
        // utility class
    }

    /**
     * Save-time safety net: guarantees the registered caregiver values end up in
     * the referral event obs even if NeatForm did not capture the values that were
     * pre-filled at form-launch (see {@link #prefillFromRegistration}).
     *
     * <p>Only injects entries that NeatForm did <em>not</em> already produce, so
     * any value the CHW typed, cleared, or a deliberate "No" on the gate is
     * respected. No-op when no caregiver is on record. Never throws.
     *
     * <p>Call before the form data is converted to obs (e.g. from the issue
     * referral presenter's {@code saveForm}).
     */
    public static void ensureTreatmentSupporterObs(@Nullable String baseEntityId,
                                                   @Nullable Map<String, NFormViewData> formData) {
        if (formData == null) {
            return;
        }
        try {
            TreatmentSupporterDao.Caregiver caregiver =
                    TreatmentSupporterDao.getRegisteredCaregiver(baseEntityId);
            if (caregiver == null || !caregiver.isPresent()) {
                return;
            }

            boolean gateYes;
            if (!formData.containsKey(FIELD_GATE)) {
                formData.put(FIELD_GATE, viewData(TYPE_SPINNER, FIELD_GATE, "Yes"));
                gateYes = true;
            } else {
                gateYes = "Yes".equalsIgnoreCase(stringValue(formData.get(FIELD_GATE)));
            }

            if (!gateYes) {
                // CHW explicitly answered "No" — do not persist supporter details.
                return;
            }
            if (isNotBlank(caregiver.getName()) && !formData.containsKey(FIELD_NAME)) {
                formData.put(FIELD_NAME, viewData(TYPE_EDIT_TEXT, FIELD_NAME, caregiver.getName().trim()));
            }
            if (isNotBlank(caregiver.getPhone()) && !formData.containsKey(FIELD_PHONE)) {
                formData.put(FIELD_PHONE, viewData(TYPE_EDIT_TEXT, FIELD_PHONE, caregiver.getPhone().trim()));
            }
            if (isNotBlank(caregiver.getRelationship()) && !formData.containsKey(FIELD_RELATIONSHIP)) {
                formData.put(FIELD_RELATIONSHIP,
                        viewData(TYPE_SPINNER, FIELD_RELATIONSHIP, caregiver.getRelationship().trim()));
            }
        } catch (Exception e) {
            Timber.e(e, "Failed to ensure treatment supporter obs");
        }
    }

    /**
     * Adds the treatment supporter values submitted on a native (JSON-Wizard)
     * screening form into the referral-event {@code formData} map, so they are
     * emitted as obs on the generated "Referral Registration" event (alongside
     * {@code chw_referral_hf}, {@code is_emergency_case}, ...). Details are only
     * persisted when the gate is "Yes". Mirrors the obs shape of the other referral
     * fields. Never throws.
     */
    public static void addScreeningReferralObs(@Nullable JSONObject submittedForm,
                                               @Nullable Map<String, NFormViewData> formData) {
        if (submittedForm == null || formData == null) {
            return;
        }
        try {
            String gate = JsonFormUtils.getValue(submittedForm, FIELD_GATE);
            if (!"Yes".equalsIgnoreCase(gate)) {
                return;
            }
            formData.put(FIELD_GATE, viewData(TYPE_SPINNER, FIELD_GATE, "Yes"));
            putScreeningObs(formData, submittedForm, FIELD_NAME, TYPE_EDIT_TEXT);
            putScreeningObs(formData, submittedForm, FIELD_PHONE, TYPE_EDIT_TEXT);
            putScreeningObs(formData, submittedForm, FIELD_RELATIONSHIP, TYPE_SPINNER);
        } catch (Exception e) {
            Timber.e(e, "Failed to add screening referral treatment supporter obs");
        }
    }

    private static void putScreeningObs(@NonNull Map<String, NFormViewData> formData,
                                        @NonNull JSONObject form, @NonNull String key,
                                        @NonNull String type) {
        String value = JsonFormUtils.getValue(form, key);
        if (isNotBlank(value)) {
            formData.put(key, viewData(type, key, value.trim()));
        }
    }

    /**
     * Builds an {@link NFormViewData} carrying the metadata the referral library's
     * {@code getObs} reads (openmrs_entity / _id / _parent) so the value is emitted
     * as a proper obs on the Referral Registration event.
     */
    @VisibleForTesting
    static NFormViewData viewData(String type, String conceptId, String value) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("openmrs_entity", "concept");
        metadata.put("openmrs_entity_id", conceptId);
        metadata.put("openmrs_entity_parent", "");
        return new NFormViewData(type, value, metadata, true);
    }

    private static String stringValue(@Nullable NFormViewData data) {
        if (data == null || data.getValue() == null) {
            return null;
        }
        return String.valueOf(data.getValue());
    }

    /**
     * JSON-Wizard variant (NCD screening form): pre-fills the treatment supporter
     * fields from the registered caregiver by setting each field's {@code value}
     * (spinner values are the option keys, e.g. "Yes" / "Mother"). Scans every step
     * so it is independent of the referral step's index. No-op when no caregiver is
     * on record or the fields are absent; never throws.
     */
    public static void prefillNcdScreeningForm(@Nullable String baseEntityId, @Nullable JSONObject form) {
        if (form == null) {
            return;
        }
        try {
            TreatmentSupporterDao.Caregiver caregiver =
                    TreatmentSupporterDao.getRegisteredCaregiver(baseEntityId);
            if (caregiver == null || !caregiver.isPresent()) {
                return;
            }
            java.util.Iterator<String> stepKeys = form.keys();
            while (stepKeys.hasNext()) {
                JSONObject step = form.optJSONObject(stepKeys.next());
                if (step == null) {
                    continue;
                }
                JSONArray fields = step.optJSONArray("fields");
                if (fields == null) {
                    continue;
                }
                applyWizardValues(fields, caregiver);
            }
        } catch (Exception e) {
            Timber.e(e, "Failed to pre-fill NCD screening treatment supporter");
        }
    }

    private static void applyWizardValues(@NonNull JSONArray fields,
                                          @NonNull TreatmentSupporterDao.Caregiver caregiver) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) {
                continue;
            }
            switch (field.optString("key")) {
                case FIELD_GATE:
                    setWizardValue(field, "Yes");
                    break;
                case FIELD_NAME:
                    if (isNotBlank(caregiver.getName())) {
                        setWizardValue(field, caregiver.getName().trim());
                    }
                    break;
                case FIELD_PHONE:
                    if (isNotBlank(caregiver.getPhone())) {
                        setWizardValue(field, caregiver.getPhone().trim());
                    }
                    break;
                case FIELD_RELATIONSHIP:
                    if (isNotBlank(caregiver.getRelationship())) {
                        setWizardValue(field, caregiver.getRelationship().trim());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Native JSON-Wizard fields carry their pre-filled value directly as the
     * {@code value} attribute on the field object (spinner values are the option
     * keys), unlike NeatForm which nests it under {@code properties}.
     */
    private static void setWizardValue(@NonNull JSONObject field, @NonNull String value) {
        try {
            field.put("value", value);
        } catch (Exception e) {
            Timber.e(e, "Failed to set treatment supporter value on %s", field.optString("key"));
        }
    }

    /**
     * Looks up the registered caregiver for the member and injects the values into
     * the form. Safe to call for every referral launch; never throws.
     */
    public static void prefillFromRegistration(@Nullable String baseEntityId, @Nullable JSONObject form) {
        if (form == null) {
            return;
        }
        try {
            TreatmentSupporterDao.Caregiver caregiver =
                    TreatmentSupporterDao.getRegisteredCaregiver(baseEntityId);
            if (caregiver == null || !caregiver.isPresent()) {
                return;
            }
            injectValues(form, caregiver.getName(), caregiver.getPhone(), caregiver.getRelationship());
        } catch (Exception e) {
            Timber.e(e, "Failed to pre-fill treatment supporter from registration");
        }
    }

    /**
     * Pre-selects the gate to "Yes" and sets the supporter name/phone/relationship
     * as the fields' initial editable values. Returns silently if the form does not
     * contain the treatment-supporter fields.
     */
    @VisibleForTesting
    static void injectValues(@NonNull JSONObject form, @Nullable String name, @Nullable String phone,
                             @Nullable String relationship) {
        JSONArray fields = getFirstStepFields(form);
        if (fields == null) {
            return;
        }

        boolean gateFound = false;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) {
                continue;
            }
            String fieldName = field.optString("name");
            switch (fieldName) {
                case FIELD_GATE:
                    // Spinner default selection is a 0-based option index, not the value.
                    selectSpinnerOption(field, "Yes");
                    gateFound = true;
                    break;
                case FIELD_NAME:
                    if (isNotBlank(name)) {
                        setProperty(field, "text", name.trim());
                    }
                    break;
                case FIELD_PHONE:
                    if (isNotBlank(phone)) {
                        setProperty(field, "text", phone.trim());
                    }
                    break;
                case FIELD_RELATIONSHIP:
                    // relationship is a spinner — pre-select by option index, still editable
                    if (isNotBlank(relationship)) {
                        selectSpinnerOption(field, relationship.trim());
                    }
                    break;
                default:
                    break;
            }
        }

        if (!gateFound) {
            Timber.d("Referral form has no treatment supporter section; skipping pre-fill");
        }
    }

    /**
     * Pre-selects a NeatForm spinner option. NeatForm reads {@code properties.selection}
     * as a 0-based index into the field's {@code options} array (it is parsed with
     * {@code Integer.parseInt} and applied via {@code setSelection(int)}), so a value
     * string never matches — we must resolve the option's index by its {@code name}.
     * No-op when the option is not present, leaving the spinner unselected.
     */
    private static void selectSpinnerOption(@NonNull JSONObject field, @NonNull String optionName) {
        int index = optionIndex(field, optionName);
        if (index >= 0) {
            setProperty(field, "selection", String.valueOf(index));
        } else {
            Timber.d("Spinner option '%s' not found on %s; leaving unselected",
                    optionName, field.optString("name"));
        }
    }

    private static int optionIndex(@NonNull JSONObject field, @NonNull String optionName) {
        JSONArray options = field.optJSONArray("options");
        if (options == null) {
            return -1;
        }
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null && optionName.equalsIgnoreCase(option.optString("name"))) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private static JSONArray getFirstStepFields(@NonNull JSONObject form) {
        JSONArray steps = form.optJSONArray("steps");
        if (steps == null || steps.length() == 0) {
            return null;
        }
        JSONObject firstStep = steps.optJSONObject(0);
        if (firstStep == null) {
            return null;
        }
        return firstStep.optJSONArray("fields");
    }

    private static void setProperty(@NonNull JSONObject field, @NonNull String key, @NonNull String value) {
        try {
            JSONObject properties = field.optJSONObject("properties");
            if (properties == null) {
                properties = new JSONObject();
                field.put("properties", properties);
            }
            properties.put(key, value);
        } catch (Exception e) {
            Timber.e(e, "Failed to set %s on treatment supporter field", key);
        }
    }

    private static boolean isNotBlank(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }
}
