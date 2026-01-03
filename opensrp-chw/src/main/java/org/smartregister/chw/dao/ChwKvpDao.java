package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.util.Constants;

import java.util.List;

public class ChwKvpDao extends KvpDao {
    public static String getDominantKVPGroup(String baseEntityId) {
        String sql = "SELECT client_group FROM ec_kvp_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_group");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static String getDominantKVPGroupFromFacility(String baseEntityId) {
        String sql = "SELECT client_group FROM ec_facility_kvp_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_group");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
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

    public static boolean isLatestFollowupHivPositive(String baseEntityId) {
        String latestStatus = getLatestFollowupDetail(baseEntityId, "client_hiv_status");
        if (StringUtils.isBlank(latestStatus)) {
            return false;
        }

        String normalizedStatus = latestStatus
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .toLowerCase()
                .trim();

        return normalizedStatus.contains("positive") || normalizedStatus.contains("chanya") || normalizedStatus.contains("ana maambukizi");
    }

    public static boolean hasCtcNumber(String baseEntityId) {
        String ctcNumber = getLatestFollowupDetail(baseEntityId, "ctc_number");

        if (StringUtils.isBlank(ctcNumber)) {
            return false;
        }

        String normalizedCtc = ctcNumber
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        return StringUtils.isNotBlank(normalizedCtc);
    }

    private static String getLatestFollowupDetail(String baseEntityId, String detailKey) {
        String sql = "SELECT vd.details FROM visits v " +
                "INNER JOIN visit_details vd ON vd.visit_id = v.visit_id " +
                "WHERE v.visit_type = '" + Constants.Events.KVP_PREP_FOLLOWUP_VISIT + "' " +
                "AND v.base_entity_id = '" + baseEntityId + "' " +
                "AND vd.visit_key = '" + detailKey + "' " +
                "ORDER BY v.visit_date DESC LIMIT 1";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "details");
        List<String> res = readData(sql, dataMap);

        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }

        return null;
    }
}
