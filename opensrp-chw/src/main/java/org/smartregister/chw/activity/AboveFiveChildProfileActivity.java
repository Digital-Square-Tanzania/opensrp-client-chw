package org.smartregister.chw.activity;

import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;
import static org.smartregister.chw.util.AllClientsUtils.setMenuItemVisibility;
import static org.smartregister.chw.util.Constants.MALARIA_REFERRAL_FORM;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreAboveFiveChildProfileActivity;
import org.smartregister.chw.core.model.CoreChildProfileModel;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.FamilyMemberFloatingMenu;
import org.smartregister.chw.hps.dao.HpsDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.presenter.AboveFiveChildProfilePresenter;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.util.UtilsFlv;
import org.smartregister.family.util.Constants;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AboveFiveChildProfileActivity extends CoreAboveFiveChildProfileActivity {
    public FamilyMemberFloatingMenu familyFloatingMenu;
    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private ChildProfileActivity.Flavor flavor = new ChildProfileActivityFlv();

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.action_malaria_followup_visit).setVisible(false);
        if (ChwApplication.getApplicationFlavor().hasMalaria())
            UtilsFlv.updateMalariaMenuItems(memberObject.getBaseEntityId(), menu);

        AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
        SharedPreferences preferences = allSharedPreferences.getPreferences();
        String teamRoleIdentifier = preferences != null ? preferences.getString(TEAM_ROLE_IDENTIFIER, "") : "";

        int age = memberObject.getAge();
        if (ChwApplication.getApplicationFlavor().hasHps() && teamRoleIdentifier.contains("icchw")) {
            setMenuItemVisibility(menu, R.id.action_hps_enrollment, !HpsDao.isRegisteredForHps(memberObject.getBaseEntityId()) && age >= 10);
        }

        return true;
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        initializePresenter();
        setupViews();
        setUpToolbar();
        registerReceiver(mDateTimeChangedReceiver, sIntentFilter);
        invisibleRecordVisitPanel();
        if (((ChwApplication) ChwApplication.getInstance()).hasReferrals()) {
            addChildReferralTypes();
        }
    }

    protected void startHpsEnrollment() {
        HpsRegisterActivity.startRegistration(AboveFiveChildProfileActivity.this, memberObject.getBaseEntityId(), org.smartregister.chw.hps.util.Constants.FORMS.HPS_CLIENT_ENROLLMENT, null);
    }

    private void invisibleRecordVisitPanel() {
        if (recordVisitPanel != null) {
            recordVisitPanel.setVisibility(View.GONE);
        }
    }

    @Override
    protected void initializePresenter() {
        String familyName = getIntent().getStringExtra(Constants.INTENT_KEY.FAMILY_NAME);
        if (familyName == null) {
            familyName = "";
        }

        presenter = new AboveFiveChildProfilePresenter(this, new CoreChildProfileModel(familyName), childBaseEntityId);
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

        onClickFloatingMenu = flavor.getOnClickFloatingMenu(this, (AboveFiveChildProfilePresenter) presenter);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove_member:
                IndividualProfileRemoveActivity.startIndividualProfileActivity(AboveFiveChildProfileActivity.this, presenter().getChildClient(),
                        ((AboveFiveChildProfilePresenter) presenter()).getFamilyID()
                        , ((AboveFiveChildProfilePresenter) presenter()).getFamilyHeadID(), ((AboveFiveChildProfilePresenter) presenter()).getPrimaryCareGiverID(), ChildRegisterActivity.class.getCanonicalName());

                return true;

            case R.id.action_hps_enrollment:
                startHpsEnrollment();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int i = view.getId();
        if (i == R.id.family_has_row) {
            openFamilyDueTab();
        }
    }

    private void openFamilyDueTab() {
        Intent intent = new Intent(this, FamilyProfileActivity.class);

        intent.putExtra(Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, ((AboveFiveChildProfilePresenter) presenter()).getFamilyId());
        intent.putExtra(Constants.INTENT_KEY.FAMILY_HEAD, ((AboveFiveChildProfilePresenter) presenter()).getFamilyHeadID());
        intent.putExtra(Constants.INTENT_KEY.PRIMARY_CAREGIVER, ((AboveFiveChildProfilePresenter) presenter()).getPrimaryCareGiverID());
        intent.putExtra(Constants.INTENT_KEY.FAMILY_NAME, ((AboveFiveChildProfilePresenter) presenter()).getFamilyName());

        intent.putExtra(org.smartregister.chw.util.Constants.INTENT_KEY.SERVICE_DUE, true);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CoreConstants.ProfileActivityResults.CHANGE_COMPLETED && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(AboveFiveChildProfileActivity.this, ChildProfileActivity.class);
            intent.putExtras(getIntent().getExtras());
            startActivity(intent);
            finish();
        }
        ChwScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), CoreConstants.EventType.CHILD_HOME_VISIT, new Date());
    }

    private void addChildReferralTypes() {
        referralTypeModels.add(new ReferralTypeModel(getString(R.string.sick_child),
                BuildConfig.USE_UNIFIED_REFERRAL_APPROACH ? CoreConstants.JSON_FORM.getChildUnifiedReferralForm()
                        : CoreConstants.JSON_FORM.getChildReferralForm(), CoreConstants.TASKS_FOCUS.SICK_CHILD));

        if (memberObject.getAge() >= 5) {
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.suspected_malaria),
                    BuildConfig.USE_UNIFIED_REFERRAL_APPROACH ? CoreConstants.JSON_FORM.getMalariaReferralForm()
                            : MALARIA_REFERRAL_FORM, CoreConstants.TASKS_FOCUS.SUSPECTED_MALARIA));
        }
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.child_gbv_referral),
                    CoreConstants.JSON_FORM.getChildGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_CHILD_GBV));
        }
    }

}
