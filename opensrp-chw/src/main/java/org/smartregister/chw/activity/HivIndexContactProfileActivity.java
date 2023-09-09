package org.smartregister.chw.activity;

import static org.smartregister.chw.hiv.util.Constants.ActivityPayload.HIV_MEMBER_OBJECT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreHivIndexContactProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.interactor.CoreHivIndexContactProfileInteractor;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.model.CoreAllClientsMemberModel;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.custom_view.HivIndexContactFloatingMenu;
import org.smartregister.chw.hiv.activity.BaseHivFormsActivity;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.dao.HivIndexDao;
import org.smartregister.chw.hiv.domain.HivIndexContactObject;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.presenter.HivIndexContactProfilePresenter;
import org.smartregister.chw.tb.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.interactor.FamilyProfileInteractor;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.opd.utils.OpdConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

public class HivIndexContactProfileActivity extends CoreHivIndexContactProfileActivity implements FamilyProfileExtendedContract.PresenterCallBack, OnRetrieveNotifications {

    public final static String REGISTERED_TO_HIV_REGISTRY = "registered_to_hiv_registry";
    private CommonPersonObjectClient commonPersonObjectClient;
    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private NotificationListAdapter notificationListAdapter = new NotificationListAdapter();

    public static void startHivIndexContactProfileActivity(Activity activity, HivIndexContactObject hivIndexContactObject) {
        Intent intent = new Intent(activity, HivIndexContactProfileActivity.class);
        intent.putExtra(HIV_MEMBER_OBJECT, hivIndexContactObject);
        activity.startActivity(intent);
    }

    public static void startHivIndexContactFollowupActivity(Activity activity, String baseEntityID) throws JSONException {

        Intent intent = new Intent(activity, BaseHivFormsActivity.class);
        intent.putExtra(org.smartregister.chw.hiv.util.Constants.ActivityPayload.BASE_ENTITY_ID, baseEntityID);

        HivIndexContactObject hivIndexContactObject = HivIndexDao.getMember(baseEntityID);

        JSONObject form = (new FormUtils()).getFormJsonFromRepositoryOrAssets(activity, CoreConstants.JSON_FORM.getHivIndexContactFollowupVisit());
        intent.putExtra(org.smartregister.chw.hiv.util.Constants.ActivityPayload.JSON_FORM, form.toString());

        intent.putExtra(org.smartregister.chw.hiv.util.Constants.ActivityPayload.ACTION, Constants.ActivityPayloadType.FOLLOW_UP_VISIT);
        intent.putExtra(org.smartregister.chw.hiv.util.Constants.ActivityPayload.USE_DEFAULT_NEAT_FORM_LAYOUT, false);

        activity.startActivityForResult(intent, org.smartregister.chw.anc.util.Constants.REQUEST_CODE_HOME_VISIT);
    }

    @Override
    public void setupViews() {
        super.setupViews();
        if (getHivIndexContactObject().getFollowedUpByChw()) {

        }
    }


