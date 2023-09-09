package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.util.Constants.EventType;
import static org.smartregister.chw.util.Constants.JSON_FORM;
import static org.smartregister.chw.util.Constants.ProfileActivityResults;
import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;
import static org.smartregister.chw.util.Utils.getClientGender;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;
import static org.smartregister.util.Utils.getAllSharedPreferences;

import android.app.Activity;
import android.content.Intent;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.presenter.BaseAncMemberProfilePresenter;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.contract.PncMemberProfileContract;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CorePncMemberProfileActivity;
import org.smartregister.chw.core.activity.CorePncRegisterActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.dao.ChwNotificationDao;
import org.smartregister.chw.core.interactor.CorePncMemberProfileInteractor;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.model.CoreAllClientsMemberModel;
import org.smartregister.chw.core.rule.PncVisitAlertRule;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.custom_view.AncFloatingMenu;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.interactor.ChildProfileInteractor;
import org.smartregister.chw.interactor.FamilyProfileInteractor;
import org.smartregister.chw.interactor.PncMemberProfileInteractor;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.model.ChildRegisterModel;
import org.smartregister.chw.model.FamilyProfileModel;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.presenter.PncMemberProfilePresenter;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class PncMemberProfileActivity extends CorePncMemberProfileActivity implements PncMemberProfileContract.View {
    public static final String CLOSE_PNC_VISITS = "Close PNC Visits";

    private Flavor flavor = new PncMemberProfileActivityFlv();
    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private NotificationListAdapter notificationListAdapter = new NotificationListAdapter();

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, PncMemberProfileActivity.class);
        intent.putExtra(Constants.ANC_MEMBER_OBJECTS.BASE_ENTITY_ID, baseEntityID);
        passToolbarTitle(activity, intent);
        activity.startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(),
                baseEntityID, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        switch (requestCode) {
            case ProfileActivityResults.CHANGE_COMPLETED:
                Intent intent = new Intent(PncMemberProfileActivity.this, PncRegisterActivity.class);
                intent.putExtras(getIntent().getExtras());
                startActivity(intent);
                finish();
                break;
            case JsonFormUtils.REQUEST_CODE_GET_JSON:
                try {
                    String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                    JSONObject form = new JSONObject(jsonString);
                    if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyMemberRegister.updateEventType)) {

                        FamilyEventClient familyEventClient =
                                new FamilyProfileModel(memberObject.getFamilyName()).processUpdateMemberRegistration(jsonString, memberObject.getBaseEntityId());
                        new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, (FamilyProfileContract.InteractorCallBack) pncMemberProfilePresenter());
                    } else if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyRegister.updateEventType)) {
                        CommonPersonObjectClient client = getCommonPersonObjectClient(memberObject.getBaseEntityId());
                        FamilyEventClient familyEventClient = new CoreAllClientsMemberModel().processJsonForm(jsonString, UpdateDetailsUtil.getFamilyBaseEntityId(client));
                        familyEventClient.getEvent().setEntityType(CoreConstants.TABLE_NAME.INDEPENDENT_CLIENT);
                        new org.smartregister.family.interactor.FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, (FamilyProfileContract.InteractorCallBack) pncMemberProfilePresenter());
                    }

                    if (EventType.UPDATE_CHILD_REGISTRATION.equals(form.getString(JsonFormUtils.ENCOUNTER_TYPE))) {
                        Pair<Client, Event> pair = new ChildRegisterModel().processRegistration(jsonString);

                        if (pair != null) {
                            ((PncMemberProfileInteractor) pncMemberProfileInteractor).updateChild(pair, jsonString, null);
                        }

                    } else if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(CoreConstants.EventType.PNC_REFERRAL)) {
                        pncMemberProfilePresenter().createReferralEvent(Utils.getAllSharedPreferences(), jsonString);
                        showToast(this.getString(R.string.referral_submitted));
                    }

                } catch (Exception e) {
                    Timber.e(e);
                }
                break;
            case Constants.REQUEST_CODE_HOME_VISIT:
                this.setupViews();
                ChwScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), CoreConstants.EventType.PNC_HOME_VISIT, new Date());
                refreshOnHomeVisitResult();
                break;
            default:
                break;
        }
    }

    @Override
    public void setupViews() {
        super.setupViews();
//        textview_record_visit.setVisibility(GONE);

        PncVisitAlertRule summaryVisit = getVisitDetails();
        String statusVisit = summaryVisit.getButtonStatus();
        if (statusVisit.equals("OVERDUE")) {
            updateUiForVisitsOverdue();
        } else if (statusVisit.equals("DUE")) {
            updateUiForVisitsDue();
        } else if (ChildProfileInteractor.VisitType.VISIT_DONE.name().equals(statusVisit)) {
            Visit lastVisit = getVisit(Constants.EVENT_TYPE.PNC_HOME_VISIT);
            if (lastVisit != null) {
                if ((Days.daysBetween(new DateTime(lastVisit.getCreatedAt()), new DateTime()).getDays() < 1) &&
                        (Days.daysBetween(new DateTime(lastVisit.getDate()), new DateTime()).getDays() <= 1)) {
                    setEditViews(true, true, lastVisit.getDate().getTime());
                } else updateUiForNoVisits();

            } else updateUiForVisitsDue();

        } else updateUiForNoVisits();
    }

    protected void updateUiForNoVisits() {
        textview_record_visit.setVisibility(View.GONE);
        layoutRecordView.setVisibility(View.GONE);
    }

    protected void updateUiForVisitsDue() {
        layoutRecordView.setVisibility(View.VISIBLE);
        textview_record_visit.setVisibility(View.VISIBLE);
        textview_record_visit.setBackgroundResource(R.drawable.rounded_blue_btn);
    }

    protected void updateUiForVisitsOverdue() {
        layoutRecordView.setVisibility(View.VISIBLE);
        textview_record_visit.setVisibility(View.VISIBLE);
        textview_record_visit.setBackgroundResource(R.drawable.rounded_red_btn);
    }


    private void refreshOnHomeVisitResult() {
        Observable<Visit> observable = Observable.create(e -> {
            Visit lastVisit = getVisit(CoreConstants.EventType.PNC_HOME_VISIT);
            e.onNext(lastVisit);
            e.onComplete();
        });

        final Disposable[] disposable = new Disposable[1];
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Visit>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
            }

            @Override
            public void onNext(Visit visit) {
                displayView();
                setLastVisit(visit.getDate());
                setupViews();
                (pncMemberProfileInteractor).refreshProfileInfo(memberObject, (BaseAncMemberProfilePresenter) pncMemberProfilePresenter());

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
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return FamilyProfileActivity.class;
    }

    @Override
    protected CorePncMemberProfileInteractor getPncMemberProfileInteractor() {
        return new PncMemberProfileInteractor(this);
    }

    @Override
    protected void removePncMember() {
        IndividualProfileRemoveActivity.startIndividualProfileActivity(PncMemberProfileActivity.this, clientObject(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), PncRegisterActivity.class.getCanonicalName());
    }

    @Override
    protected Class<? extends CorePncRegisterActivity> getPncRegisterActivityClass() {
        return PncRegisterActivity.class;
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        if (((ChwApplication) ChwApplication.getInstance()).hasReferrals()) {
            addPncReferralTypes();
        }
        notificationAndReferralRecyclerView.setAdapter(notificationListAdapter);
        notificationListAdapter.setOnClickListener(this);
    }

