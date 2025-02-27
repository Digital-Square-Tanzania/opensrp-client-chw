package org.smartregister.chw.activity;

import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;
import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;
import static org.smartregister.chw.util.Utils.getClientGender;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.agyw.dao.AGYWDao;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreFamilyOtherMemberProfileActivity;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.FamilyMemberFloatingMenu;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.fragment.FamilyOtherMemberProfileFragment;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.chw.sbc.dao.SbcDao;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.adapter.ViewPagerAdapter;
import org.smartregister.family.fragment.BaseFamilyOtherMemberProfileFragment;
import org.smartregister.family.model.BaseFamilyOtherMemberProfileActivityModel;
import org.smartregister.family.util.DBConstants;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.view.contract.BaseProfileContract;

import timber.log.Timber;

public class FamilyOtherMemberProfileActivity extends CoreFamilyOtherMemberProfileActivity {
    private FamilyMemberFloatingMenu familyFloatingMenu;
    private Flavor flavor = new FamilyOtherMemberProfileActivityFlv();

    @Override
    protected void onCreation() {
        super.onCreation();
        setIndependentClient(false);
        updateToolbarTitle(this, R.id.toolbar_title, familyName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        menu.findItem(R.id.action_location_info).setVisible(true);
        menu.findItem(R.id.action_tb_registration).setVisible(false);
        menu.findItem(R.id.action_sick_child_follow_up).setVisible(false);
        menu.findItem(R.id.action_malaria_diagnosis).setVisible(false);
        menu.findItem(R.id.action_remove_member).setVisible(false);


        AllSharedPreferences allSharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
        SharedPreferences preferences = allSharedPreferences.getPreferences();
        String teamRoleIdentifier = "";
        if (preferences != null) {
            teamRoleIdentifier = preferences.getString(TEAM_ROLE_IDENTIFIER, "");
        }

        if (!teamRoleIdentifier.isEmpty()) {
            switch (teamRoleIdentifier) {
                case "cbhs_provider":
                    if (!HivDao.isRegisteredForHiv(baseEntityId)) {
                        menu.findItem(R.id.action_cbhs_registration).setVisible(true);
                    }
                    break;
                case "iccm_provider":
                    if (!IccmDao.isRegisteredForIccm(baseEntityId)) {
                        menu.findItem(R.id.action_iccm_registration).setVisible(true);
                    }
                    menu.findItem(R.id.action_anc_registration).setVisible(false);
                    menu.findItem(R.id.action_cbhs_registration).setVisible(false);
                    menu.findItem(R.id.action_pregnancy_out_come).setVisible(false);
                    break;
                default:
                    if (!ChwApplication.getApplicationFlavor().hasHIV()) {
                        menu.findItem(R.id.action_cbhs_registration).setVisible(false);
                    } else {
                        flavor.updateHivMenuItems(baseEntityId, menu);
                    }
                    if (ChwApplication.getApplicationFlavor().hasFamilyPlanning() && flavor.isOfReproductiveAge(commonPersonObject, gender)) {
                        flavor.updateFpMenuItems(baseEntityId, menu);
                    } else {
                        menu.findItem(R.id.action_fp_initiation).setVisible(false);
                    }

                    menu.findItem(R.id.action_anc_registration).setVisible(ChwApplication.getApplicationFlavor().hasANC() && !presenter().isWomanAlreadyRegisteredOnAnc(commonPersonObject) && flavor.isOfReproductiveAge(commonPersonObject, "Female") && gender.equalsIgnoreCase("Female"));
                    menu.findItem(R.id.action_pregnancy_out_come).setVisible(ChwApplication.getApplicationFlavor().hasANC() && flavor.isOfReproductiveAge(commonPersonObject, "Female") && gender.equalsIgnoreCase("Female"));
                    if (ChwApplication.getApplicationFlavor().hasMalaria())
                        flavor.updateMalariaMenuItems(baseEntityId, menu);
                    else {
                        menu.findItem(R.id.action_malaria_registration).setVisible(false);
                    }

                    if (ChwApplication.getApplicationFlavor().hasHIVST()) {
                        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
                        int age = Utils.getAgeFromDate(dob);
                        menu.findItem(R.id.action_hivst_registration).setVisible(!HivstDao.isRegisteredForHivst(baseEntityId) && age >= 15);
                    }

                    if (ChwApplication.getApplicationFlavor().hasAGYW()) {
                        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
                        int age = Utils.getAgeFromDate(dob);
                        if (gender.equalsIgnoreCase("Female") && age >= 10 && age <= 24 && !AGYWDao.isRegisteredForAgyw(baseEntityId)) {
                            menu.findItem(R.id.action_agyw_screening).setVisible(true);
                        }
                    }

                    if (ChwApplication.getApplicationFlavor().hasKvp()) {
                        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
                        int age = Utils.getAgeFromDate(dob);
                        menu.findItem(R.id.action_kvp_prep_registration).setVisible(!KvpDao.isRegisteredForKvpPrEP(baseEntityId) && age >= 15);
                    }

                    if (ChwApplication.getApplicationFlavor().hasSbc()) {
                        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
                        int age = Utils.getAgeFromDate(dob);
                        menu.findItem(R.id.action_sbc_registration).setVisible(!SbcDao.isRegisteredForSbc(baseEntityId) && age >= 10);
                    }
                    break;
            }
        } else {
            if (!ChwApplication.getApplicationFlavor().hasHIV()) {
                menu.findItem(R.id.action_cbhs_registration).setVisible(false);
            } else {
                flavor.updateHivMenuItems(baseEntityId, menu);
            }
            if (ChwApplication.getApplicationFlavor().hasFamilyPlanning() && flavor.isOfReproductiveAge(commonPersonObject, gender)) {
                flavor.updateFpMenuItems(baseEntityId, menu);
            } else {
                menu.findItem(R.id.action_fp_initiation).setVisible(false);
            }

            menu.findItem(R.id.action_anc_registration).setVisible(ChwApplication.getApplicationFlavor().hasANC() && !presenter().isWomanAlreadyRegisteredOnAnc(commonPersonObject) && flavor.isOfReproductiveAge(commonPersonObject, "Female") && gender.equalsIgnoreCase("Female"));
            menu.findItem(R.id.action_pregnancy_out_come).setVisible(ChwApplication.getApplicationFlavor().hasANC() && flavor.isOfReproductiveAge(commonPersonObject, "Female") && gender.equalsIgnoreCase("Female"));
            if (ChwApplication.getApplicationFlavor().hasMalaria())
                flavor.updateMalariaMenuItems(baseEntityId, menu);
            else {
                menu.findItem(R.id.action_malaria_registration).setVisible(false);
            }

            if (ChwApplication.getApplicationFlavor().hasHIVST()) {
                String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
                int age = Utils.getAgeFromDate(dob);
                menu.findItem(R.id.action_hivst_registration).setVisible(!HivstDao.isRegisteredForHivst(baseEntityId) && age >= 15);
            }

            if (ChwApplication.getApplicationFlavor().hasAGYW()) {
                String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
                int age = Utils.getAgeFromDate(dob);
                if (gender.equalsIgnoreCase("Female") && age >= 10 && age <= 24 && !AGYWDao.isRegisteredForAgyw(baseEntityId)) {
                    menu.findItem(R.id.action_agyw_screening).setVisible(true);
                }
            }

            if (ChwApplication.getApplicationFlavor().hasKvp()) {
                String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
                int age = Utils.getAgeFromDate(dob);
                menu.findItem(R.id.action_kvp_prep_registration).setVisible(!KvpDao.isRegisteredForKvpPrEP(baseEntityId) && age >= 15);
            }

            if (ChwApplication.getApplicationFlavor().hasSbc()) {
                String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
                int age = Utils.getAgeFromDate(dob);
                menu.findItem(R.id.action_sbc_registration).setVisible(!SbcDao.isRegisteredForSbc(baseEntityId) && age >= 10);
            }
        }
        return true;
    }

    @Override
    public FamilyOtherMemberActivityPresenter presenter() {
        return (FamilyOtherMemberActivityPresenter) presenter;
    }

    @Override
    protected void startAncRegister() {
        AncRegisterActivity.startAncRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, PhoneNumber,
                Constants.JSON_FORM.getAncRegistration(), null, familyBaseEntityId, familyName);
    }

