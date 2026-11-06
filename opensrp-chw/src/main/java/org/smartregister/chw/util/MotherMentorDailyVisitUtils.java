package org.smartregister.chw.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherVisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Allows one completed Mother Mentor service visit per client and local calendar day. */
public final class MotherMentorDailyVisitUtils {
    private MotherMentorDailyVisitUtils() {}

    public static boolean hasCompletedServiceVisitToday(List<Visit> visits, Date now) {
        for (Visit visit : visits) {
            if (visit == null
                    || !Boolean.TRUE.equals(visit.getProcessed())
                    || !Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(visit.getVisitType())) {
                continue;
            }
            try {
                JSONObject event = new JSONObject(visit.getJson());
                JSONArray observations = event.optJSONArray("obs");
                Map<String, String> values =
                        MotherMentorVisitCompletion.observationValues(observations);
                if (!MotherMentorVisitCompletion.isComplete(observations)
                        && !isCompleteLegacyRepeatVisit(values)) continue;
                Date visitDate = getVisitDate(visit, event, values);
                if (visitDate != null && isSameDay(visitDate, now)) return true;
            } catch (Exception ignored) {
                // An incomplete or malformed record must not block a valid service visit.
            }
        }
        return false;
    }

    private static boolean isCompleteLegacyRepeatVisit(Map<String, String> values) {
        String attendance = StringUtils.trimToEmpty(values.get(DBConstants.KEY.ATTENDANCE_TYPE));
        if (!("repeat".equalsIgnoreCase(attendance) || "marudio".equalsIgnoreCase(attendance))
                || StringUtils.isNotBlank(values.get(DBConstants.KEY.FOLLOW_UP_CATEGORY))) return false;

        switch (StringUtils.trimToEmpty(values.get(DBConstants.KEY.FOLLOW_UP_STATUS))
                .toLowerCase(Locale.ROOT)) {
            case "continuing_services":
            case "continuing services":
            case "anaendelea na huduma":
                // Reuse the service sections for a found repeat visit. The old entry did not
                // include the clinical questions now required by the submission validator.
                values.put(DBConstants.KEY.ATTENDANCE_TYPE, "repeat");
                values.put(DBConstants.KEY.FOLLOW_UP_STATUS, "found");
                return MotherMentorMotherVisitUtils.isComplete(values);
            case "deceased":
            case "lost_to_follow_up":
            case "relocated":
            case "transferred_to_another_facility":
            case "stopped_services":
            case "not_available":
                return true; // The old form skipped service sections for these outcomes.
            default:
                return false;
        }
    }

    private static Date getVisitDate(Visit visit, JSONObject event, Map<String, String> values) {
        String recordedDate = values.get(DBConstants.KEY.MOTHERMENTOR_VISIT_DATE);
        if (recordedDate != null && !recordedDate.trim().isEmpty()) {
            for (String pattern : new String[] {"dd-MM-yyyy", "yyyy-MM-dd"}) {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                ParsePosition position = new ParsePosition(0);
                Date date = format.parse(recordedDate, position);
                if (date != null && position.getIndex() == recordedDate.length()) return date;
            }
            return null;
        }
        // Imported visits are stamped with their import time in the visit table. Prefer the
        // original event date when an older event has no explicit visit-date observation.
        Object eventDate = event.opt("eventDate");
        if (eventDate instanceof Number) return new Date(((Number) eventDate).longValue());
        if (eventDate instanceof String && !((String) eventDate).isEmpty()) {
            try {
                return new DateTime(eventDate).toDate();
            } catch (IllegalArgumentException ignored) {
                // Older local visits can have only the persisted visit timestamp.
            }
        }
        return visit.getDate();
    }

    private static boolean isSameDay(Date visitDate, Date now) {
        Calendar visitDay = Calendar.getInstance();
        visitDay.setTime(visitDate);
        Calendar today = Calendar.getInstance();
        today.setTime(now);
        return visitDay.get(Calendar.ERA) == today.get(Calendar.ERA)
                && visitDay.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && visitDay.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }
}
