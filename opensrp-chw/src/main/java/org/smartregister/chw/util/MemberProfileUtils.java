package org.smartregister.chw.util;

import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;
import static org.smartregister.family.util.JsonFormUtils.fields;

import android.app.Activity;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.AgywRegisterActivity;
import org.smartregister.chw.activity.AllClientsMemberProfileActivity;
import org.smartregister.chw.activity.AncRegisterActivity;
import org.smartregister.chw.activity.AsrhRegisterActivity;
import org.smartregister.chw.activity.CecapRegisterActivity;
import org.smartregister.chw.activity.FamilyOtherMemberProfileActivity;
import org.smartregister.chw.activity.FpRegisterActivity;
import org.smartregister.chw.activity.HivRegisterActivity;
import org.smartregister.chw.activity.HivstRegisterActivity;
import org.smartregister.chw.activity.IccmRegisterActivity;
import org.smartregister.chw.activity.KvpPrEPRegisterActivity;
import org.smartregister.chw.activity.MalariaRegisterActivity;
import org.smartregister.chw.activity.NcdFormWizardActivity;
import org.smartregister.chw.activity.PncRegisterActivity;
import org.smartregister.chw.activity.SbcRegisterActivity;
import org.smartregister.chw.activity.TbRegisterActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.family.util.DBConstants;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import java.util.Map;

import timber.log.Timber;

public class MemberProfileUtils {
    public static void startAncRegister(Activity activity, String baseEntityId, String phoneNumber, String familyBaseEntityId, String familyName) {
        AncRegisterActivity.startAncRegistrationActivity(activity, baseEntityId, phoneNumber,
                Constants.JSON_FORM.getAncRegistration(), null, familyBaseEntityId, familyName);
    }

    public static void startPncRegister(Activity activity, String baseEntityId, String phoneNumber, String familyBaseEntityId, String familyName) {
        PncRegisterActivity.startPncRegistrationActivity(activity, baseEntityId, phoneNumber,
                CoreConstants.JSON_FORM.getPregnancyOutcome(), null, familyBaseEntityId, familyName, null);
    }

    public static void startMalariaRegister(Activity activity, String baseEntityId, String familyBaseEntityId) {
        MalariaRegisterActivity.startMalariaRegistrationActivity(activity, baseEntityId, familyBaseEntityId);
    }

    public static void startVmmcRegister(Activity activity, String baseEntityId, String phoneNumber, String familyBaseEntityId, String familyName) {
        //implement
    }

    public static void startIntegratedCommunityCaseManagementEnrollment(Activity activity, String baseEntityId, String familyBaseEntityId) {
        IccmRegisterActivity.startIccmRegistrationActivity(activity, baseEntityId, familyBaseEntityId);
    }

    public static void startHivRegister(Activity activity, String baseEntityId, String gender, String dob) {
        int age = Utils.getAgeFromDate(dob);
        try {
            String formName = Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(activity, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(activity, baseEntityId, formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startTbRegister(Activity activity, String baseEntityId) {
        try {
            TbRegisterActivity.startTbFormActivity(activity, baseEntityId, Constants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(activity, Constants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    public static void startFpRegister(Activity activity, String baseEntityId, String gender) {
        FpRegisterActivity.startFpRegistrationActivity(activity, baseEntityId, CoreConstants.JSON_FORM.getFpRegistrationForm(gender));
    }

    public static void startFpEcpScreening(Activity activity) {
        //NOT Required in CHW
    }

    public static void startHivstRegistration(Activity activity, String baseEntityId, String gender) {
        HivstRegisterActivity.startHivstRegistrationActivity(activity, baseEntityId, gender);
    }

    public static void startAgywScreening(Activity activity, String baseEntityId, String dob) {
        int age = Utils.getAgeFromDate(dob);
        AgywRegisterActivity.startRegistration(activity, baseEntityId, age);
    }

    public static void startSbcRegistration(Activity activity, String baseEntityId) {
        SbcRegisterActivity.startRegistration(activity, baseEntityId);
    }

    public static void startKvpPrEPRegistration(Activity activity, String baseEntityId, String gender, String dob) {
        int age = Utils.getAgeFromDate(dob);
        KvpPrEPRegisterActivity.startRegistration(activity, baseEntityId, gender, age);
    }

    public static void startAsrhRegistration(Activity activity, String baseEntityId) {
        AsrhRegisterActivity.startRegistration(activity, baseEntityId);
    }

    public static void startCancerPreventiveServicesRegistration(Activity activity, String baseEntityId) {
        CecapRegisterActivity.startRegistration(activity, baseEntityId);
    }

    public static void startDiabetesRiskAssessment(Activity activity, String baseEntityId, int age) {
        try {
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(
                    activity,
                    Constants.JsonForm.getDiabetesScreeningForm()
            );
            prepopulateDiabetesScreeningForm(formJsonObject, baseEntityId, age);
            assert formJsonObject != null;
            startNcdFormActivity(formJsonObject, activity, baseEntityId);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public static void startNcdFormActivity(JSONObject jsonForm, Activity activity, String baseEntityId) {
        Form form = new Form();
        String formTitle = activity.getString(R.string.diabetes_and_hypertension_screening_form_title);
        form.setName(formTitle);
        form.setActionBarBackground(R.color.family_actionbar);
        form.setNavigationBackground(R.color.family_navigation);
        form.setHomeAsUpIndicator(R.mipmap.ic_cross_white);
        form.setWizard(true);
        form.setHideNextButton(false);
        form.setHidePreviousButton(false);
        form.setSaveLabel("");
        form.setHideSaveLabel(true);

        Intent intent = new Intent(activity, NcdFormWizardActivity.class);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(org.smartregister.family.util.Constants.WizardFormActivity.EnableOnCloseDialog, false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(JsonFormConstants.PERFORM_FORM_TRANSLATION, true);
        activity.startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    private static void prepopulateDiabetesScreeningForm(JSONObject formJsonObject, String baseEntityId, int age) throws JSONException {

        Map<String, String> facilityOptions = LocationUtils.INSTANCE.getFacilitiesKeyAndName();

        // Populate Client age
        JSONArray step3Fields = fields(formJsonObject, "step3");
        JSONObject ageField = getFieldJSONObject(step3Fields, "age");

        formJsonObject.put("entity_id", baseEntityId);

        if (ageField != null) {
            ageField.put("value", age);
        }
        formJsonObject.getJSONObject(JsonFormConstants.GLOBAL).put("age", age);

        // Populate referral facilities
        JsonFormUtilsFlv.overwriteQuestionOptions("chw_referral_hf", facilityOptions, formJsonObject);
    }


}
