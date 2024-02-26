package org.smartregister.chw.activity;

import static org.smartregister.chw.util.Constants.CECAP_REFERRAL_FORM;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.agyw.dao.AGYWDao;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.asrh.dao.AsrhDao;
import org.smartregister.chw.cecap.CecapLibrary;
import org.smartregister.chw.cecap.activity.BaseCecapProfileActivity;
import org.smartregister.chw.cecap.domain.MemberObject;
import org.smartregister.chw.cecap.presenter.BaseCecapProfilePresenter;
import org.smartregister.chw.cecap.util.CecapJsonFormUtils;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.cecap.util.VisitUtils;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.custom_view.CecapFloatingMenu;
import org.smartregister.chw.dataloader.AncMemberDataLoader;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.gbv.util.GbvJsonFormUtils;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.interactor.CecapMemberProfileInteractor;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.sbc.dao.SbcDao;
import org.smartregister.chw.util.MemberProfileUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class CecapMemberProfileActivity extends BaseCecapProfileActivity {
    private final FamilyOtherMemberProfileActivity.Flavor flavor = new FamilyOtherMemberProfileActivityFlv();
    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, CecapMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        profilePresenter = new BaseCecapProfilePresenter(this, new CecapMemberProfileInteractor(), memberObject);
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    @Override
    public void recordCecap(MemberObject memberObject) {
        JSONObject jsonObject;
        try {
            jsonObject = CecapJsonFormUtils.getFormAsJson(Constants.FORMS.CECAP_COMMUNITY_VISIT);

            String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            GbvJsonFormUtils.getRegistrationForm(jsonObject, memberObject.getBaseEntityId(), locationId);
            startFormActivity(jsonObject);
        } catch (Exception e) {
            Timber.e(e);
        }
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

        if (ChwApplication.getApplicationFlavor().hasAsrh()) {
            menu.findItem(R.id.action_asrh_registration).setVisible(!AsrhDao.isRegisteredForAsrh(memberObject.getBaseEntityId()) && age >= 10 && age < 25);
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
            MemberProfileUtils.startAncRegister(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_pregnancy_out_come) {
            MemberProfileUtils.startPncRegister(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_initiation) {
            MemberProfileUtils.startFpRegister(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_ecp_provision) {
            MemberProfileUtils.startFpEcpScreening(CecapMemberProfileActivity.this);
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_malaria_registration) {
            MemberProfileUtils.startMalariaRegister(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_iccm_registration) {
            MemberProfileUtils.startIntegratedCommunityCaseManagementEnrollment(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_vmmc_registration) {
            MemberProfileUtils.startVmmcRegister(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
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
            MemberProfileUtils.startHivRegister(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            MemberProfileUtils.startHivRegister(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_tb_registration) {
            MemberProfileUtils.startTbRegister(CecapMemberProfileActivity.this, memberObject.getBaseEntityId());
        } else if (i == org.smartregister.chw.core.R.id.action_hivst_registration) {
            MemberProfileUtils.startHivstRegistration(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_agyw_screening) {
            MemberProfileUtils.startAgywScreening(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_kvp_prep_registration) {
            MemberProfileUtils.startKvpPrEPRegistration(CecapMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_sbc_registration) {
            MemberProfileUtils.startSbcRegistration(CecapMemberProfileActivity.this, memberObject.getBaseEntityId());
        } else if (i == R.id.action_asrh_registration) {
            MemberProfileUtils.startAsrhRegistration(CecapMemberProfileActivity.this, memberObject.getBaseEntityId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        try {
            VisitUtils.processVisits(CecapLibrary.getInstance().visitRepository(), CecapLibrary.getInstance().visitDetailsRepository(), CecapMemberProfileActivity.this);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openMedicalHistory() {
        CecapMedicalHistoryActivity.startMe(this, memberObject);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    private void addReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            getReferralTypeModels().add(new ReferralTypeModel(getString(R.string.cecap_referral), CECAP_REFERRAL_FORM, CoreConstants.TASKS_FOCUS.CECAP_REFERRAL));

            if (isClientEligibleForAnc(memberObject)) {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.anc_danger_signs), org.smartregister.chw.util.Constants.JSON_FORM.getAncUnifiedReferralForm(), CoreConstants.TASKS_FOCUS.ANC_DANGER_SIGNS));
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.pnc_referral), CoreConstants.JSON_FORM.getPncUnifiedReferralForm(), CoreConstants.TASKS_FOCUS.PNC_DANGER_SIGNS));
                if (!AncDao.isANCMember(memberObject.getBaseEntityId())) {
                    referralTypeModels.add(new ReferralTypeModel(getString(R.string.pregnancy_confirmation), CoreConstants.JSON_FORM.getPregnancyConfirmationReferralForm(), CoreConstants.TASKS_FOCUS.PREGNANCY_CONFIRMATION));
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
        baseCecapFloatingMenu = new CecapFloatingMenu(this, memberObject);
        baseCecapFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseCecapFloatingMenu, linearLayoutParams);


        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.cecap_fab:
                    checkPhoneNumberProvided();
                    ((CecapFloatingMenu) baseCecapFloatingMenu).animateFAB();
                    break;
                case R.id.cecap_call_layout:
                    ((CecapFloatingMenu) baseCecapFloatingMenu).launchCallWidget();
                    ((CecapFloatingMenu) baseCecapFloatingMenu).animateFAB();
                    break;
                case R.id.cecap_refer_to_facility_layout:
                    org.smartregister.chw.util.Utils.launchClientReferralActivity(CecapMemberProfileActivity.this, getReferralTypeModels(), memberObject.getBaseEntityId());
                    ((CecapFloatingMenu) baseCecapFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((CecapFloatingMenu) baseCecapFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(memberObject.getPhoneNumber()));
        ((CecapFloatingMenu) baseCecapFloatingMenu).redraw(phoneNumberAvailable);
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
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }
}
