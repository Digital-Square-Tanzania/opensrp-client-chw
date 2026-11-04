package org.smartregister.chw.util;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import org.smartregister.chw.mothermentor.R;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosureLabels;

/** Confirmation shared by normal submission and completion of a saved mother visit. */
public final class MotherMentorMotherClosureDialog {
    private MotherMentorMotherClosureDialog() {}

    public static AlertDialog show(
            Context context, MotherMentorMotherClosure state, Runnable confirm, Runnable cancel) {
        String message = context.getString(R.string.mothermentor_mother_close_message) + "\n\n"
                + MotherMentorMotherClosureLabels.label(context, state.getStatus()) + "\n"
                + MotherMentorMotherClosureLabels.note(context, state.getStatus());
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
