package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;
import org.smartregister.chw.util.JsonFormUtils;

import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.R;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.activity.CoreAypProfileActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.AllClientsUtils;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        AllClientsUtils.addTbLeprosyMenuItem(menu, memberObject.getBaseEntityId());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ((item.getItemId() == org.smartregister.chw.core.R.id.action_registration
                || item.getItemId() == org.smartregister.chw.core.R.id.action_location_info)
                && TextUtils.isEmpty(memberObject.getFamilyBaseEntityId())) {
            if (item.getItemId() == org.smartregister.chw.core.R.id.action_registration) {
                JSONObject form = JsonFormUtils.prepareIndependentEditForm(this,
                        CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm(),
                        memberObject.getBaseEntityId(),
                        getString(org.smartregister.chw.core.R.string.registration_info));
                if (form != null) startFormActivity(form);
            } else {
                JSONObject form = JsonFormUtils.prepareIndependentEditForm(this,
                        CoreConstants.JSON_FORM.getFamilyDetailsRegister(),
                        memberObject.getBaseEntityId(),
                        getString(R.string.edit_location_details));
                if (form != null) startFormActivity(form);
            }
            return true;
        }

        if (item.getItemId() == R.id.action_tbleprosy_screening) {
            startTbLeprosyScreening();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void openMedicalHistory() {
        AypParentalMedicalHistoryActivity.startMe(this, memberObject);
    }

    protected void startTbLeprosyScreening() {
        TbLeprosyRegisterActivity.startRegistration(AypParentalMemberProfileActivity.this, memberObject.getBaseEntityId());
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return AypDao.getParentalMember(baseEntityId);
    }

}
