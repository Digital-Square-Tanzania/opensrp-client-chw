package org.smartregister.chw.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorDailyVisitUtilsTest {
    private TimeZone previousZone;

    @Before
    public void useLocalTimeZone() {
        previousZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
    }

    @After
    public void restoreTimeZone() {
        TimeZone.setDefault(previousZone);
    }

    @Test
    public void completedVisitLocksOnlyItsLocalCalendarDay() throws Exception {
        Visit visit = completedVisit("21-10-2026");
        assertTrue(locked(visit, "2026-10-21 23:59:59"));
        assertFalse(locked(visit, "2026-10-22 00:00:00"));
        assertFalse(locked(visit, "2026-10-20 23:59:59"));
    }

    @Test
    public void midnightResetDoesNotWaitTwentyFourHours() throws Exception {
        Visit visit = completedVisit("21-10-2026");
        visit.setDate(date("2026-10-21 23:59:59"));
        assertFalse(locked(visit, "2026-10-22 00:00:01"));
    }

    @Test
    public void savedDraftDoesNotCountAsCompletedVisit() throws Exception {
        Visit visit = completedVisit("21-10-2026");
        visit.setProcessed(false);
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
        visit.setProcessed(null);
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
    }

    @Test
    public void automaticallyProcessedIncompleteDraftDoesNotBlockServices() throws Exception {
        Visit visit = completedVisit("21-10-2026");
        visit.setJson(new JSONObject().put("obs", new JSONArray()).toString());
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
    }

    @Test
    public void oldVisitImportedTodayUsesRecordedServiceDate() throws Exception {
        Visit visit = completedVisit("20-10-2026");
        visit.setDate(date("2026-10-21 12:00:00"));
        visit.setCreatedAt(date("2026-10-21 12:00:00"));
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
    }

    @Test
    public void legacyImportedVisitUsesOriginalEventDate() throws Exception {
        Visit visit = completedVisit(null);
        visit.setDate(date("2026-10-21 12:00:00"));
        JSONObject event = new JSONObject(visit.getJson());
        event.put("eventDate", "2026-10-20T20:00:00.000Z");
        visit.setJson(event.toString());
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
        event.put("eventDate", "2026-10-20T22:00:00.000Z");
        visit.setJson(event.toString());
        assertTrue(locked(visit, "2026-10-21 12:00:00"));
    }

    @Test
    public void legacyLocalVisitUsesPersistedTimestampInCurrentTimeZone() throws Exception {
        Visit visit = completedVisit(null);
        visit.setDate(date("2026-10-21 00:01:00"));
        assertTrue(locked(visit, "2026-10-21 12:00:00"));
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
    }

    @Test
    public void unrelatedVisitsCannotLockPrimaryServiceVisit() throws Exception {
        Visit visit = completedVisit("21-10-2026");
        visit.setVisitType(Constants.EVENT_TYPE.MOTHER_MENTOR_FOLLOW_UP_VISIT);
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
    }

    @Test
    public void allSavedVisitsAreCheckedRatherThanOnlyLatestImport() throws Exception {
        Visit yesterday = completedVisit("20-10-2026");
        Visit today = completedVisit("21-10-2026");
        Visit future = completedVisit("22-10-2026");
        assertTrue(
                MotherMentorDailyVisitUtils.hasCompletedServiceVisitToday(
                        Arrays.asList(yesterday, today, future), date("2026-10-21 12:00:00")));
    }

    @Test
    public void malformedRecordedDateDoesNotCreateLockFromImportTimestamp() throws Exception {
        Visit visit = completedVisit("32-10-2026");
        visit.setDate(date("2026-10-21 12:00:00"));
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
        visit.setJson("invalid json");
        assertFalse(locked(visit, "2026-10-21 12:00:00"));
    }

    private boolean locked(Visit visit, String now) throws Exception {
        return MotherMentorDailyVisitUtils.hasCompletedServiceVisitToday(
                Collections.singletonList(visit), date(now));
    }

    private Date date(String value) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(value);
    }

    private Visit completedVisit(String recordedDate) throws Exception {
        JSONArray observations = new JSONArray();
        String[] fields = {
            DBConstants.KEY.ATTENDANCE_TYPE, "new",
            DBConstants.KEY.COMMENTS, "Services provided",
            DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP, "no",
            DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP, "no",
            DBConstants.KEY.EDUCATION_PROVIDED, "no",
            DBConstants.KEY.REFERRAL_GIVEN, "no",
            DBConstants.KEY.NEXT_APPOINTMENT_DATE, "21-11-2026"
        };
        for (int i = 0; i < fields.length; i += 2) {
            observations.put(
                    new JSONObject()
                            .put("fieldCode", fields[i])
                            .put("values", new JSONArray().put(fields[i + 1])));
        }
        if (recordedDate != null) {
            observations.put(
                    new JSONObject()
                            .put("fieldCode", DBConstants.KEY.MOTHERMENTOR_VISIT_DATE)
                            .put("values", new JSONArray().put(recordedDate)));
        }
        Visit visit = new Visit();
        visit.setVisitType(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        visit.setProcessed(true);
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return visit;
    }
}
