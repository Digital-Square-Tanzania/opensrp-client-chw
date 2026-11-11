package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorIitState;

import java.util.Collections;
import java.util.Date;

public class MotherMentorIitDailyVisitTest extends BaseUnitTest {
    private static final String CLIENT = "iit-client";
    private static final MotherMentorFollowUpType TYPE = MotherMentorFollowUpType.MOTHER_IIT;

    private MotherMentorLibrary previousLibrary;
    private VisitRepository visits;

    @Before
    public void setUpVisits() {
        previousLibrary = ReflectionHelpers.getStaticField(MotherMentorLibrary.class, "instance");
        MotherMentorLibrary library = mock(MotherMentorLibrary.class);
        visits = mock(VisitRepository.class);
        when(library.visitRepository()).thenReturn(visits);
        ReflectionHelpers.setStaticField(MotherMentorLibrary.class, "instance", library);
    }

    @After
    public void restoreVisits() {
        ReflectionHelpers.setStaticField(MotherMentorLibrary.class, "instance", previousLibrary);
    }

    @Test
    public void savedVisitBlocksStaleTapAndRecreatedProfileUntilNextDay() throws Exception {
        ProfileActivity profile = profile();
        profile.setupButtons();
        assertTrue(button(profile).isEnabled());
        savedVisit(true, false, profile.day);

        profile.openRecordClientVisit();
        assertFalse(button(profile).isEnabled());
        assertNull(Shadows.shadowOf(profile).getNextStartedActivityForResult());

        ProfileActivity reopened = profile();
        reopened.setupButtons();
        assertFalse(button(reopened).isEnabled());
        reopened.day = reopened.day.plusDays(1);
        reopened.setupButtons();
        reopened.openRecordClientVisit();
        assertTrue(button(reopened).isEnabled());
        Intent launch = Shadows.shadowOf(reopened).getNextStartedActivityForResult().intent;
        assertFalse(launch.getBooleanExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, true));
    }

    @Test
    public void draftVoidedVisitAndEarlierServiceDateDoNotLockToday() throws Exception {
        ProfileActivity profile = profile();
        savedVisit(false, false, profile.day);
        profile.setupButtons();
        assertTrue(button(profile).isEnabled());
        savedVisit(true, true, profile.day);
        profile.setupButtons();
        assertTrue(button(profile).isEnabled());
        savedVisit(true, false, profile.day.minusDays(1));
        profile.setupButtons();
        assertTrue(button(profile).isEnabled());
    }

    @Test
    public void alreadyOpenNewFormCannotSaveButEditingTodaysVisitCan() throws Exception {
        savedVisit(true, false, LocalDate.now());
        VisitActivity newForm = visitForm(false);
        newForm.submitVisit();
        assertTrue(newForm.isFinishing());
        assertEquals(0, newForm.submissions);

        VisitActivity edit = visitForm(true);
        edit.submitVisit();
        assertFalse(edit.isFinishing());
        assertEquals(1, edit.submissions);
    }

    private void savedVisit(boolean processed, boolean voided, LocalDate serviceDay) throws Exception {
        Visit visit = new Visit();
        visit.setVisitType(TYPE.getEventType());
        visit.setBaseEntityId(CLIENT);
        visit.setDate(new Date());
        visit.setProcessed(processed);
        visit.setJson(new JSONObject().put(DBConstants.KEY.EVENT_VOIDED, voided)
                .put("obs", new JSONArray().put(new JSONObject()
                .put("fieldCode", DBConstants.KEY.MOTHERMENTOR_VISIT_DATE)
                .put("values", new JSONArray().put(serviceDay.toString("dd-MM-yyyy"))))).toString());
        when(visits.getVisits(CLIENT, TYPE.getEventType())).thenReturn(Collections.singletonList(visit));
    }

    private static ProfileActivity profile() {
        Intent intent = new Intent().putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, TYPE.getValue());
        ProfileActivity profile = Robolectric.buildActivity(ProfileActivity.class, intent).get();
        MemberObject member = new MemberObject();
        member.setBaseEntityId(CLIENT);
        ReflectionHelpers.setField(profile, "memberObject", member);
        ReflectionHelpers.setField(profile, "textViewRecordMotherMentor", new TextView(profile));
        ReflectionHelpers.setField(profile, "manualProcessVisit", new TextView(profile));
        ReflectionHelpers.setField(profile, "rlLastVisit", new RelativeLayout(profile));
        ReflectionHelpers.setField(profile, "mothermentorServiceInProgress", new RelativeLayout(profile));
        return profile;
    }

    private static TextView button(ProfileActivity profile) {
        return ReflectionHelpers.getField(profile, "textViewRecordMotherMentor");
    }

    private static VisitActivity visitForm(boolean edit) {
        VisitActivity activity = Robolectric.buildActivity(VisitActivity.class).get();
        ReflectionHelpers.setField(activity, "baseEntityID", CLIENT);
        ReflectionHelpers.setField(activity, "isEditMode", edit);
        return activity;
    }

    public static class ProfileActivity extends MotherMentorProfileActivity {
        LocalDate day = LocalDate.now();

        @Override Date getVisitAvailabilityDate() { return day.toDate(); }
        @Override protected boolean isLinkedFollowUpCardClosed() { return false; }
        @Override public void refreshMedicalHistory(boolean hasHistory) {}
    }

    public static class VisitActivity extends MotherMentorVisitActivity {
        int submissions;

        @Override boolean isFollowUpVisit() { return true; }
        @Override MotherMentorFollowUpType getFollowUpType() { return TYPE; }
        @Override protected boolean isFollowUpCardClosed() { return false; }
        @Override protected MotherMentorIitState getIitClosurePreview() { return MotherMentorIitState.active(); }
        @Override protected void submitConfirmedVisit() { submissions++; }
    }
}
