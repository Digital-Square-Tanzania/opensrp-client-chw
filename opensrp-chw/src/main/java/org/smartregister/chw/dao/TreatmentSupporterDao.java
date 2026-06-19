package org.smartregister.chw.dao;

import org.smartregister.dao.AbstractDao;

import java.util.List;

/**
 * Reads the Treatment Supporter / primary caregiver captured at client
 * registration so it can pre-populate referral forms.
 *
 * <p>The columns ({@code has_primary_caregiver}, {@code primary_caregiver_name},
 * {@code other_phone_number}) live on {@code ec_family_member} and are only
 * guaranteed to exist on the NACP flavor. Callers must treat a {@code null}
 * result (e.g. missing columns on another flavor) as "no caregiver on record".
 */
public class TreatmentSupporterDao extends AbstractDao {

    /**
     * @return the registered caregiver for the member, or {@code null} when none
     * is on record or the lookup is not available on this flavor.
     */
    public static Caregiver getRegisteredCaregiver(String baseEntityId) {
        if (baseEntityId == null || baseEntityId.isEmpty()) {
            return null;
        }

        String sql = "SELECT has_primary_caregiver, primary_caregiver_name, other_phone_number" +
                " FROM ec_family_member WHERE base_entity_id = '" + baseEntityId + "' LIMIT 1";

        DataMap<Caregiver> dataMap = cursor -> new Caregiver(
                getCursorValue(cursor, "has_primary_caregiver"),
                getCursorValue(cursor, "primary_caregiver_name"),
                getCursorValue(cursor, "other_phone_number")
        );

        List<Caregiver> result = readData(sql, dataMap);
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Immutable holder for the registered caregiver fields.
     */
    public static class Caregiver {
        private final String hasCaregiver;
        private final String name;
        private final String phone;

        public Caregiver(String hasCaregiver, String name, String phone) {
            this.hasCaregiver = hasCaregiver;
            this.name = name;
            this.phone = phone;
        }

        public String getHasCaregiver() {
            return hasCaregiver;
        }

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }

        /**
         * A caregiver is considered present when registration explicitly recorded
         * "Yes", or when a name/phone was captured.
         */
        public boolean isPresent() {
            return "Yes".equalsIgnoreCase(trim(hasCaregiver))
                    || !isBlank(name)
                    || !isBlank(phone);
        }

        private static String trim(String value) {
            return value == null ? null : value.trim();
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
