package org.smartregister.chw.activity;

import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreTbProfileActivity;
import org.smartregister.chw.core.activity.CoreTbUpcomingServicesActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.interactor.CoreTbProfileInteractor;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.task.RunnableTask;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.TbFloatingMenu;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.presenter.TbProfilePresenter;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.tb.activity.BaseTbRegistrationFormsActivity;
import org.smartregister.chw.tb.domain.TbMemberObject;
import org.smartregister.chw.tb.util.Constants;
import org.smartregister.chw.tb.util.TbUtil;
import org.smartregister.chw.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.annotations.Nullable;
import timber.log.Timber;

public class TbProfileActivity extends CoreTbProfileActivity
        implements FamilyProfileExtendedContract.PresenterCallBack, OnRetrieveNotifications {

    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private NotificationListAdapter notificationListAdapter = new NotificationListAdapter();
    private Flavor flavor = new TbProfileActivityFlv();

    public static void startTbProfileActivity(Activity activity, TbMemberObject memberObject) {
        Intent intent = new Intent(activity, TbProfileActivity.class);
        intent.putExtra(Constants.ActivityPayload.MEMBER_OBJECT, memberObject);
        activity.startActivity(intent);
    }

    public void startTbFollowupActivity(Activity activity, String baseEntityID) throws JSONException {
        Intent intent = new Intent(activity, BaseTbRegistrationFormsActivity.class);
        intent.putExtra(Constants.ActivityPayload.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ActivityPayload.JSON_FORM, (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, org.smartregister.chw.util.Constants.JSON_FORM.getTbFollowupVisit()).toString());
        intent.putExtra(Constants.ActivityPayload.USE_DEFAULT_NEAT_FORM_LAYOUT, false);

        activity.startActivityForResult(intent, org.smartregister.chw.anc.util.Constants.REQUEST_CODE_HOME_VISIT);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        addTbReferralTypes();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationAndReferralRecyclerView.setAdapter(notificationListAdapter);
        notificationListAdapter.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(),
                getTbMemberObject().getBaseEntityId(), this);
    }

    @Override
    protected void removeMember() {
        IndividualProfileRemoveActivity.startIndividualProfileActivity(TbProfileActivity.this,
                getClientDetailsByBaseEntityID(getTbMemberObject().getBaseEntityId()),
                getTbMemberObject().getFamilyBaseEntityId(), getTbMemberObject().getFamilyHead(),
                getTbMemberObject().getPrimaryCareGiver(), FpRegisterActivity.class.getCanonicalName());
    }

    @Override
    protected void startTbCaseClosure() {
        try {
            TbRegisterActivity.startTbFormActivity(this, getTbMemberObject().getBaseEntityId(), CoreConstants.JSON_FORM.getTbCaseClosure(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, CoreConstants.JSON_FORM.getTbCaseClosure()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        setTbProfilePresenter(new TbProfilePresenter(this, new CoreTbProfileInteractor(this), getTbMemberObject()));
        fetchProfileData();
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(getTbMemberObject().getPhoneNumber())
                || StringUtils.isNotBlank(getTbMemberObject().getPrimaryCareGiverPhoneNumber()));

        ((TbFloatingMenu) getTbFloatingMenu()).redraw(phoneNumberAvailable);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.record_tb_followup_visit) {
            openFollowUpVisitForm(false);
        }
        handleNotificationRowClick(this, view, notificationListAdapter, getTbMemberObject().getBaseEntityId());

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // recompute schedule
        Runnable runnable = () -> ChwScheduleTaskExecutor.getInstance().execute(getTbMemberObject().getBaseEntityId(), Constants.EventType.FOLLOW_UP_VISIT, new Date());
        Utils.startAsyncTask(new RunnableTask(runnable), null);

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CoreConstants.ProfileActivityResults.CHANGE_COMPLETED && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, TbRegisterActivity.class);
            intent.putExtras(getIntent().getExtras());
            startActivity(intent);
            finish();

        }
    }

    @Override
    public void openMedicalHistory() {
        //TODO implement
    }

    @Override
    public void openTbRegistrationForm() {
        try {
            TbRegisterActivity.startTbFormActivity(this, getTbMemberObject().getBaseEntityId(), CoreConstants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, CoreConstants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public void openUpcomingServices() {
        CoreTbUpcomingServicesActivity.startMe(this, TbUtil.toMember(getTbMemberObject()));
    }

    @Override
    public void openFamilyDueServices() {
        Intent intent = new Intent(this, FamilyProfileActivity.class);

        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, getTbMemberObject().getFamilyBaseEntityId());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, getTbMemberObject().getFamilyHead());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, getTbMemberObject().getPrimaryCareGiver());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME, getTbMemberObject().getFamilyName());

        intent.putExtra(CoreConstants.INTENT_KEY.SERVICE_DUE, true);
        startActivity(intent);
    }

    @Override
    public void openFollowUpVisitForm(boolean isEdit) {
        if (!isEdit) {
            try {
                startTbFollowupActivity(this, getTbMemberObject().getBaseEntityId());
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

    private void addTbReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_referral),
                    CoreConstants.JSON_FORM.getTbReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_TB));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral),
                    CoreConstants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void initializeCallFAB() {
        setTbFloatingMenu(new TbFloatingMenu(this, getTbMemberObject()));

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.tb_fab:
                    checkPhoneNumberProvided();
                    ((TbFloatingMenu) getTbFloatingMenu()).animateFAB();
                    break;
                case R.id.call_layout:
                    ((TbFloatingMenu) getTbFloatingMenu()).launchCallWidget();
                    ((TbFloatingMenu) getTbFloatingMenu()).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    ((TbProfilePresenter) getTbProfilePresenter()).referToFacility();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((TbFloatingMenu) getTbFloatingMenu()).setFloatMenuClickListener(onClickFloatingMenu);
        getTbFloatingMenu().setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(getTbFloatingMenu(), linearLayoutParams);
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(org.smartregister.chw.core.R.menu.tb_profile_menu, menu);
        flavor.updateHivMenuItems(getTbMemberObject().getBaseEntityId(), menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            startHivRegister();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void startHivRegister() {
        try {
            String formName = org.smartregister.chw.util.Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");


            int age = Utils.getAgeFromDate(getTbMemberObject().getAge());
            try {
                updateAgeAndGender(fields, age, getTbMemberObject().getGender());
            } catch (Exception e) {
                Timber.e(e);
            }

            HivRegisterActivity.startHIVFormActivity(this, getTbMemberObject().getBaseEntityId(), formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    public interface Flavor {
        void updateHivMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);
    }

}

