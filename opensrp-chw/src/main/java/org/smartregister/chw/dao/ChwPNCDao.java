package org.smartregister.chw.dao;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.Nullable;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.domain.PNCHealthFacilityVisitSummary;
import org.smartregister.dao.AbstractDao;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ChwPNCDao extends AbstractDao {

    public static @Nullable PNCHealthFacilityVisitSummary getLastHealthFacilityVisitSummary(String baseEntityID) {
        return getLastHealthFacilityVisitSummary(baseEntityID, null);
    }

    public static @Nullable PNCHealthFacilityVisitSummary getLastHealthFacilityVisitSummary(String baseEntityID, SQLiteDatabase sqLiteDatabase) {
        String sql = "select  last_health_facility_visit_date , confirmed_health_facility_visits, delivery_date from ec_pregnancy_outcome " +
                "where base_entity_id = '" + baseEntityID + "'" + " COLLATE NOCASE ";

        DataMap<PNCHealthFacilityVisitSummary> dataMap = c -> {
            try {
                return new PNCHealthFacilityVisitSummary(
                        getCursorValue(c, "delivery_date"),
                        getCursorValue(c, "last_health_facility_visit_date"),
                        getCursorValue(c, "confirmed_health_facility_visits")
                );
            } catch (ParseException e) {
                Timber.e(e);
            }
            return null;
        };
        List<PNCHealthFacilityVisitSummary> res;
        if (sqLiteDatabase != null) {
            res = AbstractDao.readData(sql, dataMap, sqLiteDatabase);
        } else {
            res = AbstractDao.readData(sql, dataMap);
        }
        return (res != null && res.size() > 0) ? res.get(0) : null;
    }

    public static @Nullable List<VisitDetail> getLastPNCHealthFacilityVisits(String motherBaseEntityId) {
        String sql = "SELECT DISTINCT vd.visit_key \n" +
                " FROM Visit_details vd  \n" +
                " INNER JOIN visits v \n" +
                " on v.visit_id = vd.visit_id\n" +
                " AND v.visit_type = 'PNC Home Visit'\n" +
                " AND v.base_entity_id   = '" + motherBaseEntityId + "'" +
                " AND vd.visit_key LIKE 'pnc_hf_visit%'" +
                " ORDER by vd.details DESC\n" +
                " LIMIT 1";

        List<VisitDetail> details = new ArrayList<>();
        DataMap<VisitDetail> dataMap = c -> {
            VisitDetail detail = new VisitDetail();
            detail.setVisitKey(getCursorValue(c, "visit_key"));
            details.add(detail);
            return detail;
        };
        List<VisitDetail> res = AbstractDao.readData(sql, dataMap);
        if (res == null || res.size() == 0)
            return new ArrayList<>();

        return details;
    }

    public static List<MemberObject> getPncMembersWithMoreThan42Days() {
        String sql = "select m.base_entity_id,\n" +
                "       m.unique_id,\n" +
                "       m.relational_id,\n" +
                "       m.dob,\n" +
                "       m.first_name,\n" +
                "       m.middle_name,\n" +
                "       m.last_name,\n" +
                "       m.gender,\n" +
                "       m.phone_number,\n" +
                "       m.other_phone_number,\n" +
                "       f.first_name    family_name,\n" +
                "       f.primary_caregiver,\n" +
                "       f.family_head,\n" +
                "       fh.first_name   family_head_first_name,\n" +
                "       fh.middle_name  family_head_middle_name,\n" +
                "       fh.last_name    family_head_last_name,\n" +
                "       fh.phone_number family_head_phone_number,\n" +
                "       f.village_town,\n" +
                "        epo.delivery_date\n" +
                "from ec_family_member m\n" +
                "         inner join ec_family f on m.relational_id = f.base_entity_id\n" +
                "        inner join ec_pregnancy_outcome epo on m.base_entity_id = epo.base_entity_id\n" +
                "         left join ec_family_member fh on fh.base_entity_id = f.family_head\n" +
                "where cast(julianday(datetime('now')) - julianday(datetime(substr(epo.delivery_date, 7,4)\n" +
                "                || '-' || substr(epo.delivery_date, 4,2) || '-' || substr(epo.delivery_date, 1,2))) as integer) >= 43\n" +
                "  AND epo.is_closed = 0 ";

        DataMap<MemberObject> dataMap = cursor -> {
            MemberObject memberObject = new MemberObject();
            memberObject.setLastMenstrualPeriod(getCursorValue(cursor, "last_menstrual_period"));
            memberObject.setChwMemberId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id", ""));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " "
                    + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName = (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();

            memberObject.setFamilyHeadName(familyHeadName);
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setLastContactVisit(getCursorValue(cursor, "last_contact_visit"));
            memberObject.setLastInteractedWith(getCursorValue(cursor, "last_interacted_with"));
            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setDob(getCursorValue(cursor, "dob"));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setConfirmedContacts(getCursorIntValue(cursor, "confirmed_visits", 0));
            memberObject.setDateCreated(getCursorValue(cursor, "date_created"));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setHasAncCard(getCursorValue(cursor, "has_anc_card", ""));

            return memberObject;
        };
        List<MemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() == 0)
            return null;

        return res;
    }


    public interface Flavor {

    }
}
