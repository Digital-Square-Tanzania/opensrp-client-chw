package org.smartregister.chw.activity;

import static org.smartregister.chw.activity.FamilyOtherMemberProfileActivity.convertDateToLong;
import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;
import static org.smartregister.chw.util.Utils.truncateTimeFromDate;
import static org.smartregister.family.util.Utils.metadata;
import static org.smartregister.util.Utils.getValue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.nerdstone.neatformcore.domain.model.NFormViewData;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.agyw.dao.AGYWDao;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.core.activity.CoreHpsProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.custom_view.HpsFloatingMenu;
import org.smartregister.chw.dao.ChwHpsDao;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.dao.NcdDao;
import org.smartregister.chw.dataloader.AncMemberDataLoader;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.hps.HpsLibrary;
import org.smartregister.chw.hps.dao.HpsDao;
import org.smartregister.chw.hps.domain.MemberObject;
import org.smartregister.chw.hps.domain.Visit;
import org.smartregister.chw.hps.util.Constants;
import org.smartregister.chw.hps.util.VisitUtils;
import org.smartregister.chw.interactor.IssueReferralInteractor;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.model.FamilyDetailsModel;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.referral.contract.BaseIssueReferralContract;
import org.smartregister.chw.sbc.dao.SbcDao;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.chw.util.MemberProfileUtils;
import org.smartregister.chw.util.TreatmentSupporterFormUtil;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class HpsMemberProfileActivity extends CoreHpsProfileActivity implements OnRetrieveNotifications {
    private final FamilyOtherMemberProfileActivity.Flavor flavor = new FamilyOtherMemberProfileActivityFlv();
    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private final NotificationListAdapter notificationListAdapter = new NotificationListAdapter();
    private List<FamilyDetailsModel> headedFamilies = Collections.emptyList();

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, HpsMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            notificationAndReferralRecyclerView.setAdapter(notificationListAdapter);
            notificationListAdapter.setOnClickListener(this);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        try {
            VisitUtils.processVisits(HpsLibrary.getInstance().visitRepository(), HpsLibrary.getInstance().visitDetailsRepository(), memberObject.getBaseEntityId());
        } catch (Exception e) {
            Timber.e(e);
        }

        // Load households headed by this client for HH chip
        try {
            headedFamilies = FamilyDao.getFamiliesByHead(memberObject.getBaseEntityId());
        } catch (Exception e) {
            Timber.e(e);
            headedFamilies = Collections.emptyList();
        }
        try {
            delayInvalidateOptionsMenu();
        } catch (Exception ignore) {
        }

        if (ChwHpsDao.wereSelfTestingKitsDistributed(memberObject.getBaseEntityId())) {
            if (HivstDao.isRegisteredForHivst(memberObject.getBaseEntityId())) {
                boolean shouldIssueHivSelfTestingKits = false;
                String lastSelfTestingFollowupDateString = HivstDao.clientLastFollowup(memberObject.getBaseEntityId());
                if (lastSelfTestingFollowupDateString == null) {
                    shouldIssueHivSelfTestingKits = true;
                } else {
                    try {
                        Date lastSelfTestingFollowupDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(lastSelfTestingFollowupDateString);
                        Visit lastVisit = getServiceVisit();
                        if (truncateTimeFromDate(lastSelfTestingFollowupDate).before(truncateTimeFromDate(lastVisit.getDate())) && lastVisit.getProcessed()) {
                            shouldIssueHivSelfTestingKits = true;
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }

                if (shouldIssueHivSelfTestingKits) {
                    textViewRecordHps.setVisibility(View.GONE);
                    visitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setText(R.string.issue_selft_testing_kits);
                    textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_followup));
                    textViewVisitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setOnClickListener(view -> HivstProfileActivity.startProfile(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), true));
                    imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
                } else {
                    textViewRecordHps.setVisibility(View.VISIBLE);
                    visitDone.setVisibility(View.GONE);
                    textViewVisitDone.setVisibility(View.GONE);
                    if (isVisitOnProgress(getServiceVisit())) {
                        textViewRecordHps.setVisibility(View.GONE);
                        visitInProgress.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                textViewRecordHps.setVisibility(View.GONE);
                visitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setText(R.string.register_client);
                textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_registration));
                textViewVisitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setOnClickListener(v -> startHivstRegistration());
                imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
            }
        }

        if (ChwApplication.getApplicationFlavor().hasNCD()
                && !NcdDao.isNcdClient(memberObject.getBaseEntityId())
                && ChwHpsDao.isBloodPressureAboveThreshold(memberObject.getBaseEntityId())) {
            textViewRecordHps.setVisibility(View.GONE);
            visitDone.setVisibility(View.VISIBLE);
            textViewVisitDone.setText(getString(R.string.hps_high_bp_detected));
            textViewVisitDone.setVisibility(View.VISIBLE);
            textViewVisitDoneEdit.setText(R.string.hps_screen_for_diabetes);
            textViewVisitDoneEdit.setOnClickListener(v ->
                    MemberProfileUtils.startDiabetesRiskAssessment(
                            HpsMemberProfileActivity.this,
                            memberObject.getBaseEntityId(),
                            memberObject.getAge()));
            imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
        }
    }

    @Override
    public void openMedicalHistory() {
        HpsVisitHistoryActivity.startMe(this, memberObject);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
        memberObject = HpsDao.getMember(memberObject.getBaseEntityId());
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(),
                memberObject.getBaseEntityId(), this);
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

            if (memberObject.getGender().equalsIgnoreCase("male"))
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.aysrh_referral), CoreConstants.JSON_FORM.getMaleAysrhFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.AYSRH_FRIENDLY_SERVICES));
            else
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.aysrh_referral), CoreConstants.JSON_FORM.getFemaleAysrhFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.AYSRH_FRIENDLY_SERVICES));
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.hts_referral), CoreConstants.JSON_FORM.getHtsReferralForm(), CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST));
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_referral), CoreConstants.JSON_FORM.getTbReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_TB));

        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        handleNotificationRowClick(this, view, notificationListAdapter, memberObject.getBaseEntityId());
    }

    @Override
    public void initializeFloatingMenu() {
        baseHpsFloatingMenu = new HpsFloatingMenu(this, memberObject, true);
        baseHpsFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseHpsFloatingMenu, linearLayoutParams);


        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.hps_fab:
                    checkPhoneNumberProvided();
                    ((HpsFloatingMenu) baseHpsFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((HpsFloatingMenu) baseHpsFloatingMenu).launchCallWidget();
                    ((HpsFloatingMenu) baseHpsFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    org.smartregister.chw.util.Utils.launchClientReferralActivity(HpsMemberProfileActivity.this, getReferralTypeModels(), memberObject.getBaseEntityId());
                    ((HpsFloatingMenu) baseHpsFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }
        };

        ((HpsFloatingMenu) baseHpsFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(memberObject.getPhoneNumber()));
        ((HpsFloatingMenu) baseHpsFloatingMenu).redraw(phoneNumberAvailable);
    }

    protected boolean isClientEligibleForAnc(MemberObject hivMemberObject) {
        if (hivMemberObject.getGender().equalsIgnoreCase("Female")) {
            //Obtaining the clients CommonPersonObjectClient used for checking is the client is Of Reproductive Age
            CommonRepository commonRepository = Utils.context().commonrepository(metadata().familyMemberRegister.tableName);

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

        int age = memberObject.getAge();

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

        if (ChwApplication.getApplicationFlavor().hasAGYW() && gender.equalsIgnoreCase("Female") && age >= 10 && age <= 24 && !AGYWDao.isRegisteredForAgyw(memberObject.getBaseEntityId())) {
            menu.findItem(R.id.action_agyw_screening).setVisible(true);
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
        AllClientsUtils.addTbLeprosyMenuItem(menu, memberObject.getBaseEntityId());

        if (age >= 30 && ChwApplication.getApplicationFlavor().hasNCD()) {
            menu.findItem(R.id.action_diabetes_risk).setVisible(true);
        }

        // Add/Update HH chip action (Households HH (N))
        try {
            int hhCount = headedFamilies != null ? headedFamilies.size() : 0;

            MenuItem householdsItem = menu.findItem(org.smartregister.chw.R.id.action_view_households);
            if (householdsItem == null) {
                householdsItem = menu.add(Menu.NONE, org.smartregister.chw.R.id.action_view_households, Menu.NONE, "");
            }

            householdsItem.setVisible(hhCount > 0);
            householdsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            householdsItem.setActionView(org.smartregister.chw.R.layout.action_households_action);
            View av = householdsItem.getActionView();
            if (av != null) {
                TextView tv = av.findViewById(org.smartregister.chw.R.id.tv_households_label);
                if (tv != null) {
                    boolean shortLabel = getResources().getBoolean(org.smartregister.chw.R.bool.use_short_hh_label);
                    tv.setText(getString(shortLabel ? org.smartregister.chw.R.string.hh_with_count : org.smartregister.chw.R.string.household_with_count, hhCount));
                }
                av.setOnClickListener(v -> {
                    if (hhCount <= 0) return;
                    if (hhCount == 1) openFamilyProfile(headedFamilies.get(0));
                    else handleViewHouseholdsClick();
                });
                String fullTitle = getString(org.smartregister.chw.R.string.view_households_with_count, hhCount);
                av.setContentDescription(fullTitle);
            }
        } catch (Exception e) {
            Timber.e(e);
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
            MemberProfileUtils.startAncRegister(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_pregnancy_out_come) {
            MemberProfileUtils.startPncRegister(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_initiation) {
            MemberProfileUtils.startFpRegister(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_ecp_provision) {
            MemberProfileUtils.startFpEcpScreening(HpsMemberProfileActivity.this);
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_malaria_registration) {
            MemberProfileUtils.startMalariaRegister(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_iccm_registration) {
            MemberProfileUtils.startIntegratedCommunityCaseManagementEnrollment(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_vmmc_registration) {
            MemberProfileUtils.startVmmcRegister(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
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
            MemberProfileUtils.startHivRegister(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            MemberProfileUtils.startHivRegister(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_tb_registration) {
            MemberProfileUtils.startTbRegister(HpsMemberProfileActivity.this, memberObject.getBaseEntityId());
        } else if (i == org.smartregister.chw.core.R.id.action_hivst_registration) {
            MemberProfileUtils.startHivstRegistration(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_agyw_screening) {
            MemberProfileUtils.startAgywScreening(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_kvp_prep_registration) {
            MemberProfileUtils.startKvpPrEPRegistration(HpsMemberProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getDob());
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_sbc_registration) {
            MemberProfileUtils.startSbcRegistration(HpsMemberProfileActivity.this, memberObject.getBaseEntityId());
        } else if (i == org.smartregister.chw.core.R.id.action_cancer_preventive_services_registration) {
            MemberProfileUtils.startCancerPreventiveServicesRegistration(HpsMemberProfileActivity.this, memberObject.getBaseEntityId());
        } else if (i == R.id.action_tbleprosy_screening) {
            startTbLeprosyScreening();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_location_info) {
            JSONObject preFilledForm = CoreJsonFormUtils.getAutoPopulatedJsonEditFormString(CoreConstants.JSON_FORM.getFamilyDetailsRegister(), this, UpdateDetailsUtil.getFamilyRegistrationDetails(UpdateDetailsUtil.getFamilyBaseEntityId(org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient(this.memberObject.getBaseEntityId()))), org.smartregister.family.util.Utils.metadata().familyRegister.updateEventType);
            if (preFilledForm != null) {
                UpdateDetailsUtil.startUpdateClientDetailsActivity(preFilledForm, this);
            }

            return true;
        } else if (i == R.id.action_remove_member) {
            removeIndividualProfile();
            return true;
        } else if (i == org.smartregister.chw.R.id.action_view_households) {
            handleViewHouseholdsClick();
            return true;
        } else if (i == R.id.action_diabetes_risk) {
            MemberProfileUtils.startDiabetesRiskAssessment(HpsMemberProfileActivity.this,
                    memberObject.getBaseEntityId(), memberObject.getAge());
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void handleViewHouseholdsClick() {
        if (headedFamilies == null || headedFamilies.isEmpty()) return;
        if (headedFamilies.size() == 1) {
            openFamilyProfile(headedFamilies.get(0));
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(org.smartregister.chw.R.layout.dialog_households_list, null, false);
        ListView listView = dialogView.findViewById(org.smartregister.chw.R.id.list_households);
        HouseholdsAdapter adapter = new HouseholdsAdapter(this, headedFamilies);
        listView.setAdapter(adapter);
        TextView btnCancel = dialogView.findViewById(org.smartregister.chw.R.id.btn_cancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < headedFamilies.size()) {
                openFamilyProfile(headedFamilies.get(position));
                dialog.dismiss();
            }
        });
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void openFamilyProfile(FamilyDetailsModel family) {
        try {
            Intent intent = new Intent(this, FamilyProfileActivity.class);
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, family.getBaseEntityId());
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, family.getFamilyHead());
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, family.getPrimaryCareGiver());
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME, family.getFamilyName());
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.VILLAGE_TOWN, family.getVillageTown());
            startActivity(intent);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void delayInvalidateOptionsMenu() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(this::invalidateOptionsMenu, 2000);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void startTbLeprosyScreening() {
        TbLeprosyRegisterActivity.startRegistration(HpsMemberProfileActivity.this, memberObject.getBaseEntityId());
    }

    public void startFormForEdit(Integer title_resource, String formName) {
        try {
            JSONObject form = null;
            boolean isPrimaryCareGiver = memberObject.getPrimaryCareGiver() != null
                    && memberObject.getPrimaryCareGiver().equals(memberObject.getBaseEntityId());
            String titleString = title_resource != null ? getResources().getString(title_resource) : null;

            if (formName.equals(CoreConstants.JSON_FORM.getAncRegistration())) {

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new AncMemberDataLoader(titleString));
                form = binder.getPrePopulatedForm(formName);
                if (form != null) {
                    form.put(JsonFormUtils.ENCOUNTER_TYPE, CoreConstants.EventType.UPDATE_ANC_REGISTRATION);
                }
            } else if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {

                String eventName = metadata().familyMemberRegister.updateEventType;

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new FamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString, eventName, memberObject.getUniqueId()));

                form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getFamilyMemberRegister());
            } else if (formName.equals(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
                String eventName = metadata().familyMemberRegister.updateEventType;

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
        Intent intent = new Intent(this, metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.chw.cecap.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, org.smartregister.chw.cecap.util.Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void startHivstRegistration() {
        CommonRepository commonRepository = context().commonrepository(metadata().familyMemberRegister.tableName);

        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        String gender = getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);

        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), gender);
    }

    protected void removeIndividualProfile() {
        CommonRepository commonRepository = Utils.context().commonrepository(metadata().familyMemberRegister.tableName);
        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());

        IndividualProfileRemoveActivity.startIndividualProfileActivity(HpsMemberProfileActivity.this,
                client, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), FamilyRegisterActivity.class.getCanonicalName());
    }

    @Override
    public void startServiceForm() {
        HpsClientServicesVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void continueService() {
        HpsClientServicesVisitActivity.startMe(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void openFollowupVisit() {
        HpsClientServicesVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit lastVisit = getServiceVisit();
        if (lastVisit != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
        } else {
            rlLastVisit.setVisibility(View.GONE);
        }
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            try {
                String jsonForm = data.getStringExtra(org.smartregister.family.util.Constants.INTENT_KEY.JSON);
                if (jsonForm == null) return;
                JSONObject jsonObject= org.smartregister.chw.util.JsonFormUtils.getFieldJSONObject(org.smartregister.chw.util.JsonFormUtils.fields(new JSONObject(jsonForm)),"db_save_n_refer");
                if (jsonObject == null) return;
                if(Boolean.parseBoolean(jsonObject.optString("value"))){
                    sendNCDReferralToFacility(createReferralForm(data));
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }
    private void sendNCDReferralToFacility(HashMap<String, NFormViewData> data) {
        try {
            JSONObject NCDForm = new FormUtils().getFormJsonFromRepositoryOrAssets(HpsMemberProfileActivity.this, "referrals/referral_form");
            if (NCDForm == null) {
                return;
            }

            NCDForm.put("referral_task_focus", "Diabetes and Hypertension Testing");

            BaseIssueReferralContract.InteractorCallBack interactorCallback = new BaseIssueReferralContract.InteractorCallBack() {
                @Override
                public void onUniqueIdFetched(@NonNull Triple<String, String, String> triple, @NonNull String entityId) {
                    // No-op: referral form uses existing entity id
                }

                @Override
                public void onNoUniqueId() {
                    Timber.w("No unique ID fetched while saving NCD referral for %s", baseEntityId);
                }

                @Override
                public void onRegistrationSaved(boolean isSaved) {
                    Timber.i("NCD referral save status: %s", isSaved ? "success" : "failed");
                }

                @Override
                public void onRegistrationSaved(boolean isSaved, boolean isAddoLinkage) {
                    Timber.i("NCD referral save status: %s, isAddoLinkage: %s", isSaved ? "success" : "failed", isAddoLinkage);
                    if (isSaved) {
                        Toast.makeText(HpsMemberProfileActivity.this, R.string.referral_submitted, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(HpsMemberProfileActivity.this, R.string.referral_not_submitted, Toast.LENGTH_LONG).show();
                    }
                }
            };

            new IssueReferralInteractor().saveRegistration(baseEntityId, data, NCDForm, interactorCallback, false);
        } catch (Exception e) {
            Timber.e(e);
        }
    }



    private HashMap<String, NFormViewData> createReferralForm(Intent data) {
        try {
            HashMap<String, NFormViewData> formData = new HashMap<>();
            String jsonForm = data.getStringExtra(org.smartregister.family.util.Constants.INTENT_KEY.JSON);

            assert jsonForm != null;

            // Referral problem
            HashMap<String, NFormViewData> problemValue = new HashMap<>();
            NFormViewData dbRisk = createFormViewData("Risk for diabetes and hypertension",null,metaData("","risk_for_diabetes_and_hypertension", ""));
            problemValue.put("risk_for_diabetes_and_hypertension", dbRisk);
            NFormViewData problemFormViewData = createFormViewData(problemValue, "MultiChoiceCheckBox",metaData("", "concept", "problem"));
            problemFormViewData.setType("MultiChoiceCheckBox");
            formData.put("problem", problemFormViewData);

            // Referral facility
            String facilityValue = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "chw_referral_hf");
            JSONArray jsonArray = org.smartregister.chw.util.JsonFormUtils.fields(new JSONObject(jsonForm));
            JSONObject chwReferralHf = org.smartregister.chw.util.JsonFormUtils.getFieldJSONObject(jsonArray, "chw_referral_hf");
            assert chwReferralHf != null;
            JSONArray options = chwReferralHf.getJSONArray("options");
            String facilityText = "";
            for (int i=0; i < options.length();i++){
                JSONObject option = options.getJSONObject(i);
                if(facilityValue.equals(option.getString("key"))){
                    facilityText = option.getString("text");
                }
            }
            NFormViewData chwReferralValue = createFormViewData(facilityText, null, metaData("location_uuid",facilityValue,""));
            formData.put("chw_referral_hf", createFormViewData(chwReferralValue,"SpinnerNFormView", metaData("concept", "chw_referral_hf", "")));

            // Service before referral
            String serviceBReferralValue = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "service_before_referral");
            JSONArray serviceBReferralArray = new JSONArray(serviceBReferralValue);
            HashMap<String, NFormViewData> serviceBReferralNFormValue = new HashMap<>();
            for(int i=0; i < serviceBReferralArray.length(); i++){
                String serviceValue = serviceBReferralArray.getString(i);
                NFormViewData valueItem = createFormViewData( serviceValue, null, metaData("", serviceValue, ""));
                serviceBReferralNFormValue.put(serviceValue, valueItem);
            }
            formData.put("service_before_referral", createFormViewData(serviceBReferralNFormValue, "MultiChoiceCheckBox", metaData("concept", "service_before_referral", "")));

            // Diabetes risk score
            String dbRiskScore = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "diabetes_risk_score_output");
            formData.put("diabetes_risk_score", createFormViewData(dbRiskScore,"Calculation",metaData("concept", "diabetes_risk_score", "")));

            // Appointment data
            String appointmentDate = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "referral_appointment_date");
            formData.put("referral_appointment_date", createFormViewData(String.valueOf(convertDateToLong(appointmentDate)),"Calculation",metaData("concept", "referral_appointment_date", "")));

            formData.put("referral_status", createFormViewData("PENDING", "Calculation", null));
            String isEmergencyCase = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "is_emergency_case");
            if (isEmergencyCase != null && !isEmergencyCase.isEmpty()) {
                formData.put("is_emergency_case", createFormViewData(isEmergencyCase, null, metaData("concept", "is_emergency_case", "")));
            }
            formData.put("chw_referral_service", createFormViewData("Diabetes And Hypertension Screening", null, null));
            formData.put("referral_date", createFormViewData(System.currentTimeMillis(), "Calculation",null));
            formData.put("referral_type", createFormViewData("community_to_facility_referral","Calculation",null));
            formData.put("referral_time", createFormViewData(new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH).format(System.currentTimeMillis()),"Calculation",null));

            // Treatment supporter / caregiver obs (only when captured)
            TreatmentSupporterFormUtil.addScreeningReferralObs(new JSONObject(jsonForm), formData);
            return formData;
        } catch (Exception e) {
            Timber.e(e);
        }
        return new HashMap<>();
    }
    private NFormViewData createFormViewData(Object value, String type, HashMap<String, Object> metaData) {
        NFormViewData data = new NFormViewData();
        data.setValue(value);
        data.setType(type);
        data.setVisible(true);
        data.setMetadata(metaData);
        return data;
    }
    private HashMap<String, Object> metaData(String openmrs_entity,String openmrs_entity_id, String openmrs_entity_parent) {
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("openmrs_entity", openmrs_entity);
        metadata.put("openmrs_entity_id", openmrs_entity_id);
        metadata.put("openmrs_entity_parent", openmrs_entity_parent);
        return metadata;
    }

    private static class HouseholdsAdapter extends BaseAdapter {
        private final List<FamilyDetailsModel> data;
        private final LayoutInflater inflater;
        private final Context context;

        HouseholdsAdapter(Context context, List<FamilyDetailsModel> data) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.data = data != null ? data : Collections.emptyList();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(org.smartregister.chw.R.layout.item_household_row, parent, false);
                holder = new ViewHolder();
                holder.title = convertView.findViewById(org.smartregister.chw.R.id.tv_title);
                holder.subtitle = convertView.findViewById(org.smartregister.chw.R.id.tv_subtitle);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FamilyDetailsModel item = data.get(position);
            String name = item != null ? item.getFamilyName() : "";
            String village = item != null ? item.getVillageTown() : "";

            holder.title.setText(!TextUtils.isEmpty(name) ? name : context.getString(org.smartregister.chw.R.string.family_profile_title, ""));
            holder.subtitle.setText(village);
            convertView.setContentDescription(name + ", " + village);
            return convertView;
        }

        static class ViewHolder {
            TextView title;
            TextView subtitle;
        }
    }
}
