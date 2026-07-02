package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.dao.AbstractDao;

import java.util.List;

public class ChwHpsDao extends AbstractDao {

    private static final double BLOOD_GLUCOSE_THRESHOLD = 7.0;

    public static boolean isBloodPressureAboveThreshold(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) return false;
        String sql = "SELECT systolic, diastolic FROM ec_hps_client_services" +
                " WHERE entity_id = '" + baseEntityId + "'" +
                " ORDER BY last_interacted_with DESC LIMIT 1";
        DataMap<Boolean> dataMap = cursor -> {
            int systolic = getCursorIntValue(cursor, "systolic", 0);
            int diastolic = getCursorIntValue(cursor, "diastolic", 0);
            return systolic >= 140 || diastolic >= 90;
        };
        List<Boolean> res = readData(sql, dataMap);
        return res != null && !res.isEmpty() && Boolean.TRUE.equals(res.get(0));
    }

    public static boolean isBloodGlucoseAboveThreshold(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) return false;
        String sql = "SELECT blood_sugar_result FROM ec_hps_client_services" +
                " WHERE entity_id = '" + baseEntityId + "'" +
                " ORDER BY last_interacted_with DESC LIMIT 1";
        DataMap<Boolean> dataMap = cursor -> {
            String bloodSugarResult = getCursorValue(cursor, "blood_sugar_result");
            if (StringUtils.isBlank(bloodSugarResult)) return false;
            try {
                return Double.parseDouble(bloodSugarResult.trim()) > BLOOD_GLUCOSE_THRESHOLD;
            } catch (NumberFormatException e) {
                return false;
            }
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
