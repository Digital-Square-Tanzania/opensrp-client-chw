package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreHarmReductionProfileActivity;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.harmreduction.R;
import org.smartregister.chw.harmreduction.dao.HarmReductionDao;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.chw.harmreduction.activity.BaseHarmReductionSoberHouseVisitActivity;
import org.smartregister.chw.harmreduction.util.HarmReductionVisitsUtil;
import org.smartregister.domain.SortableVisit;
import org.smartregister.chw.interactor.HarmReductionVisitHistoryInteractor;

import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class HarmReductionSoberHouseProfileActivity extends CoreHarmReductionProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, HarmReductionSoberHouseProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.HARM_REDUCTION_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void setupButtons() {
        textViewRecordHarmReductionVisit.setVisibility(View.GONE);
        textViewRecordSoberHouseVisit.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        if (StringUtils.isBlank(baseEntityId)) {
            finish();
            return;
        }
        if (memberObject == null) {
            memberObject = HarmReductionDao.getSoberHouseMember(baseEntityId);
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
        BaseHarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openFollowupVisit() {
        BaseHarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openRecordTbContactVisit() {
        BaseHarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openMedicalHistory() {
        HarmReductionVisitHistoryActivity.startMe(this, memberObject);
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
        BaseHarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void continueService() {
        BaseHarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueContactVisit() {
        BaseHarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), true);
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

    private boolean hasVisitsAfterMatConsent() {
        return hasMinimumVisitsAfterMatConsent(1);
    }

    private boolean hasMinimumVisitsAfterMatConsent(int minimumVisits) {
        if (minimumVisits <= 0) {
            return true;
        }
        if (memberObject == null) {
            return false;
        }

        try {
            List<SortableVisit> visits = HarmReductionVisitHistoryInteractor.getVisits(
                    memberObject.getBaseEntityId(),
                    Constants.EVENT_TYPE.HARM_REDUCTION_SOBER_HOUSE_VISIT
            );

            int visitCount = 0;
            for (SortableVisit visit : visits) {
                Date visitDate = visit.getDate();
                if (visitDate != null) {
                    visitCount++;
                    if (visitCount >= minimumVisits) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return false;
    }
}
