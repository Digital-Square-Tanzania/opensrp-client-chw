package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient;
import static org.smartregister.chw.util.Utils.getClientGender;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreHivstProfileActivity;
import org.smartregister.chw.core.interactor.CoreHivstProfileInteractor;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.presenter.CoreHivstMemberProfilePresenter;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.hivst.util.Constants;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.util.HivstUtils;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.AlertStatus;
import org.smartregister.family.util.DBConstants;

import timber.log.Timber;


public class HivstProfileActivity extends CoreHivstProfileActivity {

    public static void startProfile(Activity activity, String baseEntityId, boolean openIssueSelfTestingKitsForm) {
        Intent intent = new Intent(activity, HivstProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(OPEN_ISSUE_SELF_TESTING_KITS_FORM, openIssueSelfTestingKitsForm);
        activity.startActivity(intent);
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        memberObject = HivstDao.getMember(baseEntityId);
        profilePresenter = new CoreHivstMemberProfilePresenter(this, new CoreHivstProfileInteractor(), memberObject);
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    @Override
    protected void startHivServicesRegistration() {
        CommonPersonObjectClient commonPersonObjectClient = getCommonPersonObjectClient(memberObject.getBaseEntityId());
        String gender = Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.GENDER, false);
        String dob = Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);

        try {
            String formName = org.smartregister.chw.util.Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new com.vijay.jsonwizard.utils.FormUtils()).getFormJsonFromRepositoryOrAssets(this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(this, memberObject.getBaseEntityId(), formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    @Override
    public void startIssueSelfTestingKitsForm(String baseEntityId) {
        JSONObject form = FormUtils.getFormUtils().getFormJson(Constants.FORMS.HIVST_ISSUE_KITS);
        try {
            form.put(org.smartregister.util.JsonFormUtils.ENTITY_ID, baseEntityId);
            JSONObject global = form.getJSONObject("global");
            boolean knownPositiveFromHIV = HivDao.isRegisteredForHiv(baseEntityId) && StringUtils.isNotBlank(HivDao.getMember(baseEntityId).getCtcNumber());
            global.put("known_positive", HivstDao.isTheClientKnownPositiveAtReg(baseEntityId) || knownPositiveFromHIV);
        } catch (JSONException e) {
            Timber.e(e);
        }
        startFormActivity(form);
    }

    @Override
    public void startResultViewActivity(Context context, String baseEntityId) {
        HivstResultViewActivity.startResultViewActivity(context, baseEntityId);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.rlSelfTestingResults) {
            startResultViewActivity(this, memberObject.getBaseEntityId());
        } else {
            super.onClick(view);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(org.smartregister.chw.core.R.id.action_cbhs_registration).setVisible(!HivDao.isRegisteredForHiv(memberObject.getBaseEntityId()));
        if (ChwApplication.getApplicationFlavor().hasKvp()) {
            menu.findItem(R.id.action_kvp_prep_registration).setVisible(!KvpDao.isRegisteredForKvpPrEP(memberObject.getBaseEntityId()));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_kvp_prep_registration) {
            String gender = getClientGender(memberObject.getBaseEntityId());
            String dob = memberObject.getAge();
            int age = Utils.getAgeFromDate(dob);
            KvpPrEPRegisterActivity.startRegistration(HivstProfileActivity.this, memberObject.getBaseEntityId(), gender, age);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startReferralForm() {
        HivstUtils.startHIVSTReferral(this, memberObject.getBaseEntityId());
    }

    @Override
    public void showReferralView() {
        boolean knownPositiveFromHIV = HivDao.isRegisteredForHiv(memberObject.getBaseEntityId()) && StringUtils.isNotBlank(HivDao.getMember(memberObject.getBaseEntityId()).getCtcNumber());
        if (knownPositiveFromHIV || HivstDao.isTheClientKnownPositiveAtReg(memberObject.getBaseEntityId())) {
            baseHivstFloatingMenu.findViewById(R.id.refer_to_facility_layout).setVisibility(View.GONE);
        } else {
            baseHivstFloatingMenu.findViewById(R.id.refer_to_facility_layout).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void removeMember() {
        //implement
    }


    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {
        //implement
    }

    @Override
    public void setProfileDetailThree(String s) {
        //implement
    }

    @Override
    public void toggleFamilyHead(boolean b) {
        //implement
    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {
        //implement
    }

    @Override
    public void refreshList() {
        //implement
    }

    @Override
    public void updateHasPhone(boolean b) {
        //implement
    }

    @Override
    public void setFamilyServiceStatus(String s) {
        //implement
    }

    @Override
    public void verifyHasPhone() {
        //implement
    }

    @Override
    public void notifyHasPhone(boolean b) {
        //implement
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        rlLastVisit.setVisibility(View.GONE);
    }

    @Override
    public void refreshFamilyStatus(AlertStatus status) {
        super.refreshFamilyStatus(status);
        rlFamilyServicesDue.setVisibility(View.GONE);
    }
}
