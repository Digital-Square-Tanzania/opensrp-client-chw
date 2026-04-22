package org.smartregister.chw.dao;

import org.smartregister.dao.AbstractDao;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class ReferralDao extends AbstractDao {
    public static String getTaskIdByReasonReference(String formId) {
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "_id");

        String sql = String.format(
                "SELECT _id FROM %s WHERE reason_reference = '%s' ",
                "task",
                formId
        );

        List<String> res = readData(sql, dataMap);
        return res.size() > 0 ? res.get(0) : "";

    }

    public static Date getLastAppointmentDate(String baseEntityId) {
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "last_appointment_date");

        String sql = "SELECT last_appointment_date  from ec_referral " +
                      "WHERE base_entity_id = '" + baseEntityId + "' ";
        List<String> res = readData(sql, dataMap);

        if (res != null && res.size() > 0 && res.get(0) != null) {
            Calendar cal = Calendar.getInstance();
            try {
                cal.setTimeInMillis(new BigDecimal(res.get(0)).longValue());
            } catch (Exception e) {
                //NEEDED FOR THE ISSUE IN SOME TABLETS FAILING TO CREATE A TIMESTAMP
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    cal.setTime(sdf.parse(res.get(0)));
                } catch (ParseException parseException) {
                    Timber.e(parseException);
                    return null;
                }
            }
            return new Date(cal.getTimeInMillis());
        }
        return null;

    }

    public static String getRecGuid(String baseEntityId) {
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "rec_guid");

        String sql = "SELECT rec_guid  from ec_referral " +
                      "WHERE base_entity_id = '" + baseEntityId + "' ";
        List<String> res = readData(sql, dataMap);

        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return null;

    }
}
