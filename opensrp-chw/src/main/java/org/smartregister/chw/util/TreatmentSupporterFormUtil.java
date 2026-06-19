package org.smartregister.chw.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.dao.TreatmentSupporterDao;

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

    private TreatmentSupporterFormUtil() {
        // utility class
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
            injectValues(form, caregiver.getName(), caregiver.getPhone());
        } catch (Exception e) {
            Timber.e(e, "Failed to pre-fill treatment supporter from registration");
        }
    }

    /**
     * Pre-selects the gate to "Yes" and sets the supporter name/phone as the
     * fields' initial editable values. Relationship is intentionally left blank
     * (it is not captured at registration). Returns silently if the form does not
     * contain the treatment-supporter fields.
     */
    @VisibleForTesting
    static void injectValues(@NonNull JSONObject form, @Nullable String name, @Nullable String phone) {
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
                    setProperty(field, "selection", "Yes");
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
                default:
                    break;
            }
        }

        if (!gateFound) {
            Timber.d("Referral form has no treatment supporter section; skipping pre-fill");
        }
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
