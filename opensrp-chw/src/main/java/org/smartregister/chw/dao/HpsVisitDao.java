package org.smartregister.chw.dao;

import org.smartregister.dao.AbstractDao;
import org.smartregister.chw.hps.util.Constants;

import java.util.List;

/**
 * DAO utilities for HPS visit/session lookups.
 */
public class HpsVisitDao extends AbstractDao {

    /**
     * Returns the base_entity_id (session UUID) of the latest saved HPS Annual Census visit
     * whose visit_details contains SESSION_META with original_base_entity_id = originalId.
     * If none found, returns null.
     */
    public static String getLatestSessionBaseEntityIdByOriginalId(String originalId) {
        if (originalId == null || originalId.trim().isEmpty()) return null;

        // Match JSON stored in visit_details.details containing original_base_entity_id: "<originalId>"
        // Using LIKE due to lack of JSON functions in SQLite.
        String likeClause = "\"original_base_entity_id\":\"" + originalId + "\"";

        String sql = "SELECT v.base_entity_id FROM visits v " +
                "INNER JOIN visit_details vd ON vd.visit_id = v.visit_id " +
                "WHERE v.visit_type = '" + Constants.EVENT_TYPE.HPS_ANNUAL_CENSUS + "' " +
                "AND vd.visit_key = 'SESSION_META' " +
                "AND vd.details LIKE '%" + likeClause + "%' " +
                "ORDER BY v.visit_date DESC LIMIT 1";

        DataMap<String> dataMap = c -> getCursorValue(c, "base_entity_id");
        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }
        return null;
    }
}

