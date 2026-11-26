package org.smartregister.chw.model;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorRegisterFragmentModel;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.Constants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MotherMentorRegisterFragmentModel extends BaseMotherMentorRegisterFragmentModel {

    @NonNull
    @Override
    public String mainSelect(@NonNull String tableName, @NonNull String mainCondition) {
        return select(tableName, mainCondition, mainColumns(tableName));
    }

    public String partnerCandidates(String indexedCondition, boolean closed, String filter, String order) {
        String table = MotherMentorFollowUpType.PARTNER.getEnrollmentTable();
        // Finalized visits can support a closure even when the derived enrollment or EC row lags.
        String replay = "coalesce(nullif(trim(" + table + "." + DBConstants.KEY.MWENZI_STATUS
                + "), ''), 'active') = 'active' AND EXISTS (SELECT 1 FROM "
                + VisitRepository.VISIT_TABLE + " partner_evidence WHERE partner_evidence."
                + DBConstants.KEY.BASE_ENTITY_ID + " = " + table + "." + DBConstants.KEY.BASE_ENTITY_ID
                + " AND partner_evidence." + DBConstants.KEY.PROCESSED + " = 1 AND partner_evidence."
                + DBConstants.KEY.VISIT_TYPE + " IN ('" + MotherMentorFollowUpType.PARTNER.getEventType()
                + "', '" + org.smartregister.chw.mothermentor.util.Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES
                + "'))";
        String[] columns = mainColumns(table);
        int size = columns.length;
        columns = Arrays.copyOf(columns, size + 2);
        columns[size] = "CASE WHEN " + indexedCondition + " THEN 1 ELSE 0 END AS partner_indexed_match";
        columns[size + 1] = "(" + replay + ") AS partner_replay_closure";
        String condition = closed ? "((" + indexedCondition + ") OR (" + replay + "))" : indexedCondition;
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder(select(table, condition, columns));
        if (filter != null && !filter.isEmpty()) queryBuilder.addCondition("AND " + filter);
        return queryBuilder.orderbyCondition(order);
    }

    private String select(String tableName, String mainCondition, String[] columns) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, columns);
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
                (org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_IIT
                        .equals(tableName) ? "LEFT JOIN " : "INNER JOIN ")
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

        if (org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER
                .equals(tableName)) {
            columnList.add(tableName + "." + DBConstants.KEY.PARTNER_NEXT_FOLLOW_UP_PHASE);
            columnList.add(tableName + "." + DBConstants.KEY.MWENZI_STATUS);
            columnList.add(tableName + "." + DBConstants.KEY.MWENZI_TAREHE_YA_KUFUNGWA);
            columnList.add(tableName + "." + DBConstants.KEY.MWENZI_SABABU_YA_KUFUNGWA);
        }
        return columnList.toArray(new String[0]);
    }
}
