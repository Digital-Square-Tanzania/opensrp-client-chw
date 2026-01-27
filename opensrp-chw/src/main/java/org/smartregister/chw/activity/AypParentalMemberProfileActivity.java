package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.vijay.jsonwizard.utils.FormUtils;
import org.json.JSONObject;
import timber.log.Timber;

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
                launchIndependentEditForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm(), org.smartregister.chw.core.R.string.registration_info);
            } else {
                launchIndependentEditForm(CoreConstants.JSON_FORM.getFamilyDetailsRegister(), R.string.edit_location_details);
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

    private void launchIndependentEditForm(String formName, int titleRes) {
        try {
            JSONObject jsonForm = new FormUtils().getFormJsonFromRepositoryOrAssets(this, formName);
            if (jsonForm == null) return;

            jsonForm.put("entity_id", memberObject.getBaseEntityId());
            jsonForm.put("relational_id", memberObject.getBaseEntityId());

            if (jsonForm.has(com.vijay.jsonwizard.constants.JsonFormConstants.STEP1)) {
                jsonForm.getJSONObject(com.vijay.jsonwizard.constants.JsonFormConstants.STEP1)
                        .put("title", getString(titleRes));
            }
            jsonForm.put("encounter_type", getString(titleRes));

            startFormActivity(jsonForm);
        } catch (Exception e) {
            Timber.e(e);
            Toast.makeText(this, R.string.family_details_not_available, Toast.LENGTH_SHORT).show();
        }
    }
}
