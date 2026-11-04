package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Executes the real profile controls and guards stale taps after a completed partner visit. */
public class MotherMentorPartnerFollowUpProfileTest extends BaseUnitTest {
    private MotherMentorLibrary previousLibrary;
    private VisitRepository repository;
    private MockedStatic<MotherMentorPartnerClosureRepository> closures;

    @Before
    public void useSavedVisits() {
        closures = Mockito.mockStatic(MotherMentorPartnerClosureRepository.class);
        closures.when(() -> MotherMentorPartnerClosureRepository.getState(
                Mockito.any(), Mockito.anyString(), Mockito.isNull()))
                .thenReturn(MotherMentorPartnerClosure.active());
        previousLibrary = ReflectionHelpers.getStaticField(MotherMentorLibrary.class, "instance");
        MotherMentorLibrary library = mock(MotherMentorLibrary.class);
        repository = mock(VisitRepository.class);
        ReflectionHelpers.setStaticField(MotherMentorLibrary.class, "instance", library);
        when(library.visitRepository()).thenReturn(repository);
    }

    @After
    public void restoreLibrary() {
        closures.close();
        ReflectionHelpers.setStaticField(MotherMentorLibrary.class, "instance", previousLibrary);
    }

    @Test
    public void completedPartnerVisitDisablesOriginalAndRecreatedProfile() throws Exception {
        savedPartnerVisit(completedVisit());
        for (int i = 0; i < 2; i++) {
            PartnerProfileActivity activity = profile();
            activity.setupButtons();
            assertFalse(button(activity).isEnabled());
            assertEquals(0.5f, button(activity).getAlpha(), 0f);
            activity.openRecordClientVisit();
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
        }
    }

    @Test
    public void staleEnabledButtonCannotOpenAnotherVisitAfterCompletion() throws Exception {
        PartnerProfileActivity activity = profile();
        activity.setupButtons();
        assertTrue(button(activity).isEnabled());
        savedPartnerVisit(completedVisit());

        activity.openRecordClientVisit();

        assertFalse(button(activity).isEnabled());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
    }