    @Override
    protected void startPncRegister() {
        PncRegisterActivity.startPncRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, PhoneNumber,
                CoreConstants.JSON_FORM.getPregnancyOutcome(), null, familyBaseEntityId, familyName, null);
    }

    @Override
    protected void startMalariaRegister() {
        MalariaRegisterActivity.startMalariaRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, familyBaseEntityId);
    }

    @Override
    protected void startVmmcRegister() {
        // Not required
    }

    @Override
    protected void startIntegratedCommunityCaseManagementEnrollment() {
        IccmRegisterActivity.startIccmRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, familyBaseEntityId);
    }

    @Override
    protected void startFpRegister() {
        String dob = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        String gender = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);

        FpRegisterActivity.startFpRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, CoreConstants.JSON_FORM.getFpRegistrationForm(gender));
    }

    @Override
    protected void startFpEcpScreening() {
        //DO Nothing. Not Required in CHW
    }

    @Override
    protected void removeIndividualProfile() {
        IndividualProfileRemoveActivity.startIndividualProfileActivity(FamilyOtherMemberProfileActivity.this,
                commonPersonObject, familyBaseEntityId, familyHead, primaryCaregiver, FamilyRegisterActivity.class.getCanonicalName());
    }

    @Override
    protected void startHivRegister() {
        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);

        try {
            String formName = Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(FamilyOtherMemberProfileActivity.this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void startTbRegister() {
        try {
            TbRegisterActivity.startTbFormActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, Constants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, Constants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void startEditMemberJsonForm(Integer title_resource, CommonPersonObjectClient client) {
        String titleString = title_resource != null ? getResources().getString(title_resource) : null;
        boolean isPrimaryCareGiver = commonPersonObject.getCaseId().equalsIgnoreCase(primaryCaregiver);
        String eventName = Utils.metadata().familyMemberRegister.updateEventType;
        String everSchool = client.getColumnmaps().get(CoreConstants.JsonAssets.FAMILY_MEMBER.EVER_SCHOOL);
        String schoolLevel = client.getColumnmaps().get(CoreConstants.JsonAssets.FAMILY_MEMBER.SCHOOL_LEVEL);

        String uniqueID = commonPersonObject.getColumnmaps().get(DBConstants.KEY.UNIQUE_ID);

        NativeFormsDataBinder binder = new NativeFormsDataBinder(getContext(), client.getCaseId());
        binder.setDataLoader(new FamilyMemberDataLoader(familyName, isPrimaryCareGiver, everSchool, schoolLevel, titleString, eventName, uniqueID));
        JSONObject jsonObject = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getFamilyMemberRegister());

        try {
            if (jsonObject != null)
                startFormActivity(jsonObject);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected BaseProfileContract.Presenter getFamilyOtherMemberActivityPresenter(
            String familyBaseEntityId, String baseEntityId, String familyHead, String primaryCaregiver, String villageTown, String familyName) {
        return new FamilyOtherMemberActivityPresenter(this, new BaseFamilyOtherMemberProfileActivityModel(),
                null, familyBaseEntityId, baseEntityId, familyHead, primaryCaregiver, villageTown, familyName);
    }

    @Override
    protected FamilyMemberFloatingMenu getFamilyMemberFloatingMenu() {
        if (familyFloatingMenu == null) {
            familyFloatingMenu = new FamilyMemberFloatingMenu(this);
        }
        return familyFloatingMenu;
    }

    @Override
    protected Context getFamilyOtherMemberProfileActivity() {
        return FamilyOtherMemberProfileActivity.this;
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivity() {
        return FamilyProfileActivity.class;
    }

    @Override
    protected void initializePresenter() {
        super.initializePresenter();
        onClickFloatingMenu = flavor.getOnClickFloatingMenu(this, familyBaseEntityId, baseEntityId);
    }

    @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        BaseFamilyOtherMemberProfileFragment profileOtherMemberFragment = FamilyOtherMemberProfileFragment.newInstance(this.getIntent().getExtras());
        adapter.addFragment(profileOtherMemberFragment, "");

        viewPager.setAdapter(adapter);

        return viewPager;
    }

    @Override
    protected BaseFamilyOtherMemberProfileFragment getFamilyOtherMemberProfileFragment() {
        return FamilyOtherMemberProfileFragment.newInstance(getIntent().getExtras());
    }

    @Override
    protected void startMalariaFollowUpVisit() {
        MalariaFollowUpVisitActivity.startMalariaFollowUpActivity(this, baseEntityId);
    }

    @Override
    protected void setIndependentClient(boolean isIndependentClient) {
        super.isIndependent = isIndependentClient;
    }

    @Override
    protected void startHfMalariaFollowupForm() {
        //Implements from super
    }

    @Override
    protected void startPmtctRegisration() {
        //do nothing - implementation in hf
    }

    @Override
    protected void startLDRegistration() {
        //do nothing - implementation in hf
    }

    @Override
    protected void startHivstRegistration() {
        String gender = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        HivstRegisterActivity.startHivstRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, gender);
    }

    @Override
    protected void startAgywScreening() {
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);
        AgywRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId, age);
    }

    @Override
    protected void startSbcRegistration() {
        SbcRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startGbvRegistration() {
        //TOBE Implementented
    }

    @Override
    protected void startKvpPrEPRegistration() {
        String gender = getClientGender(baseEntityId);
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);
        KvpPrEPRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId, gender, age);
    }

    @Override
    protected void startKvpRegistration() {
        //do nothing
    }

    @Override
    protected void startPrEPRegistration() {
        //do nothing
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        delayInvalidateOptionsMenu();
    }

    private void delayInvalidateOptionsMenu() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(this::invalidateOptionsMenu, 2000);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    /**
     * build implementation differences file
     */
    public interface Flavor {
        OnClickFloatingMenu getOnClickFloatingMenu(final Activity activity, final String familyBaseEntityId, final String baseEntityId);

        boolean isOfReproductiveAge(CommonPersonObjectClient commonPersonObject, String gender);

        void updateFpMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);

        void updateMalariaMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);

        void updateMaleFpMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);

        void updateHivMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);

        void updateTbMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);
    }
}
