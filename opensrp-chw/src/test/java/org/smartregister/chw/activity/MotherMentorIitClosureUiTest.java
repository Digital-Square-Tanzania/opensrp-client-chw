package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;
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

import java.util.Date;
import java.util.Locale;

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
    @Config(qualifiers = "sw-w320dp")
    public void flatSummaryAlignsAndAllowsWrappingInActiveAndReturnedStates() {
        MotherMentorProfileActivity activity =
                Robolectric.buildActivity(IitProfileActivity.class).get();
        Configuration config = new Configuration(activity.getResources().getConfiguration());
        config.fontScale = 1.5f;
        config.setLocale(new Locale("sw"));
        activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());
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
        updateSummary(activity, profile, active);
        View summary = profile.findViewById(R.id.iit_summary);
        View closure = summary.findViewById(R.id.iit_summary_closure);
        TextView missed = summary.findViewById(R.id.iit_summary_missed_value);

        layout(root);
        assertHorizontalEdges(root, action, summary);
        assertEquals(View.GONE, closure.getVisibility());
        assertEquals("20-10-2026 · siku 17 zilizopita", missed.getText().toString());
        assertFalse(summary.isClickable());
        assertNull(summary.getBackground());

        action.setEnabled(false);
        MotherMentorIitState returned = active.close(
                MotherMentorIitState.RETURNED_TO_CARE, new LocalDate(2026, 11, 6), "");
        updateSummary(activity, profile, returned);
        TextView reason = closure.findViewById(R.id.iit_summary_closure_value);

        layout(root);
        assertHorizontalEdges(root, action, summary);
        assertHorizontalEdges(root, action, missed);
        assertHorizontalEdges(root, action, reason);
        assertEquals(View.VISIBLE, closure.getVisibility());
        assertEquals("Amerudi kwenye huduma · 06-11-2026", reason.getText().toString());
        assertFalse(action.isEnabled());
        for (TextView value : new TextView[] {missed, reason}) {
            assertTrue(value.getMaxLines() > 1);
            assertNull(value.getEllipsize());
            assertFalse(value.isClickable());
            assertNull(value.getBackground());
        }
    }

    private void updateSummary(MotherMentorProfileActivity activity, LinearLayout profile,
            MotherMentorIitState state) {
        ReflectionHelpers.callInstanceMethod(activity, "updateIitSummary",
                ReflectionHelpers.ClassParameter.from(LinearLayout.class, profile),
                ReflectionHelpers.ClassParameter.from(MotherMentorIitState.class, state));
    }

    private void layout(View root) {
        root.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
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

    public static class IitProfileActivity extends MotherMentorProfileActivity {
        @Override
        Date getVisitAvailabilityDate() {
            return new LocalDate(2026, 11, 6).toDate();
        }
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
