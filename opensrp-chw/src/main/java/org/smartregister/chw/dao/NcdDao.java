package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.dao.AbstractDao;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NcdDao extends AbstractDao {

    private static final String DIABETES_SCREENING_EVENT_TYPE = "Diabetes and Hypertension Screening";

    private NcdDao() {
        // no-op
    }

    public static Date getLastDiabetesScreeningDate(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        String sql = String.format(Locale.US,
                "SELECT eventDate FROM event WHERE eventType = '%s' AND baseEntityId = '%s' ORDER BY eventDate DESC LIMIT 1",
                DIABETES_SCREENING_EVENT_TYPE,
                baseEntityId
        );

        DataMap<Date> dataMap = cursor -> getCursorValueAsDate(cursor, "eventDate", getDobDateFormat());
        List<Date> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }
}
