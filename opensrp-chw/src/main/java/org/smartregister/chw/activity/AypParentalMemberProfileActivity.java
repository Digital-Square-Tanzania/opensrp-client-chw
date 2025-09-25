package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.ayp.activity.BaseAypParentalMedicalHistoryActivity;
import org.smartregister.chw.ayp.activity.BaseAypParentalVisitActivity;
import org.smartregister.chw.core.activity.CoreAypProfileActivity;

public class AypParentalMemberProfileActivity extends CoreAypProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AypParentalMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        refreshMedicalHistory(false);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        boolean showLastVisit = getLatestParentalVisit() != null;
        rlLastVisit.setVisibility(showLastVisit ? View.VISIBLE : View.GONE);
    }

    private Visit getLatestParentalVisit() {
        if (memberObject == null || AypLibrary.getInstance() == null) {
            return null;
        }
        return AypLibrary.getInstance().visitRepository().getLatestVisit(
                memberObject.getBaseEntityId(),
                Constants.EVENT_TYPE.AYP_PARENTAL_SERVICES
        );
    }

    @Override
    protected Visit getServiceVisit() {
        Visit visit = getLatestParentalVisit();
        return visit != null ? visit : super.getServiceVisit();
    }

    @Override
    public void startHivstRegistration() {
        // HIVST registration not supported for parental profile
    }

    @Override
    public void continueService() {
        // No ongoing parental service workflow
    }

    @Override
    public void continueDischarge() {
        // No discharge workflow for parental profile
    }

    @Override
    public void openFollowupVisit() {
        AypParentalVisitActivity.startAypParentalVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void startServiceForm() {
        AypParentalVisitActivity.startAypParentalVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openMedicalHistory() {
        BaseAypParentalMedicalHistoryActivity.startMe(this, memberObject);
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return AypDao.getParentalMember(baseEntityId);
    }
}
