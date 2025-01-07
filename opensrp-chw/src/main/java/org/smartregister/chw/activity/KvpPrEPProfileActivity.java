package org.smartregister.chw.activity;

import static org.smartregister.chw.util.Utils.getCommonReferralTypes;
import static org.smartregister.chw.util.Utils.launchClientReferralActivity;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreKvpProfileActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.ChwKvpDao;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.chw.util.KvpVisitUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class KvpPrEPProfileActivity extends CoreKvpProfileActivity {
    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, KvpPrEPProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.KVP_PrEP_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    public void openFollowupVisit() {
        KvpPrEPVisitActivity.startKvpPrEPVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        setupViews();
        refreshMedicalHistory(true);
    }

    @Override
    protected boolean showReferralView() {
        return true;
    }

    @Override
    public void startReferralForm() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
            if (memberObject.getGender().equalsIgnoreCase("male")) {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.kvp_friendly_services),
                        CoreConstants.JSON_FORM.getMaleKvpFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.KVP_FRIENDLY_SERVICES));
            } else {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.kvp_friendly_services),
                        CoreConstants.JSON_FORM.getFemaleKvpFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.KVP_FRIENDLY_SERVICES));
            }
            referralTypeModels.addAll(getCommonReferralTypes(this, memberObject.getBaseEntityId()));

            launchClientReferralActivity(this, referralTypeModels, memberObject.getBaseEntityId());
        } else {
            Toast.makeText(this, "Refer to facility", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void setupViews() {
        try {
            KvpVisitUtils.processVisits(this);
        } catch (Exception e) {
            Timber.e(e);
        }
        super.setupViews();
        if (ChwKvpDao.wereSelfTestingKitsDistributed(memberObject.getBaseEntityId())) {
            if (HivstDao.isRegisteredForHivst(memberObject.getBaseEntityId())) {
                boolean shouldIssueHivSelfTestingKits = false;
                String lastSelfTestingFollowupDateString = HivstDao.clientLastFollowup(memberObject.getBaseEntityId());
                if (lastSelfTestingFollowupDateString == null) {
                    shouldIssueHivSelfTestingKits = true;
                } else {
                    try {
                        Date lastSelfTestingFollowupDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(lastSelfTestingFollowupDateString);
                        Visit lastVisit = getVisit(org.smartregister.chw.util.Constants.Events.KVP_PREP_FOLLOWUP_VISIT);
                        if (truncateTimeFromDate(lastSelfTestingFollowupDate).before(truncateTimeFromDate(lastVisit.getDate()))) {
                            shouldIssueHivSelfTestingKits = true;
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }

                if (shouldIssueHivSelfTestingKits) {
                    textViewRecordKvp.setVisibility(View.GONE);
                    visitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setText(R.string.issue_selft_testing_kits);
                    textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_followup));
                    textViewVisitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setOnClickListener(view -> HivstProfileActivity.startProfile(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), true));
                    imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
                } else {
                    textViewRecordKvp.setVisibility(View.VISIBLE);
                    visitDone.setVisibility(View.GONE);
                    textViewVisitDone.setVisibility(View.GONE);
                }
            } else {
                textViewRecordKvp.setVisibility(View.GONE);
                visitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setText(R.string.register_client);
                textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_registration));
                textViewVisitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setOnClickListener(v -> startHivstRegistration());
                imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
            }
        }
    }

    private Date truncateTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit lastVisit = getVisit(org.smartregister.chw.util.Constants.Events.KVP_PREP_FOLLOWUP_VISIT);
        if (lastVisit != null) {
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
        KvpPrEPMedicalHistoryActivity.startMe(this, memberObject);
    }

    private Visit getVisit(String eventType) {
        return KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
    }

    @Override
    protected void showKvpGroups(String baseEntityId) {
        // Early exit if profile type is PrEP_PROFILE
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            textViewDominantKvpGroup.setVisibility(View.GONE);
            textViewOtherKvpGroups.setVisibility(View.GONE);
            return;
        }

        // Get the dominant KVP group from facility and general group
        String dominantKVPGroupFromFacility = ChwKvpDao.getDominantKVPGroupFromFacility(baseEntityId);
        String dominantKVPGroup = StringUtils.isNotBlank(dominantKVPGroupFromFacility) ?
                dominantKVPGroupFromFacility : ChwKvpDao.getDominantKVPGroup(baseEntityId);

        // Check if there is a dominant KVP group and update UI accordingly
        if (StringUtils.isNotBlank(dominantKVPGroup)) {
            textViewDominantKvpGroup.setVisibility(View.VISIBLE);
            textViewDominantKvpGroup.setText(getString(org.smartregister.kvp.R.string.dominant_kvp_group,
                    readStringResourcesWithPrefix(Arrays.asList(dominantKVPGroup), "kvp_")));
        } else {
            textViewDominantKvpGroup.setVisibility(View.GONE);
        }

        // Always hide other KVP groups for now
        textViewOtherKvpGroups.setVisibility(View.GONE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);
        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());

        AllClientsUtils.updateOptionsMenu(menu, client);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == org.smartregister.chw.core.R.id.action_anc_registration) {
            startAncRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_pregnancy_out_come) {
            startPncRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_initiation) {
            startFpRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_ecp_provision) {
            startFpEcpScreening();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_malaria_registration) {
            startMalariaRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_iccm_registration) {
            startIntegratedCommunityCaseManagementEnrollment();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_vmmc_registration) {
            startVmmcRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_hiv_registration) {
            startHivRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            startHivRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_tb_registration) {
            startTbRegister();
        } else if (i == org.smartregister.chw.core.R.id.action_hivst_registration) {
            startHivstRegistration();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_agyw_screening) {
            startAgywScreening();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_kvp_prep_registration) {
            startKvpPrEPRegistration();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_prep_registration) {
            startPrEPRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_sbc_registration) {
            startSbcRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_gbv_registration) {
            startGbvRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_cancer_preventive_services_registration) {
            startCancerPreventiveServicesRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_asrh_registration) {
            startAsrhRegistration();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void startAncRegister() {
        AncRegisterActivity.startAncRegistrationActivity(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(),
                org.smartregister.chw.util.Constants.JSON_FORM.getAncRegistration(), null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
    }


    protected void startPncRegister() {
        PncRegisterActivity.startPncRegistrationActivity(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(),
                CoreConstants.JSON_FORM.getPregnancyOutcome(), null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName(), null);
    }

    protected void startMalariaRegister() {
        MalariaRegisterActivity.startMalariaRegistrationActivity(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
    }


    protected void startVmmcRegister() {
        //implement
    }


    protected void startIntegratedCommunityCaseManagementEnrollment() {
        IccmRegisterActivity.startIccmRegistrationActivity(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
    }


    protected void startHivRegister() {
        String gender = memberObject.getGender();
        int age = memberObject.getAge();


        try {
            String formName = org.smartregister.chw.util.Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(KvpPrEPProfileActivity.this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), formName, formJsonObject.toString());
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    protected void startTbRegister() {
        try {
            TbRegisterActivity.startTbFormActivity(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), org.smartregister.chw.util.Constants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, org.smartregister.chw.util.Constants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }


    protected void startFpRegister() {
        String gender = memberObject.getGender();
        int age = memberObject.getAge();
        FpRegisterActivity.startFpRegistrationActivity(this, memberObject.getBaseEntityId(), CoreConstants.JSON_FORM.getFpRegistrationForm(gender));
    }


    protected void startFpEcpScreening() {
        //NOT Required in CHW
    }

    protected void startAgywScreening() {
        int age = memberObject.getAge();
        AgywRegisterActivity.startRegistration(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), age);
    }


    protected void startSbcRegistration() {
        SbcRegisterActivity.startRegistration(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId());
    }


    protected void startGbvRegistration() {
        //Implement
    }


    protected void startCancerPreventiveServicesRegistration() {
        CecapRegisterActivity.startRegistration(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId());
    }


    protected void startAsrhRegistration() {
        AsrhRegisterActivity.startRegistration(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId());
    }


    protected void startKvpPrEPRegistration() {
        String gender = memberObject.getGender();
        int age = memberObject.getAge();
        KvpPrEPRegisterActivity.startRegistration(KvpPrEPProfileActivity.this, memberObject.getBaseEntityId(), gender, age);
    }

    @Override
    protected void startPrEPRegistration() {
        //do nothing
    }

    @Override
    public void startHivstRegistration() {
        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client =
                new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), gender);
    }

    @Override
    protected void showUICID(String baseEntityId) {
        String UIC_ID = KvpDao.getUIC_ID(baseEntityId, "ec_facility_kvp_register");
        if (StringUtils.isNotBlank(UIC_ID)) {
            textViewId.setVisibility(View.VISIBLE);
            textViewId.setText(getString(R.string.uic_id, UIC_ID.toUpperCase(Locale.ROOT)));
        } else {
            textViewId.setVisibility(View.GONE);
        }

    }
}
