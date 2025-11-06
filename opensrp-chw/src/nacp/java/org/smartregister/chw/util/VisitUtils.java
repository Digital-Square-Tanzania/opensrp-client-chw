package org.smartregister.chw.util;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.Collections;
import java.util.List;

public class VisitUtils {

    private static final DateTimeFormatter LMP_DATE_FORMAT = DateTimeFormat.forPattern("dd-MM-yyyy");
    private static final DateTimeFormatter VISIT_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CREATED_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static boolean isFirstVisit(final MemberObject member) {
        int gaWeeks = member.getGestationAge();
        return gaWeeks < 16 && isVisitInRangeWithoutECD(member, 0, 16);
    }

    public static boolean isSecondVisit(final MemberObject member) {
        int gaWeeks = member.getGestationAge();
        return gaWeeks >= 16 && gaWeeks <= 24 && isVisitInRangeWithoutECD(member, 16, 24);
    }

    public static boolean isThirdVisit(final MemberObject member) {
        return member.getGestationAge() >= 32;
    }

    public static List<Visit> getPrevVisits(MemberObject member) {
        if (member == null) return Collections.emptyList();

        List<Visit> visits = AncLibrary.getInstance()
                .visitRepository()
                .getVisits(member.getBaseEntityId(), CoreConstants.EventType.ANC_HOME_VISIT);

        return visits != null ? visits : Collections.emptyList();
    }

    private static boolean isVisitInRangeWithoutECD(MemberObject member, int minWeeks, int maxWeeks) {
        List<Visit> visits = getPrevVisits(member);
        if (visits.isEmpty()) return true;

        LocalDate lmpDate = parseLmpDate(member.getLastMenstrualPeriod());
        if (lmpDate == null) return true;

        for (Visit visit : visits) {
            LocalDate visitDate = getVisitDate(visit, member.getDateCreated());
            if (visitDate == null) continue;

            int weeksSinceLMP = Days.daysBetween(lmpDate, visitDate).getDays() / 7;
            if (weeksSinceLMP >= minWeeks && weeksSinceLMP <= maxWeeks) {
                if (containsECDField(visit)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static LocalDate parseLmpDate(String lmp) {
        try {
            return LMP_DATE_FORMAT.parseLocalDate(lmp);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDate getVisitDate(Visit visit, String fallbackDate) {
        try {
            if (visit != null && visit.getDate() != null) {
                String formatted = VISIT_DATE_FORMAT.print(new LocalDate(visit.getDate()));
                return VISIT_DATE_FORMAT.parseLocalDate(formatted);
            } else {
                return CREATED_DATE_FORMAT.parseLocalDate(fallbackDate);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean containsECDField(Visit visit) {
        if (visit == null || visit.getJson() == null) return false;

        try {
            JSONObject visitJson = new JSONObject(visit.getJson());
            JSONArray obsArray = visitJson.optJSONArray("obs");
            if (obsArray == null) return false;

            for (int i = 0; i < obsArray.length(); i++) {
                JSONObject obsItem = obsArray.optJSONObject(i);
                if (obsItem == null) continue;

                String fieldCode = obsItem.optString("fieldCode", "");
                String formField = obsItem.optString("formSubmissionField", "");

                if (isECDField(fieldCode) || isECDField(formField)) {
                    return true;
                }
            }
        } catch (JSONException e) {}

        return false;
    }

    private static boolean isECDField(String field) {
        if (field == null) return false;

        switch (field.toLowerCase()) {
            case "partner_head_of_household":
            case "partner_head_of_household_stay_visit":
            case "partner_head_of_households_interaction":
                return true;
            default:
                return false;
        }
    }
}