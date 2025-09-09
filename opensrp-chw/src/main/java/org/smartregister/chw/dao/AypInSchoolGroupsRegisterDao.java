package org.smartregister.chw.dao;

import org.smartregister.chw.domain.AypInSchoolGroupListItem;
import org.smartregister.dao.AbstractDao;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO to read AYP in-school groups from ec_ayp_in_school_group_details.
 */
public class AypInSchoolGroupsRegisterDao extends AbstractDao {

    public static List<AypInSchoolGroupListItem> getGroups() {
        String sql = "SELECT base_entity_id, group_name, group_type, age_band " +
                "FROM ec_ayp_in_school_group_details " +
                "ORDER BY COALESCE(last_interacted_with, 0) DESC";

        DataMap<AypInSchoolGroupListItem> dataMap = c -> new AypInSchoolGroupListItem(
                getCursorValue(c, "base_entity_id"),
                getCursorValue(c, "group_name"),
                getCursorValue(c, "group_type"),
                getCursorValue(c, "age_band")
        );

        List<AypInSchoolGroupListItem> res = readData(sql, dataMap);
        return res != null ? res : new ArrayList<>();
    }
}

