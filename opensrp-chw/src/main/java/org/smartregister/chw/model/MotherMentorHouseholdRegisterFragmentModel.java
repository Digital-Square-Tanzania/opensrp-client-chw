package org.smartregister.chw.model;

import androidx.annotation.NonNull;

import org.smartregister.chw.mothermentor.model.BaseMotherMentorContactsRegisterFragmentModel;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

public class MotherMentorHouseholdRegisterFragmentModel
        extends BaseMotherMentorContactsRegisterFragmentModel {
    @Override
    public String mainSelect(@NonNull String tableName, @NonNull String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName));
        return queryBuilder.mainCondition(mainCondition);
    }

    @Override
    protected String[] mainColumns(String tableName) {
        return new String[] {
            tableName + "." + DBConstants.KEY.RELATIONALID,
            tableName + "." + DBConstants.KEY.LAST_INTERACTED_WITH,
            tableName + "." + DBConstants.KEY.BASE_ENTITY_ID,
            tableName + "." + DBConstants.KEY.FIRST_NAME,
            tableName + "." + DBConstants.KEY.LAST_NAME,
            tableName + "." + DBConstants.KEY.UNIQUE_ID,
            tableName + "." + DBConstants.KEY.VILLAGE_TOWN,
            tableName + "." + DBConstants.KEY.FAMILY_HEAD,
            tableName + "." + DBConstants.KEY.PRIMARY_CARE_GIVER
        };
    }
}