    @Test
    public void startButtonReopensNextCalendarDay() throws Exception {
        savedPartnerVisit(completedVisit());
        PartnerProfileActivity activity = profile();
        activity.setupButtons();
        assertFalse(button(activity).isEnabled());
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTime(activity.now);
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        activity.now = tomorrow.getTime();

        activity.setupButtons();
        activity.openRecordClientVisit();

        assertTrue(button(activity).isEnabled());
        Intent launch = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
        assertTrue(launch.getBooleanExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, false));
        assertFalse(launch.getBooleanExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, true));
        assertEquals(
                MotherMentorFollowUpType.PARTNER.getValue(),
                launch.getStringExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE));
    }

    @Test
    public void pendingPartnerDraftKeepsContinuePathAndStaleTapResumesIt() throws Exception {
        Visit draft = completedVisit();
        draft.setProcessed(false);
        savedPartnerVisit(draft);
        PartnerProfileActivity activity = profile();

        activity.setupButtons();

        assertEquals(View.GONE, button(activity).getVisibility());
        RelativeLayout inProgress =
                ReflectionHelpers.getField(activity, "mothermentorServiceInProgress");
        assertEquals(View.VISIBLE, inProgress.getVisibility());
        activity.openRecordClientVisit();
        Intent resume = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
        assertTrue(resume.getBooleanExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, false));
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    public void profileLeftOpenRefreshesPartnerAvailabilityAtMidnight() throws Exception {
        savedPartnerVisit(completedVisit());
        PartnerProfileActivity activity = profile();
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(activity.now);
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 0);
        activity.now = endOfDay.getTime();
        activity.setupButtons();
        assertFalse(button(activity).isEnabled());
        ReflectionHelpers.callInstanceMethod(activity, "scheduleVisitAvailabilityRefresh");

        endOfDay.add(Calendar.SECOND, 1);
        activity.now = endOfDay.getTime();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);

        assertTrue(button(activity).isEnabled());
    }

    @Test
    public void partnerCompletionDoesNotDisableOtherFollowUpProfiles() throws Exception {
        savedPartnerVisit(completedVisit());
        for (MotherMentorFollowUpType type :
                new MotherMentorFollowUpType[] {
                    MotherMentorFollowUpType.MOTHER_IIT, MotherMentorFollowUpType.CHILD_EID
                }) {
            PartnerProfileActivity activity = profile();
            activity.getIntent()
                    .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, type.getValue());
            activity.setupButtons();
            assertTrue(button(activity).isEnabled());
        }
    }

    @Test
    public void closedPartnerStaysReadableAndCannotStartContinueOrManuallyProcessVisits() {
        for (String reason : new String[] {"deceased", "moved"}) {
            PartnerProfileActivity activity = profile();
            activity.closure = closed(reason);
            LinearLayout bar = new LinearLayout(activity);
            bar.setOrientation(LinearLayout.VERTICAL);
            bar.addView(button(activity));

            activity.setupButtons();
            activity.openRecordClientVisit();
            activity.continueService();
            activity.continueContactVisit();
            activity.openTbContactFollowUpVisit();
            activity.openRecordTbContactVisit();

            assertFalse(activity.isFinishing());
            assertFalse(button(activity).isEnabled());
            assertEquals(View.VISIBLE, button(activity).getVisibility());
            assertEquals(View.GONE, ((View) ReflectionHelpers.getField(activity, "manualProcessVisit")).getVisibility());
            assertEquals(View.GONE, ((View) ReflectionHelpers.getField(activity, "mothermentorServiceInProgress")).getVisibility());
            assertEquals(View.GONE, ((View) ReflectionHelpers.getField(activity, "visitInProgress")).getVisibility());
            assertEquals(View.GONE, ((View) ReflectionHelpers.getField(activity, "textViewVisitDoneEdit")).getVisibility());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
            TextView banner = ReflectionHelpers.getField(activity, "partnerClosureBanner");
            assertTrue(banner.getText().toString().contains("2026-11-04"));
            assertEquals(View.VISIBLE, banner.getVisibility());
        }
    }

    @Test
    public void closedPartnerCanOpenRetainedMedicalHistory() {
        PartnerProfileActivity activity = profile();
        activity.closure = closed("moved");

        activity.openMedicalHistory();

        Intent history = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(MotherMentorMedicalHistoryActivity.class.getName(), history.getComponent().getClassName());
        assertFalse(activity.isFinishing());
    }

    @Test
    public void closureWhileProfileIsOpenDisablesStaleRecordAndContinueControlsWithoutNavigation() {
        PartnerProfileActivity activity = profile();
        activity.setupButtons();
        assertTrue(button(activity).isEnabled());
        activity.closure = closed("moved");

        activity.openRecordClientVisit();
        activity.continueService();

        assertFalse(activity.isFinishing());
        assertFalse(button(activity).isEnabled());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
    }

    private MotherMentorPartnerClosure closed(String reason) {
        Map<String, String> values = new HashMap<>();
        values.put(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE, "tracing");
        values.put(DBConstants.KEY.FOLLOW_UP_STATUS, "not_found");
        values.put(DBConstants.KEY.PARTNER_NOT_FOUND_REASON, reason);
        return MotherMentorPartnerClosure.active().advance(values, "2026-11-04");
    }

    @Test
    public void newAttendancePhaseManualCompletionUsesTestsWithoutRequiringTracingStatus() throws Exception {
        PartnerProfileActivity activity = profile();
        Visit visit = completedVisit();
        JSONObject event = new JSONObject(visit.getJson());
        JSONArray observations = new JSONArray();
        for (String[] answer : new String[][] {
                {DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA, "2"},
                {DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE, "attendance"},
                {DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY, "yes"},
                {DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE, "28-10-2026"},
                {DBConstants.KEY.PARTNER_EDUCATION_PROVIDED, "no"},
                {DBConstants.KEY.PARTNER_HIV_TESTED, "yes"},
                {DBConstants.KEY.PARTNER_HIV_RESULT, "negative"},
                {DBConstants.KEY.PARTNER_SYPHILIS_TESTED, "yes"},
                {DBConstants.KEY.PARTNER_SYPHILIS_RESULT, "negative"},
                {DBConstants.KEY.PARTNER_HEPATITIS_B_TESTED, "yes"}
        }) {
            observations.put(new JSONObject().put("fieldCode", answer[0])
                    .put("values", new JSONArray().put(answer[1])));
        }
        event.put("obs", observations);
        visit.setJson(event.toString());
        assertFalse(ReflectionHelpers.callInstanceMethod(activity, "hasCompletedMotherMentorServiceSections",
                ReflectionHelpers.ClassParameter.from(Visit.class, visit)));
        observations.put(new JSONObject().put("fieldCode", DBConstants.KEY.PARTNER_HEPATITIS_B_RESULT)
                .put("values", new JSONArray().put("negative")));
        visit.setJson(event.toString());
        assertTrue(ReflectionHelpers.callInstanceMethod(activity, "hasCompletedMotherMentorServiceSections",
                ReflectionHelpers.ClassParameter.from(Visit.class, visit)));
    }

    private void savedPartnerVisit(Visit visit) {
        when(repository.getVisits("partner-id", MotherMentorFollowUpType.PARTNER.getEventType()))
                .thenReturn(Collections.singletonList(visit));
        when(repository.getLatestVisit("partner-id", MotherMentorFollowUpType.PARTNER.getEventType()))
                .thenReturn(visit);
    }

    private PartnerProfileActivity profile() {
        Intent intent =
                new Intent()
                        .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                        .putExtra(
                                MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                                MotherMentorFollowUpType.PARTNER.getValue());
        PartnerProfileActivity activity =
                Robolectric.buildActivity(PartnerProfileActivity.class, intent).get();
        MemberObject member = new MemberObject();
        member.setBaseEntityId("partner-id");
        ReflectionHelpers.setField(activity, "memberObject", member);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", new TextView(activity));
        ReflectionHelpers.setField(activity, "manualProcessVisit", new TextView(activity));
        ReflectionHelpers.setField(activity, "textViewVisitDoneEdit", new TextView(activity));
        ReflectionHelpers.setField(activity, "visitInProgress", new RelativeLayout(activity));
        ReflectionHelpers.setField(activity, "rlLastVisit", new RelativeLayout(activity));
        ReflectionHelpers.setField(
                activity, "mothermentorServiceInProgress", new RelativeLayout(activity));
        return activity;
    }

    private TextView button(MotherMentorProfileActivity activity) {
        return ReflectionHelpers.getField(activity, "textViewRecordMotherMentor");
    }

    private Visit completedVisit() throws Exception {
        JSONArray observations =
                new JSONArray()
                        .put(
                                new JSONObject()
                                        .put("fieldCode", DBConstants.KEY.FOLLOW_UP_STATUS)
                                        .put("values", new JSONArray().put("not_found")));
        Visit visit = new Visit();
        visit.setVisitType(MotherMentorFollowUpType.PARTNER.getEventType());
        visit.setProcessed(true);
        visit.setDate(new Date());
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return visit;
    }

    public static class PartnerProfileActivity extends MotherMentorProfileActivity {
        @Override
        protected boolean isLinkedFollowUpCardClosed() {
            // These availability/death fixtures use an open enrollment. Real closure mapping
            // and stale linked-card guards are covered by the repository and closure UI tests.
            return false;
        }

        Date now = new Date();
        MotherMentorPartnerClosure closure = MotherMentorPartnerClosure.active();

        @Override
        protected MotherMentorPartnerClosure loadPartnerClosure() {
            return closure;
        }

        @Override
        Date getVisitAvailabilityDate() {
            return now;
        }

        @Override
        public void refreshMedicalHistory(boolean hasHistory) {
            // Exercise visit controls without loading unrelated history views.
        }
    }
}
