package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.activity.BaseMotherMentorVisitActivity;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.interactor.MotherMentorHouseholdVisitInteractor;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorVisitPresenter;
import org.smartregister.chw.mothermentor.repository.MotherMentorHouseholdClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorHouseholdClosure;
import org.smartregister.chw.util.MotherMentorConsecutiveClosureDialog;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.util.MotherMentorHouseholdTitle;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

public class MotherMentorHouseholdVisitActivity extends BaseMotherMentorVisitActivity {
    private MotherMentorHouseholdVisitInteractor interactor;
    private boolean closureConfirmationOpen;
    private boolean submitting;

    public static void startMotherMentorHouseholdVisitActivity(
            Activity activity, String baseEntityId, Boolean editMode) {
        if (MotherMentorHouseholdClosureRepository.getState(
                ChwApplication.getInstance().getRepository().getReadableDatabase(), baseEntityId, null)
                .isClosed()) return;
        Intent intent = new Intent(activity, MotherMentorHouseholdVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(
                Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE,
                Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void submitVisit() {
        if (submitting || closureConfirmationOpen) return;
        MotherMentorHouseholdClosure preview = getClosurePreview();
        if (preview.isConsecutiveClosure()) {
            closureConfirmationOpen = true;
            if (tvSubmit != null) tvSubmit.setEnabled(false);
            MotherMentorConsecutiveClosureDialog.show(this, consecutiveClosureReason(preview), () -> {
                closureConfirmationOpen = false;
                submitConfirmedVisit();
            }, this::onSubmissionCancelled);
            return;
        }
        submitConfirmedVisit();
    }

    protected MotherMentorHouseholdClosure getClosurePreview() {
        return interactor == null ? MotherMentorHouseholdClosure.active() : interactor.previewClosure();
    }

    private String consecutiveClosureReason(MotherMentorHouseholdClosure preview) {
        switch (preview.getClosureReason()) {
            case MotherMentorHouseholdClosure.HOUSE_CLOSED_3:
                return getString(org.smartregister.chw.mothermentor.R.string.mothermentor_household_missing_house_closed);
            case MotherMentorHouseholdClosure.OTHER_3:
                return getString(org.smartregister.chw.R.string.mothermentor_consecutive_closure_household_other);
            default:
                return getString(org.smartregister.chw.R.string.mothermentor_consecutive_closure_lost_to_follow_up);
        }
    }

    private void submitConfirmedVisit() {
        if (submitting) return;
        submitting = true;
        if (tvSubmit != null) tvSubmit.setEnabled(false);
        super.submitVisit();
    }

    @Override
    protected void onSubmissionCancelled() {
        closureConfirmationOpen = false;
        submitting = false;
        super.onSubmissionCancelled();
    }

    @Override
    public void displayToast(String message) {
        submitting = false;
        if (tvSubmit != null) tvSubmit.setEnabled(true);
        super.displayToast(message);
    }

    @Override
    protected void registerPresenter() {
        interactor = new MotherMentorHouseholdVisitInteractor(
                Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, Utils.metadata().familyRegister.tableName);
        presenter = new BaseMotherMentorVisitPresenter(memberObject, this, interactor);
    }

    @Override
    public void redrawHeader(MemberObject memberObject) {
        tvTitle.setText(MotherMentorHouseholdTitle.format(this, memberObject.getFirstName()));
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = MotherMentorFormWizardUtils.getFormIntent(this, jsonForm);
        intent.putExtra(
                org.smartregister.family.util.Constants.WizardFormActivity.EnableOnCloseDialog,
                false);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void submittedAndClose(String results) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(Constants.JSON_FORM_EXTRA.JSON, results);
        setResult(Activity.RESULT_OK, returnIntent);
        close();
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return null;
        }

        MemberObject householdMember =
                MotherMentorDao.getHouseholdMember(
                        baseEntityId, Utils.metadata().familyRegister.tableName);
        if (householdMember != null) {
            return householdMember;
        }

        MemberObject memberObject = MotherMentorDao.getMember(baseEntityId);
        return memberObject != null ? memberObject : MotherMentorDao.getContact(baseEntityId);
    }

    @Override
    protected void attachBaseContext(Context base) {
        String lang = LangUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(LangUtils.setAppLocale(base, lang));
    }
}
