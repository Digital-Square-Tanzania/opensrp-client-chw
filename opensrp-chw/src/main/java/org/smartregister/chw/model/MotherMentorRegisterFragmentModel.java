package org.smartregister.chw.model;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorRegisterFragmentModel;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.util.Constants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

import java.util.HashSet;
import java.util.Set;

public class MotherMentorRegisterFragmentModel extends BaseMotherMentorRegisterFragmentModel {

    @NonNull
    @Override
    public String mainSelect(@NonNull String tableName, @NonNull String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName));
        queryBuilder.customJoin(
                "INNER JOIN "
                        + Constants.TABLE_NAME.FAMILY_MEMBER
                        + " ON "
                        + tableName
                        + "."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = "
                        + Constants.TABLE_NAME.FAMILY_MEMBER
                        + "."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " COLLATE NOCASE ");
        queryBuilder.customJoin(
                "INNER JOIN "
                        + Constants.TABLE_NAME.FAMILY
                        + " ON "
                        + Constants.TABLE_NAME.FAMILY_MEMBER
                        + "."
                        + DBConstants.KEY.RELATIONAL_ID
                        + " = "
                        + Constants.TABLE_NAME.FAMILY
                        + "."
                        + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin(
                "LEFT JOIN "
                        + Constants.TABLE_NAME.FAMILY_MEMBER
                        + " as T1 ON "
                        + Constants.TABLE_NAME.FAMILY
                        + "."
                        + DBConstants.KEY.PRIMARY_CARE_GIVER
                        + " = T1."
                        + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin(
                "LEFT JOIN "
                        + Constants.TABLE_NAME.FAMILY_MEMBER
                        + " as T2 ON "
                        + Constants.TABLE_NAME.FAMILY
                        + "."
                        + DBConstants.KEY.FAMILY_HEAD
                        + " = T2."
                        + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin(
                "LEFT JOIN ec_anc_register as ANC ON "
                        + tableName
                        + "."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = ANC."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " AND ANC."
                        + DBConstants.KEY.IS_CLOSED
                        + " = 0");
        return queryBuilder.mainCondition(mainCondition);
    }

    @NotNull
    protected String[] mainColumns(String tableName) {
        Set<String> columnList = new HashSet<>();

        columnList.add(tableName + "." + DBConstants.KEY.BASE_ENTITY_ID);
        columnList.add(
                Constants.TABLE_NAME.FAMILY_MEMBER
                        + "."
                        + DBConstants.KEY.RELATIONAL_ID
                        + " as "
                        + ChildDBConstants.KEY.RELATIONAL_ID);
        columnList.add(Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.FIRST_NAME);
        columnList.add(Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.MIDDLE_NAME);
        columnList.add(Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.LAST_NAME);
        columnList.add(Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.DOB);
        columnList.add(Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.GENDER);
        columnList.add(Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.UNIQUE_ID);
        columnList.add(Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.RELATIONAL_ID);
        columnList.add(Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.PHONE_NUMBER);
        columnList.add(
                Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.OTHER_PHONE_NUMBER);
        columnList.add(
                "T2."
                        + DBConstants.KEY.PHONE_NUMBER
                        + " AS "
                        + org.smartregister.chw.tb.util.DBConstants.Key.FAMILY_HEAD_PHONE_NUMBER);
        columnList.add(Constants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.VILLAGE_TOWN);
        columnList.add(
                "T1."
                        + DBConstants.KEY.FIRST_NAME
                        + " || ' ' || T1."
                        + DBConstants.KEY.MIDDLE_NAME
                        + " || ' ' || T1."
                        + DBConstants.KEY.LAST_NAME
                        + " AS "
                        + DBConstants.KEY.PRIMARY_CARE_GIVER);
        columnList.add(
                "T2."
                        + DBConstants.KEY.FIRST_NAME
                        + " || ' ' || T2."
                        + DBConstants.KEY.MIDDLE_NAME
                        + " || ' ' || T2."
                        + DBConstants.KEY.LAST_NAME
                        + " AS "
                        + DBConstants.KEY.FAMILY_HEAD);
        columnList.add(
                Constants.TABLE_NAME.FAMILY
                        + "."
                        + DBConstants.KEY.FIRST_NAME
                        + " as "
                        + org.smartregister.chw.anc.util.DBConstants.KEY.FAMILY_NAME);
        columnList.add("ANC.edd as pregnancy_edd");
        if ("ec_mothermentor_enroll_it".equals(tableName)) {
            columnList.add(
                    tableName + "." + DBConstants.KEY.WEEKS_LOST + " as follow_up_weeks_lost");
            columnList.add(
                    tableName
                            + "."
                            + DBConstants.KEY.EXPECTED_RETURN_DATE
                            + " as follow_up_expected_return_date");
        } else {
            columnList.add("NULL as follow_up_weeks_lost");
            columnList.add("NULL as follow_up_expected_return_date");
        }

        return columnList.toArray(new String[0]);
    }
}
