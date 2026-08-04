package org.smartregister.chw.dao;

import android.util.Log;

import org.smartregister.chw.ayp.dao.AypDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class AypOutSchoolDao extends AypDao {

    public static boolean wereSelfTestingKitsDistributed(String baseEntityId) {
        String sql = "SELECT self_testing_service_provided FROM ec_ayp_out_school_client_followup_visits p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' ORDER BY last_interacted_with DESC LIMIT 1";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "self_testing_service_provided");

        List<String> res = readData(sql, dataMap);

        if (res != null && !res.isEmpty() && res.get(0) != null) {
            Log.d("hello hivst", res.toString());
            return res.get(0).equalsIgnoreCase("yes");
        }
        return false;
    }

    public static boolean isClientHivPositive(String baseEntityId) {
        String sql = "SELECT base_entity_id FROM ec_ayp_out_school_client_followup_visits " +
                "WHERE base_entity_id = '" + baseEntityId + "' " +
                "AND (LOWER(COALESCE(hiv_positive, '')) = 'true' " +
                "OR LOWER(COALESCE(client_hiv_status, '')) = 'positive' " +
                "OR LOWER(COALESCE(hiv_result_recent, '')) = 'positive' " +
                "OR LOWER(COALESCE(hiv_result, '')) = 'positive') LIMIT 1";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "base_entity_id");
        List<String> results = readData(sql, dataMap);
        return results != null && !results.isEmpty();
    }
}
