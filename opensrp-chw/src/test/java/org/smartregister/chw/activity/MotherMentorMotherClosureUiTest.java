package org.smartregister.chw.activity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure.VisitOutcome;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosureLabels;

/** The same terminal preview must be confirmed before either workflow finalizes a visit. */
public class MotherMentorMotherClosureUiTest extends BaseUnitTest {
    @Test
    @Config(qualifiers = "en")
    public void allTenEnglishClosuresRequireConfirmation() {
        checkAllClosures();
    }

    @Test
    @Config(qualifiers = "sw")
    public void allTenSwahiliClosuresRequireConfirmation() {
        checkAllClosures();
    }

    private void checkAllClosures() {
        int terminals = 0;
        for (VisitOutcome outcome : VisitOutcome.values()) {
            MotherMentorMotherClosure state = MotherMentorMotherClosure.active();
            for (int i = 0; i < 3; i++) state = state.advance(outcome, "2026-11-04");
            if (!state.isClosed()) continue;
            terminals++;
            ConfirmationVisitActivity activity = visit();
            activity.preview = state;
            activity.submitVisit();
            verifyNoInteractions(activity.submitter);
            AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
            assertTrue(dialog.isShowing());
            String message =
                    ((TextView) dialog.findViewById(android.R.id.message)).getText().toString();
            assertTrue(message.contains(
                    MotherMentorMotherClosureLabels.label(activity, state.getStatus())));
            assertTrue(message.contains(
                    MotherMentorMotherClosureLabels.note(activity, state.getStatus())));
            activity.submitVisit();
            assertSame(dialog, ShadowAlertDialog.getLatestDialog());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            verify(activity.submitter).submitVisit();
            activity.submitVisit();
            verifyNoMoreInteractions(activity.submitter);
        }
        assertEquals(10, terminals);
    }

    @Test
    public void cancelRetainsAnswersAndAllowsAnotherConfirmation() {
        ConfirmationVisitActivity activity = visit();
        activity.preview =
                MotherMentorMotherClosure.active().advance(VisitOutcome.MOVED_AREA, "2026-11-04");
        BaseMotherMentorVisitAction answer = mock(BaseMotherMentorVisitAction.class);
        when(answer.getJsonPayload()).thenReturn("original user selections");
        activity.answer(answer);
        activity.submitVisit();
        AlertDialog first = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        first.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals("original user selections", answer.getJsonPayload());
        verify(answer, never()).setJsonPayload(anyString());
        verifyNoInteractions(activity.submitter);
        assertTrue(activity.submitButton().isEnabled());
        activity.submitVisit();
        AlertDialog second = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertNotSame(first, second);
        second.cancel();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue(activity.submitButton().isEnabled());
        activity.submitVisit();
        ((AlertDialog) ShadowAlertDialog.getLatestDialog())
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        verify(activity.submitter).submitVisit();
    }

    @Test
    public void aStaleConfirmationCannotFinalizeAfterCardCloses() {
        ConfirmationVisitActivity activity = visit();
        activity.preview =
                MotherMentorMotherClosure.active().advance(VisitOutcome.STILLBIRTH, "2026-11-04");
        activity.submitVisit();
        activity.closed = true;
        ((AlertDialog) ShadowAlertDialog.getLatestDialog())
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        verifyNoInteractions(activity.submitter);
        assertFalse(activity.isFinishing());
        assertFalse(activity.submitButton().isEnabled());
    }

    @Test
    public void staleOpenVisitRetainsSelectionsAndLocksFormAndSubmitWithoutClosingIt()
            throws Exception {
        ConfirmationVisitActivity activity = visit();
        BaseMotherMentorVisitAction entry = mock(BaseMotherMentorVisitAction.class);
        when(entry.getJsonPayload()).thenReturn("unsaved user selections");
        activity.answer(entry);
        activity.closed = true;
        activity.onResumption();
        activity.startForm(entry);
        activity.startFormActivity(new org.json.JSONObject());
        activity.submitVisit();
        activity.redrawVisitUI();
        assertFalse(activity.isFinishing());
        assertFalse(activity.submitButton().isEnabled());
        assertFalse(activity.submitButton().hasOnClickListeners());
        assertEquals("unsaved user selections", entry.getJsonPayload());
        verify(entry, never()).setJsonPayload(anyString());
        verifyNoInteractions(activity.submitter);
        assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
    }

    @Test
    public void closedDirectEntryFinishesBeforeInitializingThePresenter() {
        ConfirmationVisitActivity activity = visit();
        activity.closed = true;
        activity.registerPresenter();
        assertTrue(activity.isFinishing());
        verifyNoInteractions(activity.submitter);
    }

