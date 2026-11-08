package org.smartregister.chw.model;

import android.database.DatabaseUtils;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.mothermentor.dao.MotherMentorHouseholdFollowUpDao;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorContactsRegisterFragmentModel;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

public class MotherMentorHouseholdRegisterFragmentModel
        extends BaseMotherMentorContactsRegisterFragmentModel {
    public String searchCondition(String tableName, String enrollmentCondition, String search) {
        String term = StringUtils.trimToEmpty(search);
        if (term.isEmpty()) {
            return enrollmentCondition;
        }
        String escaped = term.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        String match = " LIKE " + DatabaseUtils.sqlEscapeString("%" + escaped + "%") + " ESCAPE '!'";
        return enrollmentCondition + " AND ("
                + tableName + "." + DBConstants.KEY.FIRST_NAME + match + " OR "
                + tableName + "." + DBConstants.KEY.UNIQUE_ID + match + " OR "
                + tableName + "." + DBConstants.KEY.BASE_ENTITY_ID + match + ")";
    }

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
            tableName + "." + DBConstants.KEY.PRIMARY_CARE_GIVER,
            MotherMentorHouseholdFollowUpDao.summaryColumn(tableName, DBConstants.KEY.KAYA_STATUS),
            MotherMentorHouseholdFollowUpDao.summaryColumn(tableName, DBConstants.KEY.KAYA_SABABU_YA_KUFUNGWA),
            MotherMentorHouseholdFollowUpDao.summaryColumn(tableName, DBConstants.KEY.KAYA_TAREHE_YA_KUFUNGWA)
        };
    }
}
