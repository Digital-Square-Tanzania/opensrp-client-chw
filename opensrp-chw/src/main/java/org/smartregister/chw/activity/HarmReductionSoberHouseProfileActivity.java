package org.smartregister.chw.activity;

import static org.smartregister.chw.util.Utils.getCommonReferralTypes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.agyw.dao.AGYWDao;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreHarmReductionSoberHouseProfileActivity;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.custom_view.HarmReductionFloatingMenu;
import org.smartregister.chw.domain.SortableVisit;
import org.smartregister.chw.harmreduction.dao.HarmReductionDao;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.chw.harmreduction.util.HarmReductionVisitsUtil;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.interactor.HarmReductionVisitHistoryInteractor;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.sbc.dao.SbcDao;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.chw.util.MemberProfileUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class HarmReductionSoberHouseProfileActivity extends CoreHarmReductionSoberHouseProfileActivity {
    private final FamilyOtherMemberProfileActivity.Flavor flavor = new FamilyOtherMemberProfileActivityFlv();
    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, HarmReductionSoberHouseProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.HARM_REDUCTION_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void setupButtons() {
        if (memberObject == null || StringUtils.isBlank(memberObject.getBaseEntityId())) {
            updateDeceasedClientStatusTag(false, org.smartregister.chw.R.string.harm_reduction_followup_visit_client_deceased);
            hideDeceasedClientActionViews();
            return;
        }

        boolean deceasedClient = isClientDeceased();
        updateDeceasedClientStatusTag(deceasedClient, org.smartregister.chw.R.string.harm_reduction_followup_visit_client_deceased);
        if (deceasedClient) {
            hideDeceasedClientActionViews();
            return;
        }

        textViewRecordHarmReductionVisit.setVisibility(View.GONE);
        textViewRecordSoberHouseVisit.setVisibility(View.VISIBLE);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        TextView toolbarTitle = findViewById(org.smartregister.chw.R.id.toolbar_title);
        toolbarTitle.setText(org.smartregister.chw.harmreduction.R.string.return_to_sober_house_clients);
    }

    @Override
    public void initializeFloatingMenu() {
        if (memberObject == null) {
            return;
        }

        baseHarmReductionFloatingMenu = new HarmReductionFloatingMenu(this, memberObject);
        OnClickFloatingMenu onFloatingMenuClick = viewId -> {
            switch (viewId) {
                case org.smartregister.chw.harmreduction.R.id.harm_reduction_fab:
                    checkPhoneNumberProvided();
                    ((HarmReductionFloatingMenu) baseHarmReductionFloatingMenu).animateFAB();
                    break;
                case org.smartregister.chw.R.id.call_layout:
                    ((HarmReductionFloatingMenu) baseHarmReductionFloatingMenu).launchCallWidget();
                    ((HarmReductionFloatingMenu) baseHarmReductionFloatingMenu).animateFAB();
                    break;
                case org.smartregister.chw.R.id.refer_to_facility_layout:
                    org.smartregister.chw.util.Utils.launchClientReferralActivity(HarmReductionSoberHouseProfileActivity.this, getReferralTypeModels(), memberObject.getBaseEntityId());
                    ((HarmReductionFloatingMenu) baseHarmReductionFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }
        };

        ((HarmReductionFloatingMenu) baseHarmReductionFloatingMenu).setFloatMenuClickListener(onFloatingMenuClick);

        checkPhoneNumberProvided();

        baseHarmReductionFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseHarmReductionFloatingMenu, linearLayoutParams);
        baseHarmReductionFloatingMenu.setVisibility(View.VISIBLE);
        baseHarmReductionFloatingMenu.bringToFront();
    }

    private void checkPhoneNumberProvided() {
        if (baseHarmReductionFloatingMenu != null) {
            ((HarmReductionFloatingMenu) baseHarmReductionFloatingMenu).redraw(StringUtils.isNotBlank(memberObject.getPhoneNumber()));
        }
    }

    private List<ReferralTypeModel> getReferralTypeModels() {
        referralTypeModels.clear();
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            List<ReferralTypeModel> commonReferralTypes = getCommonReferralTypes(this, memberObject.getBaseEntityId());
            if (commonReferralTypes != null) {
                referralTypeModels.addAll(commonReferralTypes);
            }
        }
        return referralTypeModels;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        if (StringUtils.isBlank(baseEntityId)) {
            finish();
            return;
        }
        if (memberObject == null) {
            memberObject = HarmReductionDao.getSoberHouseMember(baseEntityId);
            if (memberObject == null) {
                memberObject = HarmReductionDao.getContact(baseEntityId);
            }
        }
        super.onCreate(savedInstanceState);
        try {
            HarmReductionVisitsUtil.processVisits();
        } catch (Exception e) {
            Timber.e(e);
        }
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
    public void openRecordClientVisit() {
        HarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openFollowupVisit() {
        HarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openRecordTbContactVisit() {
        HarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openMedicalHistory() {
        HarmReductionSoberHouseVisitHistoryActivity.startMe(this, memberObject);
    }

    @Override
    public void openObservationResults() {
        // no-op
    }

    @Override
    public void openHarmReductionContactRegister() {
        // no-op
    }

    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    protected void removeMember() {
        // no-op
    }

    @NonNull
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    public void setProfileImage(String s, String s1) {
        // no-op
    }

    public void setProfileDetailThree(String s) {
        // no-op
    }

    public void toggleFamilyHead(boolean b) {
        // no-op
    }

    public void togglePrimaryCaregiver(boolean b) {
        // no-op
    }

    @Override
    public void startServiceForm() {
        HarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void continueService() {
        HarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueContactVisit() {
        HarmReductionSoberHouseVisitActivity.startHarmReductionSoberHouseVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void startHivstRegistration() {
        MemberProfileUtils.startHivstRegistration(this, memberObject.getBaseEntityId(), memberObject.getGender());
    }

    public void refreshList() {
        // no-op
    }

    public void updateHasPhone(boolean b) {
        // no-op
    }

    public void setFamilyServiceStatus(String s) {
        // no-op
    }

    public void verifyHasPhone() {
        // no-op
    }

    public void notifyHasPhone(boolean b) {
        // no-op
    }

    @Override
    public void onEventSaveComplete(boolean b) {
        // no-op
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyHarmReductionSoberHouseDeceasedHandling();
        refreshMedicalHistory(true);
    }

    protected boolean isClientDeceased() {
        if (memberObject == null || StringUtils.isBlank(memberObject.getBaseEntityId())) {
            return false;
        }

        try {
            return HarmReductionDao.isSoberHouseClientDeceased(memberObject.getBaseEntityId());
        } catch (Throwable throwable) {
            Timber.e(throwable);
            return false;
        }
    }

    void applyHarmReductionSoberHouseDeceasedHandling() {
        boolean deceasedClient = isClientDeceased();
        updateDeceasedClientStatusTag(deceasedClient, org.smartregister.chw.R.string.harm_reduction_followup_visit_client_deceased);
        if (!deceasedClient) {
            return;
        }

        hideDeceasedClientActionViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (memberObject == null) {
            return super.onCreateOptionsMenu(menu);
        }

        MenuItem addMember = menu.findItem(org.smartregister.chw.core.R.id.add_member);
        if (addMember != null) {
            addMember.setVisible(false);
        }

        getMenuInflater().inflate(org.smartregister.chw.core.R.menu.other_member_menu, menu);

        int age = memberObject.getAge();
        String gender = memberObject.getGender();
        String baseEntityId = memberObject.getBaseEntityId();

        AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_location_info, true);
        if (ChwApplication.getApplicationFlavor().hasHIV()) {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_cbhs_registration, true);
        }
        AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_tb_registration, false);

        if (ChwApplication.getApplicationFlavor().hasFamilyPlanning() && age >= 10 && age <= 49) {
            flavor.updateFpMenuItems(baseEntityId, menu);
        } else {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_fp_initiation, false);
        }

        if (ChwApplication.getApplicationFlavor().hasANC() && !AncDao.isANCMember(baseEntityId) && age >= 10 && age <= 49 && gender.equalsIgnoreCase("Female")) {
            flavor.updateFpMenuItems(baseEntityId, menu);
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_anc_registration, true);
        } else {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_anc_registration, false);
        }

        if (ChwApplication.getApplicationFlavor().hasANC() && age >= 10 && age <= 49 && gender.equalsIgnoreCase("Female")) {
            flavor.updateFpMenuItems(baseEntityId, menu);
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_pregnancy_out_come, true);
        } else {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_pregnancy_out_come, false);
        }

        AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_sick_child_follow_up, false);
        AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_malaria_diagnosis, false);
        if (ChwApplication.getApplicationFlavor().hasMalaria()) {
            flavor.updateMalariaMenuItems(baseEntityId, menu);
        }

        if (ChwApplication.getApplicationFlavor().hasHIVST()) {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_hivst_registration, !HivstDao.isRegisteredForHivst(baseEntityId) && age >= 15);
        }

        if (ChwApplication.getApplicationFlavor().hasAGYW() && gender.equalsIgnoreCase("Female") && age >= 10 && age <= 24 && !AGYWDao.isRegisteredForAgyw(baseEntityId)) {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_agyw_screening, true);
        }

        if (ChwApplication.getApplicationFlavor().hasKvp()) {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_kvp_prep_registration, !KvpDao.isRegisteredForKvpPrEP(baseEntityId) && age >= 15);
        }

        if (ChwApplication.getApplicationFlavor().hasICCM() && !IccmDao.isRegisteredForIccm(baseEntityId)) {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_iccm_registration, true);
        }

        if (ChwApplication.getApplicationFlavor().hasSbc()) {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_sbc_registration, !SbcDao.isRegisteredForSbc(baseEntityId) && age >= 10);
        }

        if (ChwApplication.getApplicationFlavor().hasCecap()) {
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_cancer_preventive_services_registration, !CecapDao.isRegisteredForCecap(baseEntityId) && age >= 14);
        }

        AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_harm_reduction_assessment, false);
        if (ChwApplication.getApplicationFlavor().hasHarmReductionSoberHouse()) {
            boolean isRegisteredForSoberHouse = HarmReductionDao.getSoberHouseMember(baseEntityId) != null;
            AllClientsUtils.setMenuItemVisibility(menu, org.smartregister.chw.R.id.action_harm_reduction_sober_house_enrollment, !isRegisteredForSoberHouse && age >= 14);
        }

        AllClientsUtils.addTbLeprosyMenuItem(menu, baseEntityId);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (memberObject == null) {
            return super.onOptionsItemSelected(item);
        }

        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_anc_registration) {
            MemberProfileUtils.startAncRegister(this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_pregnancy_out_come) {
            MemberProfileUtils.startPncRegister(this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_initiation) {
            MemberProfileUtils.startFpRegister(this, memberObject.getBaseEntityId(), memberObject.getGender());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_ecp_provision) {
            MemberProfileUtils.startFpEcpScreening(this);
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_malaria_registration) {
            MemberProfileUtils.startMalariaRegister(this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_iccm_registration) {
            MemberProfileUtils.startIntegratedCommunityCaseManagementEnrollment(this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_vmmc_registration) {
            MemberProfileUtils.startVmmcRegister(this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_hiv_registration || i == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            MemberProfileUtils.startHivRegister(this, memberObject.getBaseEntityId(), memberObject.getGender(), getMemberDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_tb_registration) {
            MemberProfileUtils.startTbRegister(this, memberObject.getBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_hivst_registration) {
            MemberProfileUtils.startHivstRegistration(this, memberObject.getBaseEntityId(), memberObject.getGender());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_agyw_screening) {
            MemberProfileUtils.startAgywScreening(this, memberObject.getBaseEntityId(), getMemberDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_kvp_prep_registration) {
            MemberProfileUtils.startKvpPrEPRegistration(this, memberObject.getBaseEntityId(), memberObject.getGender(), getMemberDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_sbc_registration) {
            MemberProfileUtils.startSbcRegistration(this, memberObject.getBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_cancer_preventive_services_registration) {
            MemberProfileUtils.startCancerPreventiveServicesRegistration(this, memberObject.getBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.R.id.action_harm_reduction_sober_house_enrollment) {
            HarmReductionSoberHouseRegisterActivity.startRegistration(this, memberObject.getBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.R.id.action_tbleprosy_screening) {
            startTbLeprosyScreening();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_location_info) {
            JSONObject preFilledForm = CoreJsonFormUtils.getAutoPopulatedJsonEditFormString(
                    CoreConstants.JSON_FORM.getFamilyDetailsRegister(),
                    this,
                    UpdateDetailsUtil.getFamilyRegistrationDetails(
                            UpdateDetailsUtil.getFamilyBaseEntityId(
                                    org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient(memberObject.getBaseEntityId())
                            )
                    ),
                    Utils.metadata().familyRegister.updateEventType
            );
            if (preFilledForm != null) {
                UpdateDetailsUtil.startUpdateClientDetailsActivity(preFilledForm, this);
            }
            return true;
        } else if (i == org.smartregister.chw.R.id.action_remove_member) {
            removeIndividualProfile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startTbLeprosyScreening() {
        TbLeprosyRegisterActivity.startRegistration(this, memberObject.getBaseEntityId());
    }

    private void removeIndividualProfile() {
        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);
        CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());

        IndividualProfileRemoveActivity.startIndividualProfileActivity(
                this,
                client,
                memberObject.getFamilyBaseEntityId(),
                memberObject.getFamilyHead(),
                memberObject.getPrimaryCareGiver(),
                FamilyRegisterActivity.class.getCanonicalName()
        );
    }

    private String getMemberDob() {
        try {
            CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);
            CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
            if (commonPersonObject == null || commonPersonObject.getColumnmaps() == null) {
                return null;
            }
            return Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    private boolean hasVisitsAfterMatConsent() {
        return hasMinimumVisitsAfterMatConsent(1);
    }

    private boolean hasMinimumVisitsAfterMatConsent(int minimumVisits) {
        if (minimumVisits <= 0) {
            return true;
        }
        if (memberObject == null) {
            return false;
        }

        try {
            List<SortableVisit> visits = HarmReductionVisitHistoryInteractor.getVisits(
                    memberObject.getBaseEntityId(),
                    Constants.EVENT_TYPE.HARM_REDUCTION_SOBER_HOUSE_VISIT
            );

            int visitCount = 0;
            for (SortableVisit visit : visits) {
                Date visitDate = visit.getDate();
                if (visitDate != null) {
                    visitCount++;
                    if (visitCount >= minimumVisits) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return false;
    }
}