//    @Override
//    public void setUpComingServicesStatus(String service, AlertStatus status, Date date) {
//        findViewById(R.id.rlUpcomingServices).setVisibility(GONE);
//    }
//
//    @Override
//    public void setFamilyStatus(AlertStatus status) {
//        findViewById(R.id.view_family_row).setVisibility(GONE);
//    }

    @Override
    public void registerPresenter() {
        presenter = new PncMemberProfilePresenter(this, new PncMemberProfileInteractor(this), memberObject);
    }

    @Override
    public void initializeFloatingMenu() {
        baseAncFloatingMenu = new AncFloatingMenu(this, getAncWomanName(),
                memberObject.getPhoneNumber(), getFamilyHeadName(), getFamilyHeadPhoneNumber(), getProfileType());

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.anc_fab:
                    redrawFabWithNoPhone();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((AncFloatingMenu) baseAncFloatingMenu).launchCallWidget();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    pncMemberProfilePresenter().referToFacility();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }
        };

        ((AncFloatingMenu) baseAncFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
        baseAncFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        addContentView(baseAncFloatingMenu, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void openVisitMonthView() {
        return;
    }

    @Override
    public void openUpcomingService() {
        PncUpcomingServicesActivity.startMe(this, memberObject);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        flavor.onCreateOptionsMenu(menu, memberObject.getBaseEntityId());
        if (ChwApplication.getApplicationFlavor().hasHIVST()) {
            int age = memberObject.getAge();
            menu.findItem(R.id.action_hivst_registration).setVisible(!HivstDao.isRegisteredForHivst(memberObject.getBaseEntityId()) && age >= 15);
        }
        if(ChwApplication.getApplicationFlavor().hasKvp()){
            menu.findItem(R.id.action_kvp_prep_registration).setVisible(!KvpDao.isRegisteredForKvpPrEP(baseEntityID));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == org.smartregister.chw.core.R.id.action_pnc_member_registration) {
            if (UpdateDetailsUtil.isIndependentClient(baseEntityID)) {
                startFormForEdit(org.smartregister.chw.core.R.string.registration_info,
                        CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            } else {
                startFormForEdit(org.smartregister.chw.core.R.string.edit_member_form_title,
                        CoreConstants.JSON_FORM.getFamilyMemberRegister());
            }
            return true;
        }
        if (itemId == R.id.action_hivst_registration) {
            CommonPersonObjectClient commonPersonObjectClient = getCommonPersonObjectClient(memberObject.getBaseEntityId());
            String gender = Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.GENDER, false);
            HivstRegisterActivity.startHivstRegistrationActivity(this, baseEntityID, gender);
        }
        if(itemId == R.id.action_kvp_prep_registration){
            String gender = getClientGender(baseEntityID);
            int age = memberObject.getAge();
            KvpPrEPRegisterActivity.startRegistration(PncMemberProfileActivity.this, baseEntityID, gender, age);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startFormForEdit(Integer title_resource, String formName) {
        try {
            JSONObject form = CoreJsonFormUtils.getAncPncForm(title_resource, formName, memberObject, this);
            startActivityForResult(CoreJsonFormUtils.getAncPncStartFormIntent(form, this), JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        handleNotificationRowClick(this, view, notificationListAdapter, baseEntityID);
        switch (view.getId()) {
            case R.id.textview_record_visit:
            case R.id.textview_record_reccuring_visit:
                PncHomeVisitActivity.startMe(this, memberObject, false);
                break;
            case R.id.textview_edit:
                PncHomeVisitActivity.startMe(this, memberObject, true);
                break;
            default:
                break;
        }
    }

    public PncMemberProfileContract.Presenter pncMemberProfilePresenter() {
        return new PncMemberProfilePresenter(this, new PncMemberProfileInteractor(this), memberObject);
    }

    private CommonPersonObjectClient clientObject() {
        CommonRepository commonRepository = org.smartregister.chw.util.Utils.context().commonrepository(org.smartregister.chw.util.Utils.metadata().familyMemberRegister.tableName);
        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client =
                new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        return client;
    }

    private PncVisitAlertRule getVisitDetails() {
        return ((PncMemberProfileInteractor) pncMemberProfileInteractor).getVisitSummary(memberObject.getBaseEntityId());
    }

    private void setEditViews(boolean enable, boolean within24Hours, Long longDate) {
        if (enable) {
            if (within24Hours) {
                Calendar cal = Calendar.getInstance();
                int offset = cal.getTimeZone().getOffset(cal.getTimeInMillis());
                new Date(longDate - (long) offset);
                String pncDay = pncMemberProfileInteractor.getPncDay(memberObject.getBaseEntityId());
                layoutNotRecordView.setVisibility(View.VISIBLE);
                tvEdit.setVisibility(View.VISIBLE);
                textViewUndo.setVisibility(View.GONE);
                textViewNotVisitMonth.setVisibility(View.VISIBLE);
                textViewNotVisitMonth.setText(MessageFormat.format(getContext().getString(R.string.pnc_visit_done), pncDay));
                imageViewCross.setImageResource(R.drawable.activityrow_visited);
                textview_record_visit.setVisibility(View.GONE);
            } else {
                layoutNotRecordView.setVisibility(View.GONE);

            }
        } else {
            layoutNotRecordView.setVisibility(View.GONE);
        }
    }

    @Override
    public void openMedicalHistory() {
        PncMedicalHistoryActivity.startMe(this, memberObject);
    }

    private void redrawFabWithNoPhone() {
        ((AncFloatingMenu) baseAncFloatingMenu).redraw(!StringUtils.isBlank(memberObject.getPhoneNumber())
                || !StringUtils.isBlank(getFamilyHeadPhoneNumber()));
    }

    @Override
    public void startFormActivity(JSONObject formJson) {
        startActivityForResult(CoreJsonFormUtils.getJsonIntent(this, formJson, Utils.metadata().familyMemberFormActivity),
                JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    private void addPncReferralTypes() {
        referralTypeModels.add(new ReferralTypeModel(getString(R.string.pnc_referral),
                BuildConfig.USE_UNIFIED_REFERRAL_APPROACH ? JSON_FORM.getPncUnifiedReferralForm() : JSON_FORM.getPncReferralForm(), CoreConstants.TASKS_FOCUS.PNC_DANGER_SIGNS));

        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral),
                    CoreConstants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    @Override
    protected void startMalariaRegister() {
        MalariaRegisterActivity.startMalariaRegistrationActivity(this, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
    }

    @Override
    protected void startFpRegister() {
        FpRegisterActivity.startFpRegistrationActivity(this, memberObject.getBaseEntityId(), memberObject.getDob(), CoreConstants.JSON_FORM.getFpRegistrationForm("Female"), FamilyPlanningConstants.ActivityPayload.REGISTRATION_PAYLOAD_TYPE);
    }

    @Override
    protected void startHivRegister() {
        CommonPersonObjectClient client = getCommonPersonObjectClient(memberObject.getBaseEntityId());
        String gender = org.smartregister.chw.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.GENDER, false);
        String dob = org.smartregister.chw.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = org.smartregister.chw.util.Utils.getAgeFromDate(dob);

        try {
            String formName = org.smartregister.chw.util.Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(PncMemberProfileActivity.this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");
            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(PncMemberProfileActivity.this, memberObject.getBaseEntityId(), formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void startTbRegister() {
        try {
            TbRegisterActivity.startTbFormActivity(this, memberObject.getBaseEntityId(), JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void startFpChangeMethod() {
        FpRegisterActivity.startFpRegistrationActivity(this, memberObject.getBaseEntityId(), memberObject.getDob(), CoreConstants.JSON_FORM.getFpChangeMethodForm("female"), FamilyPlanningConstants.ActivityPayload.CHANGE_METHOD_PAYLOAD_TYPE);
    }

    @Override
    protected void startMalariaFollowUpVisit() {
        MalariaFollowUpVisitActivity.startMalariaFollowUpActivity(this, memberObject.getBaseEntityId());
    }

    @Override
    protected void startHfMalariaFollowupForm() {
        //Implements from super
    }

    @Override
    protected void getRemoveBabyMenuItem(MenuItem item) {
        for (CommonPersonObjectClient child : getChildren(memberObject)) {
            for (Map.Entry<String, String> entry : menuItemRemoveNames.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(item.getTitle().toString()) && entry.getValue().equalsIgnoreCase(child.entityId())) {
                    IndividualProfileRemoveActivity.startIndividualProfileActivity(PncMemberProfileActivity.this, child,
                            memberObject.getFamilyBaseEntityId()
                            , memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), ChildRegisterActivity.class.getCanonicalName());
                }
            }
        }
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    public static void closePncMemberVisits(String baseEntityId) {
        AllSharedPreferences sharedPreferences = getAllSharedPreferences();
        Event baseEvent = (Event) new Event()
                .withBaseEntityId(baseEntityId)
                .withEventDate(new Date())
                .withEventType(CLOSE_PNC_VISITS)
                .withFormSubmissionId(org.smartregister.util.JsonFormUtils.generateRandomUUIDString())
                .withEntityType(CoreConstants.TABLE_NAME.PNC_MEMBER)
                .withProviderId(sharedPreferences.fetchRegisteredANM())
                .withLocationId(ChwNotificationDao.getSyncLocationId(baseEntityId))
                .withTeamId(sharedPreferences.fetchDefaultTeamId(sharedPreferences.fetchRegisteredANM()))
                .withTeam(sharedPreferences.fetchDefaultTeam(sharedPreferences.fetchRegisteredANM()))
                .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION)
                .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                .withDateCreated(new Date());
        try {
            org.smartregister.chw.util.JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), baseEvent);
        } catch (Exception e) {
            Timber.e(e);
        }
        try {
            NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(baseEvent)));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public interface Flavor {
        Boolean onCreateOptionsMenu(@Nullable Menu menu, @Nullable String baseEntityId);
    }
}