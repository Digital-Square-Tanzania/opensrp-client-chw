package org.smartregister.chw.provider;

import androidx.annotation.NonNull;

/**
 * Dedicated query provider for the "Use an existing client" head-selection flow.
 *
 * Scoped behavior:
 * - Only affects the existing-head picker flow.
 * - Filters picker results to clients with age >= 15 years.
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
                "ORDER BY existing_head_clients.last_interacted_with DESC;";
    }
}
