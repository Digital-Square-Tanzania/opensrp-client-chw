package org.smartregister.chw.model;

import org.smartregister.chw.core.model.PncRegisterFragmentModel;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.ChwDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.util.DBConstants;

import java.util.HashSet;
import java.util.Set;

public class PncChildNoMotherFragmentModel extends PncRegisterFragmentModel {

    @Override
    protected String[] mainColumns(String tableName) {
        Set<String> columnList = new HashSet<>();

        columnList.add(tableName + "." + DBConstants.KEY.BASE_ENTITY_ID);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.DOB + " as " + ChwDBConstants.DELIVERY_DATE);
        columnList.add(tableName + "." + "caregiver_name");
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.RELATIONAL_ID + " as " + ChildDBConstants.KEY.RELATIONAL_ID);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.FIRST_NAME);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.MIDDLE_NAME);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.LAST_NAME);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.DOB);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.PHONE_NUMBER);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.RELATIONAL_ID);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.UNIQUE_ID);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.GENDER);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FAMILY_HEAD);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.PRIMARY_CAREGIVER);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.VILLAGE_TOWN);
        columnList.add(CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.FIRST_NAME + " as " + org.smartregister.chw.anc.util.DBConstants.KEY.FAMILY_NAME);

        return columnList.toArray(new String[columnList.size()]);
    }

    @Override
    public String mainSelect(String tableName, String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName));
        queryBuilder.customJoin("INNER JOIN " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + " ON  " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + " = " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.BASE_ENTITY_ID + " AND " + tableName + "." + ChwDBConstants.IS_CLOSED + " IS " + 0  + " AND " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + ChwDBConstants.IS_CLOSED + " IS " + 0 + " COLLATE NOCASE ");
        queryBuilder.customJoin("INNER JOIN " + CoreConstants.TABLE_NAME.FAMILY + " ON  " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.RELATIONAL_ID + " = " + CoreConstants.TABLE_NAME.FAMILY + "." + DBConstants.KEY.BASE_ENTITY_ID + " COLLATE NOCASE ");
        queryBuilder.customJoin("LEFT JOIN (select base_entity_id , max(visit_date) visit_date from visits GROUP by base_entity_id) VISIT_SUMMARY ON VISIT_SUMMARY.base_entity_id = " + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID);


        //This query is used to obtain a list of children whose mother's died while the children are still in PNC
        //This children are moved to No Mother Children Register for continuation of PNC Visits
        return queryBuilder.mainCondition(mainCondition) + " UNION Select \n" +
                "ec_pregnancy_outcome.id as _id , \n" +
                "ec_family_member.dob as delivery_date , \n" +
                "childFamilyMember.middle_name , \n" +
                "childFamilyMember.first_name , \n" +
                "childFamilyMember.gender , \n" +
                "ec_family_member.first_name || ' ' ||ec_family_member.middle_name ||' '||ec_family_member.last_name   as caregiver_name, \n" +
                "ec_family.primary_caregiver , \n" +
                "childFamilyMember.last_name , \n" +
                "ec_family_member.phone_number , \n" +
                "ec_family.village_town , \n" +
                "childFamilyMember.dob , \n" +
                "ec_family.family_head , \n" +
                "ec_family.first_name as family_name , \n" +
                "childFamilyMember.unique_id , \n" +
                "childFamilyMember.base_entity_id , \n" +
                "ec_family_member.relational_id as relationalid , \n" +
                "ec_family_member.relational_id \n" +
                "FROM ec_pregnancy_outcome \n" +
                "INNER JOIN ec_family_member ON  ec_pregnancy_outcome.base_entity_id = ec_family_member.base_entity_id AND ec_pregnancy_outcome.is_closed IS 0 AND ec_family_member.is_closed IS 1 AND ec_pregnancy_outcome.delivery_date IS NOT NULL COLLATE NOCASE  \n" +
                "INNER JOIN ec_family ON  ec_family_member.relational_id = ec_family.base_entity_id COLLATE NOCASE  \n" +
                "INNER JOIN ec_child ON  ec_family_member.base_entity_id = ec_child.mother_entity_id COLLATE NOCASE  \n" +
                "INNER JOIN ec_family_member childFamilyMember ON  childFamilyMember.base_entity_id = ec_child.base_entity_id COLLATE NOCASE  \n" +
                "LEFT JOIN (select base_entity_id , max(visit_date) visit_date from visits GROUP by base_entity_id) VISIT_SUMMARY ON VISIT_SUMMARY.base_entity_id = ec_pregnancy_outcome.base_entity_id";
    }
}
