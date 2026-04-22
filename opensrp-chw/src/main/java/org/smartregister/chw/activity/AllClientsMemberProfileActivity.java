package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.chw.util.Utils.getClientGender;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;
import static org.smartregister.family.util.JsonFormUtils.fields;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;
import androidx.annotation.NonNull;

import com.nerdstone.neatformcore.domain.model.NFormViewData;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreAllClientsMemberProfileActivity;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.contract.CoreAllClientsMemberContract;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.fragment.FamilyCallDialogFragment;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.FamilyMemberFloatingMenu;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.fragment.FamilyOtherMemberProfileFragment;
import org.smartregister.chw.interactor.IssueReferralInteractor;
import org.smartregister.chw.presenter.AllClientsMemberPresenter;
import org.smartregister.chw.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.chw.referral.contract.BaseIssueReferralContract;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.JsonFormUtilsFlv;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.adapter.ViewPagerAdapter;
import org.smartregister.family.fragment.BaseFamilyOtherMemberProfileFragment;
import org.smartregister.family.model.BaseFamilyOtherMemberProfileActivityModel;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.view.contract.BaseProfileContract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class AllClientsMemberProfileActivity extends CoreAllClientsMemberProfileActivity {

    private final FamilyOtherMemberProfileActivity.Flavor flavor = new FamilyOtherMemberProfileActivityFlv();
    private FamilyMemberFloatingMenu familyFloatingMenu;
    private CoreAllClientsMemberContract.Presenter allClientsMemberPresenter;
    private LinearLayout layoutRecordNcdScreening;

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
        AncRegisterActivity.startAncRegistrationActivity(AllClientsMemberProfileActivity.this, baseEntityId, PhoneNumber,
                Constants.JSON_FORM.getAncRegistration(), null, familyBaseEntityId, familyName);
    }

    @Override
    protected void startPncRegister() {
        PncRegisterActivity.startPncRegistrationActivity(AllClientsMemberProfileActivity.this, baseEntityId, PhoneNumber,
                CoreConstants.JSON_FORM.getPregnancyOutcome(), null, familyBaseEntityId, familyName, null);
    }

    @Override
    protected void startMalariaRegister() {
        MalariaRegisterActivity.startMalariaRegistrationActivity(AllClientsMemberProfileActivity.this, baseEntityId, familyBaseEntityId);
    }

    @Override
    protected void startVmmcRegister() {
        //implement
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        configureNcdScreeningCta();
    }

    private void configureNcdScreeningCta() {
        if (commonPersonObject == null) {
            return;
        }

        layoutRecordNcdScreening = findViewById(R.id.record_visit_panel_container);
        if (layoutRecordNcdScreening == null) {
            return;
        }

        int age = Utils.getAgeFromDate(Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false));
        if (age >= 30) {
            layoutRecordNcdScreening.setVisibility(View.VISIBLE);
            layoutRecordNcdScreening.setOnClickListener(v -> startDiabetesRiskAssessment());
        } else {
            layoutRecordNcdScreening.setOnClickListener(null);
        }
    }

    @Override
    protected void startIntegratedCommunityCaseManagementEnrollment() {
        IccmRegisterActivity.startIccmRegistrationActivity(AllClientsMemberProfileActivity.this, baseEntityId, familyBaseEntityId);
    }

    @Override
    protected void startDiabetesRiskAssessment() {
        try {
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(
                    AllClientsMemberProfileActivity.this,
                    Constants.JsonForm.getDiabetesScreeningForm()
            );
            prepopulateDiabetesScreeningForm(formJsonObject);
            startNcdFormActivity(formJsonObject);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void prepopulateDiabetesScreeningForm(JSONObject formJsonObject) throws JSONException {
        if (formJsonObject == null || commonPersonObject == null) {
            return;
        }

        int age = Utils.getAgeFromDate(Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false));
        Map<String, String> facilityOptions = LocationUtils.INSTANCE.getFacilitiesKeyAndName();

        JSONArray step3Fields = fields(formJsonObject, "step3");
        JSONObject ageField = getFieldJSONObject(step3Fields, "age");
        if (ageField != null) {
            ageField.put("value", age);
        }
        formJsonObject.getJSONObject(JsonFormConstants.GLOBAL).put("age", age);

        JsonFormUtilsFlv.overwriteQuestionOptions("chw_referral_hf", facilityOptions, formJsonObject);
    }

    private void startNcdFormActivity(JSONObject jsonForm) {
        if (jsonForm == null) {
            return;
        }

        Form form = new Form();
        form.setName(getString(R.string.diabetes_and_hypertension_screening_form_title));
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
    protected void startHivRegister() {
        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);

        try {
            String formName = Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(AllClientsMemberProfileActivity.this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(AllClientsMemberProfileActivity.this, baseEntityId, formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void startTbRegister() {
        try {
            TbRegisterActivity.startTbFormActivity(AllClientsMemberProfileActivity.this, baseEntityId, Constants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, Constants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void startFpRegister() {
        String dob = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        String gender = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);

        FpRegisterActivity.startFpRegistrationActivity(this, baseEntityId, CoreConstants.JSON_FORM.getFpRegistrationForm(gender));
    }

    @Override
    protected void startFpEcpScreening() {
        //NOT Required in CHW
    }

    @Override
    protected void startHivstRegistration() {
        String gender = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        HivstRegisterActivity.startHivstRegistrationActivity(AllClientsMemberProfileActivity.this, baseEntityId, gender);
    }

    @Override
    protected void startAgywScreening() {
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);
        AgywRegisterActivity.startRegistration(AllClientsMemberProfileActivity.this, baseEntityId, age);
    }

    @Override
    protected void startSbcRegistration() {
        SbcRegisterActivity.startRegistration(AllClientsMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startGbvRegistration() {
        //Implement
    }

    @Override
    protected void startCancerPreventiveServicesRegistration() {
        CecapRegisterActivity.startRegistration(AllClientsMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startAsrhRegistration() {
        AsrhRegisterActivity.startRegistration(AllClientsMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startHtsScreening() {
        //NOT Required in WAJA
    }

    @Override
    protected void startHpsEnrollment() {
        HpsRegisterActivity.startRegistration(AllClientsMemberProfileActivity.this, baseEntityId, org.smartregister.chw.hps.util.Constants.FORMS.HPS_CLIENT_ENROLLMENT, null);
    }

    @Override
    protected void startKvpPrEPRegistration() {
        String gender = getClientGender(baseEntityId);
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);
        KvpPrEPRegisterActivity.startRegistration(AllClientsMemberProfileActivity.this, baseEntityId, gender, age);
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
    protected void removeIndividualProfile() {
        IndividualProfileRemoveActivity.startIndividualProfileActivity(AllClientsMemberProfileActivity.this,
                commonPersonObject, familyBaseEntityId, familyHead, primaryCaregiver, AllClientsRegisterActivity.class.getCanonicalName());
    }

    @Override
    protected void startEditMemberJsonForm(Integer title_resource, CommonPersonObjectClient client) {
        String titleString = title_resource != null ? getResources().getString(title_resource) : null;
        CommonPersonObjectClient commonPersonObjectClient = getFamilyRegistrationDetails();
        String uniqueID = commonPersonObjectClient.getColumnmaps().get(DBConstants.KEY.UNIQUE_ID);
        boolean isPrimaryCareGiver = commonPersonObject.getCaseId().equalsIgnoreCase(primaryCaregiver);

        NativeFormsDataBinder binder = new NativeFormsDataBinder(getContext(), commonPersonObject.getCaseId());
        binder.setDataLoader(new FamilyMemberDataLoader(familyName, isPrimaryCareGiver, titleString,
                Utils.metadata().familyMemberRegister.updateEventType, uniqueID));
        JSONObject jsonObject = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());

        try {
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
        return AllClientsMemberProfileActivity.this;
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivity() {
        return FamilyProfileActivity.class;
    }

    @Override
    protected void initializePresenter() {
        super.initializePresenter();
        onClickFloatingMenu = this;
        allClientsMemberPresenter = new AllClientsMemberPresenter(this, baseEntityId);
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
    protected void setIndependentClient(boolean isIndependentClient) {
        super.isIndependent = isIndependentClient;
    }

    @Override
    public void onClickMenu(int viewId) {
        switch (viewId) {
            case R.id.call_layout:
                FamilyCallDialogFragment.launchDialog(this, familyBaseEntityId);
                break;
            case R.id.refer_to_facility_layout:
                Utils.launchClientReferralActivity(this, Utils.getCommonReferralTypes(this, baseEntityId), baseEntityId);
                break;
            default:
                break;
        }
    }

    @Override
    public CoreAllClientsMemberContract.Presenter getAllClientsMemberPresenter() {
        return allClientsMemberPresenter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            try {
                String jsonForm = data.getStringExtra(org.smartregister.family.util.Constants.INTENT_KEY.JSON);
                if (jsonForm != null) {
                    JSONObject jsonObject = JsonFormUtils.getFieldJSONObject(fields(new JSONObject(jsonForm)), "db_save_n_refer");
                    if (jsonObject != null && Boolean.parseBoolean(jsonObject.optString("value"))) {
                        sendNCDReferralToFacility(createReferralForm(data));
                    }
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        delayInvalidateOptionsMenu();
    }

    private void sendNCDReferralToFacility(HashMap<String, NFormViewData> data) {
        try {
            JSONObject ncdForm = new FormUtils().getFormJsonFromRepositoryOrAssets(AllClientsMemberProfileActivity.this, "referrals/referral_form");
            if (ncdForm == null) {
                return;
            }

            ncdForm.put("referral_task_focus", "Diabetes and Hypertension Testing");

            BaseIssueReferralContract.InteractorCallBack interactorCallback = new BaseIssueReferralContract.InteractorCallBack() {
                @Override
                public void onUniqueIdFetched(@NonNull Triple<String, String, String> triple, @NonNull String entityId) {
                    // Uses existing entity id
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
                    if (isSaved) {
                        Toast.makeText(AllClientsMemberProfileActivity.this, R.string.referral_submitted, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(AllClientsMemberProfileActivity.this, R.string.referral_not_submitted, Toast.LENGTH_LONG).show();
                    }
                }
            };

            new IssueReferralInteractor().saveRegistration(baseEntityId, data, ncdForm, interactorCallback, false);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private HashMap<String, NFormViewData> createReferralForm(Intent data) {
        HashMap<String, NFormViewData> formData = new HashMap<>();
        try {
            String jsonForm = data.getStringExtra(org.smartregister.family.util.Constants.INTENT_KEY.JSON);
            if (jsonForm == null) {
                return formData;
            }

            HashMap<String, NFormViewData> problemValue = new HashMap<>();
            NFormViewData dbRisk = createFormViewData("Risk for diabetes and hypertension", null, metaData("", "risk_for_diabetes_and_hypertension", ""));
            problemValue.put("risk_for_diabetes_and_hypertension", dbRisk);
            NFormViewData problemFormViewData = createFormViewData(problemValue, "MultiChoiceCheckBox", metaData("", "concept", "problem"));
            problemFormViewData.setType("MultiChoiceCheckBox");
            formData.put("problem", problemFormViewData);

            JSONObject chwReferralHf = JsonFormUtils.getFieldJSONObject(fields(new JSONObject(jsonForm)), "chw_referral_hf");
            String facilityValue = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "chw_referral_hf");
            String facilityText = "";
            if (chwReferralHf != null) {
                JSONArray options = chwReferralHf.optJSONArray("options");
                if (options != null) {
                    for (int i = 0; i < options.length(); i++) {
                        JSONObject option = options.optJSONObject(i);
                        if (option != null && facilityValue.equals(option.optString("key"))) {
                            facilityText = option.optString("text");
                            break;
                        }
                    }
                }
            }
            NFormViewData chwReferralValue = createFormViewData(facilityText, null, metaData("location_uuid", facilityValue, ""));
            formData.put("chw_referral_hf", createFormViewData(chwReferralValue, "SpinnerNFormView", metaData("concept", "chw_referral_hf", "")));

            String serviceBeforeReferral = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "service_before_referral");
            if (serviceBeforeReferral != null) {
                JSONArray serviceArray = new JSONArray(serviceBeforeReferral);
                HashMap<String, NFormViewData> serviceValues = new HashMap<>();
                for (int i = 0; i < serviceArray.length(); i++) {
                    String serviceValue = serviceArray.getString(i);
                    serviceValues.put(serviceValue, createFormViewData(serviceValue, null, metaData("", serviceValue, "")));
                }
                formData.put("service_before_referral", createFormViewData(serviceValues, "MultiChoiceCheckBox", metaData("concept", "service_before_referral", "")));
            }

            String dbRiskScore = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "diabetes_risk_score_output");
            formData.put("diabetes_risk_score", createFormViewData(dbRiskScore, "Calculation", null));

            String appointmentDate = org.smartregister.chw.util.JsonFormUtils.getValue(new JSONObject(jsonForm), "referral_appointment_date");
            formData.put("referral_appointment_date", createFormViewData(String.valueOf(convertDateToLong(appointmentDate)), "Calculation", metaData("concept", "referral_appointment_date", "")));

            formData.put("referral_status", createFormViewData("PENDING", "Calculation", null));
            formData.put("chw_referral_service", createFormViewData("Diabetes & Hypertension Screening", null, null));
            formData.put("referral_date", createFormViewData(System.currentTimeMillis(), "Calculation", null));
            formData.put("referral_type", createFormViewData("community_to_facility_referral", "Calculation", null));
            formData.put("referral_time", createFormViewData(new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH).format(System.currentTimeMillis()), "Calculation", null));
        } catch (Exception e) {
            Timber.e(e);
        }
        return formData;
    }

    public static Long convertDateToLong(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            Date formattedDate = dateFormat.parse(date);
            return formattedDate != null ? formattedDate.getTime() : null;
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private HashMap<String, Object> metaData(String openmrsEntity, String openmrsEntityId, String openmrsEntityParent) {
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("openmrs_entity", openmrsEntity);
        metadata.put("openmrs_entity_id", openmrsEntityId);
        metadata.put("openmrs_entity_parent", openmrsEntityParent);
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

    private void delayInvalidateOptionsMenu() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(this::invalidateOptionsMenu, 2000);
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
