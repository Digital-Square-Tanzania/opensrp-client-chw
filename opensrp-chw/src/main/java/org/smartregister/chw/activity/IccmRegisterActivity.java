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
            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(ICCM_ENROLLMENT)) {
                    JSONArray fields = form.getJSONObject("step3").getJSONArray(FIELDS);


                    JSONObject shouldBeReferred = JsonFormUtils.getFieldJSONObject(fields, "should_be_referred");
                    String shouldBeReferredValue = shouldBeReferred.getString("value");
                    if (shouldBeReferredValue.equalsIgnoreCase("true")) {
                        String baseEntityId = form.getString(ENTITY_ID);

                        JSONArray selectedDangerSigns = JsonFormUtils.getFieldJSONObject(fields, "danger_signs").getJSONArray(VALUE);
                        JSONArray preReferralServicesGiven = new JSONArray();

                        try {
                            if (JsonFormUtils.getFieldJSONObject(fields, "dispensed_anti_pyretic").getString(VALUE).equalsIgnoreCase("yes")) {
                                preReferralServicesGiven.put("anti_pyretic");
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                        }

                        try {
                            if (JsonFormUtils.getFieldJSONObject(fields, "administered_artesunate").getString(VALUE).equalsIgnoreCase("yes")) {
                                preReferralServicesGiven.put("rectal_artesunate");
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                        }

                        CommonPersonObjectClient commonPersonObjectClient = getCommonPersonObjectClient(baseEntityId);

                        JSONObject referralFormJsonObject = (new com.vijay.jsonwizard.utils.FormUtils()).getFormJsonFromRepositoryOrAssets(this, ICCM_REFERRAL_FORM);
                        referralFormJsonObject.put(REFERRAL_TASK_FOCUS, CoreConstants.TASKS_FOCUS.SUSPECTED_MALARIA);

                        boolean isFemaleOfReproductiveAge = isMemberOfReproductiveAge(commonPersonObjectClient, 10, 49) && org.smartregister.chw.util.Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.GENDER, false).equalsIgnoreCase("Female");

                        JSONArray steps = referralFormJsonObject.getJSONArray("steps");
                        JSONObject step = steps.getJSONObject(0);
                        JSONArray referralFormFields = step.getJSONArray("fields");
                        updateFieldsWithDangerSignsAndPreReferralServices(referralFormFields, selectedDangerSigns, preReferralServicesGiven, isFemaleOfReproductiveAge);

                        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
                            ReferralRegistrationActivity.startGeneralReferralFormActivityForResults(this, baseEntityId, referralFormJsonObject, false);
                        }
                    }

                }
            } catch (Exception ex) {
                Timber.e(ex);
            }
        }
    }


    public static void updateFieldsWithDangerSignsAndPreReferralServices(JSONArray fields, JSONArray dangerSigns, JSONArray preReferralManagement, boolean isFemaleOfReproductiveAge) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field.getString("name").equals("problem")) {
                JSONArray options = field.getJSONArray("options");
                if (!isFemaleOfReproductiveAge) {
                    options.remove(options.length() - 1);
                }
                for (int j = 0; j < options.length(); j++) {
                    JSONObject option = options.getJSONObject(j);
                    if (isKeyInJsonArray(dangerSigns, option.getString("name"))) {
                        JSONObject properties = new JSONObject();
                        properties.put("checked", true);
                        option.put("properties", properties);
                    }
                }
            } else if (field.getString("name").equals("service_before_referral")) {
                JSONArray options = field.getJSONArray("options");
                for (int j = 0; j < options.length(); j++) {
                    JSONObject option = options.getJSONObject(j);
                    if (isKeyInJsonArray(preReferralManagement, option.getString("name"))) {
                        JSONObject properties = new JSONObject();
                        properties.put("checked", true);
                        option.put("properties", properties);
                    }
                }
            }
        }
    }

    public static boolean isKeyInJsonArray(JSONArray values, String optionName) throws Exception {
        for (int i = 0; i < values.length(); i++) {
            if (values.getString(i).equals(optionName)) {
                return true;
            }
        }
        return false;
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