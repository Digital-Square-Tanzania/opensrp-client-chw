package org.smartregister.chw.activity;

import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;
import org.smartregister.chw.util.JsonFormUtils;

import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.R;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreAypProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.AllClientsUtils;

import java.util.List;

public class AypInSchoolMemberProfileActivity extends CoreAypProfileActivity implements OnRetrieveNotifications {

    private final NotificationListAdapter notificationListAdapter = new NotificationListAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationAndReferralRecyclerView.setAdapter(notificationListAdapter);
        notificationListAdapter.setOnClickListener(this);
    }

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AypInSchoolMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        refreshMedicalHistory(false);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(),
                memberObject.getBaseEntityId(), this);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        boolean showLastVisit = getLatestFollowUpVisit() != null;
        rlLastVisit.setVisibility(showLastVisit ? View.VISIBLE : View.GONE);
    }

    private Visit getLatestFollowUpVisit() {
        return AypLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.AYP_IN_SCHOOL_FOLLOW_UP_VISIT);
    }

    @Override
    public void startHivstRegistration() {
        // Launch HIVST registration from AYP profile using member gender
        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), memberObject.getGender());
    }

    @Override
    public void continueService() {

    }

    @Override
    public void continueDischarge() {

    }

    @Override
    public void openFollowupVisit() {
        AypInSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void startServiceForm() {
        AypInSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), false);
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

        if (item.getItemId() == R.id.action_tbleprosy_screening) {
            startTbLeprosyScreening();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void openMedicalHistory() {
        AypInSchoolMedicalHistoryActivity.startMe(this, memberObject);
    }

    protected void startTbLeprosyScreening() {
        TbLeprosyRegisterActivity.startRegistration(AypInSchoolMemberProfileActivity.this, memberObject.getBaseEntityId());
    }


    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return AypDao.getInSchoolMember(baseEntityId);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        handleNotificationRowClick(this, view, notificationListAdapter, memberObject.getBaseEntityId());
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

}
