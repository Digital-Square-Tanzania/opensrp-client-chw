package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.dao.AbstractDao;

import java.util.List;

public class ChwHpsDao extends AbstractDao {

    public static boolean isBloodPressureAboveThreshold(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) return false;
        String sql = "SELECT systolic, diastolic FROM ec_hps_client_services" +
                " WHERE entity_id = '" + baseEntityId + "'" +
                " AND systolic IS NOT NULL AND diastolic IS NOT NULL" +
                " ORDER BY last_interacted_with DESC LIMIT 1";
        DataMap<Boolean> dataMap = cursor -> {
            int systolic = getCursorIntValue(cursor, "systolic", 0);
            int diastolic = getCursorIntValue(cursor, "diastolic", 0);
            return systolic >= 140 || diastolic >= 80;
        };
        List<Boolean> res = readData(sql, dataMap);
        return res != null && !res.isEmpty() && Boolean.TRUE.equals(res.get(0));
    }

    public static boolean wereSelfTestingKitsDistributed(String baseEntityId) {
        String sql = "SELECT preventive_services FROM ec_hps_client_services p " +
                " WHERE p.entity_id = '" + baseEntityId + "' ORDER BY last_interacted_with DESC LIMIT 1";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "preventive_services");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0).contains("hiv_self_test_kits");
        }
        return false;
    }
}
