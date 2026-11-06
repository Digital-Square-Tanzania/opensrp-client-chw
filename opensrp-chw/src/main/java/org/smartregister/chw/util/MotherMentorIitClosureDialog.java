package org.smartregister.chw.util;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import org.smartregister.chw.mothermentor.R;
import org.smartregister.chw.mothermentor.util.MotherMentorIitState;
import org.smartregister.chw.mothermentor.util.MotherMentorIitText;

public final class MotherMentorIitClosureDialog {
    private MotherMentorIitClosureDialog() {}

    public static void show(Context context, MotherMentorIitState preview, Runnable confirm,
            Runnable cancel) {
        String message = MotherMentorIitText.reason(context, preview.getStatus());
        if (preview.isMamaKinaraLinked()) {
            message += "\n\n" + context.getString(R.string.mothermentor_iit_linked_close_note);
        }
        new AlertDialog.Builder(context)
                .setTitle(R.string.mothermentor_iit_confirm_close_title)
                .setMessage(message)
                .setPositiveButton(R.string.mothermentor_iit_confirm_close_confirm,
                        (dialog, which) -> confirm.run())
                .setNegativeButton(R.string.mothermentor_iit_confirm_close_cancel,
                        (dialog, which) -> cancel.run())
                .setOnCancelListener(dialog -> cancel.run())
                .show();
    }
}
