package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreFamilyPlanningMemberProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.interactor.CoreFamilyPlanningProfileInteractor;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.presenter.CoreFamilyPlanningProfilePresenter;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.FamilyPlanningFloatingMenu;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.fp.domain.FpMemberObject;
import org.smartregister.chw.fp.domain.Visit;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.presenter.FamilyPlanningMemberProfilePresenter;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FPMemberProfileActivity extends CoreFamilyPlanningMemberProfileActivity implements FamilyProfileExtendedContract.PresenterCallBack, OnRetrieveNotifications {

    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private NotificationListAdapter notificationListAdapter = new NotificationListAdapter();

    public static void startFpMemberProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, FPMemberProfileActivity.class);
        passToolbarTitle(activity, intent);
        intent.putExtra(FamilyPlanningConstants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        addFpReferralTypes();
        notificationAndReferralRecyclerView.setAdapter(notificationListAdapter);
        notificationListAdapter.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(), fpMemberObject.getBaseEntityId(), this);

        Visit lastVisit = FpDao.getLatestVisit(fpMemberObject.getBaseEntityId(), FamilyPlanningConstants.EVENT_TYPE.FP_CBD_FOLLOW_UP_VISIT);
        if (lastVisit != null)
            refreshMedicalHistory(true);
    }

    @Override
    protected void removeMember() {
        IndividualProfileRemoveActivity.startIndividualProfileActivity(FPMemberProfileActivity.this, getClientDetailsByBaseEntityID(fpMemberObject.getBaseEntityId()), fpMemberObject.getFamilyBaseEntityId(), fpMemberObject.getFamilyHead(), fpMemberObject.getPrimaryCareGiver(), FpRegisterActivity.class.getCanonicalName());
    }

    @Override
    protected void startFamilyPlanningRegistrationActivity() {
        //FpRegisterActivity.startFpRegistrationActivity(this, fpMemberObject.getBaseEntityId(), fpMemberObject.getAge(), CoreConstants.JSON_FORM.getFpChangeMethodForm(fpMemberObject.getGender()), FamilyPlanningConstants.ACTIVITY_PAYLOAD_TYPE.CHANGE_METHOD_PAYLOAD_TYPE);
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        fpProfilePresenter = new FamilyPlanningMemberProfilePresenter(this, new CoreFamilyPlanningProfileInteractor(), fpMemberObject);
        fpProfilePresenter.refreshProfileBottom();
    }

    @Override
    public void initializeFloatingMenu() {
        FpMemberObject memberObject = FpDao.getMember(fpMemberObject.getBaseEntityId());
        baseFpFloatingMenu = new FamilyPlanningFloatingMenu(this, memberObject);

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.family_planning_fab:
                    checkPhoneNumberProvided();
                    ((FamilyPlanningFloatingMenu) baseFpFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((FamilyPlanningFloatingMenu) baseFpFloatingMenu).launchCallWidget();
                    ((FamilyPlanningFloatingMenu) baseFpFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    ((FamilyPlanningMemberProfilePresenter) fpProfilePresenter).referToFacility();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((FamilyPlanningFloatingMenu) baseFpFloatingMenu).setFloatingMenuOnClickListener(onClickFloatingMenu);
        baseFpFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseFpFloatingMenu, linearLayoutParams);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();

        handleNotificationRowClick(this, view, notificationListAdapter, fpMemberObject.getBaseEntityId());
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(fpMemberObject.getPhoneNumber()) || StringUtils.isNotBlank(fpMemberObject.getFamilyHeadPhoneNumber()));

        ((FamilyPlanningFloatingMenu) baseFpFloatingMenu).redraw(phoneNumberAvailable);
    }

    @Override
    public void setupViews() {
        super.setupViews();
        textViewRecordFp.setText(org.smartregister.chw.fp.R.string.record_fp_followup_visit);
        delayRefreshSetupViews();
    }

    private void delayRefreshSetupViews() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Visit lastVisit = FpDao.getLatestVisit(fpMemberObject.getBaseEntityId(), FamilyPlanningConstants.EVENT_TYPE.FP_CBD_FOLLOW_UP_VISIT);
                if (lastVisit != null) {
                    refreshMedicalHistory(true);
                }
            }, 500);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public void verifyHasPhone() {
        // Implement
    }

    @Override
    public void notifyHasPhone(boolean b) {
        // Implement
    }

    @Override
    public void onEventSaveComplete(boolean b) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON) {
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(CoreConstants.EventType.FAMILY_PLANNING_REFERRAL)) {
                    ((CoreFamilyPlanningProfilePresenter) fpProfilePresenter).createReferralEvent(Utils.getAllSharedPreferences(), jsonString);
                    showToast(this.getString(R.string.referral_submitted));
                }
            } catch (Exception ex) {
                Timber.e(ex);
            }
        }
    }

    @Override
    public Visit getLastVisit() {
        return null;
    }

    @Override
    public boolean isFirstVisit() {
        return false;
    }

    @Override
    public void startPointOfServiceDeliveryForm() {
        //Not Required
    }

    @Override
    public void startFpCounselingForm() {
        //Not Required
    }

    @Override
    public void startFpScreeningForm() {
        //Not Required
    }

    @Override
    public void startProvideFpMethod() {
        //Not Required
    }

    @Override
    public void startProvideOtherServices() {
        //Not Required
    }

    @Override
    public void startFpFollowupVisit() {
        FpCbdFollowupVisitProvisionOfServicesActivity.startMe(this, fpMemberObject.getBaseEntityId(), false);
    }

    private void addFpReferralTypes() {
        referralTypeModels.add(new ReferralTypeModel(getString(R.string.family_planning_referral), BuildConfig.USE_UNIFIED_REFERRAL_APPROACH ? JSON_FORM.getFamilyPlanningUnifiedReferralForm(fpMemberObject.getGender()) : JSON_FORM.getFamilyPlanningReferralForm(fpMemberObject.getGender()), CoreConstants.TASKS_FOCUS.FP_SIDE_EFFECTS));
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.suspected_malaria), JSON_FORM.getMalariaReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_MALARIA));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.hiv_referral), JSON_FORM.getHivReferralForm(), CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_referral), JSON_FORM.getTbReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_TB));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral), JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    @Override
    public void setFollowUpButtonOverdue() {
        showFollowUpVisitButton();
        textViewRecordFp.setBackground(getResources().getDrawable(org.smartregister.chw.fp.R.drawable.record_btn_selector));
    }

    @Override
    public void openMedicalHistory() {
        FpMedicalHistoryActivity.startMe(FPMemberProfileActivity.this, fpMemberObject);
    }


}

