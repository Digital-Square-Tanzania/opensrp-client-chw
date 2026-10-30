package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.annotation.LooperMode;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.repository.VisitRepository;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/** Guards against routing the localized follow-up button by its displayed text. */
public class MotherMentorProfileActivityTest extends BaseUnitTest {

    @Test
    public void completedServiceDisablesButtonAndRecreatedProfileCannotStartDuplicate()
            throws Exception {
        Visit visit = completedServiceVisit();
        try (MockedStatic<MotherMentorLibrary> libraryMock = persistedVisits(visit)) {
            MotherMentorProfileActivity first = serviceProfile();
            first.setupButtons();
            assertFalse(recordButton(first).isEnabled());

            MotherMentorProfileActivity recreated = serviceProfile();
            recreated.setupButtons();
            assertFalse(recordButton(recreated).isEnabled());
            recreated.openRecordClientVisit();
            assertEquals(null, Shadows.shadowOf(recreated).getNextStartedActivityForResult());
        }
    }

    @Test
    public void startButtonReopensNextCalendarDay() throws Exception {
        Visit visit = completedServiceVisit();
        try (MockedStatic<MotherMentorLibrary> libraryMock = persistedVisits(visit)) {
            MotherMentorProfileActivity activity = serviceProfile();
            activity.setupButtons();
            assertFalse(recordButton(activity).isEnabled());

            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            ((DailyVisitProfileActivity) activity).now = tomorrow.getTime();
            activity.setupButtons();
            assertTrue(recordButton(activity).isEnabled());
            activity.openRecordClientVisit();
            assertEquals(
                    MotherMentorVisitActivity.class.getName(),
                    Shadows.shadowOf(activity)
                            .getNextStartedActivityForResult()
                            .intent.getComponent().getClassName());
        }
    }

    @Test
    public void unprocessedDraftRetainsContinueVisitInsteadOfStartingAnother() throws Exception {
        Visit visit = completedServiceVisit();
        visit.setProcessed(false);
        try (MockedStatic<MotherMentorLibrary> libraryMock = persistedVisits(visit)) {
            MotherMentorProfileActivity activity = serviceProfile();
            activity.setupButtons();
            assertEquals(View.GONE, recordButton(activity).getVisibility());
            RelativeLayout inProgress =
                    ReflectionHelpers.getField(activity, "mothermentorServiceInProgress");
            assertEquals(View.VISIBLE, inProgress.getVisibility());
            // A stale start button also resumes the saved draft instead of creating a duplicate.
            activity.openRecordClientVisit();
            Intent resume = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
            assertTrue(resume.getBooleanExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, false));
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    public void profileLeftOpenRefreshesAtMidnight() throws Exception {
        Visit visit = completedServiceVisit();
        try (MockedStatic<MotherMentorLibrary> libraryMock = persistedVisits(visit)) {
            MotherMentorProfileActivity activity = serviceProfile();
            Calendar endOfDay = Calendar.getInstance();
            endOfDay.set(Calendar.HOUR_OF_DAY, 23);
            endOfDay.set(Calendar.MINUTE, 59);
            endOfDay.set(Calendar.SECOND, 59);
            endOfDay.set(Calendar.MILLISECOND, 0);
            ((DailyVisitProfileActivity) activity).now = endOfDay.getTime();
            activity.setupButtons();
            assertFalse(recordButton(activity).isEnabled());
            ReflectionHelpers.callInstanceMethod(activity, "scheduleVisitAvailabilityRefresh");

            endOfDay.add(Calendar.SECOND, 1);
            ((DailyVisitProfileActivity) activity).now = endOfDay.getTime();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
            assertTrue(recordButton(activity).isEnabled());
        }
    }

    @Test
    public void completedPrimaryServiceDoesNotDisableFollowUpCategory() throws Exception {
        Visit visit = completedServiceVisit();
        try (MockedStatic<MotherMentorLibrary> libraryMock = persistedVisits(visit)) {
            MotherMentorProfileActivity activity = serviceProfile();
            activity.setIntent(
                    new Intent()
                            .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                            .putExtra(
                                    MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                                    MotherMentorFollowUpType.PARTNER.getValue()));
            activity.setupButtons();
            assertTrue(recordButton(activity).isEnabled());
            activity.openRecordClientVisit();
            Intent intent = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
            assertTrue(intent.getBooleanExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, false));
        }
    }

