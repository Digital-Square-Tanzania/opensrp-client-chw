package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.dao.AbstractDao;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NcdDao extends AbstractDao {

    private static final String DIABETES_SCREENING_EVENT_TYPE = "Diabetes and Hypertension Screening";
    private static final String COLUMN_DIABETES_RESULT = "diabetes_result";
    private static final String COLUMN_HYPERTENSION_RESULT = "hypertension_result";

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

    public static ConfirmationStatus getLatestConfirmationStatus(String baseEntityId) {
        ConfirmationStatus emptyStatus = ConfirmationStatus.empty();
        if (StringUtils.isBlank(baseEntityId)) {
            return emptyStatus;
        }

        String sql = String.format(Locale.US,
                "SELECT %s, %s FROM %s WHERE base_entity_id = '%s' " +
                        "ORDER BY COALESCE(last_interacted_with, visit_date) DESC LIMIT 1",
                COLUMN_DIABETES_RESULT,
                COLUMN_HYPERTENSION_RESULT,
                Constants.TABLES.DIABETES_HYPERTENSION_CONFIRMATION,
                baseEntityId
        );

        DataMap<ConfirmationStatus> dataMap = cursor -> new ConfirmationStatus(
                getCursorValue(cursor, COLUMN_DIABETES_RESULT),
                getCursorValue(cursor, COLUMN_HYPERTENSION_RESULT)
        );

        List<ConfirmationStatus> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty()) ? results.get(0) : emptyStatus;
    }

    public static class ConfirmationStatus {
        private final String diabetesResult;
        private final String hypertensionResult;

        ConfirmationStatus(String diabetesResult, String hypertensionResult) {
            this.diabetesResult = diabetesResult;
            this.hypertensionResult = hypertensionResult;
        }

        public boolean isDiabetesPositive() {
            return isPositive(diabetesResult);
        }

        public boolean isHypertensionPositive() {
            return isPositive(hypertensionResult);
        }

        public boolean isEmpty() {
            return StringUtils.isBlank(diabetesResult) && StringUtils.isBlank(hypertensionResult);
        }

        public static ConfirmationStatus empty() {
            return new ConfirmationStatus(null, null);
        }

        public static boolean isPositive(String result) {
            if (StringUtils.isBlank(result)) {
                return false;
            }
            String normalized = result.trim().toLowerCase(Locale.US);
            return normalized.equals("positive")
                    || normalized.equals("confirmed")
                    || normalized.equals("yes")
                    || normalized.equals("true");
        }
    }
}
