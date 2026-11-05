package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.ATTENDANCE_TYPE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.BASE_ENTITY_ID;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.CREATED_AT;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.EDUCATION_PROVIDED;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.FOLLOW_UP_STATUS;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MOTHERMENTOR_VISIT_DATE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.NEXT_APPOINTMENT_DATE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.PROCESSED;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.REFERRAL_GIVEN;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.UPDATED_AT;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.VISIT_DATE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.VISIT_ID;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.VISIT_JSON;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.VISIT_TYPE;

import android.content.ContentValues;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;
import org.smartregister.chw.mothermentor.util.VisitUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class MotherMentorDailyVisitInstrumentedTest {
    @Test
    public void savedLegacyRepeatLocksOnlyItsServiceDayAfterCompletion() throws Exception {
        String clientId = UUID.randomUUID().toString();
        Calendar day = Calendar.getInstance();
        Date serviceDay = day.getTime();
        day.add(Calendar.DAY_OF_YEAR, 1);
        Date nextDay = day.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        JSONArray observations = new JSONArray()
                .put(observation(ATTENDANCE_TYPE, "repeat"))
                .put(observation(FOLLOW_UP_STATUS, "continuing_services"))
                .put(observation(MOTHERMENTOR_VISIT_DATE, dateFormat.format(serviceDay)))
                .put(observation(HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP, "no"))
                .put(observation(HAS_BEEN_LINKED_TO_IGA_GROUP, "no"))
                .put(observation(EDUCATION_PROVIDED, "no"))
                .put(observation(REFERRAL_GIVEN, "no"))
                .put(observation(NEXT_APPOINTMENT_DATE, dateFormat.format(nextDay)));
        JSONObject event = new JSONObject().put("obs", observations);
        ContentValues saved = new ContentValues();
        saved.put(VISIT_ID, UUID.randomUUID().toString());
        saved.put(BASE_ENTITY_ID, clientId);
        saved.put(VISIT_TYPE, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        saved.put(VISIT_JSON, event.toString());
        saved.put(VISIT_DATE, nextDay.getTime()); // Imported later than the recorded service date.
        saved.put(CREATED_AT, nextDay.getTime());
        saved.put(UPDATED_AT, nextDay.getTime());
        saved.put(PROCESSED, 0);

        SQLiteDatabase database = ChwApplication.getInstance().getRepository().getWritableDatabase();
        database.beginTransaction();
        try {
            database.insertOrThrow(VisitRepository.VISIT_TABLE, null, saved);
            assertEquals(1, VisitUtils.getVisitsOnly(
                    clientId, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES).size());
            assertFalse(locked(clientId, serviceDay));

            ContentValues processed = new ContentValues();
            processed.put(PROCESSED, 1);
            assertEquals(1, database.update(VisitRepository.VISIT_TABLE, processed, VISIT_ID + " = ?",
                    new String[] {saved.getAsString(VISIT_ID)}));
            assertTrue(locked(clientId, serviceDay));
            assertFalse(locked(clientId, nextDay));
            assertFalse(MotherMentorVisitCompletion.isComplete(observations));

            JSONArray stopped = new JSONArray()
                    .put(observation(ATTENDANCE_TYPE, "repeat"))
                    .put(observation(FOLLOW_UP_STATUS, "stopped_services"))
                    .put(observation(MOTHERMENTOR_VISIT_DATE, dateFormat.format(serviceDay)));
            ContentValues olderOutcome = new ContentValues();
            olderOutcome.put(VISIT_JSON, new JSONObject().put("obs", stopped).toString());
            assertEquals(1, database.update(VisitRepository.VISIT_TABLE, olderOutcome, VISIT_ID + " = ?",
                    new String[] {saved.getAsString(VISIT_ID)}));
            assertTrue(locked(clientId, serviceDay));
            assertFalse(MotherMentorVisitCompletion.isComplete(stopped));
        } finally {
            database.endTransaction();
        }
        assertTrue(VisitUtils.getVisitsOnly(clientId, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES)
                .isEmpty());
    }

    private static boolean locked(String clientId, Date day) {
        return MotherMentorDailyVisitUtils.hasCompletedServiceVisitToday(
                VisitUtils.getVisitsOnly(clientId, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES), day);
    }

    private static JSONObject observation(String field, String value) throws Exception {
        return new JSONObject().put("fieldCode", field).put("values", new JSONArray().put(value));
    }
}
