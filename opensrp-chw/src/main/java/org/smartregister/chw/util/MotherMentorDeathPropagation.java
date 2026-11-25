package org.smartregister.chw.util;

import android.content.Context;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.dao.MotherMentorChildEidDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherDeliveryRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants.KEY;
import org.smartregister.chw.mothermentor.util.MotherMentorDeceasedVisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorDeliveryChildren;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorHeiSchedule;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;
import org.smartregister.domain.Event;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public final class MotherMentorDeathPropagation {
    private MotherMentorDeathPropagation() {}

    public static void process(Context context, Event event) throws Exception {
        String type = event.getEventType();
        if (!isSupportedVisit(type)) return;
        if (StringUtils.isAnyBlank(event.getBaseEntityId(), event.getFormSubmissionId())) return;

        String json = JsonFormUtils.gson.toJson(event);
        JSONObject visit = new JSONObject(json);
        if (visit.optBoolean(KEY.EVENT_VOIDED)) return;
        boolean clientDeath = MotherMentorDeceasedVisitUtils.isDeceased(json);
        Map<String, String> answers = MotherMentorVisitCompletion.observationValues(visit.optJSONArray("obs"));
        boolean maternalChildDeath = Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(type)
                && hasMaternalChildDeath(answers)
                && MotherMentorMotherDeliveryRepository.isMotherEvent(event);
        if (!maternalChildDeath && !clientDeath) return;

        SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
        LocalDate day = recordedDay(event, json);
        boolean childEid = MotherMentorFollowUpType.CHILD_EID.getEventType().equals(type);
        db.beginTransaction();
        try {
            if (clientDeath && day != null) {
                record(context, db, event, event.getBaseEntityId(), day, childEid);
            }
            if (maternalChildDeath) {
                recordLinkedChildDeaths(context, db, event, answers, day);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static boolean isSupportedVisit(String type) {
        return Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(type)
                || MotherMentorFollowUpType.CHILD_EID.getEventType().equals(type)
                || Constants.EVENT_TYPE.MOTHER_MENTOR_RECORD_VISIT.equals(type)
                || MotherMentorFollowUpType.MOTHER_IIT.getEventType().equals(type)
                || MotherMentorFollowUpType.PARTNER.getEventType().equals(type);
    }

    private static LocalDate recordedDay(Event event, String visitJson) {
        Date recordedDate = MotherMentorDeceasedVisitUtils.getRecordedDate(visitJson,
                event.getEventDate() == null ? null : Long.toString(event.getEventDate().getMillis()));
        return recordedDate == null ? null : LocalDate.fromDateFields(recordedDate);
    }

    private static void recordLinkedChildDeaths(Context context, SQLiteDatabase db, Event event,
                                                 Map<String, String> answers, LocalDate day) throws Exception {
        for (Map.Entry<String, LocalDate> death : MotherMentorDeceasedVisitUtils
                .linkedChildDeaths(db, event.getBaseEntityId(), answers, day).entrySet()) {
            record(context, db, event, death.getKey(), death.getValue(), true);
        }
    }

    private static boolean hasMaternalChildDeath(Map<String, String> answers) {
        if ("deceased".equals(answers.get(KEY.MAMA_CHILD_STATUS))) return true;
        for (int child = 1; child <= MotherMentorDeliveryChildren.MAX_COUNT; child++) {
            if ("deceased".equals(answers.get(
                    MotherMentorDeliveryChildren.observationKey(child, KEY.MAMA_CHILD_STATUS)))) return true;
        }
        return false;
    }

    private static void record(Context context, SQLiteDatabase db, Event visit, String personId,
                               LocalDate date, boolean child) throws Exception {
        Map<String, String> person = MotherMentorChildEidDao.person(db, personId);
        if (person.isEmpty() || MotherMentorDao.hasRecordedDeath(personId)) return;
        LocalDate dob = MotherMentorHeiSchedule.parseBirthDate(person.get(KEY.DOB));
        if (!MotherMentorHeiSchedule.validDate(date, dob, LocalDate.now())) return;

        String source = visit.getFormSubmissionId() + ":death:" + personId;
        String removalId = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
        MarkClientAsDeceasedHelper.markAsDeceased(context, personId, person.get("first_name"),
                person.get("middle_name"), person.get("last_name"), person.get("gender"),
                date.toString(), child, removalId);
        if (!MotherMentorDao.hasRecordedDeath(personId)) {
            throw new IllegalStateException("Unable to project the shared death event");
        }
    }
}
