package org.smartregister.chw.dao;

import org.smartregister.chw.kvp.dao.KvpDao;

import java.util.List;

public class ChwKvpDao extends KvpDao {
    public static String getDominantKVPGroup(String baseEntityId) {
        String sql = "SELECT client_group FROM ec_kvp_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_group");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static boolean hasFollowupVisits(String baseEntityId) {
        String sql = "SELECT visit_type FROM ec_kvp_prep_followup p " +
                " WHERE p.entity_id = '" + baseEntityId + "'";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "visit_type");

        List<String> res = readData(sql, dataMap);
        if (res != null) {
            return res.size() > 0;
        }
        return false;
    }

    public static boolean wereSelfTestingKitsDistributed(String baseEntityId) {
        String sql = "SELECT kits_distributed FROM ec_kvp_prep_followup p " +
                " WHERE p.entity_id = '" + baseEntityId + "'  ORDER BY last_interacted_with DESC LIMIT 1 ";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "kits_distributed");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() > 0 && res.get(0) != null) {
            return res.get(0).equalsIgnoreCase("yes");
        }
        return false;
    }
}
