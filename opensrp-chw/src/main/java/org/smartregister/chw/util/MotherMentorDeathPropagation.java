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
        boolean maternal = MotherMentorMotherDeliveryRepository.isMotherEvent(event);
        boolean childEid = MotherMentorFollowUpType.CHILD_EID.getEventType().equals(type);
        if (!maternal && !childEid
                && !Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(type)
                && !Constants.EVENT_TYPE.MOTHER_MENTOR_RECORD_VISIT.equals(type)
                && !MotherMentorFollowUpType.MOTHER_IIT.getEventType().equals(type)
                && !MotherMentorFollowUpType.PARTNER.getEventType().equals(type)) return;
        if (StringUtils.isAnyBlank(event.getBaseEntityId(), event.getFormSubmissionId())) return;

        String json = JsonFormUtils.gson.toJson(event);
        JSONObject visit = new JSONObject(json);
        if (visit.optBoolean(KEY.EVENT_VOIDED)) return;
        boolean clientDeath = MotherMentorDeceasedVisitUtils.isDeceased(json);
        if (!maternal && !clientDeath) return;

        SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
        Map<String, String> answers = MotherMentorVisitCompletion.observationValues(visit.optJSONArray("obs"));
        Date recordedDate = MotherMentorDeceasedVisitUtils.getRecordedDate(json,
                event.getEventDate() == null ? null : Long.toString(event.getEventDate().getMillis()));
        LocalDate day = recordedDate == null ? null : LocalDate.fromDateFields(recordedDate);
        db.beginTransaction();
        try {
            if (clientDeath && day != null) {
                record(context, db, event, event.getBaseEntityId(), day, childEid);
            }
            if (maternal) {
                for (Map.Entry<String, LocalDate> death : MotherMentorDeceasedVisitUtils
                        .linkedChildDeaths(db, event.getBaseEntityId(), answers, day).entrySet()) {
                    record(context, db, event, death.getKey(), death.getValue(), true);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