    @Override
    protected void onCreation() {
        super.onCreation();
        setCommonPersonObjectClient(getClientDetailsByBaseEntityID(getHivIndexContactObject().getBaseEntityId()));
        addHivReferralTypes();
    }

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
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(),
                getHivIndexContactObject().getBaseEntityId(), this);
    }

    public CommonPersonObjectClient getCommonPersonObjectClient() {
        return commonPersonObjectClient;
    }

    public void setCommonPersonObjectClient(CommonPersonObjectClient commonPersonObjectClient) {
        this.commonPersonObjectClient = commonPersonObjectClient;
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        setHivContactProfilePresenter(new HivIndexContactProfilePresenter(this, new CoreHivIndexContactProfileInteractor(), getHivIndexContactObject()));
        fetchProfileData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        try {
            if (itemId == R.id.action_issue_hiv_community_followup_referral) {
                HivRegisterActivity.startHIVFormActivity(this, getHivIndexContactObject().getBaseEntityId(), CoreConstants.JSON_FORM.getHivIndexContactCommunityFollowupReferral(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, CoreConstants.JSON_FORM.getHivIndexContactCommunityFollowupReferral()).toString());
                return true;
            } else if (itemId == R.id.action_kvp_prep_registration) {
                String gender = Utils.getClientGender(getHivIndexContactObject().getBaseEntityId());
                String dob = getHivIndexContactObject().getDob();
                int age = Utils.getAgeFromDate(dob);
                KvpPrEPRegisterActivity.startRegistration(HivIndexContactProfileActivity.this, getHivIndexContactObject().getBaseEntityId(), gender, age);
                return true;
            } else if (itemId == R.id.action_hivst_registration) {
                startHivstRegistration();
                return true;
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(org.smartregister.chw.core.R.menu.hiv_profile_menu, menu);
        menu.findItem(R.id.action_location_info).setVisible(UpdateDetailsUtil.isIndependentClient(getHivIndexContactObject().getBaseEntityId()));
        if (ChwApplication.getApplicationFlavor().hasHIVST()) {
            String dob = Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_hivst_registration).setVisible(!HivstDao.isRegisteredForHivst(getHivIndexContactObject().getBaseEntityId()) && age >= 15);
        }
        if (ChwApplication.getApplicationFlavor().hasKvp()) {
            menu.findItem(R.id.action_kvp_prep_registration).setVisible(!KvpDao.isRegisteredForKvpPrEP(getHivIndexContactObject().getBaseEntityId()));
        }
        return true;
    }

    @Override
    public void openFollowUpVisitForm(boolean isEdit) {
        if (!isEdit) {
            try {
                startHivIndexContactFollowupActivity(this, getHivIndexContactObject().getBaseEntityId());
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

    @Override
    protected void removeMember() {
        // Not required for HF (as seen in other profile activities)?
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.record_hiv_followup_visit) {
            openFollowUpVisitForm(false);
        }
    }

    private void addHivReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.hts_referral),
                    CoreConstants.JSON_FORM.getHtsReferralForm(), CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST));

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral),
                    CoreConstants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void initializeCallFAB() {
        setHivFloatingMenu(new HivIndexContactFloatingMenu(this, getHivIndexContactObject()));

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.hiv_fab:
                    checkPhoneNumberProvided();
                    ((HivIndexContactFloatingMenu) getHivFloatingMenu()).animateFAB();
                    break;
                case R.id.call_layout:
                    ((HivIndexContactFloatingMenu) getHivFloatingMenu()).launchCallWidget();
                    ((HivIndexContactFloatingMenu) getHivFloatingMenu()).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    ((HivIndexContactProfilePresenter) getHivContactProfilePresenter()).referToFacility();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }
        };

        ((HivIndexContactFloatingMenu) getHivFloatingMenu()).setFloatMenuClickListener(onClickFloatingMenu);
        getHivFloatingMenu().setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(getHivFloatingMenu(), linearLayoutParams);
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(getHivIndexContactObject().getPhoneNumber()));
        ((HivIndexContactFloatingMenu) getHivFloatingMenu()).redraw(phoneNumberAvailable);
    }

    @Override
    public Context getContext() {
        return HivIndexContactProfileActivity.this;
    }

    @Override
    public void verifyHasPhone() {
        // Implement
    }

    @Override
    public void notifyHasPhone(boolean b) {
        // Implement
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        try {
            String jsonString = data.getStringExtra(OpdConstants.JSON_FORM_EXTRA.JSON);
            if (jsonString == null) {
                finish();
            }
            JSONObject form = new JSONObject(jsonString);
            if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyRegister.updateEventType)) {
                FamilyEventClient familyEventClient = new CoreAllClientsMemberModel().processJsonForm(jsonString, getHivIndexContactObject().getFamilyBaseEntityId());
                familyEventClient.getEvent().setEntityType(CoreConstants.TABLE_NAME.INDEPENDENT_CLIENT);
                new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, (FamilyProfileContract.InteractorCallBack) getHivContactProfilePresenter());
            } else {
                boolean savedToHivRegistry = data.getBooleanExtra(REGISTERED_TO_HIV_REGISTRY, false);
                if (savedToHivRegistry) {
                    HivProfileActivity.startHivProfileActivity(this, Objects.requireNonNull(HivDao.getMember(getHivIndexContactObject().getBaseEntityId())));
                    finish();
                } else {
                    setHivIndexContactObject(HivIndexDao.getMember(getHivIndexContactObject().getBaseEntityId()));
                    initializePresenter();
                    fetchProfileData();
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void setFollowUpButtonDue() {
        super.setFollowUpButtonDue();
        showFollowUpVisitButton(!getHivIndexContactObject().getFollowedUpByChw());
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> list) {

    }

    private void startHivstRegistration() {
        CommonRepository commonRepository = org.smartregister.family.util.Utils.context().commonrepository(org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);

        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(getHivIndexContactObject().getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        String gender = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);

        HivstRegisterActivity.startHivstRegistrationActivity(this, getHivIndexContactObject().getBaseEntityId(), gender);
    }
}

