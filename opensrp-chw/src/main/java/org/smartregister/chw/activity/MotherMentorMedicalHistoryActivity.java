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
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import java.util.List;

public class MotherMentorMedicalHistoryActivity extends CoreAncMedicalHistoryActivity {
    private static MemberObject motherMentorMemberObject;
    private Flavor flavor;
    private ProgressBar progressBar;

    public static void startMe(Activity activity, MemberObject memberObject) {
        startMe(activity, memberObject, null);
    }

    public static void startMe(Activity activity, MemberObject memberObject, MotherMentorFollowUpType followUpType) {
        Intent intent = new Intent(activity, MotherMentorMedicalHistoryActivity.class);
        motherMentorMemberObject = memberObject;
        if (followUpType != null) {
            intent.putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, followUpType.getValue());
        }
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        MotherMentorFollowUpType followUpType = getFollowUpType();
        flavor = new MotherMentorMedicalHistoryActivityFlv(followUpType);
        presenter = new BaseAncMedicalHistoryPresenter(
                new MotherMentorMedicalHistoryInteractor(followUpType), this,
                motherMentorMemberObject.getBaseEntityId());
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
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, motherMentorMemberObject.getFullName()));

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
        visitTitle.setText(R.string.mothermentor_visit_history_title);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }
}
