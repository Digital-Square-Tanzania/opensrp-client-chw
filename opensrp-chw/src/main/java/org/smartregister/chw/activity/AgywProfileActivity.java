package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.agyw.activity.BaseAGYWProfileActivity;
import org.smartregister.chw.agyw.util.Constants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.pmtct.PmtctLibrary;
import org.smartregister.chw.pmtct.domain.Visit;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.chw.util.Utils;
import org.smartregister.common.Gender;

import java.util.ArrayList;
import java.util.List;

public class AgywProfileActivity extends BaseAGYWProfileActivity {

    public static void startProfile(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AgywProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        AllClientsUtils.addTbLeprosyMenuItem(menu, memberObject.getBaseEntityId());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_tbleprosy_screening) {
            startTbLeprosyScreening();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void startAGYWServices() {
        AGYWServicesActivity.startMe(this, memberObject.getBaseEntityId());
    }

    @Override
    public void startReferralForm() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.sti_referral),
                    org.smartregister.chw.util.Constants.JSON_FORM.getSTIServicesReferralForm(), CoreConstants.TASKS_FOCUS.STI_REFERRAL));
            referralTypeModels.addAll(Utils.getCommonReferralTypes(this, memberObject.getBaseEntityId()));
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.kvp_friendly_services),
                    CoreConstants.JSON_FORM.getFemaleKvpFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.KVP_FRIENDLY_SERVICES));
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.family_planning_referral),
                    org.smartregister.chw.util.Constants.JSON_FORM.getFamilyPlanningUnifiedReferralForm(Gender.FEMALE.toString()), CoreConstants.TASKS_FOCUS.FP_SIDE_EFFECTS));
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_referral),
                    CoreConstants.JSON_FORM.getTbReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_TB));
            Utils.launchClientReferralActivity(this, referralTypeModels, memberObject.getBaseEntityId());
        }
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit lastAgywStructuralServices = getVisit(org.smartregister.chw.util.Constants.Events.AGYW_STRUCTURAL_SERVICES);
        Visit lastAgywBehavioralServices = getVisit(org.smartregister.chw.util.Constants.Events.AGYW_BEHAVIORAL_SERVICES);
        Visit lastAgywBioMedicalServices = getVisit(org.smartregister.chw.util.Constants.Events.AGYW_BIO_MEDICAL_SERVICES);
        if (lastAgywStructuralServices != null || lastAgywBehavioralServices != null || lastAgywBioMedicalServices != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.vViewHistory)).setText(R.string.visits_history_profile_title);
            ((TextView) findViewById(R.id.ivViewHistoryArrow)).setText(getString(R.string.view_visits_history));
        } else {
            rlLastVisit.setVisibility(View.GONE);
        }
    }

    @Override
    public void openMedicalHistory() {
        AgywMedicalHistoryActivity.startMe(this, memberObject);
    }

    private Visit getVisit(String eventType) {
        return PmtctLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
    }

    protected void startTbLeprosyScreening() {
        TbLeprosyRegisterActivity.startRegistration(AgywProfileActivity.this, memberObject.getBaseEntityId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMedicalHistory(true);
    }
}
