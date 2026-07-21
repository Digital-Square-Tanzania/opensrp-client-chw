package org.smartregister.chw.model;

import androidx.annotation.NonNull;

import org.smartregister.chw.mothermentor.model.BaseMotherMentorContactsRegisterFragmentModel;
import org.smartregister.chw.util.Constants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

public class MotherMentorHouseholdRegisterFragmentModel extends BaseMotherMentorContactsRegisterFragmentModel {
    @Override
    public String mainSelect(@NonNull String tableName, @NonNull String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName));
        return queryBuilder.mainCondition(mainCondition);
    }

    @Override
    protected String[] mainColumns(String tableName) {
        return new String[]{
                tableName + ".relationalid",
                tableName + ".last_interacted_with",
                tableName + ".base_entity_id",
                tableName + ".first_name",
                tableName + ".last_name",
                tableName + ".unique_id",
                tableName + ".village_town",
                tableName + ".family_head",
                tableName + ".primary_caregiver"
        };
    }
}
