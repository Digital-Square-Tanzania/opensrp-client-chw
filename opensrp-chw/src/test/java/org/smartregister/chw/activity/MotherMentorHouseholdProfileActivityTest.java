package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MotherMentorHouseholdProfileActivityTest extends BaseUnitTest {
    @Test
    public void completedHouseholdVisitDisablesButtonOnRecreatedProfile() throws Exception {
        Visit visit = householdVisit();
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(visit)) {
            MotherMentorHouseholdProfileActivity first = householdProfile();
            first.setupButtons();
            assertFalse(recordButton(first).isEnabled());
            assertEquals(0.5f, recordButton(first).getAlpha(), 0f);

            MotherMentorHouseholdProfileActivity recreated = householdProfile();
            recreated.setupButtons();
            assertFalse(recordButton(recreated).isEnabled());
            recreated.openRecordClientVisit();
            assertNull(Shadows.shadowOf(recreated).getNextStartedActivityForResult());
        }
    }

    @Test
    public void staleButtonAndAlternateEntryPointsCannotStartSameDayDuplicate() throws Exception {
        List<Visit> visits = new ArrayList<>();
        try (MockedStatic<MotherMentorLibrary> mocked = Mockito.mockStatic(MotherMentorLibrary.class)) {
            MotherMentorLibrary library = Mockito.mock(MotherMentorLibrary.class);
            VisitRepository repository = Mockito.mock(VisitRepository.class);
            mocked.when(MotherMentorLibrary::getInstance).thenReturn(library);
            Mockito.when(library.visitRepository()).thenReturn(repository);
            Mockito.when(
                            repository.getAllVisitsProcessed(
                                    Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES,
                                    "test-household-id"))
                    .thenReturn(visits);
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            activity.setupButtons();
            assertTrue(recordButton(activity).isEnabled());

            visits.add(householdVisit());
            activity.openRecordClientVisit();
            activity.openFollowupVisit();
            activity.openRecordTbContactVisit();
            activity.openTbContactFollowUpVisit();
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
            assertFalse(recordButton(activity).isEnabled());
        }
    }

    @Test
    public void startButtonReopensOnNextCalendarDay() throws Exception {
        Visit visit = householdVisit();
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(visit)) {
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            activity.setupButtons();
            assertFalse(recordButton(activity).isEnabled());

            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            ((DailyHouseholdProfileActivity) activity).now = tomorrow.getTime();
            activity.setupButtons();
            assertTrue(recordButton(activity).isEnabled());
            assertEquals(1f, recordButton(activity).getAlpha(), 0f);
            activity.openRecordClientVisit();
            Intent intent = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
            assertEquals(
                    MotherMentorHouseholdVisitActivity.class.getName(),
                    intent.getComponent().getClassName());
            assertFalse(intent.getBooleanExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, true));
        }
    }

    @Test
    public void pendingVisitRetainsContinueAndStaleStartResumesIt() throws Exception {
        Visit visit = householdVisit();
        visit.setProcessed(false);
        visit.setVisitId("saved-household-draft");
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(visit)) {
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            activity.setupButtons();
            assertEquals(View.GONE, recordButton(activity).getVisibility());
            RelativeLayout progress =
                    ReflectionHelpers.getField(activity, "mothermentorServiceInProgress");
            assertEquals(View.VISIBLE, progress.getVisibility());
            activity.openRecordClientVisit();
            Intent intent = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
            assertTrue(intent.getBooleanExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, false));
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    public void profileLeftOpenRefreshesAtLocalMidnight() throws Exception {
        Visit visit = householdVisit();
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(visit)) {
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            Calendar endOfDay = Calendar.getInstance();
            endOfDay.set(Calendar.HOUR_OF_DAY, 23);
            endOfDay.set(Calendar.MINUTE, 59);
            endOfDay.set(Calendar.SECOND, 59);
            endOfDay.set(Calendar.MILLISECOND, 0);
            ((DailyHouseholdProfileActivity) activity).now = endOfDay.getTime();
            activity.setupButtons();
            assertFalse(recordButton(activity).isEnabled());
            ReflectionHelpers.callInstanceMethod(activity, "scheduleVisitAvailabilityRefresh");

            endOfDay.add(Calendar.SECOND, 1);
            ((DailyHouseholdProfileActivity) activity).now = endOfDay.getTime();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
            assertTrue(recordButton(activity).isEnabled());
        }
    }

    private MockedStatic<MotherMentorLibrary> persistedVisits(Visit visit) {
        MockedStatic<MotherMentorLibrary> mocked = Mockito.mockStatic(MotherMentorLibrary.class);
        MotherMentorLibrary library = Mockito.mock(MotherMentorLibrary.class);
        VisitRepository repository = Mockito.mock(VisitRepository.class);
        mocked.when(MotherMentorLibrary::getInstance).thenReturn(library);
        Mockito.when(library.visitRepository()).thenReturn(repository);
        Mockito.when(
                        repository.getAllVisitsProcessed(
                                Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES,
                                "test-household-id"))
                .thenReturn(Collections.singletonList(visit));
        Mockito.when(
                        repository.getLatestVisit(
                                "test-household-id", Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES))
                .thenReturn(visit);
        return mocked;
    }

    private MotherMentorHouseholdProfileActivity householdProfile() {
        MotherMentorHouseholdProfileActivity activity =
                Robolectric.buildActivity(DailyHouseholdProfileActivity.class).get();
        MemberObject member = new MemberObject();
        member.setBaseEntityId("test-household-id");
        ReflectionHelpers.setField(activity, "memberObject", member);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", new TextView(activity));
        ReflectionHelpers.setField(activity, "manualProcessVisit", new TextView(activity));
        ReflectionHelpers.setField(activity, "rlLastVisit", new RelativeLayout(activity));
        ReflectionHelpers.setField(
                activity, "mothermentorServiceInProgress", new RelativeLayout(activity));
        return activity;
    }

    public static class DailyHouseholdProfileActivity extends MotherMentorHouseholdProfileActivity {
        Date now = new Date();

        Date getVisitAvailabilityDate() {
            return now;
        }

        @Override
        public void refreshMedicalHistory(boolean hasHistory) {
            // The tests exercise service controls without loading unrelated history views.
        }
    }

    private TextView recordButton(MotherMentorHouseholdProfileActivity activity) {
        return ReflectionHelpers.getField(activity, "textViewRecordMotherMentor");
    }

    private Visit householdVisit() throws Exception {
        JSONArray observations = new JSONArray();
        String[] fields = {
            DBConstants.KEY.PURPOSE_OF_VISIT, "education",
            DBConstants.KEY.PARTICIPANTS, "mother",
            DBConstants.KEY.TOPICS_TAUGHT, "hiv_prevention",
            DBConstants.KEY.COMMENTS, "Household services provided",
            DBConstants.KEY.MOTHERMENTOR_VISIT_DATE,
                    new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date())
        };
        for (int i = 0; i < fields.length; i += 2) {
            observations.put(
                    new JSONObject()
                            .put("fieldCode", fields[i])
                            .put("values", new JSONArray().put(fields[i + 1])));
        }
        Visit visit = new Visit();
        visit.setVisitType(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        visit.setProcessed(true);
        visit.setDate(new Date());
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return visit;
    }
}
