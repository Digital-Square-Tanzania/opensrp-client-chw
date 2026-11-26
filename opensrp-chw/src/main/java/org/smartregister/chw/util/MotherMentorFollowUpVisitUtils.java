package org.smartregister.chw.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.mothermentor.dao.MotherMentorMotherVisitDao;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.VisitUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Category-aware access to current and legacy sectioned follow-up visits. */
public final class MotherMentorFollowUpVisitUtils {
    private MotherMentorFollowUpVisitUtils() {}

    public static boolean hasCompletedIitVisitToday(String baseEntityId, Date now) {
        if (StringUtils.isBlank(baseEntityId) || now == null) return false;
        LocalDate today = new LocalDate(now);
        for (Visit visit : VisitUtils.getVisitsOnly(
                baseEntityId, MotherMentorFollowUpType.MOTHER_IIT.getEventType())) {
            if (visit == null || !Boolean.TRUE.equals(visit.getProcessed())
                    || StringUtils.isBlank(visit.getJson())
                    || !today.equals(MotherMentorMotherVisitDao.getServiceDate(visit))) continue;
            try {
                if (!new JSONObject(visit.getJson()).optBoolean(DBConstants.KEY.EVENT_VOIDED)) return true;
            } catch (JSONException ignored) {
                // An invalid event must not block a new follow-up.
            }
        }
        return false;
    }

    public static List<Visit> getVisits(String baseEntityId, MotherMentorFollowUpType type) {
        List<Visit> visits =
                new ArrayList<>(VisitUtils.getVisitsOnly(baseEntityId, type.getEventType()));
        if (!Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(type.getEventType())) {
            for (Visit legacyVisit :
                    VisitUtils.getVisitsOnly(
                            baseEntityId, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES)) {
                if (isSectionedFollowUp(legacyVisit)) visits.add(legacyVisit);
            }
        }
        return visits;
    }

    static boolean isSectionedFollowUp(Visit visit) {
        if (visit == null || visit.getJson() == null) return false;
        try {
            JSONArray observations = new JSONObject(visit.getJson()).optJSONArray("obs");
            if (observations == null) return false;
            boolean hasFollowUpStatus = false;
            for (int i = 0; i < observations.length(); i++) {
                JSONObject observation = observations.optJSONObject(i);
                if (observation == null) continue;
                String field = observation.optString("fieldCode");
                if (DBConstants.KEY.ATTENDANCE_TYPE.equalsIgnoreCase(field)) return false;
                if (DBConstants.KEY.FOLLOW_UP_STATUS.equalsIgnoreCase(field)) {
                    hasFollowUpStatus = true;
                }
            }
            return hasFollowUpStatus;
        } catch (Exception ignored) {
            // A malformed visit must not be counted as a category follow-up.
        }
        return false;
    }
}
