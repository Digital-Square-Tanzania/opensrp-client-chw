package org.smartregister.chw.activity;

import static org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.MEMBER_PROFILE_OBJECT;
import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;
import static org.smartregister.chw.util.Constants.MALARIA_REFERRAL_FORM;
import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;
import static org.smartregister.opd.utils.OpdConstants.DateFormat.YYYY_MM_DD;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreChildProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.model.CoreChildProfileModel;
import org.smartregister.chw.core.presenter.CoreChildProfilePresenter;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreConstants.JSON_FORM;
import org.smartregister.chw.custom_view.FamilyMemberFloatingMenu;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.presenter.ChildProfilePresenter;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.util.UtilsFlv;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.Constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChildProfileActivity extends CoreChildProfileActivity implements OnRetrieveNotifications {
    public FamilyMemberFloatingMenu familyFloatingMenu;
    private Flavor flavor = new ChildProfileActivityFlv();
    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private List<ReferralTypeModel> linkageTypeModels = new ArrayList<>();
    private NotificationListAdapter notificationListAdapter = new NotificationListAdapter();

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        initializePresenter();
        onClickFloatingMenu = flavor.getOnClickFloatingMenu(this, (ChildProfilePresenter) presenter);
        setupViews();
        setUpToolbar();
        registerReceiver(mDateTimeChangedReceiver, sIntentFilter);
        if (((ChwApplication) ChwApplication.getInstance()).hasReferrals()) {
            addChildReferralTypes();
        }
        notificationAndReferralRecyclerView.setAdapter(notificationListAdapter);
        notificationListAdapter.setOnClickListener(this);
        //  setVaccineHistoryView(lastVisitDay);
    }

    @Override
    public void setUpToolbar() {
        updateToolbarTitle(this, R.id.toolbar_title, flavor.getToolbarTitleName(memberObject));
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(),
                childBaseEntityId, this);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int i = view.getId();
        if (i == R.id.last_visit_row) {
            openMedicalHistoryScreen();
        } else if (i == R.id.vaccine_history) {
            openMedicalHistoryScreen();
        } else if (i == R.id.most_due_overdue_row) {
            openUpcomingServicePage();
        } else if (i == R.id.view_due_today) {
            openUpcomingServicePage();
        } else if (i == R.id.textview_record_visit || i == R.id.record_visit_done_bar) {
            openVisitHomeScreen(false);
        } else if (i == R.id.family_has_row) {
            openFamilyDueTab();
        } else if (i == R.id.textview_edit) {
            openVisitHomeScreen(true);
        }
        if (i == R.id.textview_visit_not) {
            presenter().updateVisitNotDone(System.currentTimeMillis());
            imageViewCrossChild.setVisibility(View.VISIBLE);
            imageViewCrossChild.setImageResource(R.drawable.activityrow_notvisited);
        } else if (i == R.id.textview_undo) {
            presenter().updateVisitNotDone(0);
        }
        handleNotificationRowClick(this, view, notificationListAdapter, childBaseEntityId);
    }

    @Override
    protected void initializePresenter() {
        String familyName = getIntent().getStringExtra(Constants.INTENT_KEY.FAMILY_NAME);
        if (familyName == null) {
            familyName = "";
        }

        presenter = new ChildProfilePresenter(this, new CoreChildProfileModel(familyName), childBaseEntityId);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        familyFloatingMenu = new FamilyMemberFloatingMenu(this);
        LinearLayout.LayoutParams linearLayoutParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        familyFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        addContentView(familyFloatingMenu, linearLayoutParams);

        familyFloatingMenu.setClickListener(onClickFloatingMenu);
        fetchProfileData();
    }

    @Override
    public void updateHasPhone(boolean hasPhone) {
        if (familyFloatingMenu != null) {
            familyFloatingMenu.reDraw(hasPhone);
        }
    }

    @Override
    public void setProfileImage(String baseEntityId) {
        int defaultImage = org.smartregister.chw.core.R.drawable.rowavatar_child;// gender.equalsIgnoreCase(Gender.MALE.toString()) ? R.drawable.row_boy : R.drawable.row_girl;
        imageViewProfile.setImageResource(defaultImage);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_malaria_registration:
                MalariaRegisterActivity.startMalariaRegistrationActivity(ChildProfileActivity.this, presenter().getChildClient().getCaseId(), ((ChildProfilePresenter) presenter()).getFamilyID());
                return true;
            case R.id.action_remove_member:
                IndividualProfileRemoveActivity.startIndividualProfileActivity(ChildProfileActivity.this, presenter().getChildClient(),
                        ((ChildProfilePresenter) presenter()).getFamilyID()
                        , ((ChildProfilePresenter) presenter()).getFamilyHeadID(), ((ChildProfilePresenter) presenter()).getPrimaryCareGiverID(), ChildRegisterActivity.class.getCanonicalName());

                return true;
            case R.id.action_iccm_registration:
                    startIntegratedCommunityCaseManagementEnrollment();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void startIntegratedCommunityCaseManagementEnrollment() {
        IccmRegisterActivity.startIccmRegistrationActivity(ChildProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.action_sick_child_form).setVisible(false);

        menu.findItem(R.id.action_sick_child_follow_up).setVisible(false);
        menu.findItem(R.id.action_malaria_diagnosis).setVisible(false);
        menu.findItem(R.id.action_malaria_followup_visit).setVisible(false);
        menu.findItem(R.id.action_thinkmd_health_assessment).setVisible(ChwApplication.getApplicationFlavor().useThinkMd()
                && flavor.isChildOverTwoMonths(((CoreChildProfilePresenter) presenter).getChildClient()));
        if (ChwApplication.getApplicationFlavor().hasMalaria())
            UtilsFlv.updateMalariaMenuItems(memberObject.getBaseEntityId(), menu);

        if (ChwApplication.getApplicationFlavor().hasICCM() && !IccmDao.isRegisteredForIccm(memberObject.getBaseEntityId())) {
//            menu.findItem(R.id.action_iccm_registration).setVisible(true);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CoreConstants.ProfileActivityResults.CHANGE_COMPLETED && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(ChildProfileActivity.this, ChildProfileActivity.class);
            intent.putExtras(getIntent().getExtras());
            startActivity(intent);
            finish();
        }
        ChwScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), CoreConstants.EventType.CHILD_HOME_VISIT, new Date());
    }

    private void openMedicalHistoryScreen() {
        ChildMedicalHistoryActivity.startMe(this, memberObject);
    }

    private void openUpcomingServicePage() {
        MemberObject memberObject = new MemberObject(presenter().getChildClient());
        if (!ChwApplication.getApplicationFlavor().hasSurname()) memberObject.setLastName("");
        UpcomingServicesActivity.startMe(this, memberObject);
    }

    private void openVisitHomeScreen(boolean isEditMode) {
        ChildHomeVisitActivity.startMe(this, memberObject, isEditMode, ChildHomeVisitActivity.class);
    }

    private void openFamilyDueTab() {
        Intent intent = new Intent(this, FamilyProfileActivity.class);

        intent.putExtra(Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, ((ChildProfilePresenter) presenter()).getFamilyId());
        intent.putExtra(Constants.INTENT_KEY.FAMILY_HEAD, ((ChildProfilePresenter) presenter()).getFamilyHeadID());
        intent.putExtra(Constants.INTENT_KEY.PRIMARY_CAREGIVER, ((ChildProfilePresenter) presenter()).getPrimaryCareGiverID());
        intent.putExtra(Constants.INTENT_KEY.FAMILY_NAME, ((ChildProfilePresenter) presenter()).getFamilyName());
        intent.putExtra(Constants.INTENT_KEY.BASE_ENTITY_ID, ((ChildProfilePresenter) presenter()).getChildBaseEntityId());

        intent.putExtra(org.smartregister.chw.util.Constants.INTENT_KEY.SERVICE_DUE, true);
        startActivity(intent);
    }

    private void addChildReferralTypes() {

        referralTypeModels.add(new ReferralTypeModel(getString(R.string.sick_child),
                BuildConfig.USE_UNIFIED_REFERRAL_APPROACH ? JSON_FORM.getChildUnifiedReferralForm()
                        : JSON_FORM.getChildReferralForm(), CoreConstants.TASKS_FOCUS.SICK_CHILD));

        if (memberObject.getAge() >= 5) {
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.suspected_malaria),
                    BuildConfig.USE_UNIFIED_REFERRAL_APPROACH ? CoreConstants.JSON_FORM.getMalariaReferralForm()
                            : MALARIA_REFERRAL_FORM, CoreConstants.TASKS_FOCUS.SUSPECTED_MALARIA));
        }

        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.child_gbv_referral),
                    JSON_FORM.getChildGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_CHILD_GBV));
        }

        linkageTypeModels.add(new ReferralTypeModel(getString(R.string.child_minor_ailments),
                BuildConfig.USE_UNIFIED_REFERRAL_APPROACH ? JSON_FORM.getChildUnifiedLinkageForm()
                        : JSON_FORM.getChildReferralForm(), CoreConstants.TASKS_FOCUS.ADDO.CHILD_MINOR_AILMENTS));
    }

    @Override
    protected View.OnClickListener getSickListener() {
        return v -> {
            Intent intent = new Intent(getApplication(), SickFormMedicalHistory.class);
            intent.putExtra(MEMBER_PROFILE_OBJECT, memberObject);
            startActivity(intent);
        };
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    @Override
    public void setServiceNameDue(String serviceName, String dueDate) {
        super.setServiceNameDue(serviceName, flavor.getFormattedDateForVisual(dueDate, YYYY_MM_DD));
    }

    @Override
    public void setServiceNameOverDue(String serviceName, String dueDate) {
        super.setServiceNameOverDue(serviceName, flavor.getFormattedDateForVisual(dueDate, YYYY_MM_DD));
    }

    @Override
    public void setServiceNameUpcoming(String serviceName, String dueDate) {
        super.setServiceNameUpcoming(serviceName, flavor.getFormattedDateForVisual(dueDate, YYYY_MM_DD));
    }

    @Override
    public void setLastVisitRowView(String days) {
        lastVisitDay = days;
        flavor.setLastVisitRowView(lastVisitDay, layoutLastVisitRow, viewLastVisitRow, textViewLastVisit, this);
        flavor.setVaccineHistoryView(lastVisitDay, layoutVaccineHistoryRow, viewVaccineHistoryRow, this);

    }

    @Override
    public void setFamilyHasNothingElseDue() {
        layoutFamilyHasRow.setVisibility(View.VISIBLE);
        viewFamilyRow.setVisibility(View.VISIBLE);
        textViewFamilyHas.setText(getString(R.string.family_has_nothing_else_due));
    }

    public interface Flavor {

        OnClickFloatingMenu getOnClickFloatingMenu(Activity activity, ChildProfilePresenter presenter);

        boolean isChildOverTwoMonths(CommonPersonObjectClient client);

        Intent getSickChildFormActivityIntent(JSONObject jsonObject, Context context);

        String getFormattedDateForVisual(String dueDate, String inputFormat);

        void setLastVisitRowView(String days, RelativeLayout layoutLastVisitRow, View viewLastVisitRow, TextView textViewLastVisit, Context context);

        void setVaccineHistoryView(String days, RelativeLayout layoutVaccineHistoryRow, View viewVaccineHistoryRow, Context context);

        String getToolbarTitleName(MemberObject memberObject);
    }

    public List<ReferralTypeModel> getLinkageTypeModels() {
        return linkageTypeModels;
    }
}
