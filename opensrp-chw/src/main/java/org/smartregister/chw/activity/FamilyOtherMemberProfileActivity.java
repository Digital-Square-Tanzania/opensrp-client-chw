package org.smartregister.chw.activity;

import static android.view.View.VISIBLE;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;
import static org.smartregister.chw.util.Utils.getClientGender;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;
import static org.smartregister.family.util.JsonFormUtils.fields;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.FormUtils;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreFamilyOtherMemberProfileActivity;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.FamilyMemberFloatingMenu;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.fragment.FamilyOtherMemberProfileFragment;
import org.smartregister.chw.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.chw.util.JsonFormUtilsFlv;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.family.adapter.ViewPagerAdapter;
import org.smartregister.family.fragment.BaseFamilyOtherMemberProfileFragment;
import org.smartregister.family.model.BaseFamilyOtherMemberProfileActivityModel;
import org.smartregister.family.util.DBConstants;
import org.smartregister.view.contract.BaseProfileContract;

import java.util.Map;

import timber.log.Timber;

public class FamilyOtherMemberProfileActivity extends CoreFamilyOtherMemberProfileActivity {
    private FamilyMemberFloatingMenu familyFloatingMenu;
    private LinearLayout layoutRecordNCDScreening;
    private Flavor flavor = new FamilyOtherMemberProfileActivityFlv();

    @Override
    protected void onCreation() {
        super.onCreation();
        setIndependentClient(false);
        updateToolbarTitle(this, R.id.toolbar_title, familyName);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        if (Utils.getAgeFromDate(Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false)) >= 40) {
            this.layoutRecordNCDScreening = findViewById(R.id.record_visit_panel_container);
            this.layoutRecordNCDScreening.setVisibility(VISIBLE);
            this.layoutRecordNCDScreening.setOnClickListener(v -> startDiabetesRiskAssessment());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        AllClientsUtils.updateOptionsMenu(menu, commonPersonObject);
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
    protected void startDiabetesRiskAssessment() {
        try {
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(
                    FamilyOtherMemberProfileActivity.this,
                    Constants.JsonForm.getDiabetesScreeningForm()
            );
            prepopulateDiabetesScreeningForm(formJsonObject);
            assert formJsonObject != null;
            startNcdFormActivity(formJsonObject);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void prepopulateDiabetesScreeningForm(JSONObject formJsonObject) throws JSONException {

        int age = Utils.getAgeFromDate(Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false));
        Map<String, String> facilityOptions = LocationUtils.INSTANCE.getFacilitiesKeyAndName();

        // Populate Client age
        JSONArray step3Fields = fields(formJsonObject, "step3");
        JSONObject ageField = getFieldJSONObject(step3Fields, "age");

        if (ageField != null) {
            ageField.put("value", age);
        }
        formJsonObject.getJSONObject(JsonFormConstants.GLOBAL).put("age", age);

        // Populate referral facilities
        JsonFormUtilsFlv.overwriteQuestionOptions("chw_referral_hf", facilityOptions, formJsonObject);

    }


    public void startNcdFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        String formTitle = getString(R.string.diabetes_and_hypertension_screening_form_title);
        form.setName(formTitle);
        form.setActionBarBackground(R.color.family_actionbar);
        form.setNavigationBackground(R.color.family_navigation);
        form.setHomeAsUpIndicator(R.mipmap.ic_cross_white);
        form.setWizard(true);
        form.setHideNextButton(true);
        form.setHidePreviousButton(true);
        form.setSaveLabel("");
        form.setHideSaveLabel(true);

        Intent intent = new Intent(this, NcdFormWizardActivity.class);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(org.smartregister.family.util.Constants.WizardFormActivity.EnableOnCloseDialog, false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(JsonFormConstants.PERFORM_FORM_TRANSLATION, true);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
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
        //Implement
    }

    @Override
    protected void startCancerPreventiveServicesRegistration() {
        CecapRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startAsrhRegistration() {
        AsrhRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
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
