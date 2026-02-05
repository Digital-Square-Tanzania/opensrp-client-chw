package org.smartregister.chw.activity;

import static org.smartregister.chw.ayp.dao.AypDao.isAypOutSchoolServiceToday;
import static org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_SCHOOL_FOLLOW_UP_VISIT;
import static org.smartregister.chw.ayp.util.Constants.FORMS.AYP_OUT_SCHOOL_GRADUATION;
import static org.smartregister.chw.util.Utils.getCommonReferralTypes;
import static org.smartregister.chw.util.Utils.launchClientReferralActivity;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.ayp.util.DBConstants;
import org.smartregister.chw.core.activity.CoreAypProfileActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.AypOutSchoolDao;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.util.JsonFormUtils;
import com.vijay.jsonwizard.utils.FormUtils;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class AypOutSchoolMemberProfileActivity extends CoreAypProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AypOutSchoolMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(org.smartregister.chw.ayp.util.Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.AYP_OUT_SCHOOL_PROFILE);
        activity.startActivity(intent);
    }

    private Visit getVisit(String eventType) {
        return AypLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
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
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.ayp_friendly_services),
                        CoreConstants.JSON_FORM.getMaleAypFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.KVP_FRIENDLY_SERVICES));
            } else {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.ayp_friendly_services),
                        CoreConstants.JSON_FORM.getFemaleAypFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.KVP_FRIENDLY_SERVICES));
            }
            referralTypeModels.addAll(getCommonReferralTypes(this, memberObject.getBaseEntityId()));

            launchClientReferralActivity(this, referralTypeModels, memberObject.getBaseEntityId());
        } else {
            Toast.makeText(this, "Refer to facility", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void setupViews() {
        super.setupViews();

        if (AypOutSchoolDao.wereSelfTestingKitsDistributed(memberObject.getBaseEntityId())) {
            if (HivstDao.isRegisteredForHivst(memberObject.getBaseEntityId())) {
                boolean shouldIssueHivSelfTestingKits = false;
                String lastSelfTestingFollowupDateString = HivstDao.clientLastFollowup(memberObject.getBaseEntityId());
                if (lastSelfTestingFollowupDateString == null) {
                    shouldIssueHivSelfTestingKits = true;
                } else {
                    try {
                        Date lastSelfTestingFollowupDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(lastSelfTestingFollowupDateString);
                        Visit lastVisit = getVisit(AYP_OUT_SCHOOL_FOLLOW_UP_VISIT);
                        if (truncateTimeFromDate(lastSelfTestingFollowupDate).before(truncateTimeFromDate(lastVisit.getDate())) && lastVisit.getProcessed()) {
                            shouldIssueHivSelfTestingKits = true;
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }

                if (shouldIssueHivSelfTestingKits) {
                    textViewRecordayp.setVisibility(View.GONE);
                    visitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setText(R.string.issue_selft_testing_kits);
                    textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_followup));
                    textViewVisitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setOnClickListener(view -> HivstProfileActivity.startProfile(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), true));
                    imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
                } else {
                    textViewRecordayp.setVisibility(View.VISIBLE);
                    visitDone.setVisibility(View.GONE);
                    textViewVisitDone.setVisibility(View.GONE);
                    if (isVisitOnProgress(getAypOutSchoolVisit())) {
                        textViewRecordayp.setVisibility(View.GONE);
                        visitInProgress.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                textViewRecordayp.setVisibility(View.GONE);
                visitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setText(R.string.register_client);
                textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_registration));
                textViewVisitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setOnClickListener(v -> startHivstRegistration());
                imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
            }
        }

        if(isAypOutSchoolServiceToday(memberObject.getBaseEntityId())) {
            textViewRecordayp.setVisibility(View.GONE);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);
        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());

        AllClientsUtils.updateOptionsMenu(menu, client);
        AllClientsUtils.addTbLeprosyMenuItem(menu, memberObject.getBaseEntityId());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Prevent crashes when no family record is linked (common for KVP/PrEP, AYP out of school, etc.)
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
        } else if (i == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            startHivRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_tb_registration) {
            startTbRegister();
        } else if (i == org.smartregister.chw.core.R.id.action_hivst_registration) {
            startHivstRegistration();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_kvp_prep_registration) {
            startKvpPrEPRegistration();
            return true;
        }else if (i == org.smartregister.chw.core.R.id.action_sbc_registration) {
            startSbcRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_gbv_registration) {
            startGbvRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_cancer_preventive_services_registration) {
            startCancerPreventiveServicesRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_asrh_registration) {
            startAsrhRegistration();
        }  else if (i == R.id.action_hps_enrollment) {
            startHpsEnrollment();
            return true;
        }else if (item.getItemId() == R.id.action_tbleprosy_screening) {
            startTbLeprosyScreening();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void startHpsEnrollment() {
        HpsRegisterActivity.startRegistration(this, memberObject.getBaseEntityId(), org.smartregister.chw.hps.util.Constants.FORMS.HPS_CLIENT_ENROLLMENT, null);
    }

    protected void startAncRegister() {
        AncRegisterActivity.startAncRegistrationActivity(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(),
                org.smartregister.chw.util.Constants.JSON_FORM.getAncRegistration(), null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
    }


    protected void startPncRegister() {
        PncRegisterActivity.startPncRegistrationActivity(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(),
                CoreConstants.JSON_FORM.getPregnancyOutcome(), null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName(), null);
    }

    protected void startMalariaRegister() {
        MalariaRegisterActivity.startMalariaRegistrationActivity(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
    }


    protected void startTbRegister() {
        try {
            TbRegisterActivity.startTbFormActivity(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), org.smartregister.chw.util.Constants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, org.smartregister.chw.util.Constants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }


    protected void startFpRegister() {
        String gender = memberObject.getGender();
        FpRegisterActivity.startFpRegistrationActivity(this, memberObject.getBaseEntityId(), CoreConstants.JSON_FORM.getFpRegistrationForm(gender));
    }


    protected void startFpEcpScreening() {
        //NOT Required in CHW
    }


    protected void startSbcRegistration() {
        SbcRegisterActivity.startRegistration(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId());
    }


    protected void startGbvRegistration() {
        //Implement
    }


    protected void startCancerPreventiveServicesRegistration() {
        CecapRegisterActivity.startRegistration(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId());
    }


    protected void startAsrhRegistration() {
        AsrhRegisterActivity.startRegistration(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId());
    }


    protected void startKvpPrEPRegistration() {
        String gender = memberObject.getGender();
        int age = memberObject.getAge();
        KvpPrEPRegisterActivity.startRegistration(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), gender, age);
    }

    protected void startIntegratedCommunityCaseManagementEnrollment() {
        IccmRegisterActivity.startIccmRegistrationActivity(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
    }

    protected void startHivRegister() {
        String gender = memberObject.getGender();
        int age = memberObject.getAge();


        try {
            String formName = org.smartregister.chw.util.Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(AypOutSchoolMemberProfileActivity.this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), formName, formJsonObject.toString());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void startTbLeprosyScreening() {
        TbLeprosyRegisterActivity.startRegistration(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId());
    }


    @Override
    public void continueService() {
        AypOutSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueDischarge() {
    }

    @Override
    public void openFollowupVisit() {
        AypOutSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void startServiceForm() {
        AypOutSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void graduateForm() {
        try {
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, AYP_OUT_SCHOOL_GRADUATION);
            startFormActivity(formJsonObject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return AypDao.getOutSchoolMember(baseEntityId);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        setupViews();
        refreshMedicalHistory(true);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit lastVisit = getVisit(AYP_OUT_SCHOOL_FOLLOW_UP_VISIT);
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
        AypOutSchoolMedicalHistoryActivity.startMe(this, memberObject);
    }

}
