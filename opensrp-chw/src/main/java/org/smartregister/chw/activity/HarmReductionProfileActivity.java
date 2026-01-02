package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;

import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreHarmReductionProfileActivity;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.harmreduction.util.Constants;

public class HarmReductionProfileActivity extends CoreHarmReductionProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, HarmReductionProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.HARM_REDUCTION_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void setupButtons() {
        textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
        textViewRecordTbLeprosy.setText(org.smartregister.chw.harmreduction.R.string.record_harm_reduction_community_visit);
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
    public void openTbContactFollowUpVisit() {
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
}