    private MockedStatic<MotherMentorLibrary> persistedVisits(Visit visit) {
        MockedStatic<MotherMentorLibrary> mocked = Mockito.mockStatic(MotherMentorLibrary.class);
        MotherMentorLibrary library = Mockito.mock(MotherMentorLibrary.class);
        VisitRepository repository = Mockito.mock(VisitRepository.class);
        mocked.when(MotherMentorLibrary::getInstance).thenReturn(library);
        Mockito.when(library.visitRepository()).thenReturn(repository);
        Mockito.when(
                        repository.getVisits(
                                "test-client-id", Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES))
                .thenReturn(Collections.singletonList(visit));
        Mockito.when(
                        repository.getLatestVisit(
                                "test-client-id", Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES))
                .thenReturn(visit);
        return mocked;
    }

    private MotherMentorProfileActivity serviceProfile() {
        MotherMentorProfileActivity activity =
                Robolectric.buildActivity(DailyVisitProfileActivity.class).get();
        MemberObject member = new MemberObject();
        member.setBaseEntityId("test-client-id");
        ReflectionHelpers.setField(activity, "memberObject", member);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", new TextView(activity));
        ReflectionHelpers.setField(activity, "manualProcessVisit", new TextView(activity));
        ReflectionHelpers.setField(activity, "rlLastVisit", new RelativeLayout(activity));
        ReflectionHelpers.setField(
                activity, "mothermentorServiceInProgress", new RelativeLayout(activity));
        return activity;
    }

    public static class DailyVisitProfileActivity extends MotherMentorProfileActivity {
        Date now = new Date();

        @Override
        Date getVisitAvailabilityDate() {
            return now;
        }

        @Override
        public void refreshMedicalHistory(boolean hasHistory) {
            // The tests exercise service controls without loading unrelated history views.
        }
    }

    private TextView recordButton(MotherMentorProfileActivity activity) {
        return ReflectionHelpers.getField(activity, "textViewRecordMotherMentor");
    }

