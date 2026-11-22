package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
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

    @Test
    public void iitSummaryRowsAlignWithFollowUpButtonInActiveAndReturnedStates() {
        MotherMentorProfileActivity activity =
                Robolectric.buildActivity(MotherMentorProfileActivity.class).get();
        activity.setTheme(R.style.ChwTheme);
        ViewGroup root = (ViewGroup) LayoutInflater.from(activity).inflate(
                org.smartregister.chw.mothermentor.R.layout.activity_mothermentor_profile, null);
        LinearLayout profile = root.findViewById(
                org.smartregister.chw.mothermentor.R.id.profile_name_layout);
        TextView action = root.findViewById(
                org.smartregister.chw.mothermentor.R.id.textview_record_mothermentor);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", action);
        MotherMentorIitState active =
                MotherMentorIitState.active("episode", "2026-10-20", "mother", 0);
        ReflectionHelpers.ClassParameter<LinearLayout> header =
                ReflectionHelpers.ClassParameter.from(LinearLayout.class, profile);
        ReflectionHelpers.callInstanceMethod(activity, "updateIitDetails", header,
                ReflectionHelpers.ClassParameter.from(MotherMentorIitState.class, active));
        TextView details = ReflectionHelpers.getField(activity, "iitDetailsView");

        layout(root);
        assertHorizontalEdges(root, action, details);

        action.setEnabled(false);
        MotherMentorIitState returned = active.close(
                MotherMentorIitState.RETURNED_TO_CARE, new LocalDate(2026, 11, 6), "");
        ReflectionHelpers.callInstanceMethod(activity, "updateIitClosureBanner", header,
                ReflectionHelpers.ClassParameter.from(MotherMentorIitState.class, returned));
        TextView banner = ReflectionHelpers.getField(activity, "iitClosureBanner");

        layout(root);
        assertHorizontalEdges(root, action, details);
        assertHorizontalEdges(root, action, banner);
        assertEquals(details.getPaddingStart(), banner.getPaddingStart());
        assertEquals(details.getPaddingEnd(), banner.getPaddingEnd());
    }

    private void layout(View root) {
        root.measure(View.MeasureSpec.makeMeasureSpec(420, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }

    private void assertHorizontalEdges(ViewGroup root, View action, View row) {
        Rect actionBounds = new Rect(0, 0, action.getWidth(), action.getHeight());
        Rect rowBounds = new Rect(0, 0, row.getWidth(), row.getHeight());
        root.offsetDescendantRectToMyCoords(action, actionBounds);
        root.offsetDescendantRectToMyCoords(row, rowBounds);
        assertTrue(actionBounds.width() > 0);
        assertEquals(actionBounds.left, rowBounds.left);
        assertEquals(actionBounds.right, rowBounds.right);
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
