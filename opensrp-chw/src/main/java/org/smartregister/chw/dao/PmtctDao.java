package org.smartregister.chw.dao;

import org.smartregister.chw.domain.PmtctReferralMemberObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PmtctDao extends org.smartregister.chw.pmtct.dao.PmtctDao {
    public static PmtctReferralMemberObject getPmtctReferralMemberObject(String baseEntityId) {
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
                "       f.first_name     family_name,\n" +
                "       f.primary_caregiver,\n" +
                "       f.family_head,\n" +
                "       f.village_town,\n" +
                "       fh.first_name    family_head_first_name,\n" +
                "       fh.middle_name   family_head_middle_name,\n" +
                "       fh.last_name     family_head_last_name,\n" +
                "       fh.phone_number  family_head_phone_number,\n" +
                "       ancr.is_closed   anc_is_closed,\n" +
                "       pncr.is_closed   pnc_is_closed,\n" +
                "       pcg.first_name   pcg_first_name,\n" +
                "       pcg.last_name    pcg_last_name,\n" +
                "       pcg.middle_name  pcg_middle_name,\n" +
                "       pcg.phone_number pcg_phone_number,\n" +
                "       mr.*\n" +
                "from ec_family_member m\n" +
                "         inner join ec_family f on m.relational_id = f.base_entity_id\n" +
                "         inner join ec_pmtct_community_followup mr on mr.entity_id = m.base_entity_id\n" +
                "         left join ec_family_member fh on fh.base_entity_id = f.family_head\n" +
                "         left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver\n" +
                "         left join ec_anc_register ancr on ancr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_pregnancy_outcome pncr on pncr.base_entity_id = m.base_entity_id\n" +
                " where mr.base_entity_id ='" + baseEntityId + "' ";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        DataMap<PmtctReferralMemberObject> dataMap = cursor -> {
            PmtctReferralMemberObject memberObject = new PmtctReferralMemberObject();

            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setGender(getCursorValue(cursor, "gender"));
            memberObject.setUniqueId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setAge(getCursorValue(cursor, "dob"));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setRelationalId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setBaseEntityId(getCursorValue(cursor, "entity_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "pcg_phone_number", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));

            memberObject.setPmtctCommunityReferralDate(getCursorValueAsDate(cursor, "pmtct_community_referral_date", getNativeFormsDateFormat()));
            memberObject.setLastFacilityVisitDate(getCursorValueAsDate(cursor, "last_client_visit_date", getNativeFormsDateFormat()));
            memberObject.setReasonsForIssuingCommunityFollowupReferral(getCursorValue(cursor, "reasons_for_issuing_community_referral", ""));
            memberObject.setComments(getCursorValue(cursor, "comment", ""));
            memberObject.setChildName(getCursorValue(cursor, "child_name", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " "
                    + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName =
                    (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();
            memberObject.setFamilyHeadName(familyHeadName);

            String familyPcgName = getCursorValue(cursor, "pcg_first_name", "") + " "
                    + getCursorValue(cursor, "pcg_middle_name", "");

            familyPcgName =
                    (familyPcgName.trim() + " " + getCursorValue(cursor, "pcg_last_name", "")).trim();
            memberObject.setPrimaryCareGiverName(familyPcgName);

            return memberObject;
        };

        List<PmtctReferralMemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }


    public static void deleteEntryFromTableByFormSubmissionId(String tableName, String submissionId) {
        String sql = "delete from " + tableName + " where base_entity_id = '" + submissionId + "'";
        updateDB(sql);
    }
}
