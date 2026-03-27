package org.smartregister.chw.provider;

import androidx.annotation.NonNull;

/**
 * Dedicated query provider for the "Use an existing client" head-selection flow.
 *
 * Scoped behavior:
 * - Only affects the existing-head picker flow.
 * - Filters picker results to clients with age >= 15 years.
 * - Limits results to independent clients and household heads.
 */
public class ExistingAllClientsSelectionQueryProvider extends ChwAllClientsRegisterQueryProvider {

    @NonNull
    @Override
    public String mainSelectWhereIDsIn() {
        String baseQuery = super.mainSelectWhereIDsIn();
        if (baseQuery.endsWith(";")) {
            baseQuery = baseQuery.substring(0, baseQuery.length() - 1);
        }

        return "SELECT * FROM (" + baseQuery + ") existing_head_clients " +
                "WHERE existing_head_clients.dob IS NOT NULL " +
                "  AND TRIM(existing_head_clients.dob) <> '' " +
                "  AND julianday(existing_head_clients.dob) IS NOT NULL " +
                "  AND CAST((julianday('now') - julianday(existing_head_clients.dob)) / 365.25 AS INTEGER) >= 15 " +
                "  AND EXISTS (" +
                "    SELECT 1 FROM ec_family " +
                "    WHERE ec_family.base_entity_id = existing_head_clients.relationalid " +
                "      AND (" +
                "        ec_family.entity_type = 'ec_independent_client' " +
                "        OR ec_family.family_head = existing_head_clients.base_entity_id" +
                "      )" +
                "  ) " +
                "ORDER BY existing_head_clients.last_interacted_with DESC;";
    }
}
