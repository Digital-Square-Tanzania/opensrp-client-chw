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
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/** Executes the real profile controls and guards stale taps after a completed child visit. */
public class MotherMentorChildEidFollowUpProfileTest extends BaseUnitTest {
    private MotherMentorLibrary previousLibrary;
    private VisitRepository repository;

    @Before
    public void useSavedVisits() {
        previousLibrary = ReflectionHelpers.getStaticField(MotherMentorLibrary.class, "instance");
        MotherMentorLibrary library = mock(MotherMentorLibrary.class);
        repository = mock(VisitRepository.class);
        ReflectionHelpers.setStaticField(MotherMentorLibrary.class, "instance", library);
        when(library.visitRepository()).thenReturn(repository);
    }

    @After
    public void restoreLibrary() {
        ReflectionHelpers.setStaticField(MotherMentorLibrary.class, "instance", previousLibrary);
    }

    @Test
    public void completedChildEidVisitDisablesOriginalAndRecreatedProfile() throws Exception {
        savedChildEidVisit(completedVisit());
        for (int i = 0; i < 2; i++) {
            ChildEidProfileActivity activity = profile();
            activity.setupButtons();
            assertFalse(button(activity).isEnabled());
            assertEquals(0.5f, button(activity).getAlpha(), 0f);
            activity.openRecordClientVisit();
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
        }
    }

    @Test
    public void staleEnabledButtonCannotOpenAnotherVisitAfterCompletion() throws Exception {
        ChildEidProfileActivity activity = profile();
        activity.setupButtons();
        assertTrue(button(activity).isEnabled());
        savedChildEidVisit(completedVisit());

        activity.openRecordClientVisit();

        assertFalse(button(activity).isEnabled());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
    }

    @Test
    public void startButtonReopensNextCalendarDay() throws Exception {
        savedChildEidVisit(completedVisit());
        ChildEidProfileActivity activity = profile();
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
                MotherMentorFollowUpType.CHILD_EID.getValue(),
                launch.getStringExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE));
    }

    @Test
    public void pendingChildDraftKeepsContinuePathAndStaleTapResumesIt() throws Exception {
        Visit draft = completedVisit();
        draft.setProcessed(false);
        savedChildEidVisit(draft);
        ChildEidProfileActivity activity = profile();

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
    public void profileLeftOpenRefreshesChildAvailabilityAtMidnight() throws Exception {
        savedChildEidVisit(completedVisit());
        ChildEidProfileActivity activity = profile();
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
    public void childCompletionDoesNotDisableOtherFollowUpProfiles() throws Exception {
        savedChildEidVisit(completedVisit());
        for (MotherMentorFollowUpType type :
                new MotherMentorFollowUpType[] {
                    MotherMentorFollowUpType.MOTHER_IIT, MotherMentorFollowUpType.PARTNER
                }) {
            ChildEidProfileActivity activity = profile();
            activity.getIntent()
                    .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, type.getValue());
            activity.setupButtons();
            assertTrue(button(activity).isEnabled());
        }
    }

    private void savedChildEidVisit(Visit visit) {
        when(repository.getVisits("child-id", MotherMentorFollowUpType.CHILD_EID.getEventType()))
                .thenReturn(Collections.singletonList(visit));
        when(repository.getLatestVisit("child-id", MotherMentorFollowUpType.CHILD_EID.getEventType()))
                .thenReturn(visit);
    }

    private ChildEidProfileActivity profile() {
        Intent intent =
                new Intent()
                        .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                        .putExtra(
                                MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                                MotherMentorFollowUpType.CHILD_EID.getValue());
        ChildEidProfileActivity activity =
                Robolectric.buildActivity(ChildEidProfileActivity.class, intent).get();
        MemberObject member = new MemberObject();
        member.setBaseEntityId("child-id");
        ReflectionHelpers.setField(activity, "memberObject", member);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", new TextView(activity));
        ReflectionHelpers.setField(activity, "manualProcessVisit", new TextView(activity));
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
        visit.setVisitType(MotherMentorFollowUpType.CHILD_EID.getEventType());
        visit.setProcessed(true);
        visit.setDate(new Date());
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return visit;
    }

    public static class ChildEidProfileActivity extends MotherMentorProfileActivity {
        Date now = new Date();

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
