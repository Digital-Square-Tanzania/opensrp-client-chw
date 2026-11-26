package org.smartregister.chw.util;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import org.smartregister.chw.mothermentor.R;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.PendingTest;
import org.smartregister.chw.mothermentor.util.MotherMentorDeliveryChildren;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUtils.TerminalOutcome;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosureLabels;

/** Confirmation shared by normal submission and completion of a saved mother visit. */
public final class MotherMentorMotherClosureDialog {
    private MotherMentorMotherClosureDialog() {}

    public static AlertDialog show(
            Context context, MotherMentorMotherClosure state, Runnable confirm, Runnable cancel) {
        return show(context, state, MotherMentorMotherClosureLabels.note(context, state.getStatus()),
                confirm, cancel);
    }

    public static AlertDialog showForResult(Context context, MotherMentorMotherClosure state,
            PendingTest sample, TerminalOutcome outcome, Runnable confirm, Runnable cancel) {
        if (outcome == TerminalOutcome.CHILD_CONFIRMED_POSITIVE
                && MotherMentorMotherClosure.GRADUATED.equals(state.getStatus())) {
            String child = sample.hasSiblings()
                    ? MotherMentorDeliveryChildren.label(context, sample.getChildNumber(), "")
                    : context.getString(org.smartregister.chw.R.string.mama_results_sample_child);
            String note = context.getString(
                    org.smartregister.chw.R.string.mama_results_positive_with_graduated_sibling, child);
            return show(context, state, note, confirm, cancel);
        }
        return show(context, state, confirm, cancel);
    }

    private static AlertDialog show(Context context, MotherMentorMotherClosure state, String note,
            Runnable confirm, Runnable cancel) {
        String message = context.getString(R.string.mothermentor_mother_close_message) + "\n\n"
                + MotherMentorMotherClosureLabels.label(context, state.getStatus()) + "\n"
                + note;
        AlertDialog dialog =
                new AlertDialog.Builder(context, com.vijay.jsonwizard.R.style.AppThemeAlertDialog)
                        .setTitle(R.string.mothermentor_mother_close_title)
                        .setMessage(message)
                        .setPositiveButton(R.string.mothermentor_mother_close_confirm,
                                (ignored, which) -> confirm.run())
                        .setNegativeButton(R.string.mothermentor_mother_close_cancel,
                                (ignored, which) -> cancel.run())
                        .setOnCancelListener(ignored -> cancel.run())
                        .create();
        dialog.show();
        return dialog;
    }
}
