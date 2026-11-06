package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.activity.BaseMotherMentorVisitActivity;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorMotherVisitDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.interactor.ChildEidFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorIitVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.PartnerFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.repository.MotherMentorIitRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorVisitPresenter;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorIitState;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherVisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;
import org.smartregister.chw.util.MotherMentorFollowUpVisitUtils;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.util.MotherMentorIitClosureDialog;
import org.smartregister.chw.util.MotherMentorMotherClosureDialog;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.util.LangUtils;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class MotherMentorVisitActivity extends BaseMotherMentorVisitActivity {
    static final String EXTRA_FOLLOW_UP_VISIT = "extra_follow_up_visit";
    private boolean submitting;
    private boolean closureConfirmationOpen;

    public static void startMotherMentorVisitActivity(
            Activity activity, String baseEntityId, Boolean editMode) {
        startMotherMentorVisitActivity(activity, baseEntityId, editMode, false);
    }

    public static void startMotherMentorFollowUpVisitActivity(
            Activity activity, String baseEntityId, Boolean editMode) {
        startMotherMentorFollowUpVisitActivity(
                activity, baseEntityId, editMode, MotherMentorFollowUpType.MOTHER_IIT);
    }

    public static void startMotherMentorFollowUpVisitActivity(Activity activity,
            String baseEntityId, Boolean editMode, MotherMentorFollowUpType followUpType) {
        startMotherMentorVisitActivity(activity, baseEntityId, editMode, true, followUpType);
    }

    private static void startMotherMentorVisitActivity(
            Activity activity, String baseEntityId, Boolean editMode, boolean followUpVisit) {
        startMotherMentorVisitActivity(activity, baseEntityId, editMode, followUpVisit,
                MotherMentorFollowUpType.MOTHER_IIT);
    }

    private static void startMotherMentorVisitActivity(
            Activity activity,
            String baseEntityId,
            Boolean editMode,
            boolean followUpVisit,
            MotherMentorFollowUpType followUpType) {
        if (followUpVisit && followUpType == MotherMentorFollowUpType.PARTNER
                && MotherMentorPartnerClosureRepository.getState(
                        ChwApplication.getInstance().getRepository().getReadableDatabase(),
                        baseEntityId, null).isClosed()) return;
        Intent intent = new Intent(activity, MotherMentorVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE,
                Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        intent.putExtra(EXTRA_FOLLOW_UP_VISIT, followUpVisit);
        intent.putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, followUpType.getValue());
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void registerPresenter() {
        if (blockClosedCard()) {
            finish();
            return;
        }
        MotherMentorFollowUpType followUpType = getFollowUpType();
        presenter = new BaseMotherMentorVisitPresenter(memberObject, this,
                !isFollowUpVisit() ? new MotherMentorServiceVisitInteractor(
                                             Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES)
                        : followUpType == MotherMentorFollowUpType.PARTNER
                        ? new PartnerFollowUpVisitInteractor(followUpType.getEventType())
                        : followUpType == MotherMentorFollowUpType.CHILD_EID
                        ? new ChildEidFollowUpVisitInteractor(followUpType.getEventType())
                        : iitInteractor());
    }

    private BaseMotherMentorFollowUpVisitInteractor iitInteractor() {
        MotherMentorFollowUpType iit = MotherMentorFollowUpType.MOTHER_IIT;
        if (Boolean.TRUE.equals(isEditMode)) {
            Visit latest = MotherMentorLibrary.getInstance().visitRepository()
                    .getLatestVisit(baseEntityID, iit.getEventType());
            if (latest != null && !isCurrentIitSchema(latest)) {
                return new MotherMentorFollowUpVisitInteractor(iit.getEventType(), iit.getValue());
            }
        }
        return new MotherMentorIitVisitInteractor();
    }

    static boolean isCurrentIitSchema(Visit visit) {
        try {
            Map<String, String> values = MotherMentorVisitCompletion.observationValues(
                    new JSONObject(visit.getJson()).optJSONArray("obs"));
            return "2".equals(values.get(DBConstants.KEY.IIT_FOLLOW_UP_SCHEMA));
        } catch (Exception e) {
            Timber.e(e, "Unable to read saved IIT workflow schema");
            return false;
        }
    }

    @Override
    public void setUpView() {
        super.setUpView();
        if (isFollowUpVisit()) {
            tvSubmit.setText(org.smartregister.chw.R.string.mothermentor_save);
        }
    }

    @Override
    public void redrawHeader(MemberObject member) {
        if (!isFollowUpVisit()) {
            super.redrawHeader(member);
            return;
        }
        int savedVisits = MotherMentorFollowUpVisitUtils
                                  .getVisits(member.getBaseEntityId(), getFollowUpType())
                                  .size();
        int visitNumber =
                Boolean.TRUE.equals(isEditMode) ? Math.max(1, savedVisits) : savedVisits + 1;
        if (getFollowUpType() == MotherMentorFollowUpType.CHILD_EID) {
            tvTitle.setText(
                    getString(org.smartregister.chw.R.string.mothermentor_child_visit_header,
                            member.getFullName(), childAge(member.getBaseEntityId()), visitNumber));
        } else {
            tvTitle.setText(getString(org.smartregister.chw.R.string.mothermentor_visit_header,
                    member.getFullName(), member.getAge(), visitNumber));
        }
    }

    private String childAge(String baseEntityId) {
        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT " + DBConstants.KEY.DOB
                                 + " FROM ec_family_member WHERE "
                                 + DBConstants.KEY.BASE_ENTITY_ID + " = ? LIMIT 1",
                         new String[] {baseEntityId})) {
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    DateTime dob = new DateTime(cursor.getString(0));
                    int totalMonths = Math.max(0,
                            Months.monthsBetween(dob.withTimeAtStartOfDay(),
                                          DateTime.now().withTimeAtStartOfDay())
                                    .getMonths());
                    int years = totalMonths / 12;
                    int months = totalMonths % 12;
                    if (years == 0)
                        return getString(
                                org.smartregister.chw.R.string.mothermentor_child_age_months,
                                months);
                    if (months == 0)
                        return getString(
                                org.smartregister.chw.R.string.mothermentor_child_age_years, years);
                    return getString(
                            org.smartregister.chw.R.string.mothermentor_child_age_years_months,
                            years, months);
                }
            }
        } catch (Exception ignored) {
        }
        return String.valueOf(memberObject.getAge());
    }

    @Override
    public void submitVisit() {
        if (submitting || closureConfirmationOpen || blockClosedCard()) return;
        if (!isFollowUpVisit()) {
            MotherMentorMotherClosure preview = getMotherClosurePreview();
            if (preview.isClosed()) {
                closureConfirmationOpen = true;
                if (tvSubmit != null) tvSubmit.setEnabled(false);
                MotherMentorMotherClosureDialog.show(this, preview, () -> {
                    closureConfirmationOpen = false;
                    submitConfirmedVisit();
                }, this::resetSubmission);
                return;
            }
        } else if (getFollowUpType() == MotherMentorFollowUpType.MOTHER_IIT) {
            MotherMentorIitState preview = getIitClosurePreview();
            if (preview.requiresConfirmation()) {
                closureConfirmationOpen = true;
                if (tvSubmit != null) tvSubmit.setEnabled(false);
                MotherMentorIitClosureDialog.show(this, preview, () -> {
                    closureConfirmationOpen = false;
                    submitConfirmedVisit();
                }, this::resetSubmission);
                return;
            }
        }
        submitConfirmedVisit();
    }

    protected void submitConfirmedVisit() {
        if (submitting || blockClosedCard()) return;
        submitting = true;
        if (tvSubmit != null) tvSubmit.setEnabled(false);
        super.submitVisit();
    }

    protected MotherMentorMotherClosure getMotherClosurePreview() {
        Map<String, String> answers = stagedAnswers();
        Visit editing = Boolean.TRUE.equals(isEditMode)
                ? org.smartregister.chw.mothermentor.MotherMentorLibrary.getInstance()
                          .visitRepository()
                          .getLatestVisit(baseEntityID, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES)
                : null;
        LocalDate serviceDate = editing == null
                ? LocalDate.now()
                : MotherMentorMotherVisitDao.getServiceDate(editing);
        return MotherMentorMotherClosureRepository.preview(
                ChwApplication.getInstance().getRepository().getReadableDatabase(), baseEntityID,
                editing == null ? null : editing.getVisitId(), answers, serviceDate);
    }

    protected MotherMentorIitState getIitClosurePreview() {
        Visit editing = Boolean.TRUE.equals(isEditMode)
                ? org.smartregister.chw.mothermentor.MotherMentorLibrary.getInstance()
                          .visitRepository().getLatestVisit(baseEntityID,
                                  MotherMentorFollowUpType.MOTHER_IIT.getEventType())
                : null;
        LocalDate serviceDate = editing == null
                ? LocalDate.now() : MotherMentorMotherVisitDao.getServiceDate(editing);
        return MotherMentorIitRepository.preview(
                ChwApplication.getInstance().getRepository().getReadableDatabase(), baseEntityID,
                editing == null ? null : editing.getVisitId(), stagedAnswers(), serviceDate);
    }

    private Map<String, String> stagedAnswers() {
        Map<String, String> answers = new HashMap<>();
        for (BaseMotherMentorVisitAction action : actionList.values()) {
            if (!action.isValid()) continue;
            try {
                answers.putAll(MotherMentorMotherVisitUtils.formValues(
                        new JSONObject(action.getJsonPayload())));
            } catch (Exception e) {
                Timber.e(e, "Unable to read Mother Mentor answers for closure confirmation");
            }
        }
        return answers;
    }

    protected boolean isMotherCardClosed() {
        return !isFollowUpVisit() && baseEntityID != null
                && MotherMentorMotherClosureRepository.isClosed(
                        ChwApplication.getInstance().getRepository().getReadableDatabase(),
                        baseEntityID);
    }

    protected boolean isFollowUpCardClosed() {
        if (!isFollowUpVisit() || baseEntityID == null) return false;
        if (getFollowUpType() == MotherMentorFollowUpType.MOTHER_IIT) {
            return MotherMentorIitRepository.getState(
                    ChwApplication.getInstance().getRepository().getReadableDatabase(),
                    baseEntityID, null).isClosed();
        }
        return MotherMentorMotherClosureRepository.isFollowUpEnrollmentClosed(
                        ChwApplication.getInstance().getRepository().getReadableDatabase(),
                        baseEntityID, getFollowUpType());
    }

    private boolean blockClosedCard() {
        if (!isMotherCardClosed() && !isFollowUpCardClosed()) return false;
        resetSubmission();
        displayToast(getString(isFollowUpVisit()
                        && getFollowUpType() == MotherMentorFollowUpType.CHILD_EID
                ? org.smartregister.chw.mothermentor.R.string.mothermentor_child_eid_closed_message
                : org.smartregister.chw.mothermentor.R.string.mothermentor_mother_closed_message));
        lockMotherVisitSubmit();
        return true;
    }

    private void lockMotherVisitSubmit() {
        if (tvSubmit != null) {
            tvSubmit.setEnabled(false);
            tvSubmit.setOnClickListener(null);
        }
    }

    @Override
    public void redrawVisitUI() {
        if (isMotherCardClosed() || isFollowUpCardClosed()) {
            lockMotherVisitSubmit();
            return;
        }
        super.redrawVisitUI();
    }

    private void resetSubmission() {
        closureConfirmationOpen = false;
        submitting = false;
        if (tvSubmit != null) tvSubmit.setEnabled(true);
    }

    @Override
    public void startForm(BaseMotherMentorVisitAction action) {
        if (!blockClosedCard()) super.startForm(action);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        blockClosedCard();
    }

    @Override
    protected void onSubmissionCancelled() {
        submitting = false;
        super.onSubmissionCancelled();
    }

    @Override
    public void displayToast(String message) {
        resetSubmission();
        super.displayToast(message);
        if (isMotherCardClosed() || isFollowUpCardClosed()) lockMotherVisitSubmit();
    }

    boolean isFollowUpVisit() {
        return getIntent() != null && getIntent().getBooleanExtra(EXTRA_FOLLOW_UP_VISIT, false);
    }

    MotherMentorFollowUpType getFollowUpType() {
        return MotherMentorFollowUpType.fromValue(getIntent() == null
                        ? null
                        : getIntent().getStringExtra(
                                  MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE));
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        if (blockClosedCard()) return;
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
        MemberObject memberObject = MotherMentorDao.getMember(baseEntityId);
        if (memberObject != null) {
            return memberObject;
        }

        memberObject = MotherMentorDao.getContact(baseEntityId);
        return memberObject != null ? memberObject
                                    : MotherMentorDao.getSecondaryEnrollmentMember(baseEntityId);
    }

    @Override
    protected void attachBaseContext(Context base) {
        String lang = LangUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(LangUtils.setAppLocale(base, lang));
    }
}
