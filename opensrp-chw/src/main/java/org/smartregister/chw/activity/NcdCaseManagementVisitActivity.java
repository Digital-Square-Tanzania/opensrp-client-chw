package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.custom_views.NcdReferralPromptDialog;
import org.smartregister.chw.interactor.NcdCaseManagementInteractor;
import org.smartregister.chw.interactor.NcdCaseManagementInteractor.PendingNcdReferral;
import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.chw.presenter.NcdCaseManagementVisitPresenter;
import org.smartregister.chw.util.NcdReferralTaskHelper;

import timber.log.Timber;

public class NcdCaseManagementVisitActivity extends NcdVisitActivity {

    public static void startMe(Activity activity, String baseEntityId, Boolean isEditMode) {
        Intent intent = new Intent(activity, NcdCaseManagementVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, isEditMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.SKELETON_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void registerPresenter() {
        presenter = new NcdCaseManagementVisitPresenter(memberObject, this);
    }

    /**
     * After the visit is submitted, check whether the interactor staged a pending referral.
     * If so, prompt the CHW to confirm before any Referral Registration event / task is
     * written, then close the activity. If not, close immediately like the base flow.
     */
    @Override
    public void submittedAndClose(String results) {
        PendingNcdReferral pending = readPendingReferral();
        if (pending == null) {
            super.submittedAndClose(results);
            return;
        }

        runOnUiThread(() -> NcdReferralPromptDialog.show(this, pending, new NcdReferralPromptDialog.Callbacks() {
            @Override
            public void onConfirm() {
                NcdReferralTaskHelper.createReferralIfNeeded(
                        pending.baseEntityId,
                        null,
                        pending.alertLevel,
                        pending.description);
                clearPendingReferral();
                NcdCaseManagementVisitActivity.super.submittedAndClose(results);
            }

            @Override
            public void onSkip() {
                Timber.i("NCD referral skipped by CHW for %s (level=%s, reasons=%s)",
                        pending.baseEntityId, pending.alertLevel, pending.reasons);
                clearPendingReferral();
                NcdCaseManagementVisitActivity.super.submittedAndClose(results);
            }
        }));
    }

    private PendingNcdReferral readPendingReferral() {
        if (!(presenter instanceof NcdCaseManagementVisitPresenter)) return null;
        NcdCaseManagementInteractor interactor =
                ((NcdCaseManagementVisitPresenter) presenter).getCaseManagementInteractor();
        return interactor != null ? interactor.getPendingReferral() : null;
    }

    private void clearPendingReferral() {
        if (!(presenter instanceof NcdCaseManagementVisitPresenter)) return;
        NcdCaseManagementInteractor interactor =
                ((NcdCaseManagementVisitPresenter) presenter).getCaseManagementInteractor();
        if (interactor != null) interactor.clearPendingReferral();
    }
}
