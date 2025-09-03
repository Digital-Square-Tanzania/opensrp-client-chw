package org.smartregister.chw.dao;

import org.jetbrains.annotations.Nullable;
import org.smartregister.chw.domain.HpsAnnualCensusListItem;
import org.smartregister.dao.AbstractDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DAO to derive the HPS Annual Census register items from visit/events data.
 * Returns one row per baseEntityId representing the latest annual census visit year.
 */
public class HpsAnnualCensusRegisterDao extends AbstractDao {

    public static List<HpsAnnualCensusListItem> getLatestAnnualCensusItems() {
        // Note: eventType value is provided by HPS module; avoid hardcoding where possible.
        // Using the literal to avoid cross-module compile issues if constants are relocated.
        final String EVENT_TYPE = "HPS Annual Census";

        String sql = "SELECT base_entity_id, MAX(visit_date) AS latestEventDate " +
                "FROM visits WHERE visit_type = '" + EVENT_TYPE + "' " +
                "GROUP BY base_entity_id ORDER BY visit_date DESC";

        DataMap<HpsAnnualCensusListItem> dataMap = c -> {
            String baseEntityId = getCursorValue(c, "base_entity_id");
            String eventDateStr = getCursorValue(c, "latestEventDate");
            String year = deriveYear(eventDateStr);
            return new HpsAnnualCensusListItem(baseEntityId, year);
        };

        List<HpsAnnualCensusListItem> res = readData(sql, dataMap);
        return res != null ? res : new ArrayList<>();
    }

    @Nullable
    private static String deriveYear(@Nullable String eventDateStr) {
        if (eventDateStr == null || eventDateStr.isEmpty()) return "";

        try {
            // Check if it's numeric (possible timestamp in milliseconds)
            if (eventDateStr.matches("\\d+")) {
                long millis = Long.parseLong(eventDateStr);
                // Only consider valid ranges: >1970 and < far future (e.g., year 3000)
                if (millis > 0 && millis < 32503680000000L) { // 01-01-3000
                    return new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date(millis));
                }
            }
        } catch (Exception ignored) { }

        try {
            // Common format stored by EventClientRepository: yyyy-MM-dd HH:mm:ss
            if (eventDateStr.length() >= 4 && Character.isDigit(eventDateStr.charAt(0))) {
                return eventDateStr.substring(0, 4);
            }
        } catch (Exception ignored) { }

        return "";
    }
}

