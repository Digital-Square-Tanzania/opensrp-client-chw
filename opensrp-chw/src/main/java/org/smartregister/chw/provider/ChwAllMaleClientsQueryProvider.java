package org.smartregister.chw.provider;

import androidx.annotation.NonNull;

import org.smartregister.chw.core.provider.CoreAllClientsRegisterQueryProvider;
import org.smartregister.chw.util.ChwQueryForMaleClients;

public class ChwAllMaleClientsQueryProvider  extends CoreAllClientsRegisterQueryProvider {
    @NonNull
    @Override
    public String mainSelectWhereIDsIn() {
        return ChwQueryForMaleClients.ALL_MALE_CLIENTS_SELECT_QUERY;
    }
}