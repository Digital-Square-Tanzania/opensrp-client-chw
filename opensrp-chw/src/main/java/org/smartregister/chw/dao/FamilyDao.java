package org.smartregister.chw.dao;

import android.util.Pair;

import org.smartregister.chw.core.dao.AlertDao;
import org.smartregister.chw.model.FamilyDetailsModel;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.AlertStatus;

import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FamilyDao extends AbstractDao {

    public static Map<String, Integer> getFamilyServiceSchedule(String familyBaseEntityID) {
        String sql = "select visit_state , count(*) totals  from (  " +
                "SELECT s.base_entity_id , m.relational_id , CASE  " +
                "WHEN completion_date  is NOT NULL  THEN  'DONE'  " +
                "WHEN not_done_date is NOT NULL THEN  'NOT_VISIT_THIS_MONTH' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN due_date AND over_due_date THEN  'DUE' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN over_due_date AND expiry_date THEN  'OVERDUE' " +
                "WHEN strftime('%Y-%m-%d')  >= expiry_date  THEN  'EXPIRY'  end  visit_state " +
                "FROM schedule_service s " +
                "LEFT join ec_family_member m on s.base_entity_id  = m.base_entity_id COLLATE NOCASE " +
                "WHERE visit_state is NOT NULL " +
                ") counters   where " +
                " ((counters.relational_id = '" + familyBaseEntityID + "' COLLATE NOCASE) or " +
                " (counters.base_entity_id = '" + familyBaseEntityID + "' COLLATE NOCASE)) " +
                "group by visit_state";


        DataMap<Pair<String, Integer>> dataMap = c -> Pair.create(getCursorValue(c, "visit_state"), getCursorIntValue(c, "totals"));

        Map<String, Integer> visits = new HashMap<>();
        List<Pair<String, Integer>> pairs = AbstractDao.readData(sql, dataMap);
        if (pairs == null || pairs.size() == 0)
            return visits;

        for (Pair<String, Integer> pair : pairs) {
            visits.put(pair.first, pair.second);
        }

        return visits;
    }

    public static Map<String, Integer> getFamilyServiceScheduleWithChildrenOnlyUnderTwo(String familyBaseEntityID) {
        String sql = "select visit_state , count(*) totals  from (  " +
                "SELECT s.base_entity_id , m.relational_id , CASE  " +
                "WHEN completion_date  is NOT NULL  THEN  'DONE'  " +
                "WHEN not_done_date is NOT NULL THEN  'NOT_VISIT_THIS_MONTH' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN due_date AND over_due_date THEN  'DUE' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN over_due_date AND expiry_date THEN  'OVERDUE' " +
                "WHEN strftime('%Y-%m-%d')  >= expiry_date  THEN  'EXPIRY'  end  visit_state " +
                "FROM schedule_service s " +
                " LEFT join ec_family_member m on s.base_entity_id  = m.base_entity_id COLLATE NOCASE \n" +
                " INNER JOIN ec_child c on c.base_entity_id = s.base_entity_id\n" +
                "WHERE visit_state is NOT NULL\n" +
                "and (((julianday('now') - julianday(c.dob))/365.25) < 2 or (c.gender = 'Female' and (((julianday('now') - julianday(c.dob))/365.25) BETWEEN 9 AND 11))) ) counters where " +
                " ((counters.relational_id = '" + familyBaseEntityID + "' COLLATE NOCASE) or " +
                " (counters.base_entity_id = '" + familyBaseEntityID + "' COLLATE NOCASE)) " +
                "group by visit_state";

        DataMap<Pair<String, Integer>> dataMap = c -> Pair.create(getCursorValue(c, "visit_state"), getCursorIntValue(c, "totals"));

        Map<String, Integer> visits = new HashMap<>();
        List<Pair<String, Integer>> pairs = AbstractDao.readData(sql, dataMap);
        if (pairs == null || pairs.size() == 0)
            return visits;

        for (Pair<String, Integer> pair : pairs) {
            visits.put(pair.first, pair.second);
        }

        return visits;
    }

    public static String getMemberDueStatus(String memberBaseEntityId) {
        String sql = "select visit_state from (  " +
                "SELECT m.relational_id , CASE  " +
                "WHEN completion_date  is NOT NULL  THEN  'DONE'  " +
                "WHEN not_done_date is NOT NULL AND not_done_date >= due_date THEN  'NOT_VISIT_THIS_MONTH' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN due_date AND over_due_date THEN  'DUE' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN over_due_date AND expiry_date THEN  'OVERDUE' " +
                "WHEN strftime('%Y-%m-%d')  >= expiry_date  THEN  'EXPIRY'  end  visit_state " +
                "FROM schedule_service s " +
                "inner join ec_family_member m on s.base_entity_id  ='" + memberBaseEntityId + "'" + " COLLATE NOCASE " +
                "WHERE visit_state is NOT NULL " +
                ") counters " +
                "group by visit_state";

        DataMap<String> dataMap = c -> getCursorValue(c, "visit_state");
        String dueStatus;
        List<String> dueStatusListString = AbstractDao.readData(sql, dataMap);
        if (dueStatusListString.size() > 0) {
            dueStatus = dueStatusListString.get(0);
        } else {
            dueStatus = "";
        }
        return dueStatus;
    }

    public static String getMemberDueStatusForUnderTwoChildren(String memberBaseEntityId) {
        String sql = "select visit_state \n" +
                "from (  \n" +
                "SELECT m.relational_id , CASE  \n" +
                "WHEN completion_date  is NOT NULL  THEN  'DONE' \n" +
                "WHEN not_done_date is NOT NULL AND not_done_date >= due_date THEN  'NOT_VISIT_THIS_MONTH' \n" +
                "WHEN strftime('%Y-%m-%d') BETWEEN due_date AND over_due_date THEN  'DUE' \n" +
                "WHEN strftime('%Y-%m-%d') BETWEEN over_due_date AND expiry_date THEN  'OVERDUE'\n" +
                "WHEN strftime('%Y-%m-%d')  >= expiry_date  THEN  'EXPIRY'  end  visit_state \n" +
                "FROM schedule_service s\n" +
                "INNER JOIN ec_family_member m on s.base_entity_id  ='" + memberBaseEntityId + "'" + " COLLATE NOCASE " +
                "INNER JOIN ec_child c on c.base_entity_id = s.base_entity_id\n" +
                "WHERE visit_state is NOT NULL\n" +
                "and (((julianday('now') - julianday(c.dob))/365.25) < 2 or (c.gender = 'Female' and (((julianday('now') - julianday(c.dob))/365.25) BETWEEN 9 AND 11)))\n ) counters \n" +
                "group by visit_state";

        DataMap<String> dataMap = c -> getCursorValue(c, "visit_state");
        String dueStatus;
        List<String> dueStatusListString = AbstractDao.readData(sql, dataMap);
        if (dueStatusListString.size() > 0) {
            dueStatus = dueStatusListString.get(0);
        } else {
            dueStatus = "";
        }
        return dueStatus;
    }

    public static boolean familyHasChildUnderFive(String baseEntityID) {
        String sql = "select count(*) underFive from ec_child c " +
                "inner join ec_family_member m on c.base_entity_id = m.base_entity_id COLLATE NOCASE " +
                "inner join ec_family f on f.base_entity_id = m.relational_id COLLATE NOCASE " +
                "where ((( julianday('now') - julianday(c.dob))/365.25) < 5) and c.is_closed = 0 " +
                "and c.relational_id = '" + baseEntityID + "'";

        DataMap<String> dataMap = c -> getCursorValue(c, "underFive");
        List<String> values = AbstractDao.readData(sql, dataMap);
        if (values == null || values.size() == 0)
            return false;

        return Integer.valueOf(values.get(0)) > 0;
    }

    public static long getFamilyCreateDate(String familyBaseEntityID) {
        String sql = "select eventDate from event where eventType = 'Family Registration' and " +
                "baseEntityId = '" + familyBaseEntityID + "' order by eventDate desc limit 1";

        DataMap<Date> dataMap = c -> getCursorValueAsDate(c, "eventDate", getDobDateFormat());
        List<Date> res = AbstractDao.readData(sql, dataMap);
        if (res == null || res.size() == 0)
            return 0;

        return res.get(0).getTime();
    }

    public static boolean isFamily(String baseEntityID) {
        String sql = "select count(*) count from ec_family where base_entity_id = '" + baseEntityID + "'";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return false;

        return res.get(0) > 0;
    }

    public static AlertStatus getFamilyAlertStatus(String baseEntityID) {
        return AlertDao.getFamilyAlertStatus(baseEntityID);
    }

    public static FamilyDetailsModel getFamilyDetail(String baseEntityId) {
        String sql = String.format(
                "SELECT ec_family.base_entity_id,\n" +
                        "       ec_family.primary_caregiver,\n" +
                        "       ec_family.first_name as family_name,\n" +
                        "       ec_family.village_town as village_town,\n" +
                        "       ec_family.family_head\n" +
                        "FROM ec_family\n" +
                        "         INNER JOIN ec_family_member ON ec_family.base_entity_id = ec_family_member.relational_id\n" +
                        "WHERE ec_family_member.base_entity_id = '%s'", baseEntityId);

        DataMap<FamilyDetailsModel> dataMap = cursor -> {
            FamilyDetailsModel familyDetailsModel = new FamilyDetailsModel(
                    getCursorValue(cursor, "base_entity_id"),
                    getCursorValue(cursor, "family_head"),
                    getCursorValue(cursor, "primary_caregiver"),
                    getCursorValue(cursor, "family_name")
            );
            familyDetailsModel.setVillageTown(getCursorValue(cursor, "village_town"));
            return familyDetailsModel;
        };

        List<FamilyDetailsModel> familyProfileModels = readData(sql, dataMap);
        if (familyProfileModels == null || familyProfileModels.size() != 1)
            return null;

        return familyProfileModels.get(0);
    }

    /**
     * Returns all families where the provided client is the household head.
     * This does not alter the behavior of getFamilyDetail which maps membership; this maps headship.
     */
    public static List<FamilyDetailsModel> getFamiliesByHead(String headClientId) {
        String sql = String.format(
                "SELECT ec_family.base_entity_id,\n" +
                        "       ec_family.primary_caregiver,\n" +
                        "       ec_family.first_name as family_name,\n" +
                        "       ec_family.village_town as village_town,\n" +
                        "       ec_family.family_head\n" +
                        "FROM ec_family\n" +
                        "WHERE ec_family.family_head = '%s'", headClientId);

        DataMap<FamilyDetailsModel> dataMap = cursor -> {
            FamilyDetailsModel familyDetailsModel = new FamilyDetailsModel(
                    getCursorValue(cursor, "base_entity_id"),
                    getCursorValue(cursor, "family_head"),
                    getCursorValue(cursor, "primary_caregiver"),
                    getCursorValue(cursor, "family_name")
            );
            familyDetailsModel.setVillageTown(getCursorValue(cursor, "village_town"));
            return familyDetailsModel;
        };

        List<FamilyDetailsModel> families = readData(sql, dataMap);
        return families != null ? families : new ArrayList<>();
    }
}