    @Test
    public void staleIitAndChildVisitsCannotSaveAfterTheirEnrollmentCloses() {
        for (MotherMentorFollowUpType type : new MotherMentorFollowUpType[] {
                     MotherMentorFollowUpType.MOTHER_IIT, MotherMentorFollowUpType.CHILD_EID}) {
            Intent intent = new Intent()
                                    .putExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true)
                                    .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                                            type.getValue());
            ConfirmationVisitActivity activity =
                    Robolectric.buildActivity(ConfirmationVisitActivity.class, intent).get();
            activity.setTheme(R.style.ChwTheme);
            activity.prepare();
            activity.followUpClosed = true;
            BaseMotherMentorVisitAction entry = mock(BaseMotherMentorVisitAction.class);
            when(entry.getJsonPayload()).thenReturn("unchanged follow-up selections");
            activity.answer(entry);
            activity.onResumption();
            activity.startForm(entry);
            activity.submitVisit();
            assertFalse(activity.isFinishing());
            assertFalse(activity.submitButton().isEnabled());
            assertEquals("unchanged follow-up selections", entry.getJsonPayload());
            verifyNoInteractions(activity.submitter);
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
        }
    }

    @Test
    public void delayedToastCannotReenableAClosedMotherOrFollowUpSubmitButton() {
        for (boolean followUp : new boolean[] {false, true}) {
            ClosedToastVisitActivity activity = Robolectric.buildActivity(ClosedToastVisitActivity.class, new Intent()).get();
            activity.followUp = followUp;
            TextView submit = new TextView(activity);
            submit.setEnabled(false);
            ReflectionHelpers.setField(activity, "tvSubmit", submit);
            activity.displayToast("Delayed result");
            assertFalse(submit.isEnabled());
            assertFalse(submit.hasOnClickListeners());
        }
    }

    @Test
    public void activeVisitFinalizesWithoutAClosureDialog() {
        ConfirmationVisitActivity activity = visit();
        activity.submitVisit();
        verify(activity.submitter).submitVisit();
    }

    @Test
    @Config(qualifiers = "en")
    public void closedEnglishProfileRetainsHistoryAndBlocksAllServiceEntryPoints() {
        checkClosedProfile();
    }

    @Test
    @Config(qualifiers = "sw")
    public void closedSwahiliProfileRetainsHistoryAndBlocksAllServiceEntryPoints() {
        checkClosedProfile();
    }

    @Test
    public void aLaterDeathRefreshKeepsTheClosedCardCtaVisibleAndDisabled() {
        ClosedProfileActivity activity =
                Robolectric.buildActivity(ClosedProfileActivity.class, new Intent()).get();
        TextView record = new TextView(activity);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", record);
        ReflectionHelpers.callInstanceMethod(activity, "hideDeceasedClientActionViews");
        assertEquals(View.VISIBLE, record.getVisibility());
        assertFalse(record.isEnabled());
        assertEquals(0.5f, record.getAlpha(), 0f);
    }

    private void checkClosedProfile() {
        ClosedProfileActivity activity =
                Robolectric.buildActivity(ClosedProfileActivity.class, new Intent()).get();
        activity.setTheme(R.style.ChwTheme);
        MemberObject mother = new MemberObject();
        mother.setBaseEntityId("closed-mother");
        mother.setFamilyBaseEntityId("household");
        ReflectionHelpers.setField(activity, "memberObject", mother);
        LinearLayout layout = new LinearLayout(activity);
        layout.setId(org.smartregister.chw.mothermentor.R.id.profile_name_layout);
        activity.setContentView(layout);
        TextView record = new TextView(activity);
        layout.addView(record);
        TextView manual = new TextView(activity);
        RelativeLayout progress = new RelativeLayout(activity);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", record);
        ReflectionHelpers.setField(activity, "manualProcessVisit", manual);
        ReflectionHelpers.setField(activity, "mothermentorServiceInProgress", progress);
        ReflectionHelpers.setField(activity, "rlLastVisit", new RelativeLayout(activity));
        activity.setupButtons();
        assertEquals(View.VISIBLE, record.getVisibility());
        assertFalse(record.isEnabled());
        assertEquals(View.GONE, manual.getVisibility());
        assertEquals(View.GONE, progress.getVisibility());
        assertTrue(activity.historyRefreshed);
        TextView banner = ReflectionHelpers.getField(activity, "motherClosureBanner");
        assertEquals(View.VISIBLE, banner.getVisibility());
        assertTrue(banner.getText().toString().contains(MotherMentorMotherClosureLabels.label(
                activity, MotherMentorMotherClosure.GRADUATED)));
        activity.openRecordClientVisit();
        activity.continueService();
        activity.continueContactVisit();
        activity.openRecordTbContactVisit();
        activity.openTbContactFollowUpVisit();
        activity.openFollowupVisit();
        assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }

    @Test
    @Config(qualifiers = "en")
    public void cancelingManualCompletionPreservesTheDraftAndRearmsConfirmation() {
        ConfirmationProfileActivity activity = manualProfile();
        Visit draft = new Visit();
        draft.setJson("original draft payload");
        activity.completeSavedMotherVisit(draft);
        assertEquals(0, activity.persisted);
        AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals("original draft payload", draft.getJson());
        assertEquals(0, activity.persisted);
        assertTrue(activity.manualButton().isEnabled());
        activity.completeSavedMotherVisit(draft);
        assertNotSame(dialog, ShadowAlertDialog.getLatestDialog());
        ((AlertDialog) ShadowAlertDialog.getLatestDialog()).cancel();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    @Config(qualifiers = "sw")
    public void manualCompletionPersistsOnlyAfterSwahiliConfirmationAndGuardsStaleClosure() {
        ConfirmationProfileActivity activity = manualProfile();
        Visit draft = new Visit();
        activity.completeSavedMotherVisit(draft);
        activity.completeSavedMotherVisit(draft);
        assertEquals(0, activity.persisted);
        ((AlertDialog) ShadowAlertDialog.getLatestDialog())
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, activity.persisted);
        assertTrue(activity.manualButton().isEnabled());
        activity.completeSavedMotherVisit(draft);
        activity.closed = true;
        ((AlertDialog) ShadowAlertDialog.getLatestDialog())
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, activity.persisted);
    }

    private ConfirmationProfileActivity manualProfile() {
        ConfirmationProfileActivity activity =
                Robolectric.buildActivity(ConfirmationProfileActivity.class, new Intent()).get();
        activity.setTheme(R.style.ChwTheme);
        activity.prepare();
        return activity;
    }

    private ConfirmationVisitActivity visit() {
        ConfirmationVisitActivity activity =
                Robolectric.buildActivity(ConfirmationVisitActivity.class, new Intent()).get();
        activity.setTheme(R.style.ChwTheme);
        activity.prepare();
        return activity;
    }

    public static class ConfirmationVisitActivity extends MotherMentorVisitActivity {
        MotherMentorMotherClosure preview = MotherMentorMotherClosure.active();
        boolean closed;
        boolean followUpClosed;
        final BaseMotherMentorVisitContract.Presenter submitter =
                mock(BaseMotherMentorVisitContract.Presenter.class);
        void prepare() {
            tvSubmit = new TextView(this);
        }
        TextView submitButton() {
            return tvSubmit;
        }
        void answer(BaseMotherMentorVisitAction answer) {
            actionList.put("entry", answer);
        }
        @Override
        protected MotherMentorMotherClosure getMotherClosurePreview() {
            return preview;
        }
        @Override
        protected boolean isMotherCardClosed() {
            return closed;
        }
        @Override
        protected boolean isFollowUpCardClosed() {
            return followUpClosed;
        }
        @Override
        public BaseMotherMentorVisitContract.Presenter getPresenter() {
            return submitter;
        }
        @Override public void displayToast(String message) { /* No toast lifecycle required. */ }
    }

    public static class ClosedToastVisitActivity extends MotherMentorVisitActivity {
        boolean followUp;
        @Override protected boolean isMotherCardClosed() { return !followUp; }
        @Override protected boolean isFollowUpCardClosed() { return followUp; }
    }

    public static class ConfirmationProfileActivity extends MotherMentorProfileActivity {
        int persisted;
        boolean closed;
        void prepare() {
            manualProcessVisit = new TextView(this);
        }
        TextView manualButton() {
            return manualProcessVisit;
        }
        @Override
        protected boolean isMotherCardClosed() {
            return closed;
        }
        @Override
        protected MotherMentorMotherClosure getSavedMotherClosurePreview(Visit visit) {
            return MotherMentorMotherClosure.active().advance(VisitOutcome.GRADUATED, "2026-11-04");
        }
        @Override
        protected void persistSavedMotherVisit(Visit visit) {
            persisted++;
        }
        @Override
        protected void setupViews() {}
        @Override
        public void refreshMedicalHistory(boolean hasHistory) {}
        @Override
        public void displayToast(int resource) {}
    }

    public static class ClosedProfileActivity extends MotherMentorProfileActivity {
        boolean historyRefreshed;
        @Override
        protected boolean isMotherCardClosed() {
            return true;
        }
        @Override
        protected MotherMentorMotherClosure getMotherClosureState() {
            return MotherMentorMotherClosure.active().advance(VisitOutcome.GRADUATED, "2026-11-04");
        }
        @Override
        protected boolean isClientDeceased() {
            return false;
        }
        @Override
        public void refreshMedicalHistory(boolean hasHistory) {
            historyRefreshed = true;
        }
    }
}
