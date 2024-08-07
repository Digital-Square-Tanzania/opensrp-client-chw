package org.smartregister.chw.dao;

import org.smartregister.chw.core.domain.Person;
import org.smartregister.chw.domain.PncBaby;
import org.smartregister.chw.pnc.util.Constants;
import org.smartregister.dao.AbstractDao;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class PersonDao extends AbstractDao {

    public static List<Person> getMothersChildren(String baseEntityID) {
        String sql = "select ec_child.base_entity_id , ec_family_member.first_name , ec_family_member.last_name , ec_family_member.middle_name , ec_family_member.dob " +
                "from ec_child " +
                "inner join ec_family_member on ec_child.base_entity_id = ec_family_member.base_entity_id " +
                "where ec_child.mother_entity_id = '" + baseEntityID + "'" + " COLLATE NOCASE " +
                "and ec_child.date_removed is null and ec_family_member.date_removed is null " +
                "order by ec_family_member.first_name , ec_family_member.last_name , ec_family_member.middle_name ";

        DataMap<Person> dataMap = c -> {
            Date dob = null;
            try {
                dob = getDobDateFormat().parse(c.getString(c.getColumnIndex("dob")));
            } catch (ParseException e) {
                Timber.e(e);
            }
            return new Person(
                    getCursorValue(c, "base_entity_id"),
                    getCursorValue(c, "first_name"),
                    getCursorValue(c, "last_name"),
                    getCursorValue(c, "middle_name"),
                    dob
            );
        };

        return AbstractDao.readData(sql, dataMap);
    }

    /**
     * returns an immutable PncBaby
     *
     * @param baseEntityID
     * @return
     */
    public static List<PncBaby> getMothersPNCBabies(String baseEntityID) {
        String sql = "select ec_child.base_entity_id , ec_family_member.first_name , ec_family_member.last_name , ec_family_member.middle_name , ec_family_member.dob, low_birth_weight " +
                "from ec_child " +
                "inner join ec_family_member on ec_child.base_entity_id = ec_family_member.base_entity_id " +
                "where ec_child.mother_entity_id = '" + baseEntityID + "'" + " COLLATE NOCASE " +
                "and ec_child.date_removed is null and ec_family_member.date_removed is null " +
                "and date(ec_child.dob, '+42 days') >=  date() " +
                "order by ec_family_member.first_name ASC, ec_family_member.last_name , ec_family_member.middle_name ";

        DataMap<PncBaby> dataMap = c -> {
            Date dob = null;
            try {
                dob = getDobDateFormat().parse(c.getString(c.getColumnIndex("dob")));
            } catch (ParseException e) {
                Timber.e(e);
            }
            return new PncBaby(
                    getCursorValue(c, "base_entity_id"),
                    getCursorValue(c, "first_name"),
                    getCursorValue(c, "last_name"),
                    getCursorValue(c, "middle_name"),
                    dob,
                    getCursorValue(c, "low_birth_weight"),
                    getCursorValue(c, Constants.KEY.GENDER)
            );
        };

        List<PncBaby> babies = AbstractDao.readData(sql, dataMap);
        if (babies != null)
            return babies;

        return new ArrayList<>();
    }

    public static String getAncCreatedDate(String baseEntityId) {
        String sql = "SELECT date_created FROM ec_anc_log " +
                " INNER JOIN ec_family_member on ec_family_member.base_entity_id = ec_anc_log.base_entity_id COLLATE NOCASE " +
                " WHERE ec_family_member.base_entity_id = '" + baseEntityId + " COLLATE NOCASE ";

        DataMap<String> dataMap = c -> getCursorValue(c, "date_created");

        List<String> res = AbstractDao.readData(sql, dataMap);
        if (res == null || res.size() == 0) {
            return null;
        }

        return res.get(0);
    }

    public static String getDob(String baseEntityID) {
        String sql = "select dob from ec_family_member where base_entity_id = '" + baseEntityID + "' COLLATE NOCASE ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "dob");

        List<String> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }

    public static Person getPncChildWithNoMother(String baseEntityId) {
        String sql = "select " +
                "ec_child_no_mother.base_entity_id ," +
                "ec_family_member.first_name ," +
                "ec_family_member.last_name ," +
                "ec_family_member.middle_name ," +
                "ec_family_member.dob " +
            "from " +
                "ec_child_no_mother " +
            "inner join " +
                "ec_family_member " +
                "on ec_child_no_mother.base_entity_id = ec_family_member.base_entity_id " +
            "where ec_child_no_mother.base_entity_id = '"+ baseEntityId +  "' COLLATE NOCASE and ec_family_member.date_removed is null " +
            "order by " +
                "ec_family_member.first_name ," +
                "ec_family_member.last_name ," +
                "ec_family_member.middle_name ";

        DataMap<Person> dataMap = c -> {
            Date dob = null;
            try {
                dob = getDobDateFormat().parse(c.getString(c.getColumnIndex("dob")));
            } catch (ParseException e) {
                Timber.e(e);
            }
            return new Person(
                    getCursorValue(c, "base_entity_id"),
                    getCursorValue(c, "first_name"),
                    getCursorValue(c, "last_name"),
                    getCursorValue(c, "middle_name"),
                    dob
            );
        };

        return AbstractDao.readSingleValue(sql, dataMap);

    }
}
