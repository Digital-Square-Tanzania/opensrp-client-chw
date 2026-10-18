package org.smartregister.chw.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.VisitUtils;

import java.util.ArrayList;
import java.util.List;

/** Category-aware access to current and legacy sectioned follow-up visits. */
public final class MotherMentorFollowUpVisitUtils {
    private MotherMentorFollowUpVisitUtils() {}

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
            for (int i = 0; i < observations.length(); i++) {
                JSONObject observation = observations.optJSONObject(i);
                if (observation != null
                        && DBConstants.KEY.FOLLOW_UP_STATUS.equalsIgnoreCase(
                                observation.optString("fieldCode"))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // A malformed visit must not be counted as a category follow-up.
        }
        return false;
    }
}
