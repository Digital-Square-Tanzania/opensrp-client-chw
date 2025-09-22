package org.smartregister.chw.dao;

import org.smartregister.dao.AbstractDao;

import java.util.List;

public class ChwHpsDao extends AbstractDao {

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
