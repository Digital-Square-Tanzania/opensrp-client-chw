package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.agyw.dao.AGYWDao;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.asrh.AsrhLibrary;
import org.smartregister.chw.asrh.dao.AsrhDao;
import org.smartregister.chw.asrh.domain.MemberObject;
import org.smartregister.chw.asrh.util.Constants;
import org.smartregister.chw.asrh.util.VisitUtils;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.core.activity.CoreAsrhMemberProfileActivity;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.custom_view.AsrhFloatingMenu;
import org.smartregister.chw.dataloader.AncMemberDataLoader;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.sbc.dao.SbcDao;
import org.smartregister.chw.util.MemberProfileUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class AsrhMemberProfileActivity extends CoreAsrhMemberProfileActivity {
    private final FamilyOtherMemberProfileActivity.Flavor flavor = new FamilyOtherMemberProfileActivityFlv();
    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, AsrhMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void recordAsrh(MemberObject memberObject) {
        AsrhVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        try {
            VisitUtils.processVisits(AsrhLibrary.getInstance().visitRepository(), AsrhLibrary.getInstance().visitDetailsRepository(), AsrhMemberProfileActivity.this);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openMedicalHistory() {
        AsrhMedicalHistoryActivity.startMe(this, memberObject);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
        TextView clientStatus = ((TextView) findViewById(R.id.family_asrh_head));
        memberObject = AsrhDao.getMember(memberObject.getBaseEntityId());
        if (memberObject.getClientStatus() != null && (memberObject.getClientStatus().equalsIgnoreCase("opt_out")) || memberObject.getClientStatus().equalsIgnoreCase("transfer_out")) {
            textViewRecordAsrh.setVisibility(View.GONE);
            clientStatus.setVisibility(View.VISIBLE);
            clientStatus.setTextColor(getResources().getColor(R.color.alert_urgent_red));

            if (memberObject.getClientStatus().equalsIgnoreCase("opt_out"))
                clientStatus.setText(getString(R.string.asrh_opted_out));
            else
                clientStatus.setText(getString(R.string.asrh_transfer_out));

        } else {
            textViewRecordAsrh.setVisibility(View.VISIBLE);
            clientStatus.setVisibility(View.GONE);
        }
    }

    private void addReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            if (isClientEligibleForAnc(memberObject)) {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.family_planning_referral), CoreConstants.JSON_FORM.getFamilyPlanningUnifiedReferralForm(memberObject.getGender()), CoreConstants.TASKS_FOCUS.FP_SIDE_EFFECTS));
                if (PNCDao.isPNCMember(memberObject.getBaseEntityId())) {
                    referralTypeModels.add(new ReferralTypeModel(getString(R.string.pnc_referral), CoreConstants.JSON_FORM.getPncUnifiedReferralForm(), CoreConstants.TASKS_FOCUS.PNC_DANGER_SIGNS));
                }
                if (!AncDao.isANCMember(memberObject.getBaseEntityId())) {
                    referralTypeModels.add(new ReferralTypeModel(getString(R.string.pregnancy_confirmation), CoreConstants.JSON_FORM.getPregnancyConfirmationReferralForm(), CoreConstants.TASKS_FOCUS.PREGNANCY_CONFIRMATION));
                } else {
                    referralTypeModels.add(new ReferralTypeModel(getString(R.string.anc_danger_signs), org.smartregister.chw.util.Constants.JSON_FORM.getAncUnifiedReferralForm(), CoreConstants.TASKS_FOCUS.ANC_DANGER_SIGNS));
                }
            }
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral), CoreConstants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void initializeFloatingMenu() {
        baseAsrhFloatingMenu = new AsrhFloatingMenu(this, memberObject);
        baseAsrhFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseAsrhFloatingMenu, linearLayoutParams);


        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.asrh_fab:
                    checkPhoneNumberProvided();
                    ((AsrhFloatingMenu) baseAsrhFloatingMenu).animateFAB();
                    break;
                case R.id.asrh_call_layout:
                    ((AsrhFloatingMenu) baseAsrhFloatingMenu).launchCallWidget();
                    ((AsrhFloatingMenu) baseAsrhFloatingMenu).animateFAB();
                    break;
                case R.id.asrh_refer_to_facility_layout:
                    org.smartregister.chw.util.Utils.launchClientReferralActivity(AsrhMemberProfileActivity.this, getReferralTypeModels(), memberObject.getBaseEntityId());
                    ((AsrhFloatingMenu) baseAsrhFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((AsrhFloatingMenu) baseAsrhFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(memberObject.getPhoneNumber()));
        ((AsrhFloatingMenu) baseAsrhFloatingMenu).redraw(phoneNumberAvailable);
    }

    protected boolean isClientEligibleForAnc(MemberObject hivMemberObject) {
        if (hivMemberObject.getGender().equalsIgnoreCase("Female")) {
            //Obtaining the clients CommonPersonObjectClient used for checking is the client is Of Reproductive Age
            CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

            final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(hivMemberObject.getBaseEntityId());
            final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
            client.setColumnmaps(commonPersonObject.getColumnmaps());

            return org.smartregister.chw.core.utils.Utils.isMemberOfReproductiveAge(client, 15, 49);
        }
        return false;
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        addReferralTypes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem addMember = menu.findItem(org.smartregister.chw.core.R.id.add_member);
        if (addMember != null) {
            addMember.setVisible(false);
        }

        getMenuInflater().inflate(org.smartregister.chw.core.R.menu.other_member_menu, menu);


        String dob = memberObject.getDob();
        int age = org.smartregister.chw.util.Utils.getAgeFromDate(dob);

        String gender = memberObject.getGender();
        menu.findItem(R.id.action_location_info).setVisible(true);
        if (ChwApplication.getApplicationFlavor().hasHIV()) {
            menu.findItem(R.id.action_cbhs_registration).setVisible(true);
        }
        menu.findItem(R.id.action_tb_registration).setVisible(false);

        if (ChwApplication.getApplicationFlavor().hasFamilyPlanning() && age >= 10 && age <= 49) {
            flavor.updateFpMenuItems(memberObject.getBaseEntityId(), menu);
        } else {
            menu.findItem(R.id.action_fp_initiation).setVisible(false);
        }

        if (ChwApplication.getApplicationFlavor().hasANC() && !AncDao.isANCMember(memberObject.getBaseEntityId()) && age >= 10 && age <= 49 && gender.equalsIgnoreCase("Female")) {
            flavor.updateFpMenuItems(memberObject.getBaseEntityId(), menu);
            menu.findItem(R.id.action_anc_registration).setVisible(true);
        } else {
            menu.findItem(R.id.action_anc_registration).setVisible(false);
        }
        if (ChwApplication.getApplicationFlavor().hasANC() && age >= 10 && age <= 49 && gender.equalsIgnoreCase("Female")) {
            flavor.updateFpMenuItems(memberObject.getBaseEntityId(), menu);
            menu.findItem(R.id.action_pregnancy_out_come).setVisible(true);
        } else {
            menu.findItem(R.id.action_pregnancy_out_come).setVisible(false);
        }
        menu.findItem(R.id.action_sick_child_follow_up).setVisible(false);
        menu.findItem(R.id.action_malaria_diagnosis).setVisible(false);
        if (ChwApplication.getApplicationFlavor().hasMalaria())
            flavor.updateMalariaMenuItems(memberObject.getBaseEntityId(), menu);

        if (ChwApplication.getApplicationFlavor().hasHIVST()) {
            menu.findItem(R.id.action_hivst_registration).setVisible(!HivstDao.isRegisteredForHivst(memberObject.getBaseEntityId()) && age >= 15);
        }

        if (ChwApplication.getApplicationFlavor().hasAGYW()) {
            if (gender.equalsIgnoreCase("Female") && age >= 10 && age <= 24 && !AGYWDao.isRegisteredForAgyw(memberObject.getBaseEntityId())) {
                menu.findItem(R.id.action_agyw_screening).setVisible(true);
            }
        }

        if (ChwApplication.getApplicationFlavor().hasKvp()) {
            menu.findItem(R.id.action_kvp_prep_registration).setVisible(!KvpDao.isRegisteredForKvpPrEP(memberObject.getBaseEntityId()) && age >= 15);
        }

        if (ChwApplication.getApplicationFlavor().hasICCM() && !IccmDao.isRegisteredForIccm((memberObject.getBaseEntityId()))) {
            menu.findItem(R.id.action_iccm_registration).setVisible(true);
        }

        if (ChwApplication.getApplicationFlavor().hasSbc()) {
            menu.findItem(R.id.action_sbc_registration).setVisible(!SbcDao.isRegisteredForSbc(memberObject.getBaseEntityId()) && age >= 10);
        }

        if (ChwApplication.getApplicationFlavor().hasCecap()) {
            menu.findItem(R.id.action_cancer_preventive_services_registration).setVisible(!CecapDao.isRegisteredForCecap(memberObject.getBaseEntityId()) && age >= 14);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_anc_registration) {
            MemberProfileUtils.startAncRegister(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_pregnancy_out_come) {
            MemberProfileUtils.startPncRegister(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_initiation) {
            MemberProfileUtils.startFpRegister(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_ecp_provision) {
            MemberProfileUtils.startFpEcpScreening(AsrhMemberProfileActivity.this);
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_malaria_registration) {
            MemberProfileUtils.startMalariaRegister(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_iccm_registration) {
            MemberProfileUtils.startIntegratedCommunityCaseManagementEnrollment(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_vmmc_registration) {
            MemberProfileUtils.startVmmcRegister(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_registration) {
            if (UpdateDetailsUtil.isIndependentClient(memberObject.getBaseEntityId())) {
                startFormForEdit(org.smartregister.chw.core.R.string.registration_info,
                        CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            } else {
                startFormForEdit(org.smartregister.chw.core.R.string.edit_member_form_title,
                        CoreConstants.JSON_FORM.getFamilyMemberRegister());
            }
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_hiv_registration) {
            MemberProfileUtils.startHivRegister(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            MemberProfileUtils.startHivRegister(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_tb_registration) {
            MemberProfileUtils.startTbRegister(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId());
        } else if (i == org.smartregister.chw.core.R.id.action_hivst_registration) {
            MemberProfileUtils.startHivstRegistration(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_agyw_screening) {
            MemberProfileUtils.startAgywScreening(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_kvp_prep_registration) {
            MemberProfileUtils.startKvpPrEPRegistration(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_sbc_registration) {
            MemberProfileUtils.startSbcRegistration(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId());
        } else if (i == org.smartregister.chw.core.R.id.action_cancer_preventive_services_registration) {
            MemberProfileUtils.startCancerPreventiveServicesRegistration(AsrhMemberProfileActivity.this, memberObject.getBaseEntityId());
        } else if (i == R.id.action_remove_member) {
            removeIndividualProfile();
        }
        return super.onOptionsItemSelected(item);
    }


    public void startFormForEdit(Integer title_resource, String formName) {
        try {
            JSONObject form = null;
            boolean isPrimaryCareGiver = memberObject.getPrimaryCareGiver().equals(memberObject.getBaseEntityId());
            String titleString = title_resource != null ? getResources().getString(title_resource) : null;

            if (formName.equals(CoreConstants.JSON_FORM.getAncRegistration())) {

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new AncMemberDataLoader(titleString));
                form = binder.getPrePopulatedForm(formName);
                if (form != null) {
                    form.put(JsonFormUtils.ENCOUNTER_TYPE, CoreConstants.EventType.UPDATE_ANC_REGISTRATION);
                }

            } else if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {

                String eventName = org.smartregister.chw.util.Utils.metadata().familyMemberRegister.updateEventType;

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new FamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString, eventName, memberObject.getUniqueId()));

                form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getFamilyMemberRegister());
            } else if (formName.equals(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
                String eventName = org.smartregister.chw.util.Utils.metadata().familyMemberRegister.updateEventType;

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new FamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString, eventName, memberObject.getUniqueId()));

                form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            }

            startActivityForResult(org.smartregister.chw.util.JsonFormUtils.getAncPncStartFormIntent(form, this), JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.chw.cecap.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, org.smartregister.chw.cecap.util.Constants.REQUEST_CODE_GET_JSON);
    }

    protected void removeIndividualProfile() {
        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);
        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());

        IndividualProfileRemoveActivity.startIndividualProfileActivity(AsrhMemberProfileActivity.this,
                client, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), FamilyRegisterActivity.class.getCanonicalName());
    }
}
