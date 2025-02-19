package org.smartregister.chw.model;

import androidx.annotation.NonNull;

import org.smartregister.chw.hps.model.BaseHpsRegisterFragmentModel;
import org.smartregister.chw.util.Constants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.util.DBConstants;

public class HpsHouseholdRegisterFragmentModel extends BaseHpsRegisterFragmentModel {
    @Override
    public String mainSelect(@NonNull String tableName, @NonNull String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName));
        queryBuilder.customJoin("INNER JOIN " + Constants.TABLE_NAME.FAMILY_MEMBER + " ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.BASE_ENTITY_ID + " COLLATE NOCASE ");
        queryBuilder.customJoin("INNER JOIN " + Constants.TABLE_NAME.FAMILY + " ON  " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.BASE_ENTITY_ID + " = " + Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FAMILY_HEAD);
        queryBuilder.customJoin("LEFT JOIN " + Constants.TABLE_NAME.FAMILY_MEMBER + " as T1 ON  " + Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.PRIMARY_CAREGIVER + " = T1." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + Constants.TABLE_NAME.FAMILY_MEMBER + " as T2 ON  " + Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FAMILY_HEAD + " = T2." + DBConstants.KEY.BASE_ENTITY_ID);
        return queryBuilder.mainCondition(mainCondition);
    }

    @Override
    protected String[] mainColumns(String tableName) {
        String[] columns = new String[]{
                Constants.TABLE_NAME.FAMILY + ".relationalid",
                Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.LAST_INTERACTED_WITH,
                Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.BASE_ENTITY_ID,
                Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FIRST_NAME,
                Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.LAST_NAME,
                Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.UNIQUE_ID,
                Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.VILLAGE_TOWN,
                Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FAMILY_HEAD,
                Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.PRIMARY_CAREGIVER
        };
        return columns;
    }

}


