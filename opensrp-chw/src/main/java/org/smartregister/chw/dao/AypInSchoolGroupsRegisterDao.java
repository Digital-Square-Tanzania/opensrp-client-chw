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
        return getGroupsByType(null);
    }

    public static List<AypInSchoolGroupListItem> getOutGroups() {
        return getAypOutSchoolGroupsByType(null);
    }

    public static List<AypInSchoolGroupListItem> getGroupsByType(String groupType) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT base_entity_id, group_name, group_type, age_band FROM ec_ayp_in_school_group_details ");
        if (groupType != null && !groupType.isEmpty()) {
            sb.append("WHERE group_type = '" + groupType.replace("'", "''") + "' ");
        }
        sb.append("ORDER BY COALESCE(last_interacted_with, 0) DESC");
        String sql = sb.toString();

        DataMap<AypInSchoolGroupListItem> dataMap = c -> new AypInSchoolGroupListItem(
                getCursorValue(c, "base_entity_id"),
                getCursorValue(c, "group_name"),
                getCursorValue(c, "group_type"),
                getCursorValue(c, "age_band")
        );

        List<AypInSchoolGroupListItem> res = readData(sql, dataMap);
        return res != null ? res : new ArrayList<>();
    }

    public static List<AypInSchoolGroupListItem> getAypOutSchoolGroupsByType(String groupType) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT base_entity_id, group_name, group_type, age_band FROM ec_ayp_out_school_group_details ");
        if (groupType != null && !groupType.isEmpty()) {
            sb.append("WHERE group_type = '" + groupType.replace("'", "''") + "' ");
        }
        sb.append("ORDER BY COALESCE(last_interacted_with, 0) DESC");
        String sql = sb.toString();

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
