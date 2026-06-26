package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.smartregister.chw.actionhelper.NcdFollowUpStatusActionHelper;
import org.smartregister.chw.custom_views.NcdReferralPromptDialog;
import org.smartregister.chw.interactor.NcdCaseManagementInteractor;
import org.smartregister.chw.interactor.NcdCaseManagementInteractor.PendingNcdReferral;
import org.smartregister.chw.dao.NcdCaseManagementDao;
import org.smartregister.chw.dao.TreatmentSupporterDao;
import org.smartregister.chw.model.NcdReferralInputs;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;
import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.chw.ncd.util.AppExecutors;
import org.smartregister.chw.ncd.util.NcdUtil;
import org.smartregister.chw.presenter.NcdCaseManagementVisitPresenter;
import org.smartregister.chw.util.MarkClientAsDeceasedHelper;
import org.smartregister.chw.util.NcdReferralTaskHelper;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

public class NcdCaseManagementVisitActivity extends NcdVisitActivity {

    private static final String CLOSE_REASON_DECEASED = "deceased";

    private final LinkedHashMap<String, BaseNcdVisitAction> completeActionList = new LinkedHashMap<>();
    private final AppExecutors appExecutors = new AppExecutors();
    private String pendingMortalityResult;

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

    @Override
    public void initializeActions(@NonNull LinkedHashMap<String, BaseNcdVisitAction> actions) {
        completeActionList.clear();
        completeActionList.putAll(actions);
        super.initializeActions(actions);
        applyFollowUpStatusGate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON
                && resultCode == RESULT_OK) {
            applyFollowUpStatusGate();
        }
    }

    private void applyFollowUpStatusGate() {
        BaseNcdVisitAction statusAction = findStatusAction(completeActionList);
        if (statusAction == null || actionList == null) {
            return;
        }

        String status = NcdFollowUpStatusActionHelper.extractValue(
                statusAction.getJsonPayload(), NcdFollowUpStatusActionHelper.KEY_STATUS);

        actionList.clear();
        if (NcdFollowUpStatusActionHelper.STATUS_CONTINUING.equals(status)) {
            actionList.putAll(completeActionList);
        } else {
            actionList.put(statusAction.getTitle(), statusAction);
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        redrawVisitUI();
    }

    private BaseNcdVisitAction findStatusAction(Map<String, BaseNcdVisitAction> actions) {
        if (actions == null) {
            return null;
        }
        for (BaseNcdVisitAction action : actions.values()) {
            if (org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_STATUS.equals(action.getFormName())) {
                return action;
            }
        }
        return null;
    }

    /**
     * After the visit is submitted, check whether the interactor staged a pending referral.
     * If so, prompt the CHW to confirm before any Referral Registration event / task is
     * written, then close the activity. If not, close immediately like the base flow.
     */
    @Override
    public void submittedAndClose(String results) {
        if (isSubmittedAsDeceased()) {
            pendingMortalityResult = results;
            createMortalityRecords(results);
            return;
        }

        PendingNcdReferral pending = readPendingReferral();
        if (pending == null) {
            super.submittedAndClose(results);
            return;
        }

        // Resolve the open-referral guard, registered caregiver, and referral facility list off
        // the main thread (these DB reads must not run on the UI thread). When the client already
        // has an open NCD referral, skip the prompt entirely — createReferralIfNeeded would dedupe
        // it anyway, so asking the CHW to fill a form that produces nothing is misleading.
        appExecutors.diskIO().execute(() -> {
            if (NcdCaseManagementDao.hasOpenReferral(pending.baseEntityId)) {
                appExecutors.mainThread().execute(() -> skipReferralPrompt(pending, results));
                return;
            }
            TreatmentSupporterDao.Caregiver caregiver =
                    TreatmentSupporterDao.getRegisteredCaregiver(pending.baseEntityId);
            Map<String, String> facilities = LocationUtils.INSTANCE.getFacilitiesKeyAndName();
            appExecutors.mainThread().execute(() ->
                    showReferralPrompt(pending, caregiver, facilities, results));
        });
    }

    private void skipReferralPrompt(PendingNcdReferral pending, String results) {
        Timber.i("NCD referral prompt skipped — open referral already exists for %s",
                pending.baseEntityId);
        displayToast(getString(org.smartregister.chw.R.string.ncd_referral_already_open));
        clearPendingReferral();
        super.submittedAndClose(results);
    }

    private void showReferralPrompt(PendingNcdReferral pending,
                                    TreatmentSupporterDao.Caregiver caregiver,
                                    Map<String, String> facilities, String results) {
        NcdReferralPromptDialog.show(this, pending, caregiver, facilities, new NcdReferralPromptDialog.Callbacks() {
            @Override
            public void onConfirm(NcdReferralInputs inputs) {
                NcdReferralTaskHelper.createReferralIfNeeded(
                        pending.baseEntityId,
                        null,
                        pending.alertLevel,
                        pending.description,
                        pending.problemKeys,
                        pending.reasons,
                        inputs);
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
        });
    }

    @Override
    public void submitVisit() {
        if (pendingMortalityResult != null) {
            createMortalityRecords(pendingMortalityResult);
            return;
        }
        super.submitVisit();
    }

    private boolean isSubmittedAsDeceased() {
        NcdCaseManagementInteractor interactor = getCaseManagementInteractor();
        return interactor != null
                && NcdFollowUpStatusActionHelper.STATUS_DEAD.equals(
                interactor.getLastSubmittedFollowUpStatus());
    }

    private void createMortalityRecords(String results) {
        displayProgressBar(true);
        if (tvSubmit != null) {
            tvSubmit.setEnabled(false);
        }

        NcdCaseManagementInteractor interactor = getCaseManagementInteractor();
        String dateOfDeath = interactor != null ? interactor.getLastSubmittedDateOfDeath() : null;
        appExecutors.diskIO().execute(() -> {
            try {
                String baseEntityId = memberObject.getBaseEntityId();
                if (!NcdCaseManagementDao.hasMortalityRecord(baseEntityId)) {
                    MarkClientAsDeceasedHelper.markAsDeceased(
                            NcdCaseManagementVisitActivity.this, memberObject, dateOfDeath);
                }
                if (!NcdCaseManagementDao.isNcdCaseClosed(baseEntityId)) {
                    closeDeceasedNcdCase();
                }
                NcdCaseManagementDao.cancelOpenTasks(baseEntityId);
                NcdCaseManagementDao.voidOpenReferrals(baseEntityId);

                appExecutors.mainThread().execute(() -> {
                    pendingMortalityResult = null;
                    NcdCaseManagementVisitActivity.super.submittedAndClose(results);
                });
            } catch (Exception e) {
                Timber.e(e, "Unable to create mortality records for NCD client");
                appExecutors.mainThread().execute(() -> {
                    displayProgressBar(false);
                    if (tvSubmit != null) {
                        tvSubmit.setEnabled(true);
                    }
                    displayToast(getString(org.smartregister.chw.R.string.ncd_death_registration_save_failed));
                });
            }
        });
    }

    private void closeDeceasedNcdCase() throws Exception {
        JSONObject form = (new FormUtils()).getFormJsonFromRepositoryOrAssets(
                this, org.smartregister.chw.util.Constants.JsonForm.NCD_CASE_MANAGEMENT_CLOSE);
        if (form == null) {
            throw new IllegalStateException("Unable to load NCD case management close form");
        }
        form.put(org.smartregister.family.util.JsonFormUtils.ENTITY_ID,
                memberObject.getBaseEntityId());
        JSONArray fields = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1)
                .getJSONArray(org.smartregister.family.util.JsonFormUtils.FIELDS);
        org.smartregister.chw.core.utils.FormUtils.updateFormField(
                fields, "close_reason", CLOSE_REASON_DECEASED);
        NcdUtil.saveFormEvent(form.toString());
    }

    private PendingNcdReferral readPendingReferral() {
        NcdCaseManagementInteractor interactor = getCaseManagementInteractor();
        return interactor != null ? interactor.getPendingReferral() : null;
    }

    private NcdCaseManagementInteractor getCaseManagementInteractor() {
        if (!(presenter instanceof NcdCaseManagementVisitPresenter)) return null;
        return ((NcdCaseManagementVisitPresenter) presenter).getCaseManagementInteractor();
    }

    private void clearPendingReferral() {
        if (!(presenter instanceof NcdCaseManagementVisitPresenter)) return;
        NcdCaseManagementInteractor interactor =
                ((NcdCaseManagementVisitPresenter) presenter).getCaseManagementInteractor();
        if (interactor != null) interactor.clearPendingReferral();
    }
}
