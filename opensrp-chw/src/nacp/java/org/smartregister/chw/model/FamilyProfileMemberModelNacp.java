package org.smartregister.chw.model;

import org.apache.commons.lang3.ArrayUtils;
import org.smartregister.chw.core.model.CoreFamilyProfileMemberModel;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.util.DBConstants;

/**
 * NACP flavor: include household head as a member without moving membership.
 */
public class FamilyProfileMemberModelNacp extends CoreFamilyProfileMemberModel {

    @Override
    public String countSelect(String tableName, String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTableCounts(tableName);
        queryBuilder.customJoin("LEFT JOIN " + CoreConstants.TABLE_NAME.CHILD + " ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = " + CoreConstants.TABLE_NAME.CHILD + "." + DBConstants.KEY.BASE_ENTITY_ID + " COLLATE NOCASE ");
        String familyIdSubquery = "(SELECT DISTINCT m.relational_id FROM " + tableName + " m WHERE " + mainCondition + " LIMIT 1)";
        String where = "(" + mainCondition + ") OR (" + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID
                + " IN (SELECT family_head FROM ec_family WHERE base_entity_id = " + familyIdSubquery + "))";
        return queryBuilder.mainCondition(where);
    }

    @Override
    public String mainSelect(String tableName, String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName));
        queryBuilder.customJoin("LEFT JOIN " + CoreConstants.TABLE_NAME.CHILD + " ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = " + CoreConstants.TABLE_NAME.CHILD + "." + DBConstants.KEY.BASE_ENTITY_ID + " COLLATE NOCASE ");
        String familyIdSubquery = "(SELECT DISTINCT m.relational_id FROM " + tableName + " m WHERE " + mainCondition + " LIMIT 1)";
        String where = "(" + mainCondition + ") OR (" + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID
                + " IN (SELECT family_head FROM ec_family WHERE base_entity_id = " + familyIdSubquery + "))";
        return queryBuilder.mainCondition(where);
    }

    @Override
    protected String[] mainColumns(String tableName) {
        String[] columns = super.mainColumns(tableName);
        String[] newColumns = new String[]{
                tableName + "." + ChildDBConstants.KEY.ENTITY_TYPE,
                tableName + "." + CoreConstants.JsonAssets.FAMILY_MEMBER.PHONE_NUMBER
        };
        return ArrayUtils.addAll(columns, newColumns);
    }
}

