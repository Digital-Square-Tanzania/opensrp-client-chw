package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.presenter.BaseAncMedicalHistoryPresenter;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.interactor.MotherMentorMedicalHistoryInteractor;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MotherMentorHouseholdTitle;

import java.util.List;

public class MotherMentorMedicalHistoryActivity extends CoreAncMedicalHistoryActivity {
    private static final String HISTORY_MEMBER = "mother_mentor_history_member";
    private static final String HISTORY_HOUSEHOLD = "mother_mentor_history_household";
    private Flavor flavor;
    private ProgressBar progressBar;

    public static void startMe(Activity activity, MemberObject memberObject) {
        startMe(activity, memberObject, null);
    }

    public static void startMe(Activity activity, MemberObject memberObject, MotherMentorFollowUpType followUpType) {
        startHistory(activity, memberObject, followUpType, false);
    }

    public static void startHouseholdHistory(Activity activity, MemberObject household) {
        startHistory(activity, household, null, true);
    }

    private static void startHistory(Activity activity, MemberObject memberObject,
            MotherMentorFollowUpType followUpType, boolean household) {
        Intent intent = new Intent(activity, MotherMentorMedicalHistoryActivity.class);
        intent.putExtra(HISTORY_MEMBER, memberObject);
        intent.putExtra(HISTORY_HOUSEHOLD, household);
        if (followUpType != null) {
            intent.putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, followUpType.getValue());
        }
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        MemberObject mother = historyMember();
        MotherMentorFollowUpType followUpType = getFollowUpType();
        flavor = new MotherMentorMedicalHistoryActivityFlv(followUpType,
                mother.getBaseEntityId());
        presenter = new BaseAncMedicalHistoryPresenter(
                new MotherMentorMedicalHistoryInteractor(followUpType), this,
                mother.getBaseEntityId());
    }

    private MemberObject historyMember() {
        return (MemberObject) getIntent().getSerializableExtra(HISTORY_MEMBER);
    }

    private MotherMentorFollowUpType getFollowUpType() {
        if (getIntent() == null || !getIntent().hasExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE)) {
            return null;
        }
        return MotherMentorFollowUpType.fromValue(
                getIntent().getStringExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE));
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        MemberObject member = historyMember();
        String title = getIntent().getBooleanExtra(HISTORY_HOUSEHOLD, false)
                ? MotherMentorHouseholdTitle.format(this, member.getFirstName())
                : member.getFullName();
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, title));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.visits_history));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(getFollowUpType() == MotherMentorFollowUpType.PARTNER
                ? R.string.mothermentor_partner_visit_history_title
                : R.string.mothermentor_visit_history_title);
        return view;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && (requestCode == MotherMentorTestResultsActivity.EDIT_FROM_HISTORY
                || requestCode == Constants.REQUEST_CODE_GET_JSON)) {
            recreate();
        }
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }
}
