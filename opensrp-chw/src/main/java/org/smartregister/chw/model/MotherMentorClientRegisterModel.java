package org.smartregister.chw.model;

import androidx.annotation.NonNull;

import org.smartregister.chw.mothermentor.dao.MotherMentorClientRegisterDao;
import org.smartregister.chw.mothermentor.util.DBConstants;

/** Main client list queries are owned by the Mother Mentor library. */
public class MotherMentorClientRegisterModel extends MotherMentorRegisterFragmentModel {
    public static final String CURRENT_STATUS = DBConstants.KEY.MOTHERMENTOR_CURRENT_STATUS;
    public static final String COMPLETED_VISITS = DBConstants.KEY.MOTHERMENTOR_COMPLETED_VISITS;

    @NonNull
    @Override
    public String mainSelect(@NonNull String tableName, @NonNull String mainCondition) {
        return selectClients(mainCondition, "", false, false, false, false);
    }

    @Override
    public String countSelect(String tableName, String mainCondition) {
        return countClients(mainCondition, "", false, false, false, false);
    }

    public String selectClients(
            String mainCondition,
            String search,
            boolean filterOn,
            boolean pregnant,
            boolean breastfeeding,
            boolean underTwo) {
        return MotherMentorClientRegisterDao.selectClients(
                mainCondition, search, filterOn, pregnant, breastfeeding, underTwo);
    }

    public String selectClientsPage(
            String mainCondition,
            String search,
            boolean filterOn,
            boolean pregnant,
            boolean breastfeeding,
            boolean underTwo,
            int limit,
            int offset) {
        return MotherMentorClientRegisterDao.selectClientsPage(
                mainCondition, search, filterOn, pregnant, breastfeeding, underTwo, limit, offset);
    }

    public String countClients(
            String mainCondition,
            String search,
            boolean filterOn,
            boolean pregnant,
            boolean breastfeeding,
            boolean underTwo) {
        return MotherMentorClientRegisterDao.countClients(
                mainCondition, search, filterOn, pregnant, breastfeeding, underTwo);
    }
}
