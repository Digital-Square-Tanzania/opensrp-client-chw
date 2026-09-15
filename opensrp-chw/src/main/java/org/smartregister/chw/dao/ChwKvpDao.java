package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.kvp.dao.KvpDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChwKvpDao extends KvpDao {
    private static final String[] HIV_TEST_DATE_FORMATS = {"dd-MM-yyyy", "yyyy-MM-dd"};

    public static String getDominantKVPGroup(String baseEntityId) {
        String sql = "SELECT client_group FROM ec_kvp_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_group");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static String getDominantKVPGroupFromFacility(String baseEntityId) {
        String sql = "SELECT client_group FROM ec_facility_kvp_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_group");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static boolean hasFollowupVisits(String baseEntityId) {
        String sql = "SELECT visit_type FROM ec_kvp_prep_followup p " +
                " WHERE p.entity_id = '" + baseEntityId + "'";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "visit_type");

        List<String> res = readData(sql, dataMap);
        if (res != null) {
            return res.size() > 0;
        }
        return false;
    }

    public static boolean wereSelfTestingKitsDistributed(String baseEntityId) {
        String sql = "SELECT kits_distributed FROM ec_kvp_prep_followup p " +
                " WHERE p.entity_id = '" + baseEntityId + "'  ORDER BY last_interacted_with DESC LIMIT 1 ";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "kits_distributed");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() > 0 && res.get(0) != null) {
            return res.get(0).equalsIgnoreCase("yes");
        }
        return false;
    }

    public static boolean isLatestFollowupHivPositive(String baseEntityId) {
        return hasAnyPositiveFollowupStatus(baseEntityId);
    }

    public static boolean isRegistrationHivPositive(String baseEntityId) {
        String hivPositive = getLatestRegistrationDetail(baseEntityId, "hiv_positive");
        if (StringUtils.equalsIgnoreCase(sanitizeDetail(hivPositive), "true")) {
            return true;
        }

        return isPositiveHivStatus(getRegistrationHivStatus(baseEntityId));
    }

    public static boolean isClientHivPositive(String baseEntityId) {
        return isRegistrationHivPositive(baseEntityId) || isLatestFollowupHivPositive(baseEntityId);
    }

    public static boolean hasCtcNumber(String baseEntityId) {
        String ctcNumber = StringUtils.defaultIfBlank(
                getLatestFollowupDetail(baseEntityId, "ctc_number"),
                StringUtils.defaultIfBlank(
                        getLatestFollowupDetail(baseEntityId, "ctc_number_a"),
                        StringUtils.defaultIfBlank(getLatestFollowupDetail(baseEntityId, "ctc_number_b"), getRegistrationCtcNumber(baseEntityId))
                )
        );

        if (StringUtils.isBlank(ctcNumber)) {
            return false;
        }

        String normalizedCtc = ctcNumber
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        return StringUtils.isNotBlank(normalizedCtc);
    }

    public static String getLatestVisitType(String baseEntityId) {
        return sanitizeDetail(getLatestFollowupDetail(baseEntityId, "visit_type"));
    }

    public static String getLatestClientHivStatus(String baseEntityId) {
        if (isLatestFollowupHivPositive(baseEntityId)) {
            return "positive";
        }

        String latestFollowupHivStatus = sanitizeDetail(getLatestFollowupDetail(baseEntityId, "client_hiv_status"));
        if (StringUtils.isNotBlank(latestFollowupHivStatus)) {
            return latestFollowupHivStatus;
        }

        return getRegistrationHivStatus(baseEntityId);
    }

    public static String getRegistrationHivStatus(String baseEntityId) {
        return sanitizeDetail(getLatestRegistrationDetail(baseEntityId, "hiv_status"));
    }

    public static String getRegistrationCtcNumber(String baseEntityId) {
        return sanitizeDetail(getLatestRegistrationDetail(baseEntityId, "ctc_number"));
    }

    public static boolean isHivRetestDue(String baseEntityId) {
        String sql = "SELECT test_date, hiv_result, hiv_result_recent, client_hiv_status " +
                "FROM ec_kvp_prep_followup " +
                "WHERE entity_id = '" + baseEntityId + "' " +
                "AND test_date IS NOT NULL " +
                "AND TRIM(test_date) != '' " +
                "ORDER BY last_interacted_with DESC";

        DataMap<HivTestRecord> dataMap = cursor -> new HivTestRecord(
                getCursorValue(cursor, "test_date"),
                firstNonBlank(
                        getCursorValue(cursor, "hiv_result"),
                        getCursorValue(cursor, "hiv_result_recent"),
                        getCursorValue(cursor, "client_hiv_status")));

        return isHivRetestDue(readData(sql, dataMap), new Date());
    }

    static boolean isHivRetestDue(List<HivTestRecord> records, Date referenceDate) {
        if (records == null || records.isEmpty() || referenceDate == null) {
            return false;
        }

        Date latestTestDate = null;
        String latestTestResult = null;
        for (HivTestRecord record : records) {
            if (record == null || !isRecognizedHivResult(record.result)) {
                continue;
            }

            Date testDate = parseHivTestDate(record.testDate);
            if (testDate != null && (latestTestDate == null || testDate.after(latestTestDate))) {
                latestTestDate = testDate;
                latestTestResult = record.result;
            }
        }

        if (latestTestDate == null || !isNegativeHivStatus(latestTestResult)) {
            return false;
        }

        Calendar retestDate = dateOnly(latestTestDate);
        retestDate.add(Calendar.MONTH, 3);
        return !dateOnly(referenceDate).before(retestDate);
    }

    public static Date getLatestProcessedFollowupDate(String baseEntityId) {
        Long latestFollowupDate = getLatestLastInteractedWith(
                "ec_kvp_prep_followup", "entity_id", baseEntityId);
        return latestFollowupDate == null ? null : new Date(latestFollowupDate);
    }

    private static String sanitizeDetail(String detail) {
        if (StringUtils.isBlank(detail)) {
            return null;
        }

        return detail
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();
    }

    private static String firstNonBlank(String... details) {
        for (String detail : details) {
            String sanitizedDetail = sanitizeDetail(detail);
            if (StringUtils.isNotBlank(sanitizedDetail)) {
                return sanitizedDetail;
            }
        }
        return null;
    }

    private static Date parseHivTestDate(String value) {
        String dateValue = sanitizeDetail(value);
        if (StringUtils.isBlank(dateValue)) {
            return null;
        }

        for (String format : HIV_TEST_DATE_FORMATS) {
            String candidate = dateValue;
            if ("yyyy-MM-dd".equals(format) && dateValue.length() > 10) {
                candidate = dateValue.substring(0, 10);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.ROOT);
            dateFormat.setLenient(false);
            try {
                return dateFormat.parse(candidate);
            } catch (ParseException ignored) {
                // Try the next supported persisted date format.
            }
        }
        return null;
    }

    private static Calendar dateOnly(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static boolean isRecognizedHivResult(String status) {
        return isPositiveHivStatus(status) || isNegativeHivStatus(status);
    }

    private static boolean isNegativeHivStatus(String status) {
        String normalizedStatus = sanitizeDetail(status);
        if (StringUtils.isBlank(normalizedStatus)) {
            return false;
        }

        normalizedStatus = normalizedStatus.toLowerCase(Locale.ROOT);
        return normalizedStatus.contains("negative") || normalizedStatus.contains("hasi") || normalizedStatus.contains("hana maambukizi");
    }

    private static boolean isPositiveHivStatus(String status) {
        String normalizedStatus = sanitizeDetail(status);
        if (StringUtils.isBlank(normalizedStatus)) {
            return false;
        }

        normalizedStatus = normalizedStatus.toLowerCase(Locale.ROOT);
        if (normalizedStatus.contains("hana maambukizi")) {
            return false;
        }
        return normalizedStatus.contains("positive") || normalizedStatus.contains("chanya") || normalizedStatus.contains("ana maambukizi");
    }

    static final class HivTestRecord {
        private final String testDate;
        private final String result;

        HivTestRecord(String testDate, String result) {
            this.testDate = testDate;
            this.result = result;
        }
    }

    private static Long getLatestLastInteractedWith(String tableName, String idColumn, String baseEntityId) {
        String sql = "SELECT last_interacted_with FROM " + tableName + " " +
                "WHERE " + idColumn + " = '" + baseEntityId + "' " +
                "AND last_interacted_with IS NOT NULL " +
                "ORDER BY last_interacted_with DESC LIMIT 1";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "last_interacted_with");
        List<String> res = readData(sql, dataMap);

        if (res != null && !res.isEmpty()) {
            try {
                return Long.parseLong(res.get(0));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private static boolean hasAnyPositiveFollowupStatus(String baseEntityId) {
        String sql = "SELECT entity_id FROM ec_kvp_prep_followup " +
                "WHERE entity_id = '" + baseEntityId + "' " +
                "AND (" +
                "LOWER(COALESCE(hiv_positive, '')) = 'true' " +
                "OR LOWER(COALESCE(client_hiv_status, '')) LIKE '%positive%' " +
                "OR LOWER(COALESCE(client_hiv_status, '')) LIKE '%chanya%' " +
                "OR (LOWER(COALESCE(client_hiv_status, '')) LIKE '%ana maambukizi%' " +
                "AND LOWER(COALESCE(client_hiv_status, '')) NOT LIKE '%hana maambukizi%') " +
                "OR LOWER(COALESCE(hiv_result_recent, '')) = 'positive' " +
                "OR LOWER(COALESCE(hiv_result, '')) = 'positive'" +
                ") LIMIT 1";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "entity_id");
        List<String> res = readData(sql, dataMap);
        return res != null && !res.isEmpty();
    }

    private static String getLatestRegistrationDetail(String baseEntityId, String detailKey) {
        String sql = "SELECT " + detailKey + " FROM ec_kvp_prep_register " +
                "WHERE base_entity_id = '" + baseEntityId + "' " +
                "AND is_closed = 0 " +
                "AND " + detailKey + " IS NOT NULL " +
                "ORDER BY last_interacted_with DESC LIMIT 1";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, detailKey);
        List<String> res = readData(sql, dataMap);

        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }

        return null;
    }

    private static String getLatestFollowupDetail(String baseEntityId, String detailKey) {
        String sql = "SELECT " + detailKey + " FROM ec_kvp_prep_followup " +
                "WHERE entity_id = '" + baseEntityId + "' " +
                "AND " + detailKey + " IS NOT NULL " +
                "AND TRIM(" + detailKey + ") != '' " +
                "ORDER BY last_interacted_with DESC LIMIT 1";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, detailKey);
        List<String> res = readData(sql, dataMap);

        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }

        return null;
    }
}
