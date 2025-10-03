package org.smartregister.chw.model;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.core.model.CoreNcdRegisterFragmentModel;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.util.DBConstants;

import java.util.HashSet;
import java.util.Set;

public class NcdRegisterAtRiskFragmentModel extends CoreNcdRegisterFragmentModel {

    @NonNull
    @Override
    public String mainSelect(@NonNull String tableName, @NonNull String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName));
        queryBuilder.customJoin("INNER JOIN " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + " ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.BASE_ENTITY_ID + " COLLATE NOCASE ");
        queryBuilder.customJoin("INNER JOIN " + CoreConstants.TABLE_NAME.FAMILY + " ON  " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.RELATIONAL_ID + " = " + CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + " as T1 ON  " + CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.PRIMARY_CAREGIVER + " = T1." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + " as T2 ON  " + CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FAMILY_HEAD + " = T2." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + CoreConstants.TABLE_NAME.DIABETES_HYPERTENSION_FOLLOWUP + " as dhf ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = dhf." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + Constants.TABLES.DIABETES_HYPERTENSION_CONFIRMATION + " as dhc ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = dhc." + DBConstants.KEY.BASE_ENTITY_ID);
        return queryBuilder.mainCondition(mainCondition);
    }

    @NonNull
    @Override
    public String countSelect(@NonNull String tableName, @NonNull String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTableCounts(tableName);
        queryBuilder.customJoin("INNER JOIN " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + " ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.BASE_ENTITY_ID + " COLLATE NOCASE ");
        queryBuilder.customJoin("INNER JOIN " + CoreConstants.TABLE_NAME.FAMILY + " ON  " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.RELATIONAL_ID + " = " + CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + " as T1 ON  " + CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.PRIMARY_CAREGIVER + " = T1." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + " as T2 ON  " + CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FAMILY_HEAD + " = T2." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + CoreConstants.TABLE_NAME.DIABETES_HYPERTENSION_FOLLOWUP + " as dhf ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = dhf." + DBConstants.KEY.BASE_ENTITY_ID);
        queryBuilder.customJoin("LEFT JOIN " + Constants.TABLES.DIABETES_HYPERTENSION_CONFIRMATION + " as dhc ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = dhc." + DBConstants.KEY.BASE_ENTITY_ID);
        return queryBuilder.mainCondition(mainCondition);
    }

    @Override
    public @NotNull String[] mainColumns(String tableName) {
        Set<String> columnList = new HashSet<>();

        columnList.add(tableName + "." + DBConstants.KEY.BASE_ENTITY_ID);
        columnList.add(tableName + ".family_history_diabetes");
        columnList.add(tableName + ".waist_circumference");
        columnList.add(tableName + ".systolic_bp");
        columnList.add(tableName + ".diastolic_bp");
        columnList.add(tableName + ".risk_score");
        columnList.add("dhf.diabetes_test_result as self_reported_diabetes_results");
        columnList.add("dhf.hypertension_test_result as self_reported_hypertension_results");
        columnList.add("dhc.diabetes_result as confirmed_diabetes_results");
        columnList.add("dhc.hypertension_result as confirmed_hypertension_results");
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.RELATIONAL_ID + " as " + ChildDBConstants.KEY.RELATIONAL_ID);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.FIRST_NAME);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.MIDDLE_NAME);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.LAST_NAME);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.DOB);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.GENDER);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.UNIQUE_ID);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.RELATIONAL_ID);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.PHONE_NUMBER);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.OTHER_PHONE_NUMBER);
        columnList.add("T2." + DBConstants.KEY.PHONE_NUMBER + " AS " + org.smartregister.chw.tb.util.DBConstants.Key.FAMILY_HEAD_PHONE_NUMBER);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.VILLAGE_TOWN);
        columnList.add("T1." + DBConstants.KEY.FIRST_NAME + " || " + "' '" + " || " + "T1." + DBConstants.KEY.MIDDLE_NAME + " || " + "' '" + " || " + "T1." + DBConstants.KEY.LAST_NAME + " AS " + DBConstants.KEY.PRIMARY_CAREGIVER);
        columnList.add("T2." + DBConstants.KEY.FIRST_NAME + " || " + "' '" + " || " + "T2." + DBConstants.KEY.MIDDLE_NAME + " || " + "' '" + " || " + "T2." + DBConstants.KEY.LAST_NAME + " AS " + DBConstants.KEY.FAMILY_HEAD);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FIRST_NAME + " as " + org.smartregister.chw.agyw.util.DBConstants.KEY.FAMILY_NAME);

        return columnList.toArray(new String[columnList.size()]);
    }
}
