package org.smartregister.chw.activity;

import android.app.Application;
import android.content.Intent;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildEidStatusUiTest {
    @Test @Config(qualifiers = "en")
    public void closedChildStatusUsesTheEnglishRecordMessageAndDoesNotOpenTheForm() {
        checkClosedChild("This record is closed; no further visits can be recorded.");
    }

    @Test @Config(qualifiers = "sw")
    public void closedChildStatusUsesTheSwahiliRecordMessageAndDoesNotOpenTheForm() {
        checkClosedChild("Rekodi hii imefungwa; haiwezi kurekodiwa tena.");
    }

    private void checkClosedChild(String expected) {
        ClosedVisitActivity activity = closedVisit(MotherMentorFollowUpType.CHILD_EID);
        BaseMotherMentorVisitAction status = mock(BaseMotherMentorVisitAction.class);
        activity.startForm(status);
        assertEquals(expected, activity.message);
        assertFalse(activity.button.isEnabled());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
        verify(status, never()).setJsonPayload(anyString());
    }

    @Test
    public void theExistingIitClosedMessageRemainsUnchanged() {
        ClosedVisitActivity activity = closedVisit(MotherMentorFollowUpType.MOTHER_IIT);
        activity.startForm(mock(BaseMotherMentorVisitAction.class));
        assertEquals(activity.getString(org.smartregister.chw.mothermentor.R.string
                .mothermentor_mother_closed_message), activity.message);
    }

    @Test
    public void manualChildDeathCompletionUsesRegisteredDobAndTheOriginalServiceDay() throws Exception {
        CompletionProfileActivity activity = completionProfile();
        assertFalse(complete(activity, FOLLOW_UP_STATUS, "found", MAMA_CHILD_STATUS, "deceased",
                MAMA_TAREHE_MTOTO_ALIFARIKI, "19-01-2026"));
        assertFalse(complete(activity, FOLLOW_UP_STATUS, "found", MAMA_CHILD_STATUS, "deceased",
                MAMA_TAREHE_MTOTO_ALIFARIKI, "21-10-2026"));
        assertTrue(complete(activity, FOLLOW_UP_STATUS, "found", MAMA_CHILD_STATUS, "deceased",
                MAMA_TAREHE_MTOTO_ALIFARIKI, "20-10-2026"));
        assertTrue(activity.dobReads > 0);
    }

    @Test
    public void aCopiedBirthDateCannotOverrideTheRegisteredBirthDate() throws Exception {
        CompletionProfileActivity activity = completionProfile();
        assertFalse(complete(activity, FOLLOW_UP_STATUS, "found", MAMA_CHILD_STATUS, "deceased",
                MAMA_TAREHE_MTOTO_ALIFARIKI, "20-01-2025", CHILD_DOB, "1900-01-01"));
        activity.dob = null;
        assertFalse(complete(activity, FOLLOW_UP_STATUS, "found", MAMA_CHILD_STATUS, "deceased",
                MAMA_TAREHE_MTOTO_ALIFARIKI, "20-10-2026", CHILD_DOB, "1900-01-01"));
    }

    @Test
    public void manualNotFoundCompletionRequiresTheSpecificDetails() throws Exception {
        CompletionProfileActivity activity = completionProfile();
        assertFalse(complete(activity, FOLLOW_UP_STATUS, "not_found"));
        assertFalse(complete(activity, FOLLOW_UP_STATUS, "not_found", MTOTO_NOT_FOUND_REASON, "other"));
        assertTrue(complete(activity, FOLLOW_UP_STATUS, "not_found", MTOTO_NOT_FOUND_REASON, "other",
                MTOTO_NOT_FOUND_REASON_OTHER, "Away today"));
        assertFalse(complete(activity, FOLLOW_UP_STATUS, "not_found", MTOTO_NOT_FOUND_REASON, "moved_facility"));
        assertTrue(complete(activity, FOLLOW_UP_STATUS, "not_found", MTOTO_NOT_FOUND_REASON, "moved_facility",
                MTOTO_NEW_FACILITY, "New facility"));
        assertTrue(complete(activity, FOLLOW_UP_STATUS, "not_found", MTOTO_NOT_FOUND_REASON, "moved_area"));
    }

    @Test
    public void legacyCompletionDoesNotRequireTheNewDobContextOrNewQuestions() throws Exception {
        CompletionProfileActivity activity = completionProfile();
        assertTrue(completeVisit(activity, visit("2", "20-10-2026", FOLLOW_UP_STATUS, "deceased")));
        assertEquals(0, activity.dobReads);
    }

    @Test
    public void foundAliveCannotUseStaleLegacyTestQuestionsToBypassTheNewHeiSection() throws Exception {
        CompletionProfileActivity activity = completionProfile();
        assertFalse(complete(activity, FOLLOW_UP_STATUS, "found", MAMA_CHILD_STATUS, "alive",
                CHILD_EID_TEST_TYPE, "nine_months", CHILD_EID_ARRIVED_AT_FACILITY, "yes",
                CHILD_EID_FACILITY_ARRIVAL_DATE, "20-10-2026"));
    }

    @Test
    public void anInvalidRecordedServiceDateCannotFallBackToAnImportTimestamp() throws Exception {
        CompletionProfileActivity activity = completionProfile();
        Visit invalid = visit("3", "31-02-2026", FOLLOW_UP_STATUS, "not_found",
                MTOTO_NOT_FOUND_REASON, "other", MTOTO_NOT_FOUND_REASON_OTHER, "Away today");
        invalid.setDate(new LocalDate(2026, 10, 20).toDate());
        assertFalse(completeVisit(activity, invalid));
    }

    private ClosedVisitActivity closedVisit(MotherMentorFollowUpType type) {
        Intent intent = new Intent().putExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, type.getValue());
        ClosedVisitActivity activity = Robolectric.buildActivity(ClosedVisitActivity.class, intent).get();
        activity.prepare(); return activity;
    }

    private CompletionProfileActivity completionProfile() {
        Intent intent = new Intent().putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                        MotherMentorFollowUpType.CHILD_EID.getValue());
        return Robolectric.buildActivity(CompletionProfileActivity.class, intent).get();
    }

    private boolean complete(CompletionProfileActivity activity, String... pairs) throws Exception {
        return completeVisit(activity, visit("3", "20-10-2026", pairs));
    }

    private boolean completeVisit(CompletionProfileActivity activity, Visit visit) {
        return ReflectionHelpers.callInstanceMethod(activity, "hasCompletedMotherMentorServiceSections",
                ReflectionHelpers.ClassParameter.from(Visit.class, visit));
    }

    private Visit visit(String schema, String day, String... pairs) throws Exception {
        JSONArray observations = new JSONArray()
                .put(observation(CHILD_EID_FOLLOW_UP_SCHEMA, schema))
                .put(observation(MOTHERMENTOR_VISIT_DATE, day));
        for (int i = 0; i < pairs.length; i += 2) observations.put(observation(pairs[i], pairs[i + 1]));
        Visit visit = new Visit(); visit.setVisitId("saved-child-visit");
        visit.setBaseEntityId("child"); visit.setVisitType(MotherMentorFollowUpType.CHILD_EID.getEventType());
        visit.setJson(new JSONObject().put("obs", observations).toString()); return visit;
    }

    private JSONObject observation(String key, String value) throws Exception {
        return new JSONObject().put("fieldCode", key).put("values", new JSONArray().put(value));
    }

    public static class ClosedVisitActivity extends MotherMentorVisitActivity {
        String message;
        TextView button;
        void prepare() { button = new TextView(this); tvSubmit = button; }
        @Override protected boolean isMotherCardClosed() { return false; }
        @Override protected boolean isFollowUpCardClosed() { return true; }
        @Override public void displayToast(String text) { message = text; }
    }

    public static class CompletionProfileActivity extends MotherMentorProfileActivity {
        LocalDate dob = new LocalDate(2026, 1, 20);
        int dobReads;
        @Override protected LocalDate getChildEidCompletionDob(Visit visit) { dobReads++; return dob; }
    }
}
