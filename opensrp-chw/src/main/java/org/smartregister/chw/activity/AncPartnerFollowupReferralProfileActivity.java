package org.smartregister.chw.activity;

import static android.view.View.GONE;
import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.util.Constants.PartnerRegistrationConstants.INTENT_BASE_ENTITY_ID;
import static org.smartregister.chw.util.Constants.PartnerRegistrationConstants.INTENT_FORM_SUBMISSION_ID;
import static org.smartregister.chw.util.Constants.PartnerRegistrationConstants.REFERRAL_FORM_SUBMISSION_ID;
import static org.smartregister.chw.util.Constants.PartnerRegistrationConstants.ReferralFormId;
import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;
import static org.smartregister.util.JsonFormUtils.STEP1;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatButton;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.contract.AncMemberProfileContract;
import org.smartregister.chw.core.activity.CoreAncMemberProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.custom_view.AncFloatingMenu;
import org.smartregister.chw.dao.AncPartnerDao;
import org.smartregister.chw.dataloader.AncMemberDataLoader;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.interactor.AncMemberProfileInteractor;
import org.smartregister.chw.model.FamilyProfileModel;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.presenter.AncMemberProfilePresenter;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.util.UtilsFlv;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.AlertStatus;
import org.smartregister.domain.Task;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.interactor.FamilyProfileInteractor;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class AncPartnerFollowupReferralProfileActivity extends CoreAncMemberProfileActivity implements AncMemberProfileContract.View {

    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private NotificationListAdapter notificationListAdapter = new NotificationListAdapter();
    private String referralFormSubmissionId;

    public static void startMe(Activity activity, String baseEntityID, String formSubmissionId) {
        Intent intent = new Intent(activity, AncPartnerFollowupReferralProfileActivity.class);
        passToolbarTitle(activity, intent);
        intent.putExtra(Constants.ANC_MEMBER_OBJECTS.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(INTENT_FORM_SUBMISSION_ID, formSubmissionId);
        activity.startActivity(intent);
    }

    private void checkPhoneNumberProvided() {
        ((AncFloatingMenu) baseAncFloatingMenu).redraw(!StringUtils.isBlank(memberObject.getPhoneNumber())
                || !StringUtils.isBlank(getFamilyHeadPhoneNumber()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationAndReferralRecyclerView.setAdapter(notificationListAdapter);
        notificationListAdapter.setOnClickListener(this);
        this.referralFormSubmissionId = getIntent().getStringExtra(INTENT_FORM_SUBMISSION_ID);
    }

    @Override
    public void setupViews() {
        super.setupViews();
        CustomFontTextView titleView = findViewById(R.id.toolbar_title);
        String titleText = TextUtils.isEmpty(getTitleViewText()) ? getString(R.string.return_to_all_partner_followup_clients) : getTitleViewText();
        titleView.setText(titleText);
        layoutRecordView.setVisibility(View.VISIBLE);
        if (AncPartnerDao.isPartnerFollowedUp(referralFormSubmissionId)) {
            textview_record_visit.setVisibility(View.GONE);
        }
        textViewAncVisitNot.setVisibility(GONE);
        tvEdit.setVisibility(View.GONE);
        textview_record_visit.setText(R.string.record_followup_feedback);
        textview_record_visit.setBackgroundResource(org.smartregister.chw.core.R.drawable.record_btn_anc_selector);
        textview_record_visit.setOnClickListener(view -> {
            JSONObject formJsonObject = null;
            try {
                formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, CoreConstants.JSON_FORM.getAncPartnerCommunityFollowupFeedback());
                AllSharedPreferences preferences = ChwApplication.getInstance().getContext().allSharedPreferences();
                JSONObject chwName = getFieldJSONObject(fields(formJsonObject, STEP1), "chw_name");
                chwName.put(VALUE, preferences.getANMPreferredName(preferences.fetchRegisteredANM()));
                startFormActivity(formJsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        RelativeLayout partnerView = findViewById(R.id.rlPartnerView);
        AppCompatButton registerBtn = findViewById(R.id.register_partner_btn);
        if (AncPartnerDao.hasPartnerAgreeForRegistration(referralFormSubmissionId) && !AncPartnerDao.isPartnerRegistered(referralFormSubmissionId)) {
            partnerView.setVisibility(View.VISIBLE);
        }

        if (AncPartnerDao.isPartnerAlreadyRegistered(referralFormSubmissionId))
            partnerView.setVisibility(View.GONE);

        partnerView.setOnClickListener(this);
        registerBtn.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(),
                memberObject.getBaseEntityId(), this);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
    }

    @Override
    public void initializeFloatingMenu() {
        baseAncFloatingMenu = new AncFloatingMenu(this, getAncWomanName(),
                memberObject.getPhoneNumber(), getFamilyHeadName(), getFamilyHeadPhoneNumber(), getProfileType());

        baseAncFloatingMenu.findViewById(R.id.refer_to_facility_layout).setVisibility(GONE);

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.anc_fab:
                    checkPhoneNumberProvided();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((AncFloatingMenu) baseAncFloatingMenu).launchCallWidget();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    ((AncMemberProfilePresenter) ancMemberProfilePresenter()).referToFacility();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((AncFloatingMenu) baseAncFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
        baseAncFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseAncFloatingMenu, linearLayoutParams);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        setupViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_cbhs_registration) {
            CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

            final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
            startCBHSRegister(commonPersonObject);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.anc_danger_signs_outcome).setVisible(false);
        menu.findItem(R.id.action_malaria_diagnosis).setVisible(false);
        menu.findItem(R.id.action_pregnancy_out_come).setVisible(false);
        menu.findItem(R.id.action_anc_registration).setVisible(false);
        menu.findItem(R.id.action_remove_member).setVisible(false);
        menu.findItem(R.id.action_cbhs_registration).setVisible(false);
        UtilsFlv.updateHivMenuItems(baseEntityID, menu);
        return true;
    }

    @Override // to chw
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON) {
            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyMemberRegister.updateEventType)) {
                    FamilyEventClient familyEventClient =
                            new FamilyProfileModel(memberObject.getFamilyName()).processUpdateMemberRegistration(jsonString, memberObject.getBaseEntityId());
                    new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, ancMemberProfilePresenter());
                } else if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(CoreConstants.EventType.UPDATE_ANC_REGISTRATION)) {
                    AllSharedPreferences allSharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
                    Event baseEvent = org.smartregister.chw.anc.util.JsonFormUtils.processJsonForm(allSharedPreferences, jsonString, Constants.TABLES.ANC_MEMBERS);
                    NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(baseEvent)));
                    AllCommonsRepository commonsRepository = CoreChwApplication.getInstance().getAllCommonsRepository(CoreConstants.TABLE_NAME.ANC_MEMBER);

                    JSONArray field = org.smartregister.util.JsonFormUtils.fields(form);
                    JSONObject phoneNumberObject = org.smartregister.util.JsonFormUtils.getFieldJSONObject(field, DBConstants.KEY.PHONE_NUMBER);
                    String phoneNumber = phoneNumberObject.getString(CoreJsonFormUtils.VALUE);
                    String baseEntityId = baseEvent.getBaseEntityId();
                    if (commonsRepository != null) {
                        ContentValues values = new ContentValues();
                        values.put(DBConstants.KEY.PHONE_NUMBER, phoneNumber);
                        CoreChwApplication.getInstance().getRepository().getWritableDatabase().update(CoreConstants.TABLE_NAME.ANC_MEMBER, values, DBConstants.KEY.BASE_ENTITY_ID + " = ?  ", new String[]{baseEntityId});
                    }

                } else if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(CoreConstants.EventType.ANC_REFERRAL)) {
                    ancMemberProfilePresenter().createReferralEvent(Utils.getAllSharedPreferences(), jsonString);
                    showToast(this.getString(R.string.referral_submitted));
                } else if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equalsIgnoreCase(CoreConstants.EventType.ANC_PARTNER_COMMUNITY_FOLLOWUP_FEEDBACK)) {
                    AllSharedPreferences allSharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
                    Event baseEvent = org.smartregister.chw.anc.util.JsonFormUtils.processJsonForm(allSharedPreferences, tagReferralFormId(jsonString, referralFormSubmissionId), CoreConstants.TABLE_NAME.ANC_PARTNER_FOLLOWUP_FEEDBACK);
                    org.smartregister.chw.anc.util.JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                    baseEvent.setBaseEntityId(baseEntityID);
                    NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(baseEvent)));
                    setupViews();
                }

            } catch (Exception e) {
                Timber.e(e, "AncMemberProfileActivity -- > onActivityResult");
            }
        } else if (requestCode == Constants.REQUEST_CODE_HOME_VISIT) {
            refreshViewOnHomeVisitResult();
        } else if (requestCode == CoreConstants.ProfileActivityResults.CHANGE_COMPLETED) {
            ChwScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), CoreConstants.EventType.ANC_HOME_VISIT, new Date());
            finish();
        }
    }

    private String tagReferralFormId(String jsonString, String formSubmissionId) throws JSONException {
        JSONObject form = new JSONObject(jsonString);
        JSONArray fields = org.smartregister.util.JsonFormUtils.fields(form);
        JSONObject referralFormId = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, ReferralFormId);
        assert referralFormId != null;
        referralFormId.put(JsonFormUtils.VALUE, formSubmissionId);
        return form.toString();
    }

    @Override
    public void startFormForEdit(Integer title_resource, String formName) {
        try {
            JSONObject form = null;
            boolean isPrimaryCareGiver = memberObject.getPrimaryCareGiver().equals(memberObject.getBaseEntityId());
            String titleString = title_resource != null ? getResources().getString(title_resource) : null;

            if (formName.equals(CoreConstants.JSON_FORM.getAncRegistration())) {

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new AncMemberDataLoader(titleString));
                form = binder.getPrePopulatedForm(formName);
                if (form != null) {
                    form.put(JsonFormUtils.ENCOUNTER_TYPE, CoreConstants.EventType.UPDATE_ANC_REGISTRATION);
                }

            } else if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {

                String eventName = org.smartregister.chw.util.Utils.metadata().familyMemberRegister.updateEventType;

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new FamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString, eventName, memberObject.getChwMemberId()));

                form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getFamilyMemberRegister());
            }

            startActivityForResult(org.smartregister.chw.util.JsonFormUtils.getAncPncStartFormIntent(form, this), JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected void registerPresenter() {
        presenter = new AncMemberProfilePresenter(this, new AncMemberProfileInteractor(this), memberObject);
    }

    @Override
    public void openUpcomingService() {
        Timber.i("openUpcomingService");
    }

    @Override
    public void openFamilyDueServices() {
        Timber.i("openFamilyDueServices");
    }

    @Override
    protected void startLDRegistration() {
        //implement
    }

    @Override
    public boolean usesPregnancyRiskProfileLayout() {
        return false;
    }

    @Override
    public void setFamilyLocation() {
        if (ChwApplication.getApplicationFlavor().flvSetFamilyLocation() && getMemberGPS() != null) {
            view_family_location_row.setVisibility(View.VISIBLE);
            rlFamilyLocation.setVisibility(View.VISIBLE);
        }
    }

    protected String getMemberGPS() {
        return memberObject.getGps();
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.textview_record_visit || id == R.id.textview_record_reccuring_visit) {
            AncHomeVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
        } else if (id == R.id.textview_edit) {
            AncHomeVisitActivity.startMe(this, memberObject.getBaseEntityId(), true);
        } else if (id == R.id.rlPartnerView || id == R.id.register_partner_btn) {
            Intent intent = new Intent(this, PartnerRegistrationActivity.class);
            intent.putExtra(INTENT_FORM_SUBMISSION_ID, AncPartnerDao.getFeedbackFormId(referralFormSubmissionId));
            intent.putExtra(REFERRAL_FORM_SUBMISSION_ID, referralFormSubmissionId);
            intent.putExtra(INTENT_BASE_ENTITY_ID, baseEntityID);
            startActivity(intent);
        }
        handleNotificationRowClick(this, view, notificationListAdapter, memberObject.getBaseEntityId());
    }

    @Override
    public void setClientTasks(Set<Task> taskList) {
        //overridden
    }

    @Override
    public void onMemberDetailsReloaded(MemberObject memberObject) {
        this.memberObject = memberObject;
        super.onMemberDetailsReloaded(memberObject);
    }

    private void refreshViewOnHomeVisitResult() {
        Observable<Visit> observable = Observable.create(visitObservableEmitter -> {
            Visit lastVisit = getVisit(CoreConstants.EventType.ANC_HOME_VISIT);
            visitObservableEmitter.onNext(lastVisit);
            visitObservableEmitter.onComplete();
        });

        final Disposable[] disposable = new Disposable[1];
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Visit>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable[0] = d;
                    }

                    @Override
                    public void onNext(Visit visit) {
                        displayView();
                        setLastVisit(visit.getDate());
                        onMemberDetailsReloaded(memberObject);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {
                        disposable[0].dispose();
                        disposable[0] = null;
                    }
                });
    }

    @Override
    public void startFormActivity(JSONObject formJson) {
        startActivityForResult(CoreJsonFormUtils.getJsonIntent(this, formJson, Utils.metadata().familyMemberFormActivity),
                JsonFormUtils.REQUEST_CODE_GET_JSON);
    }


    protected void startCBHSRegister(CommonPersonObject commonPersonObject) {
        String gender = org.smartregister.chw.util.Utils.getValue(commonPersonObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.GENDER, false);
        String dob = org.smartregister.chw.util.Utils.getValue(commonPersonObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.DOB, false);
        int age = org.smartregister.chw.util.Utils.getAgeFromDate(dob);

        try {
            String formName = org.smartregister.chw.util.Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(AncPartnerFollowupReferralProfileActivity.this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(AncPartnerFollowupReferralProfileActivity.this, memberObject.getBaseEntityId(), formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openFamilyLocation() {
        Intent intent = new Intent(this, AncMemberMapActivity.class);
        this.startActivity(intent);
    }


    @Override
    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    @Override
    public void setUpComingServicesStatus(String service, AlertStatus status, Date date) {
        this.rlUpcomingServices.setVisibility(GONE);
    }

    @Override
    public void setFamilyStatus(AlertStatus status) {
        this.rlFamilyServicesDue.setVisibility(GONE);
    }

}
