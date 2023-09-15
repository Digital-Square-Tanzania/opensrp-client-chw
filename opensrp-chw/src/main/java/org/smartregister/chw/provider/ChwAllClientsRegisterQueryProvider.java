package org.smartregister.chw.provider;

import androidx.annotation.NonNull;

import org.smartregister.chw.core.provider.CoreAllClientsRegisterQueryProvider;
import org.smartregister.chw.util.ChwQueryConstant;

public class ChwAllClientsRegisterQueryProvider extends CoreAllClientsRegisterQueryProvider {
    @NonNull
    @Override
    public String mainSelectWhereIDsIn() {
        return ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY;
    }
}
