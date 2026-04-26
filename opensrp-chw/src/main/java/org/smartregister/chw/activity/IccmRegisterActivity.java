package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static org.smartregister.chw.core.utils.CoreConstants.ENTITY_ID;
import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getIccmEnrollment;
import static org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient;
import static org.smartregister.chw.core.utils.Utils.isMemberOfReproductiveAge;
import static org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.ICCM_ENROLLMENT;
import static org.smartregister.chw.util.Constants.ICCM_REFERRAL_FORM;
import static org.smartregister.chw.util.Constants.REFERRAL_TASK_FOCUS;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreMalariaRegisterActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.fragment.IccmRegisterFragment;
import org.smartregister.chw.util.IccmReferralFormUtils;
import org.smartregister.chw.util.IccmVisitUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class IccmRegisterActivity extends CoreMalariaRegisterActivity {

    public static void startIccmRegistrationActivity(Activity activity, String baseEntityID, @Nullable String familyBaseEntityID) {
        Intent intent = new Intent(activity, IccmRegisterActivity.class);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.FAMILY_BASE_ENTITY_ID, familyBaseEntityID);
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.MALARIA_FORM_NAME, getIccmEnrollment());
        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.ACTION, org.smartregister.chw.anc.util.Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        activity.startActivity(intent);
    }

    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_iccm_menu;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new IccmRegisterFragment();
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        try {
            final CommonPersonObject personObject = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName).findByBaseEntityId(BASE_ENTITY_ID);
            String dobString = org.smartregister.util.Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = org.smartregister.chw.util.Utils.getAgeFromDate(dobString);
            jsonForm.getJSONObject("global").put("age", age);

            if (age >= 5) {
                JSONArray fields = jsonForm.getJSONObject("step3").getJSONArray(FIELDS);
                JSONObject respiratoryRate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "respiratory_rate");
                respiratoryRate.remove("v_required");
            }
            startActivityForResult(FormUtils.getStartFormActivity(jsonForm, this.getString(org.smartregister.chw.core.R.string.iccm_enrollment), this), JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON) {
            if (resultCode != RESULT_OK || data == null) {
                return;
            }

            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                if (jsonString == null) {
                    return;
                }

                JSONObject form = new JSONObject(jsonString);
                if (ICCM_ENROLLMENT.equals(form.optString(JsonFormUtils.ENCOUNTER_TYPE))) {
                    JSONObject step4 = form.optJSONObject("step4");
                    if (step4 == null) {
                        return;
                    }

                    JSONArray fields = step4.optJSONArray(FIELDS);
                    if (fields == null) {
                        return;
                    }

                    JSONObject shouldBeReferred = JsonFormUtils.getFieldJSONObject(fields, "should_be_referred");
                    if (shouldBeReferred == null || !shouldBeReferred.optString(VALUE).equalsIgnoreCase("true")) {
                        return;
                    }

                    String baseEntityId = form.getString(ENTITY_ID);

                    JSONArray selectedDangerSigns = JsonFormUtils.getFieldJSONObject(fields, "danger_signs").getJSONArray(VALUE);
                    JSONArray preReferralServicesGiven = new JSONArray();

                    if (getFieldValue(fields, "dispensed_anti_pyretic").equalsIgnoreCase("yes")) {
                        preReferralServicesGiven.put("anti_pyretic");
                    }

                    if (getFieldValue(fields, "administered_artesunate").equalsIgnoreCase("yes")) {
                        preReferralServicesGiven.put("rectal_artesunate");
                    }

                    CommonPersonObjectClient commonPersonObjectClient = getCommonPersonObjectClient(baseEntityId);

                    JSONObject referralFormJsonObject = (new com.vijay.jsonwizard.utils.FormUtils()).getFormJsonFromRepositoryOrAssets(this, ICCM_REFERRAL_FORM);
                    referralFormJsonObject.put(REFERRAL_TASK_FOCUS, CoreConstants.TASKS_FOCUS.SUSPECTED_MALARIA);

                    String dobString = org.smartregister.chw.util.Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.DOB, false);
                    int age = org.smartregister.chw.util.Utils.getAgeFromDate(dobString);
                    boolean isChild = age < 10;
                    boolean isFemaleOfReproductiveAge = isMemberOfReproductiveAge(commonPersonObjectClient, 10, 49) && org.smartregister.chw.util.Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.GENDER, false).equalsIgnoreCase("Female");

                    JSONArray steps = referralFormJsonObject.getJSONArray("steps");
                    JSONObject step = steps.getJSONObject(0);
                    JSONArray referralFormFields = step.getJSONArray("fields");
                    updateFieldsWithDangerSignsAndPreReferralServices(referralFormFields, selectedDangerSigns, preReferralServicesGiven, isChild, isFemaleOfReproductiveAge);

                    if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
                        ReferralRegistrationActivity.startGeneralReferralFormActivityForResults(this, baseEntityId, referralFormJsonObject, false, false);
                    }
                }
            } catch (Exception ex) {
                Timber.e(ex);
            }
        }
    }

    private static String getFieldValue(JSONArray fields, String fieldKey) {
        JSONObject field = JsonFormUtils.getFieldJSONObject(fields, fieldKey);
        return field == null ? "" : field.optString(VALUE);
    }


    public static void updateFieldsWithDangerSignsAndPreReferralServices(JSONArray fields, JSONArray dangerSigns, JSONArray preReferralManagement, boolean isChild, boolean isFemaleOfReproductiveAge) throws Exception {
        IccmReferralFormUtils.updateUnifiedReferralFields(fields, dangerSigns, preReferralManagement, isChild, isFemaleOfReproductiveAge);
    }

    public static boolean isKeyInJsonArray(JSONArray values, String optionName) throws Exception {
        return IccmReferralFormUtils.isKeyInJsonArray(values, optionName);
    }

    static String getAdultDrinkOptionLabel(String currentLabel) {
        return IccmReferralFormUtils.getAdultDrinkOptionLabel(currentLabel);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            IccmVisitUtils.processVisits();
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
