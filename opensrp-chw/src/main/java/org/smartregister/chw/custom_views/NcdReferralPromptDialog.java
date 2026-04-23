package org.smartregister.chw.custom_views;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import org.smartregister.chw.R;
import org.smartregister.chw.interactor.NcdCaseManagementInteractor;
import org.smartregister.chw.interactor.NcdCaseManagementInteractor.PendingNcdReferral;

import java.util.List;

/**
 * Confirmation prompt shown after an NCD Monthly Follow-Up visit is submitted and the
 * interactor has staged a {@link PendingNcdReferral}. The CHW sees the triggering reasons
 * and chooses to create the referral or skip it. Only the Confirm path invokes
 * {@code NcdReferralTaskHelper.createReferralIfNeeded(...)}; Skip leaves no event or task
 * behind (the visit itself is already persisted by the time this dialog is shown).
 */
public final class NcdReferralPromptDialog {

    private NcdReferralPromptDialog() {
        // utility
    }

    public interface Callbacks {
        void onConfirm();
        void onSkip();
    }

    public static void show(Activity activity, PendingNcdReferral pending, Callbacks callbacks) {
        if (activity == null || activity.isFinishing() || pending == null || callbacks == null) {
            if (callbacks != null) callbacks.onSkip();
            return;
        }

        int titleRes = NcdCaseManagementInteractor.ALERT_RED.equals(pending.alertLevel)
                ? R.string.ncd_referral_prompt_title_urgent
                : R.string.ncd_referral_prompt_title_non_emergency;

        new AlertDialog.Builder(activity)
                .setTitle(titleRes)
                .setMessage(buildMessage(activity, pending.reasons))
                .setCancelable(false)
                .setNegativeButton(R.string.ncd_referral_prompt_button_skip,
                        (d, w) -> callbacks.onSkip())
                .setPositiveButton(R.string.ncd_referral_prompt_button_create,
                        (d, w) -> callbacks.onConfirm())
                .show();
    }

    private static CharSequence buildMessage(Activity activity, List<String> reasons) {
        StringBuilder sb = new StringBuilder();
        sb.append(activity.getString(R.string.ncd_referral_prompt_body));
        if (reasons != null) {
            for (String reason : reasons) {
                sb.append("\n• ").append(reason);
            }
        }
        return sb.toString();
    }
}
