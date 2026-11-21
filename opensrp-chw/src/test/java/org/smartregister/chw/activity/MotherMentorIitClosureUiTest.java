package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorIitState;

public class MotherMentorIitClosureUiTest extends BaseUnitTest {
    @Test
    @Config(qualifiers = "sw")
    public void linkedTransferWarnsBeforeSavingAndCancelKeepsItUnsubmitted() {
        ConfirmationActivity activity = activity(MotherMentorIitState.MOVED_FACILITY);
        activity.submitVisit();
        AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(dialog.isShowing());
        assertTrue(((TextView) dialog.findViewById(android.R.id.message)).getText().toString()
                .contains(activity.getString(org.smartregister.chw.mothermentor.R.string
                        .mothermentor_iit_linked_close_note)));
        assertEquals(0, activity.submissions);

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, activity.submissions);
        assertTrue(activity.submitEnabled());

        activity.submitVisit();
        ((AlertDialog) ShadowAlertDialog.getLatestDialog())
                .getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, activity.submissions);
    }

    @Test
    public void returnedToCareAndThirdStrikeSubmitDirectly() {
        for (String status : new String[]{MotherMentorIitState.RETURNED_TO_CARE,
                MotherMentorIitState.REFUSED_THREE_TIMES, MotherMentorIitState.LOST_TO_FOLLOW_UP}) {
            ConfirmationActivity activity = activity(status);
            activity.submitVisit();
            assertEquals(status, 1, activity.submissions);
        }
    }

    private ConfirmationActivity activity(String status) {
        ConfirmationActivity activity = Robolectric.buildActivity(ConfirmationActivity.class).get();
        activity.setTheme(R.style.ChwTheme);
        activity.prepare(MotherMentorIitState.active("episode", "2026-10-20", "mother", 0)
                .close(status, new LocalDate(2026, 11, 6), ""));
        return activity;
    }

    public static class ConfirmationActivity extends MotherMentorVisitActivity {
        private MotherMentorIitState preview;
        int submissions;

        void prepare(MotherMentorIitState state) {
            preview = state;
            tvSubmit = new TextView(this);
        }

        boolean submitEnabled() { return tvSubmit.isEnabled(); }

        @Override boolean isFollowUpVisit() { return true; }
        @Override MotherMentorFollowUpType getFollowUpType() { return MotherMentorFollowUpType.MOTHER_IIT; }
        @Override protected boolean isFollowUpCardClosed() { return false; }
        @Override protected MotherMentorIitState getIitClosurePreview() { return preview; }
        @Override protected void submitConfirmedVisit() { submissions++; }
    }
}
