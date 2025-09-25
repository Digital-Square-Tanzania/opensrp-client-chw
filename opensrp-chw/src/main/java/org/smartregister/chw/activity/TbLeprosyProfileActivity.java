package org.smartregister.chw.activity;

import static org.smartregister.chw.tbleprosy.dao.TbLeprosyDao.getTbLeprosyClientStatus;
import static org.smartregister.chw.tbleprosy.util.Constants.JSON_FORM_EXTRA.EVENT_TYPE;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreTbLeprosyProfileActivity;
import org.smartregister.chw.core.custom_views.CoreVmmcFloatingMenu;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.custom_view.CecapFloatingMenu;
import org.smartregister.chw.custom_view.TbLeprosyFloatingMenu;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.tbleprosy.TbLeprosyLibrary;
import org.smartregister.chw.tbleprosy.dao.TbLeprosyDao;
import org.smartregister.chw.tbleprosy.domain.Visit;
import org.smartregister.chw.tbleprosy.util.Constants;
import org.smartregister.chw.tbleprosy.util.TbLeprosyVisitsUtil;
import org.smartregister.family.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


public class TbLeprosyProfileActivity extends CoreTbLeprosyProfileActivity {

    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, TbLeprosyProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.TBLEPROSY_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    public void openClientObservationResults() {
        try {
            startForm("tbleprosy_record_visit");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void observationResults() {
        try {
            startForm("tbleprosy_record_visit");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openTbContactFollowUpVisit() {
        try {
            startForm(Constants.FORMS.TBLEPROSY_FOLLOWUP_VISIT);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openRecordClientVisit() {
        try {
            startForm(Constants.FORMS.RECORD_TBLEPROSY_VISIT);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openObservationResults() {

        if (getTbLeprosyClientStatus(memberObject.getBaseEntityId()).equalsIgnoreCase("contact")) {
            startForm(Constants.FORMS.CONTACT_OBSERVATION_RESULTS);

        } else {
            startForm(Constants.FORMS.OBSERVATION_RESULTS);
        }
    }

    @Override
    public void openFollowupVisit() {
        try {
            startForm(Constants.FORMS.TBLEPROSY_FOLLOWUP_VISIT);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openRecordTbContactVisit() {
        TbLeprosyContactVisitActivity.startTbLeprosyVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openTbLeprosyContactRegister() {
        Intent intent = new Intent(this, TbLeprosyContactRegister.class);
        startActivity(intent);
    }

    @Override
    protected void setupButtons() {

        if (!getTbLeprosyClientStatus(memberObject.getBaseEntityId()).equalsIgnoreCase("contact")) {
            if (StringUtils.isNotBlank(TbLeprosyDao.getTBleprosyVisit(memberObject.getBaseEntityId()))) {
                textViewRecordTbLeprosy.setVisibility(View.GONE);
                rlObservationResults.setVisibility(View.VISIBLE);
            }

            if (StringUtils.isNotBlank(TbLeprosyDao.getTBleprosyObservationResults(memberObject.getBaseEntityId()))) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy_client_followup_visit);
                textViewRegisterTBLeprosyContact.setVisibility(View.VISIBLE);
                rlObservationResults.setVisibility(View.VISIBLE);
            }

            if (StringUtils.isNotBlank(TbLeprosyDao.getTBleprosyFollowUpVisit(memberObject.getBaseEntityId()))) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy_client_followup_visit);
                textViewRegisterTBLeprosyContact.setVisibility(View.VISIBLE);
                rlObservationResults.setVisibility(View.VISIBLE);
            }
        }


        if (getTbLeprosyClientStatus(memberObject.getBaseEntityId()).equalsIgnoreCase("contact")) {

            if(getTbLeprosyContactVisit() == null){
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy_contact_visit);
            }

            if (getTbLeprosyContactVisit() != null) {


                if (!getTbLeprosyContactVisit().getProcessed()) {
                    textViewRecordTbLeprosy.setVisibility(View.GONE);
                    manualProcessVisit.setVisibility(View.VISIBLE);
                    textViewContinueTbLeprosy.setVisibility(View.VISIBLE);
                    manualProcessVisit.setOnClickListener(view -> {
                        try {
                            TbLeprosyVisitsUtil.manualProcessVisit(getTbLeprosyContactVisit());
                            displayToast(org.smartregister.chw.tbleprosy.R.string.tbleprosy_visit_conducted);
                            setupViews();
                        } catch (Exception e) {
                            Timber.d(e);
                        }
                    });
                } else {
                    manualProcessVisit.setVisibility(View.GONE);
                    textViewContinueTbLeprosy.setVisibility(View.GONE);
                    rlLastVisit.setVisibility(View.VISIBLE);
                    rlObservationResults.setVisibility(View.VISIBLE);
                }
            }

            if (StringUtils.isNotBlank(TbLeprosyDao.getTBleprosyContactObservationResults(memberObject.getBaseEntityId()))) {
                textViewRecordTbLeprosy.setVisibility(View.VISIBLE);
                textViewRecordTbLeprosy.setText(R.string.record_tbleprosy_contact_visit_followup);
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    @Override
    protected void removeMember() {
        //do nothing
    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {
        //do nothing
    }

    @Override
    public void setProfileDetailThree(String s) {
        //do nothing
    }

    @Override
    public void toggleFamilyHead(boolean b) {
        //do nothing
    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {
        //do nothing
    }


    @Override
    public void startServiceForm() {
//        VmmcServiceActivity.startVmmcVisitActivity(this, baseEntityId, false);
    }

    @Override
    public void continueService() {
//        VmmcServiceActivity.startVmmcVisitActivity(this, baseEntityId, true);
    }


    @Override
    public void continueContactVisit() {
        TbLeprosyContactVisitActivity.startTbLeprosyVisitActivity(this, memberObject.getBaseEntityId(), true);
    }



    @Override
    public void refreshList() {
        //do nothing
    }

    @Override
    public void updateHasPhone(boolean b) {
        //do nothing
    }

    @Override
    public void setFamilyServiceStatus(String s) {
        //do nothing
    }

    @Override
    public void verifyHasPhone() {
        //do nothing
    }

    @Override
    public void notifyHasPhone(boolean b) {
        //do nothing
    }

    private void addReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.hiv_referral), CoreConstants.JSON_FORM.getHivReferralForm(), CoreConstants.TASKS_FOCUS.SICK_HIV));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_referral), CoreConstants.JSON_FORM.getTbReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_TB));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_leprosy_referral), CoreConstants.JSON_FORM.getTbLeprosyReferralForm(), CoreConstants.TASKS_FOCUS.TBLEPROSY));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral), CoreConstants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }


    @Override
    public void initializeFloatingMenu() {

        baseTbLeprosyFloatingMenu = new TbLeprosyFloatingMenu(this, memberObject);

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.vmmc_fab:
                    ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).launchCallWidget();
                    ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    org.smartregister.chw.util.Utils.launchClientReferralActivity(TbLeprosyProfileActivity.this, getReferralTypeModels(), memberObject.getBaseEntityId());
                    ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }
        };


        ((TbLeprosyFloatingMenu) baseTbLeprosyFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
        baseTbLeprosyFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseTbLeprosyFloatingMenu, linearLayoutParams);
    }

    private void startForm(String jsonForm) {
        JSONObject form = FormUtils.getFormUtils().getFormJson(jsonForm);
        try {
            form.put(org.smartregister.util.JsonFormUtils.ENTITY_ID, memberObject.getBaseEntityId());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        startFormActivity(form);
    }

    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, jsonForm.toString());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    protected Visit getTbLeprosyContactVisit() {
        return TbLeprosyLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.TBLEPROSY_CONTACT_VISIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);

            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        addReferralTypes();
    }
}