    private Visit completedServiceVisit() throws Exception {
        JSONArray observations = new JSONArray();
        String[] fields = {
            DBConstants.KEY.ATTENDANCE_TYPE, "repeat",
            DBConstants.KEY.FOLLOW_UP_STATUS, "lost_to_follow_up",
            DBConstants.KEY.COMMENTS, "Client traced",
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

    @Test
    public void partnerVisitCanCompleteWithoutRemovedReasonSection() throws Exception {
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.PARTNER,
                        DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
                        "no"));
        assertFalse(
                completedFollowUp(
                        MotherMentorFollowUpType.PARTNER,
                        DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
                        "yes"));
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.PARTNER,
                        DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
                        "yes",
                        DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE,
                        "19-10-2026"));
    }

    @Test
    public void childEidVisitNeedsTestStageAndAttendanceButNoReasonSection() throws Exception {
        assertFalse(
                completedFollowUp(
                        MotherMentorFollowUpType.CHILD_EID,
                        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                        "no"));
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.CHILD_EID,
                        DBConstants.KEY.CHILD_EID_TEST_TYPE,
                        "six_weeks",
                        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                        "no"));
        assertFalse(
                completedFollowUp(
                        MotherMentorFollowUpType.CHILD_EID,
                        DBConstants.KEY.CHILD_EID_TEST_TYPE,
                        "six_weeks",
                        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                        "yes"));
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.CHILD_EID,
                        DBConstants.KEY.CHILD_EID_TEST_TYPE,
                        "six_weeks",
                        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                        "yes",
                        DBConstants.KEY.CHILD_EID_FACILITY_ARRIVAL_DATE,
                        "19-10-2026"));
    }

    @Test
    public void motherIitVisitStillRequiresInterruptionReasonAndExpectedDate() throws Exception {
        assertFalse(
                completedFollowUp(
                        MotherMentorFollowUpType.MOTHER_IIT,
                        DBConstants.KEY.RETURNED_TO_FACILITY,
                        "no"));
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.MOTHER_IIT,
                        DBConstants.KEY.SERVICE_INTERRUPTION_REASON,
                        "transport_cost",
                        DBConstants.KEY.EXPECTED_FACILITY_VISIT_DATE,
                        "19-10-2026",
                        DBConstants.KEY.RETURNED_TO_FACILITY,
                        "no"));
    }

    private boolean completedFollowUp(MotherMentorFollowUpType type, String... answers)
            throws Exception {
        Intent intent =
                new Intent()
                        .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                        .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, type.getValue());
        MotherMentorProfileActivity activity =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, intent).get();
        JSONArray observations =
                new JSONArray()
                        .put(
                                new JSONObject()
                                        .put("fieldCode", DBConstants.KEY.FOLLOW_UP_STATUS)
                                        .put("values", new JSONArray().put("found")));
        for (int i = 0; i < answers.length; i += 2) {
            observations.put(
                    new JSONObject()
                            .put("fieldCode", answers[i])
                            .put("values", new JSONArray().put(answers[i + 1])));
        }
        Visit visit = new Visit();
        visit.setVisitType(type.getEventType());
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return ReflectionHelpers.callInstanceMethod(
                activity,
                "hasCompletedMotherMentorServiceSections",
                ReflectionHelpers.ClassParameter.from(Visit.class, visit));
    }

    @Test
    public void contactButtonIsHiddenOnlyForFollowUpNavigationContext() {
        MotherMentorProfileActivity homeProfile =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, new Intent()).get();
        assertFalse(homeProfile.isFollowUpProfile());

        Intent followUpIntent =
                new Intent().putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true);
        MotherMentorProfileActivity followUpProfile =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, followUpIntent).get();
        assertTrue(followUpProfile.isFollowUpProfile());
    }

    @Test
    public void recordFollowUpClickLaunchesClientVisitRegardlessOfButtonLabel() {
        MotherMentorProfileActivity activity =
                Mockito.spy(Robolectric.buildActivity(MotherMentorProfileActivity.class).get());
        MemberObject member = new MemberObject();
        member.setBaseEntityId("test-client-id");
        ReflectionHelpers.setField(activity, "memberObject", member);
        Mockito.doNothing().when(activity).openRecordClientVisit();

        View view = Mockito.mock(View.class);
        Mockito.when(view.getId())
                .thenReturn(org.smartregister.chw.mothermentor.R.id.textview_record_mothermentor);
        activity.onClick(view);

        Mockito.verify(activity).openRecordClientVisit();
    }

    @Test
    public void profileCategoryComesFromStableIntentExtra() {
        Intent partnerIntent =
                new Intent()
                        .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                        .putExtra(
                                MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                                MotherMentorFollowUpType.PARTNER.getValue());
        MotherMentorProfileActivity partnerProfile =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, partnerIntent).get();
        assertEquals(MotherMentorFollowUpType.PARTNER, partnerProfile.getFollowUpType());

        Intent childIntent =
                new Intent()
                        .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                        .putExtra(
                                MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                                MotherMentorFollowUpType.CHILD_EID.getValue());
        MotherMentorProfileActivity childProfile =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, childIntent).get();
        assertEquals(MotherMentorFollowUpType.CHILD_EID, childProfile.getFollowUpType());
    }
}
