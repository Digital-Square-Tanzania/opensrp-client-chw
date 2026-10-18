package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.Months;
import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.R;
import org.smartregister.chw.interactor.ChildEidFollowUpVisitInteractor;
import org.smartregister.chw.interactor.MotherMentorFollowUpVisitInteractor;
import org.smartregister.chw.interactor.MotherMentorServiceVisitInteractor;
import org.smartregister.chw.interactor.PartnerFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.activity.BaseMotherMentorVisitActivity;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorVisitPresenter;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MotherMentorFollowUpVisitUtils;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

public class MotherMentorVisitActivity extends BaseMotherMentorVisitActivity {
    static final String EXTRA_FOLLOW_UP_VISIT = "extra_follow_up_visit";
    private boolean submitting;

    public static void startMotherMentorVisitActivity(
            Activity activity, String baseEntityId, Boolean editMode) {
        startMotherMentorVisitActivity(activity, baseEntityId, editMode, false);
    }

    public static void startMotherMentorFollowUpVisitActivity(
            Activity activity, String baseEntityId, Boolean editMode) {
        startMotherMentorFollowUpVisitActivity(
                activity, baseEntityId, editMode, MotherMentorFollowUpType.MOTHER_IIT);
    }

    public static void startMotherMentorFollowUpVisitActivity(
            Activity activity,
            String baseEntityId,
            Boolean editMode,
            MotherMentorFollowUpType followUpType) {
        startMotherMentorVisitActivity(activity, baseEntityId, editMode, true, followUpType);
    }

    private static void startMotherMentorVisitActivity(
            Activity activity, String baseEntityId, Boolean editMode, boolean followUpVisit) {
        startMotherMentorVisitActivity(
                activity,
                baseEntityId,
                editMode,
                followUpVisit,
                MotherMentorFollowUpType.MOTHER_IIT);
    }

    private static void startMotherMentorVisitActivity(
            Activity activity,
            String baseEntityId,
            Boolean editMode,
            boolean followUpVisit,
            MotherMentorFollowUpType followUpType) {
        Intent intent = new Intent(activity, MotherMentorVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(
                Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE,
                Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        intent.putExtra(EXTRA_FOLLOW_UP_VISIT, followUpVisit);
        intent.putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, followUpType.getValue());
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void registerPresenter() {
        MotherMentorFollowUpType followUpType = getFollowUpType();
        presenter =
                new BaseMotherMentorVisitPresenter(
                        memberObject,
                        this,
                        !isFollowUpVisit()
                                ? new MotherMentorServiceVisitInteractor(
                                        Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES)
                                : followUpType == MotherMentorFollowUpType.PARTNER
                                        ? new PartnerFollowUpVisitInteractor(
                                                followUpType.getEventType())
                                        : followUpType == MotherMentorFollowUpType.CHILD_EID
                                                ? new ChildEidFollowUpVisitInteractor(
                                                        followUpType.getEventType())
                                                : new MotherMentorFollowUpVisitInteractor(
                                                        followUpType.getEventType(),
                                                        followUpType.getValue()));
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
        int savedVisits =
                MotherMentorFollowUpVisitUtils.getVisits(
                                member.getBaseEntityId(), getFollowUpType())
                        .size();
        int visitNumber =
                Boolean.TRUE.equals(isEditMode) ? Math.max(1, savedVisits) : savedVisits + 1;
        if (getFollowUpType() == MotherMentorFollowUpType.CHILD_EID) {
            tvTitle.setText(
                    getString(
                            org.smartregister.chw.R.string.mothermentor_child_visit_header,
                            member.getFullName(),
                            childAge(member.getBaseEntityId()),
                            visitNumber));
        } else {
            tvTitle.setText(
                    getString(
                            org.smartregister.chw.R.string.mothermentor_visit_header,
                            member.getFullName(),
                            member.getAge(),
                            visitNumber));
        }
    }

    private String childAge(String baseEntityId) {
        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
            try (Cursor cursor =
                    db.rawQuery(
                            "SELECT "
                                    + DBConstants.KEY.DOB
                                    + " FROM ec_family_member WHERE "
                                    + DBConstants.KEY.BASE_ENTITY_ID
                                    + " = ? LIMIT 1",
                            new String[] {baseEntityId})) {
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    DateTime dob = new DateTime(cursor.getString(0));
                    int totalMonths =
                            Math.max(
                                    0,
                                    Months.monthsBetween(
                                                    dob.withTimeAtStartOfDay(),
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
                            years,
                            months);
                }
            }
        } catch (Exception ignored) {
        }
        return String.valueOf(memberObject.getAge());
    }

    @Override
    public void submitVisit() {
        if (submitting) {
            return;
        }
        submitting = true;
        tvSubmit.setEnabled(false);
        super.submitVisit();
    }

    @Override
    public void displayToast(String message) {
        submitting = false;
        if (tvSubmit != null) tvSubmit.setEnabled(true);
        super.displayToast(message);
    }

    boolean isFollowUpVisit() {
        return getIntent() != null && getIntent().getBooleanExtra(EXTRA_FOLLOW_UP_VISIT, false);
    }

    MotherMentorFollowUpType getFollowUpType() {
        return MotherMentorFollowUpType.fromValue(
                getIntent() == null
                        ? null
                        : getIntent()
                                .getStringExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE));
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);
        form.setHomeAsUpIndicator(org.smartregister.chw.core.R.mipmap.ic_cross_white);
        form.setSaveLabel(getString(org.smartregister.chw.core.R.string.save));

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(
                org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(
                org.smartregister.family.util.Constants.WizardFormActivity.EnableOnCloseDialog,
                false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
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
        return memberObject != null
                ? memberObject
                : MotherMentorDao.getSecondaryEnrollmentMember(baseEntityId);
    }

    @Override
    protected void attachBaseContext(Context base) {
        String lang = LangUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(LangUtils.setAppLocale(base, lang));
    }
}
