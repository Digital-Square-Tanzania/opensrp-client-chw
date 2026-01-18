package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreHarmReductionProfileActivity;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.harmreduction.R;
import org.smartregister.chw.harmreduction.dao.HarmReductionDao;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.chw.harmreduction.util.HarmReductionVisitsUtil;

import timber.log.Timber;

public class HarmReductionProfileActivity extends CoreHarmReductionProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, HarmReductionProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.HARM_REDUCTION_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void setupButtons() {
        textViewRecordHarmReductionVisit.setVisibility(View.VISIBLE);
        if (HarmReductionDao.getRocConsentForJoiningMatServices(memberObject.getBaseEntityId()).equals("yes")) {
            textViewRecordHarmReductionVisit.setText(R.string.record_pre_mat_session);
        } else {
            textViewRecordHarmReductionVisit.setText(R.string.record_harm_reduction_community_visit);
        }
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        setupPreMatSessionsHistoryLayout();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        if (StringUtils.isBlank(baseEntityId)) {
            finish();
            return;
        }
        if (memberObject == null) {
            memberObject = HarmReductionDao.getMember(baseEntityId);
            if (memberObject == null) {
                memberObject = HarmReductionDao.getContact(baseEntityId);
            }
        }
        super.onCreate(savedInstanceState);
        try {
            HarmReductionVisitsUtil.processVisits();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openClientObservationResults() {
        // no-op
    }

    @Override
    public void observationResults() {
        // no-op
    }

    @Override
    public void openRecordClientVisit() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openFollowupVisit() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openRecordTbContactVisit() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openMedicalHistory() {
        HarmReductionVisitHistoryActivity.startMe(this, memberObject);
    }

    public void openPreMatSessionsHistory() {
        HarmReductionPreMatSessionsHistoryActivity.startMe(this, memberObject);
    }

    @Override
    public void openObservationResults() {
        // no-op
    }

    @Override
    public void openHarmReductionContactRegister() {
        // no-op
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    @Override
    protected void removeMember() {
        // no-op
    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {
        // no-op
    }

    @Override
    public void setProfileDetailThree(String s) {
        // no-op
    }

    @Override
    public void toggleFamilyHead(boolean b) {
        // no-op
    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {
        // no-op
    }

    @Override
    public void startServiceForm() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void continueService() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueContactVisit() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void refreshList() {
        // no-op
    }

    @Override
    public void updateHasPhone(boolean b) {
        // no-op
    }

    @Override
    public void setFamilyServiceStatus(String s) {
        // no-op
    }

    @Override
    public void verifyHasPhone() {
        // no-op
    }

    @Override
    public void notifyHasPhone(boolean b) {
        // no-op
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMedicalHistory(true);
    }

    private void setupPreMatSessionsHistoryLayout() {
        boolean showPreMatHistory = memberObject != null
                && StringUtils.isNotBlank(HarmReductionDao.getVisitDateForRocConsentForJoiningMatServices(memberObject.getBaseEntityId()));
        if (rlPreMatSessionHistory != null) {
            rlPreMatSessionHistory.setVisibility(showPreMatHistory ? View.VISIBLE : View.GONE);
            rlPreMatSessionHistory.setOnClickListener(view -> openPreMatSessionsHistory());
        }

        if (preMatSessionRowDivider != null) {
            preMatSessionRowDivider.setVisibility(showPreMatHistory ? View.VISIBLE : View.GONE);
        }
    }
}
