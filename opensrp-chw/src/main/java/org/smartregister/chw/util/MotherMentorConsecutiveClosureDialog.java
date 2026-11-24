package org.smartregister.chw.util;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import org.smartregister.chw.R;

/** Confirms a consecutive-event closure before handing the visit to the presenter. */
public final class MotherMentorConsecutiveClosureDialog {
    private MotherMentorConsecutiveClosureDialog() {}

    public static void show(Context context, String reason, Runnable save, Runnable cancel) {
        new AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.mothermentor_consecutive_closure_message, reason))
                .setPositiveButton(R.string.mothermentor_consecutive_closure_save, (dialog, which) -> save.run())
                .setNegativeButton(R.string.mothermentor_consecutive_closure_cancel, (dialog, which) -> cancel.run())
                .setCancelable(false)
                .show();
    }
}
