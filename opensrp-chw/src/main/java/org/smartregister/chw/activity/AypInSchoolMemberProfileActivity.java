package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.activity.CoreAypProfileActivity;

public class AypInSchoolMemberProfileActivity extends CoreAypProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AypInSchoolMemberProfileActivity.class);
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
        boolean showLastVisit = hasHistory || getLatestFollowUpVisit() != null;
        rlLastVisit.setVisibility(showLastVisit ? View.VISIBLE : View.GONE);
    }

    private Visit getLatestFollowUpVisit() {
        return AypLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.AYP_IN_SCHOOL_FOLLOW_UP_VISIT);
    }

    @Override
    public void startHivstRegistration() {
        // Launch HIVST registration from AYP profile using member gender
        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), memberObject.getGender());
    }

    @Override
    public void continueService() {

    }

    @Override
    public void continueDischarge() {

    }

    @Override
    public void openFollowupVisit() {
        AypInSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void startServiceForm() {
        AypInSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), false);
    }


    @Override
    public void openMedicalHistory() {
        AypInSchoolMedicalHistoryActivity.startMe(this, memberObject);
    }


    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return AypDao.getInSchoolMember(baseEntityId);
    }
}
