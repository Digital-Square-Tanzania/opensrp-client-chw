package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.repository.MotherMentorHouseholdClosureRepository;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorHouseholdClosure;
import org.smartregister.repository.AllSharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MotherMentorHouseholdProfileActivityTest extends BaseUnitTest {
    private MockedStatic<MotherMentorHouseholdClosureRepository> closureRepository;
    private MotherMentorHouseholdClosure closure = MotherMentorHouseholdClosure.active();
    private boolean currentEnrollment = true;

    @Before
    public void configureHouseholdClosure() {
        String language = RuntimeEnvironment.application.getResources()
                .getConfiguration().locale.getLanguage();
        new AllSharedPreferences(PreferenceManager.getDefaultSharedPreferences(
                RuntimeEnvironment.application)).saveLanguagePreference(language);
        closureRepository = Mockito.mockStatic(MotherMentorHouseholdClosureRepository.class);
        closureRepository.when(() -> MotherMentorHouseholdClosureRepository.getState(
                Mockito.any(), Mockito.anyString(), Mockito.isNull()))
                .thenAnswer(call -> closure);
        closureRepository.when(() -> MotherMentorHouseholdClosureRepository.belongsToCurrentEnrollment(
                Mockito.any(), Mockito.anyString(), Mockito.any()))
                .thenAnswer(call -> currentEnrollment);
    }

    @After
    public void closeHouseholdClosureFixture() {
        closureRepository.close();
    }

    @Test
    public void closureDisablesAllVisitEntryPointsAndPreservesHistoryAvailability() throws Exception {
        Visit visit = householdVisit();
        visit.setProcessed(false);
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(visit)) {
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            TextView continueVisit = new TextView(activity);
            TextView edit = new TextView(activity);
            RelativeLayout inheritedProgress = new RelativeLayout(activity);
            ReflectionHelpers.setField(activity, "textViewContinueMotherMentorService", continueVisit);
            ReflectionHelpers.setField(activity, "textViewVisitDoneEdit", edit);
            ReflectionHelpers.setField(activity, "visitInProgress", inheritedProgress);
            closure = MotherMentorHouseholdClosure.active()
                    .advance("repeat", "not_found", "moved", "2026-11-01");
            activity.setupButtons();
            assertFalse(recordButton(activity).isEnabled());
            assertEquals(View.GONE, continueVisit.getVisibility());
            assertEquals(View.GONE, edit.getVisibility());
            assertEquals(View.GONE, inheritedProgress.getVisibility());
            TextView manual = ReflectionHelpers.getField(activity, "manualProcessVisit");
            assertEquals(View.GONE, manual.getVisibility());
            activity.openRecordClientVisit();
            activity.openFollowupVisit();
            activity.openRecordTbContactVisit();
            activity.openTbContactFollowUpVisit();
            activity.continueService();
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
        }
    }

    @Test
    public void staleActiveControlsRecheckClosureAndReenrollmentDoesNotResumeOldDraft()
            throws Exception {
        Visit visit = householdVisit();
        visit.setProcessed(false);
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(visit)) {
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            activity.setupButtons();
            closure = MotherMentorHouseholdClosure.active()
                    .advance("repeat", "not_found", "moved", "2026-11-01");
            activity.openRecordClientVisit();
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
            assertFalse(recordButton(activity).isEnabled());
            closure = MotherMentorHouseholdClosure.active();
            currentEnrollment = false;
            activity.setupButtons();
            assertTrue(recordButton(activity).isEnabled());
            assertEquals(View.VISIBLE, recordButton(activity).getVisibility());
            RelativeLayout progress = ReflectionHelpers.getField(activity, "mothermentorServiceInProgress");
            assertEquals(View.GONE, progress.getVisibility());
            activity.openRecordClientVisit();
            Intent intent = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
            assertFalse(intent.getBooleanExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, true));
        }
    }

    @Test
    public void directLaunchAlsoRejectsClosedNewAndEditVisits() {
        MotherMentorHouseholdProfileActivity activity = householdProfile();
        closure = MotherMentorHouseholdClosure.active()
                .advance("repeat", "not_found", "moved", "2026-11-01");
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(
                activity, "test-household-id", false);
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(
                activity, "test-household-id", true);
        assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
    }

    @Test
    @Config(qualifiers = "en")
    public void englishClosedBannerShowsReasonAndDateAndHidesAfterReenrollment() throws Exception {
        assertClosedBanner("Household record is closed", "Moved from area");
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliClosedBannerShowsReasonAndDateAndHidesAfterReenrollment() throws Exception {
        assertClosedBanner("Faili la Kaya limefungwa", "Imehama eneo");
    }

    private void assertClosedBanner(String expectedMessage, String expectedReason) throws Exception {
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(householdVisit())) {
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            TextView banner = new TextView(activity);
            banner.setId(org.smartregister.chw.mothermentor.R.id.mothermentor_household_closed_banner);
            activity.setContentView(banner);
            closure = MotherMentorHouseholdClosure.active()
                    .advance("repeat", "not_found", "moved", "2026-11-01");
            activity.setupButtons();
            assertEquals(View.VISIBLE, banner.getVisibility());
            assertTrue(banner.getText().toString(), banner.getText().toString().contains(expectedMessage));
            assertTrue(banner.getText().toString(), banner.getText().toString().contains(expectedReason));
            assertTrue(banner.getText().toString().contains("2026-11-01"));
            closure = MotherMentorHouseholdClosure.active();
            activity.setupButtons();
            assertEquals(View.GONE, banner.getVisibility());
        }
    }

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

    @Test
    public void finalizedNotFoundVisitLocksSameDayAndReopensNextDay() throws Exception {
        Visit visit = notFoundVisit();
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(visit)) {
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            activity.setupButtons();
            assertFalse(recordButton(activity).isEnabled());
            activity.openRecordClientVisit();
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            ((DailyHouseholdProfileActivity) activity).now = tomorrow.getTime();
            activity.setupButtons();
            assertTrue(recordButton(activity).isEnabled());
        }
    }

    @Test
    public void completePendingNotFoundVisitCanBeProcessedWithoutServiceAnswers() throws Exception {
        Visit visit = notFoundVisit();
        visit.setProcessed(false);
        try (MockedStatic<MotherMentorLibrary> ignored = persistedVisits(visit)) {
            MotherMentorHouseholdProfileActivity activity = householdProfile();
            activity.setupButtons();
            TextView process = ReflectionHelpers.getField(activity, "manualProcessVisit");
            assertEquals(View.VISIBLE, process.getVisibility());
            JSONObject event = new JSONObject(visit.getJson());
            JSONArray obs = event.getJSONArray("obs");
            for (int i = 0; i < obs.length(); i++) {
                if (DBConstants.KEY.HOUSEHOLD_NOT_FOUND_REASON.equals(obs.getJSONObject(i).optString("fieldCode"))) {
                    obs.getJSONObject(i).put("values", new JSONArray().put("other"));
                }
            }
            visit.setJson(event.toString());
            activity.setupButtons();
            assertEquals(View.GONE, process.getVisibility());
        }
    }

    private Visit notFoundVisit() throws Exception {
        Visit visit = householdVisit();
        JSONArray obs = new JSONArray();
        String[] fields = {
            DBConstants.KEY.HOUSEHOLD_FOLLOW_UP_SCHEMA, "2",
            DBConstants.KEY.HOUSEHOLD_VISIT_TYPE, "repeat",
            DBConstants.KEY.HOUSEHOLD_FOLLOW_UP_STATUS, "not_found",
            DBConstants.KEY.HOUSEHOLD_NOT_FOUND_REASON, "house_closed",
            DBConstants.KEY.MOTHERMENTOR_VISIT_DATE,
                new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date())
        };
        for (int i = 0; i < fields.length; i += 2) {
            obs.put(new JSONObject().put("fieldCode", fields[i])
                    .put("values", new JSONArray().put(fields[i + 1])));
        }
        visit.setJson(new JSONObject().put("obs", obs).toString());
        return visit;
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
