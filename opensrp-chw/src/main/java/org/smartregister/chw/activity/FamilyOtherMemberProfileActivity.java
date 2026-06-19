package org.smartregister.chw.activity;

import static android.view.View.VISIBLE;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;
import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;
import static org.smartregister.chw.util.Utils.getClientGender;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;
import static org.smartregister.family.util.JsonFormUtils.fields;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.nerdstone.neatformcore.domain.model.NFormViewData;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.FormUtils;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreFamilyOtherMemberProfileActivity;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.FamilyMemberFloatingMenu;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.dao.NcdDao;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.fragment.FamilyOtherMemberProfileFragment;
import org.smartregister.chw.interactor.IssueReferralInteractor;
import org.smartregister.chw.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.chw.referral.contract.BaseIssueReferralContract;
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
import org.smartregister.view.customcontrols.CustomFontTextView;
import org.smartregister.view.contract.BaseProfileContract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class FamilyOtherMemberProfileActivity extends CoreFamilyOtherMemberProfileActivity implements OnRetrieveNotifications {
    private FamilyMemberFloatingMenu familyFloatingMenu;
    private Flavor flavor = new FamilyOtherMemberProfileActivityFlv();
    private java.util.List<org.smartregister.chw.model.FamilyDetailsModel> headedFamilies = java.util.Collections.emptyList();
    private final NotificationListAdapter notificationListAdapter = new NotificationListAdapter();

    JSONObject ncdJsonObjectForm = new JSONObject();

    //    Intent data = new Intent(FamilyOtherMemberProfileActivity.this, NcdFormWizardActivity.class);
    Intent data = new Intent();

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
        ChwNotificationUtil.retrieveNotifications(org.smartregister.chw.application.ChwApplication.getApplicationFlavor().hasReferrals(),
                baseEntityId, this);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        setIndependentClient(false);
        updateToolbarTitle(this, R.id.toolbar_title, familyName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        AllClientsUtils.updateOptionsMenu(menu, commonPersonObject);
        try {
            int count = headedFamilies != null ? headedFamilies.size() : 0;

            MenuItem householdsItem = menu.findItem(org.smartregister.chw.R.id.action_view_households);
            if (householdsItem == null) {
                householdsItem = menu.add(Menu.NONE, org.smartregister.chw.R.id.action_view_households, Menu.NONE, "");
            }

            householdsItem.setVisible(count > 0);

            // Inflate a single action view; resource qualifiers swap phone/tablet versions
            householdsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            householdsItem.setActionView(org.smartregister.chw.R.layout.action_households_action);
            View av = householdsItem.getActionView();
            if (av != null) {
                android.widget.TextView tv = av.findViewById(org.smartregister.chw.R.id.tv_households_label);
                if (tv != null) {
                    boolean shortLabel = getResources().getBoolean(org.smartregister.chw.R.bool.use_short_hh_label);
                    tv.setText(getString(shortLabel ? org.smartregister.chw.R.string.hh_with_count : org.smartregister.chw.R.string.household_with_count, count));
                }
                av.setOnClickListener(v -> {
                    if (count <= 0) return;
                    if (count == 1) openFamilyProfile(headedFamilies.get(0)); else handleViewHouseholdsClick();
                });
                String fullTitle = getString(org.smartregister.chw.R.string.view_households_with_count, count);
                av.setContentDescription(fullTitle);
            }
        } catch (Exception e) {
            Timber.e(e);
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
        try {
            headedFamilies = FamilyDao.getFamiliesByHead(baseEntityId);
        } catch (Exception e) {
            Timber.e(e);
            headedFamilies = java.util.Collections.emptyList();
        }
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
    protected void startHtsScreening() {
        //Not required in WAJA
    }

    @Override
    protected void startHpsEnrollment() {
        HpsRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId, org.smartregister.chw.hps.util.Constants.FORMS.HPS_CLIENT_ENROLLMENT, null);
    }

    @Override
    protected void startAypFacilityScreening() {
        // Not required in community build
    }

    @Override
    protected void startAypInSchoolEnrollment() {
        AypInSchoolRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startAypParentalEnrollment() {
        AypParentalRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startTbLeprosyScreening() {
        TbLeprosyRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startHarmReductionAssessment() {
        HarmReductionRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startHarmReductionSoberHouseEnrollment() {
        HarmReductionSoberHouseRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startAypOutSchoolEnrollment() {
        String gender = AllClientsUtils.getClientGender(baseEntityId);
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);
        AypOutSchoolRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId,gender,age);
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
    public void onClick(View view) {
        super.onClick(view);
        handleNotificationRowClick(this, view, notificationListAdapter, baseEntityId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        delayInvalidateOptionsMenu();

        if (resultCode == RESULT_OK){
            try {
                String jsonForm = data.getStringExtra(org.smartregister.family.util.Constants.INTENT_KEY.JSON);
                if (jsonForm == null) return;
                JSONObject jsonObject= JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(new JSONObject(jsonForm)),"db_save_n_refer");
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
            JSONObject NCDForm = new FormUtils().getFormJsonFromRepositoryOrAssets(FamilyOtherMemberProfileActivity.this, "referrals/referral_form");
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
                        Toast.makeText(FamilyOtherMemberProfileActivity.this, R.string.referral_submitted, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(FamilyOtherMemberProfileActivity.this, R.string.referral_not_submitted, Toast.LENGTH_LONG).show();
                    }
                }
            };

            new IssueReferralInteractor().saveRegistration(baseEntityId, data, NCDForm, interactorCallback, false);
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
            String facilityValue = JsonFormUtils.getValue(new JSONObject(jsonForm), "chw_referral_hf");
            JSONArray jsonArray = JsonFormUtils.fields(new JSONObject(jsonForm));
            JSONObject chwReferralHf = JsonFormUtils.getFieldJSONObject(jsonArray, "chw_referral_hf");
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
            String serviceBReferralValue = JsonFormUtils.getValue(new JSONObject(jsonForm), "service_before_referral");
            JSONArray serviceBReferralArray = new JSONArray(serviceBReferralValue);
            HashMap<String, NFormViewData> serviceBReferralNFormValue = new HashMap<>();
            for(int i=0; i < serviceBReferralArray.length(); i++){
                String serviceValue = serviceBReferralArray.getString(i);
                NFormViewData valueItem = createFormViewData( serviceValue, null, metaData("", serviceValue, ""));
                serviceBReferralNFormValue.put(serviceValue, valueItem);
            }
            formData.put("service_before_referral", createFormViewData(serviceBReferralNFormValue, "MultiChoiceCheckBox", metaData("concept", "service_before_referral", "")));

            // Diabetes risk score
            String dbRiskScore = JsonFormUtils.getValue(new JSONObject(jsonForm), "diabetes_risk_score_output");
            formData.put("diabetes_risk_score", createFormViewData(dbRiskScore,"Calculation",null));

            // Appointment data
            String appointmentDate = JsonFormUtils.getValue(new JSONObject(jsonForm), "referral_appointment_date");
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
            return formData;
        } catch (Exception e) {
            Timber.e(e);
        }
        return new HashMap<>();
    }

    public static Long convertDateToLong(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            Date formattedDate = dateFormat.parse(date);
            assert formattedDate != null;
            return formattedDate.getTime();
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private HashMap<String, Object> metaData(String openmrs_entity,String openmrs_entity_id, String openmrs_entity_parent) {
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("openmrs_entity", openmrs_entity);
        metadata.put("openmrs_entity_id", openmrs_entity_id);
        metadata.put("openmrs_entity_parent", openmrs_entity_parent);
        return metadata;
    }

    private NFormViewData createFormViewData(Object value, String type, HashMap<String, Object> metaData) {
        NFormViewData data = new NFormViewData();
        data.setValue(value);
        data.setType(type);
        data.setVisible(true);
        data.setMetadata(metaData);
        return data;
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == org.smartregister.chw.R.id.action_view_households) {
            handleViewHouseholdsClick();
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
        android.view.View dialogView = inflater.inflate(org.smartregister.chw.R.layout.dialog_households_list, null, false);
        ListView listView = dialogView.findViewById(org.smartregister.chw.R.id.list_households);
        HouseholdsAdapter adapter = new HouseholdsAdapter(this, headedFamilies);
        listView.setAdapter(adapter);
        android.widget.TextView btnCancel = dialogView.findViewById(org.smartregister.chw.R.id.btn_cancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
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

    private String getHouseholdDisplay(org.smartregister.chw.model.FamilyDetailsModel family) {
        String name = family != null ? family.getFamilyName() : "";
        String village = family != null ? family.getVillageTown() : "";
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(village)) {
            return name + " • " + village;
        } else if (!TextUtils.isEmpty(name)) {
            return name;
        } else if (!TextUtils.isEmpty(village)) {
            return village;
        } else {
            return getString(org.smartregister.chw.R.string.family_profile_title, "");
        }
    }

    private void openFamilyProfile(org.smartregister.chw.model.FamilyDetailsModel family) {
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

    private static class HouseholdsAdapter extends android.widget.BaseAdapter {
        private final java.util.List<org.smartregister.chw.model.FamilyDetailsModel> data;
        private final android.view.LayoutInflater inflater;
        private final android.content.Context context;

        HouseholdsAdapter(android.content.Context context, java.util.List<org.smartregister.chw.model.FamilyDetailsModel> data) {
            this.context = context;
            this.inflater = android.view.LayoutInflater.from(context);
            this.data = data != null ? data : java.util.Collections.emptyList();
        }

        @Override
        public int getCount() { return data.size(); }

        @Override
        public Object getItem(int position) { return data.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
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

            org.smartregister.chw.model.FamilyDetailsModel item = data.get(position);
            String name = item != null ? item.getFamilyName() : "";
            String village = item != null ? item.getVillageTown() : "";

            holder.title.setText(!android.text.TextUtils.isEmpty(name) ? name : context.getString(org.smartregister.chw.R.string.family_profile_title, ""));
            holder.subtitle.setText(village);
            convertView.setContentDescription(name + ", " + village);
            return convertView;
        }

        static class ViewHolder {
            android.widget.TextView title;
            android.widget.TextView subtitle;
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
