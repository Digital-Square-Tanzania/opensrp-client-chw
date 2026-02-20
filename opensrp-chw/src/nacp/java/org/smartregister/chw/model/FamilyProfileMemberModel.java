package org.smartregister.chw.model;

import org.smartregister.chw.core.model.CoreFamilyProfileMemberModel;

/**
 * NACP flavor override: Show the household head as a member even when they don't have
 * an ec_family_member row for this household. This avoids moving membership between households
 * when the same person heads multiple households.
 */
public class FamilyProfileMemberModel extends CoreFamilyProfileMemberModel {

    @Override
    public String countSelect(String tableName, String mainCondition) {
        // Count members linked via relational_id plus (optionally) 1 head row if not already present
        String familyIdSubquery = "(SELECT DISTINCT m.relational_id FROM " + tableName + " m WHERE " + mainCondition + " LIMIT 1)";
        return "SELECT COUNT(1) FROM ("
                + " SELECT m." + tableName + "_id FROM " + tableName + " m WHERE " + mainCondition
                + " UNION "
                + " SELECT m2." + tableName + "_id FROM ec_family f"
                + " JOIN " + tableName + " m2 ON m2.base_entity_id = f.family_head"
                + " WHERE f.base_entity_id = " + familyIdSubquery
                + " AND NOT EXISTS (SELECT 1 FROM " + tableName + " m3 WHERE m3.relational_id = f.base_entity_id AND m3.base_entity_id = m2.base_entity_id)"
                + ") t";
    }

    @Override
    public String mainSelect(String tableName, String mainCondition) {
        // Return union of real members + head-as-member (avoid duplicates)
        String familyIdSubquery = "(SELECT DISTINCT m.relational_id FROM " + tableName + " m WHERE " + mainCondition + " LIMIT 1)";
        return "SELECT * FROM ("
                + " SELECT m.* FROM " + tableName + " m WHERE " + mainCondition
                + " UNION "
                + " SELECT m2.* FROM ec_family f"
                + " JOIN " + tableName + " m2 ON m2.base_entity_id = f.family_head"
                + " WHERE f.base_entity_id = " + familyIdSubquery
                + " AND NOT EXISTS (SELECT 1 FROM " + tableName + " m3 WHERE m3.relational_id = f.base_entity_id AND m3.base_entity_id = m2.base_entity_id)"
                + ") members";
    }
}

