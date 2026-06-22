package org.smartregister.chw.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.smartregister.dao.AbstractDao;

import java.util.List;

/**
 * Reads the Treatment Supporter / primary caregiver so it can pre-populate
 * referral forms. Two sources are consulted, in order:
 *
 * <ol>
 *     <li>The caregiver the client captured on their <em>own</em> registration
 *     ({@code has_primary_caregiver == "Yes"} plus {@code primary_caregiver_name},
 *     {@code other_phone_number}, {@code caregiver_relationship} on
 *     {@code ec_family_member}).</li>
 *     <li>Failing that, the <em>household</em> caregiver — {@code ec_family.primary_caregiver}
 *     (which defaults to the family head) — using that member's name and phone.
 *     The relationship is left blank for the CHW to choose, and the client is never
 *     treated as their own caregiver.</li>
 * </ol>
 *
 * <p>The columns are only guaranteed on the NACP flavor. Callers must treat a
 * {@code null} result (e.g. missing columns on another flavor) as "no caregiver".
 */
public class TreatmentSupporterDao extends AbstractDao {

    /**
     * @return the resolved caregiver for the member, or {@code null} when the
     * lookup is not available on this flavor. Use {@link Caregiver#isPresent()}
     * to tell whether a caregiver was actually found.
     */
    public static Caregiver getRegisteredCaregiver(String baseEntityId) {
        if (baseEntityId == null || baseEntityId.isEmpty()) {
            return null;
        }

        String sql = "SELECT m.has_primary_caregiver, m.primary_caregiver_name, m.other_phone_number," +
                " m.caregiver_relationship," +
                " f.primary_caregiver AS family_caregiver_id," +
                " pcg.first_name AS pcg_first_name, pcg.middle_name AS pcg_middle_name," +
                " pcg.last_name AS pcg_last_name, pcg.phone_number AS pcg_phone" +
                " FROM ec_family_member m" +
                " LEFT JOIN ec_family f ON m.relational_id = f.base_entity_id" +
                " LEFT JOIN ec_family_member pcg ON f.primary_caregiver = pcg.base_entity_id" +
                " WHERE m.base_entity_id = '" + baseEntityId + "' LIMIT 1";

        DataMap<Caregiver> dataMap = cursor -> resolve(
                baseEntityId,
                getCursorValue(cursor, "has_primary_caregiver"),
                getCursorValue(cursor, "primary_caregiver_name"),
                getCursorValue(cursor, "other_phone_number"),
                getCursorValue(cursor, "caregiver_relationship"),
                getCursorValue(cursor, "family_caregiver_id"),
                fullName(getCursorValue(cursor, "pcg_first_name"),
                        getCursorValue(cursor, "pcg_middle_name"),
                        getCursorValue(cursor, "pcg_last_name")),
                getCursorValue(cursor, "pcg_phone")
        );

        List<Caregiver> result = readData(sql, dataMap);
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Resolves the caregiver source from a member's row. Pure (no DB) so the
     * precedence rules can be unit-tested.
     *
     * @param baseEntityId      the client being referred
     * @param hasCaregiver      the client's own {@code has_primary_caregiver} answer
     * @param ownName           the client's own captured caregiver name
     * @param ownPhone          the client's own captured caregiver phone
     * @param ownRelationship   the client's own captured caregiver relationship
     * @param familyCaregiverId {@code ec_family.primary_caregiver} (a member id)
     * @param householdName     full name of the household caregiver member
     * @param householdPhone    phone of the household caregiver member
     */
    @VisibleForTesting
    public static Caregiver resolve(@Nullable String baseEntityId, @Nullable String hasCaregiver,
                             @Nullable String ownName, @Nullable String ownPhone,
                             @Nullable String ownRelationship, @Nullable String familyCaregiverId,
                             @Nullable String householdName, @Nullable String householdPhone) {
        // 1. The client's own captured treatment supporter takes precedence.
        if ("Yes".equalsIgnoreCase(trim(hasCaregiver))) {
            return new Caregiver(ownName, ownPhone, ownRelationship, true);
        }
        // 2. Fall back to the household caregiver, but never the client themselves,
        //    and only when it actually carries usable details.
        if (isNotBlank(familyCaregiverId)
                && !trim(familyCaregiverId).equals(trim(baseEntityId))
                && (isNotBlank(householdName) || isNotBlank(householdPhone))) {
            return new Caregiver(householdName, householdPhone, null, true);
        }
        return new Caregiver(null, null, null, false);
    }

    private static String fullName(String first, String middle, String last) {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{first, middle, last}) {
            if (isNotBlank(part)) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(part.trim());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Immutable holder for the resolved caregiver details.
     */
    public static class Caregiver {
        private final String name;
        private final String phone;
        private final String relationship;
        private final boolean present;

        public Caregiver(String name, String phone, String relationship, boolean present) {
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
            this.present = present;
        }

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }

        public String getRelationship() {
            return relationship;
        }

        /**
         * @return whether a caregiver was found for the client (from their own
         * registration or the household). When {@code false} the referral form is
         * left untouched.
         */
        public boolean isPresent() {
            return present;
        }
    }
}
