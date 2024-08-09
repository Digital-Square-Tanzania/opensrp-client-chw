package org.smartregister.chw.provider;

import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.smartregister.chw.core.provider.CoreAllClientsRegisterQueryProvider;
import org.smartregister.chw.util.ChwQueryConstant;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

public class ChwAllClientsRegisterQueryProvider extends CoreAllClientsRegisterQueryProvider {
    @NonNull
    @Override
    public String mainSelectWhereIDsIn() {
        AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
        SharedPreferences preferences = allSharedPreferences.getPreferences();
        String teamRoleIdentifier = "";
        if (preferences != null) {
            teamRoleIdentifier = preferences.getString(TEAM_ROLE_IDENTIFIER, "");
        }
        if (teamRoleIdentifier.equals("iccm_provider")) {
            return ChwQueryConstant.ALL_ICCM_CLIENTS_SELECT_QUERY;
        }
        return ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY;
    }
}
