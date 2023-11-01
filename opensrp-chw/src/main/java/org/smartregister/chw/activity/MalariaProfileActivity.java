package org.smartregister.chw.activity;

import static org.smartregister.chw.anc.AncLibrary.getInstance;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID;
import static org.smartregister.chw.util.Constants.Events.MALARIA_CONFIRMATION;
import static org.smartregister.chw.util.Constants.Events.UPDATE_MALARIA_CONFIGURATION;
import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.api.Rules;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.contract.MalariaProfileContract;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreMalariaProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.custom_views.CoreMalariaFloatingMenu;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.interactor.CoreMalariaProfileInteractor;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.rule.MalariaFollowUpRule;
import org.smartregister.chw.core.rule.PncVisitAlertRule;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.HomeVisitUtil;
import org.smartregister.chw.core.utils.MalariaVisitUtil;
import org.smartregister.chw.core.utils.VisitSummary;
import org.smartregister.chw.custom_view.MalariaFloatingMenu;
import org.smartregister.chw.dataloader.MalariaMemberDataLoader;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.chw.malaria.domain.MemberObject;
import org.smartregister.chw.malaria.presenter.BaseMalariaProfilePresenter;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.pnc.PncLibrary;
import org.smartregister.chw.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.model.BaseFamilyOtherMemberProfileActivityModel;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.util.FormUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class MalariaProfileActivity extends CoreMalariaProfileActivity implements MalariaProfileContract.View {

    private static String baseEntityId;
    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
    private static final String ANC = "anc";
    private static final String PNC = "pnc";
    private FormUtils formUtils;
    private NotificationListAdapter notificationListAdapter = new NotificationListAdapter();

    private List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    public static void startMalariaActivity(Activity activity, String baseEntityId) {
        MalariaProfileActivity.baseEntityId = baseEntityId;
        Intent intent = new Intent(activity, MalariaProfileActivity.class);
        intent.putExtra(BASE_ENTITY_ID, baseEntityId);
        passToolbarTitle(activity, intent);
        activity.startActivity(intent);
    }

    private FormUtils getFormUtils() throws Exception {
        if (formUtils == null) {
            formUtils = FormUtils.getInstance(ChwApplication.getInstance());
        }
        return formUtils;
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
                baseEntityId, this);
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        String baseEntityId = getIntent().getStringExtra(BASE_ENTITY_ID);
        memberObject = MalariaDao.getMember(baseEntityId);
        profilePresenter = new BaseMalariaProfilePresenter(this, new CoreMalariaProfileInteractor(), memberObject);
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        org.smartregister.util.Utils.startAsyncTask(new UpdateVisitDueTask(), null);
        this.setOnMemberTypeLoadedListener(memberType -> {
            switch (memberType.getMemberType()) {
                case CoreConstants.TABLE_NAME.ANC_MEMBER:
                    AncMedicalHistoryActivity.startMe(MalariaProfileActivity.this, memberType.getMemberObject());
                    break;
                case CoreConstants.TABLE_NAME.PNC_MEMBER:
                    PncMedicalHistoryActivity.startMe(MalariaProfileActivity.this, memberType.getMemberObject());
                    break;
                case CoreConstants.TABLE_NAME.CHILD:
                    ChildMedicalHistoryActivity.startMe(MalariaProfileActivity.this, memberType.getMemberObject());
                    break;
                default:
                    Timber.v("Member info undefined");
                    break;
            }
        });
        if (((ChwApplication) ChwApplication.getInstance()).hasReferrals()) {
            addMalariaReferralTypes();
        }
    }

    private void addMalariaReferralTypes() {
        getReferralTypeModels().add(
                new ReferralTypeModel(getString(R.string.suspected_malaria), BuildConfig.USE_UNIFIED_REFERRAL_APPROACH
                        ? CoreConstants.JSON_FORM.getMalariaReferralForm() : Constants.MALARIA_REFERRAL_FORM, CoreConstants.TASKS_FOCUS.SUSPECTED_MALARIA));
    }

    @Override
    public void referToFacility() {
        if (getReferralTypeModels().size() == 1) {
            try {
                if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
                    JSONObject formJson = getFormUtils().getFormJson(Constants.JSON_FORM.getMalariaReferralForm());
                    formJson.put(Constants.REFERRAL_TASK_FOCUS, referralTypeModels.get(0).getFocus());
                    ReferralRegistrationActivity.startGeneralReferralFormActivityForResults(this, baseEntityId, formJson, false);
                } else {
                    startFormActivity(getFormUtils().getFormJson(getReferralTypeModels().get(0).getFormName()));
                }
            } catch (Exception ex) {
                Timber.e(ex);
            }
        } else {
            Utils.launchClientReferralActivity(this, getReferralTypeModels(), baseEntityId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_registration:
                startFormForEdit(R.string.registration_info,
                        org.smartregister.chw.util.Constants.JSON_FORM.FAMILY_MEMBER_REGISTER);
                return true;
            case R.id.action_remove_member:
                IndividualProfileRemoveActivity.startIndividualProfileActivity(MalariaProfileActivity.this, getClientDetailsByBaseEntityID(memberObject.getBaseEntityId()), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), MalariaRegisterActivity.class.getCanonicalName());
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public FamilyOtherMemberActivityPresenter presenter() {
        return new FamilyOtherMemberActivityPresenter(this, new BaseFamilyOtherMemberProfileActivityModel(), null, memberObject.getRelationalId(), memberObject.getBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), memberObject.getAddress(), memberObject.getLastName());
    }

    @Override
    protected void removeMember() {
        IndividualProfileRemoveActivity.startIndividualProfileActivity(this,
                getClientDetailsByBaseEntityID(memberObject.getBaseEntityId()),
                memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(),
                memberObject.getPrimaryCareGiver(), FpRegisterActivity.class.getCanonicalName());
    }

    @Override
    public void setProfileImage(String s, String s1) {
        //implement
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.textview_record_malaria) {
            MalariaFollowUpVisitActivity.startMalariaFollowUpActivity(this, memberObject.getBaseEntityId());
        } else if (id == org.smartregister.malaria.R.id.textview_record_anc) {
            if (view.getTag() == ANC) {
                AncHomeVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
            } else if (view.getTag() == PNC) {
                PncHomeVisitActivity.startMe(this, PNCDao.getMember(memberObject.getBaseEntityId()), false);
            }
        } else if (id == org.smartregister.malaria.R.id.textview_record_anc_not_done) {
            saveAncVisit(org.smartregister.chw.anc.util.Constants.EVENT_TYPE.ANC_HOME_VISIT_NOT_DONE);
            setAncVisitViewHidden();
        } else if (id == org.smartregister.malaria.R.id.textview_undo) {
            textViewRecordAnc.setVisibility(View.VISIBLE);
            textViewRecordAncNotDone.setVisibility(View.VISIBLE);
            visitStatus.setVisibility(View.GONE);
            saveAncVisit(org.smartregister.chw.anc.util.Constants.EVENT_TYPE.ANC_HOME_VISIT_NOT_DONE_UNDO);
        } else if (id == org.smartregister.malaria.R.id.textview_edit) {
            if (view.getTag() == PNC) {
                PncHomeVisitActivity.startMe(this, PNCDao.getMember(memberObject.getBaseEntityId()), true);
            } else if (view.getTag() == ANC) {
                AncHomeVisitActivity.startMe(this, memberObject.getBaseEntityId(), true);
            }
        } else if (id == org.smartregister.malaria.R.id.rlMalariaPositiveDate) {
            JSONObject form;
            NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
            binder.setDataLoader(new MalariaMemberDataLoader());
            form = binder.getPrePopulatedForm(MALARIA_CONFIRMATION);
            if (form != null) {
                try {
                    form.put(JsonFormUtils.ENCOUNTER_TYPE, UPDATE_MALARIA_CONFIGURATION);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            startActivityForResult(org.smartregister.chw.util.JsonFormUtils.getAncPncStartFormIntent(form, this), JsonFormUtils.REQUEST_CODE_GET_JSON);
        }
        handleNotificationRowClick(this, view, notificationListAdapter, baseEntityId);
    }

    private void saveAncVisit(String eventType) {
        try {
            Event event = org.smartregister.chw.anc.util.JsonFormUtils.createUntaggedEvent(memberObject.getBaseEntityId(), eventType, org.smartregister.chw.anc.util.Constants.TABLES.ANC_MEMBERS);
            Visit visit = NCUtils.eventToVisit(event, org.smartregister.chw.anc.util.JsonFormUtils.generateRandomUUIDString());
            visit.setPreProcessedJson(new Gson().toJson(event));
            getInstance().visitRepository().addVisit(visit);
        } catch (JSONException e) {
            Timber.e(e);
        }
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
    public void startFormForEdit(Integer title_resource, String formName) {

        JSONObject form = null;
        CommonPersonObjectClient client = org.smartregister.chw.core.utils.Utils.clientForEdit(memberObject.getBaseEntityId());

        if (formName.equals(org.smartregister.chw.util.Constants.JSON_FORM.getFamilyMemberRegister())) {
            form = org.smartregister.chw.util.JsonFormUtils.getAutoPopulatedJsonEditMemberFormString(
                    (title_resource != null) ? getResources().getString(title_resource) : null,
                    org.smartregister.chw.util.Constants.JSON_FORM.getFamilyMemberRegister(),
                    this, client,
                    Utils.metadata().familyMemberRegister.updateEventType, memberObject.getLastName(), false);
        } else if (formName.equals(org.smartregister.chw.util.Constants.JSON_FORM.getAncRegistration())) {
            form = org.smartregister.chw.util.JsonFormUtils.getAutoJsonEditAncFormString(
                    memberObject.getBaseEntityId(), this, formName, org.smartregister.chw.util.Constants.EventType.UPDATE_ANC_REGISTRATION, getResources().getString(title_resource));
        }

        try {
            assert form != null;
            startFormActivity(form);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void refreshList() {
        //implement
    }

    @Override
    public void updateHasPhone(boolean hasPhone) {
        //implement
    }

    @Override
    public void setFamilyServiceStatus(String status) {
        //implement
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void openUpcomingService() {
        executeOnLoaded(memberType -> MalariaUpcomingServicesActivity.startMe(MalariaProfileActivity.this, memberType.getMemberObject()));
    }

    @Override
    public void openFamilyDueServices() {
        Intent intent = new Intent(this, FamilyProfileActivity.class);
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, memberObject.getFamilyBaseEntityId());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, memberObject.getFamilyHead());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, memberObject.getPrimaryCareGiver());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME, memberObject.getFamilyName());

        intent.putExtra(CoreConstants.INTENT_KEY.SERVICE_DUE, true);
        startActivity(intent);
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return FamilyProfileActivity.class;
    }

    @Override
    public void verifyHasPhone() {
//        TODO implement check if has phone number
    }

    @Override
    public void notifyHasPhone(boolean b) {
//        TODO notify if it has phone number
    }

    private void checkPhoneNumberProvided(boolean hasPhoneNumber) {
        ((CoreMalariaFloatingMenu) baseMalariaFloatingMenu).redraw(hasPhoneNumber);
    }

    @Override
    public void initializeFloatingMenu() {
        baseMalariaFloatingMenu = new MalariaFloatingMenu(this, memberObject);
        checkPhoneNumberProvided(StringUtils.isNotBlank(memberObject.getPhoneNumber()));
        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.malaria_fab:
                    ((CoreMalariaFloatingMenu) baseMalariaFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((CoreMalariaFloatingMenu) baseMalariaFloatingMenu).launchCallWidget();
                    ((CoreMalariaFloatingMenu) baseMalariaFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    referToFacility();
                    ((CoreMalariaFloatingMenu) baseMalariaFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((CoreMalariaFloatingMenu) baseMalariaFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
        baseMalariaFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseMalariaFloatingMenu, linearLayoutParams);
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    private class UpdateVisitDueTask extends AsyncTask<Void, Void, Void> {
        private MalariaFollowUpRule malariaFollowUpRule;

        @Override
        protected Void doInBackground(Void... voids) {
            Date malariaTestDate = MalariaDao.getMalariaTestDate(memberObject.getBaseEntityId());
            Date followUpDate = MalariaDao.getMalariaFollowUpVisitDate(memberObject.getBaseEntityId());
            malariaFollowUpRule = MalariaVisitUtil.getMalariaStatus(malariaTestDate, followUpDate);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            profilePresenter.recordMalariaButton(malariaFollowUpRule.getButtonStatus());
        }
    }

    private Visit getVisit(String eventType) {
        return getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
    }

    private boolean ancHomeVisitNotDoneEvent(Visit visit) {
        return visit != null && (new DateTime(visit.getDate()).getMonthOfYear() == new DateTime().getMonthOfYear())
                && (new DateTime(visit.getDate()).getYear() == new DateTime().getYear());
    }

    @Override
    public void recordAnc(MemberObject memberObject) {
        if (AncDao.isANCMember(memberObject.getBaseEntityId())) {
            Visit lastAncHomeVisitNotDoneEvent = getVisit(org.smartregister.chw.anc.util.Constants.EVENT_TYPE.ANC_HOME_VISIT_NOT_DONE);
            Visit lastAncHomeVisitNotDoneUndoEvent = getVisit(org.smartregister.chw.anc.util.Constants.EVENT_TYPE.ANC_HOME_VISIT_NOT_DONE_UNDO);
            org.smartregister.chw.anc.domain.MemberObject ancMemberObject = AncDao.getMember(memberObject.getBaseEntityId());
            Rules rules = CoreChwApplication.getInstance().getRulesEngineHelper().rules(CoreConstants.RULE_FILE.ANC_HOME_VISIT);

            VisitSummary visitSummary = HomeVisitUtil.getAncVisitStatus(this, rules, ancMemberObject.getLastContactVisit(), null, new DateTime(ancMemberObject.getDateCreated()).toLocalDate());
            String visitSummaryStatus = visitSummary.getVisitStatus();

            if (visitSummaryStatus.equalsIgnoreCase(CoreConstants.VISIT_STATE.DUE) ||
                    visitSummaryStatus.equalsIgnoreCase(CoreConstants.VISIT_STATE.OVERDUE)) {
                textViewRecordAnc.setVisibility(View.VISIBLE);
                textViewRecordAnc.setTag(ANC);
                textViewRecordAncNotDone.setVisibility(View.VISIBLE);
                if (visitSummaryStatus.equalsIgnoreCase(CoreConstants.VISIT_STATE.OVERDUE)) {
                    textViewRecordAnc.setBackgroundResource(org.smartregister.chw.core.R.drawable.record_btn_selector_overdue);
                }
            }

            if (lastAncHomeVisitNotDoneUndoEvent != null && lastAncHomeVisitNotDoneEvent != null
                    && lastAncHomeVisitNotDoneUndoEvent.getDate().before(lastAncHomeVisitNotDoneEvent.getDate())
                    && ancHomeVisitNotDoneEvent(lastAncHomeVisitNotDoneEvent)) {
                setAncVisitViewHidden();
            } else if (lastAncHomeVisitNotDoneUndoEvent == null && ancHomeVisitNotDoneEvent(lastAncHomeVisitNotDoneEvent)) {
                setAncVisitViewHidden();
            }
//            when visit is done
            Visit visit = getVisit(CoreConstants.EventType.ANC_HOME_VISIT);
            if (visit != null) {
                setAncVisitViewHidden();
                visitDone.setVisibility(View.VISIBLE);
                if (VisitUtils.isVisitWithin24Hours(visit)) {
                    textViewVisitDoneEdit.setTag(ANC);
                    textViewVisitDoneEdit.setVisibility(View.VISIBLE);
                } else {
                    textViewVisitDoneEdit.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setAncVisitViewHidden() {
        textViewRecordAncNotDone.setVisibility(View.GONE);
        textViewRecordAnc.setVisibility(View.GONE);
        visitStatus.setVisibility(View.VISIBLE);
    }

    private Date getLastVisitDate(String baseId) {
        Visit lastVisit = getInstance().visitRepository().getLatestVisit(baseId, org.smartregister.chw.anc.util.Constants.EVENT_TYPE.PNC_HOME_VISIT);
        if (lastVisit != null) {
            return lastVisit.getDate();
        } else {
            return getDeliveryDate(baseId);
        }
    }

    private Date getDeliveryDate(String baseId) {
        Date deliveryDate = null;
        try {
            String deliveryDateString = PncLibrary.getInstance().profileRepository().getDeliveryDate(baseId);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            deliveryDate = sdf.parse(deliveryDateString);
        } catch (Exception e) {
            Timber.e(e);
        }
        return deliveryDate;
    }

    @Override
    public void recordPnc(MemberObject memberObject) {
        if (PNCDao.isPNCMember(memberObject.getBaseEntityId())) {
            org.smartregister.chw.anc.domain.MemberObject ancMemberObject = AncDao.getMember(memberObject.getBaseEntityId());

            Rules rules = CoreChwApplication.getInstance().getRulesEngineHelper().rules(CoreConstants.RULE_FILE.PNC_HOME_VISIT);

            PncVisitAlertRule pncVisitAlertRule = HomeVisitUtil.getPncVisitStatus(rules, getLastVisitDate(ancMemberObject.getBaseEntityId()), getDeliveryDate(ancMemberObject.getBaseEntityId()));

            DateTime lastVisitDate = getPncLastVisitDate(memberObject.getBaseEntityId());
            if (lastVisitDate != null) {
                int numOfDays = Days.daysBetween(lastVisitDate.toLocalDate(), new DateTime().toLocalDate()).getDays();
                if (numOfDays <= 1) {
                    visitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setTag(PNC);
                    textViewRecordAnc.setVisibility(View.GONE);
                } else {
                    setPncViews(pncVisitAlertRule);
                }
            } else {
                setPncViews(pncVisitAlertRule);
            }
        }
    }

    private void setPncViews(PncVisitAlertRule pncVisitAlertRule) {
        visitDone.setVisibility(View.GONE);

        if (pncVisitAlertRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.DUE) || pncVisitAlertRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.OVERDUE)) {
            textViewRecordAnc.setText(R.string.record_pnc_visit);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int topBottomMargin = getMarginPixel(10);
            int rightLeftMargin = getMarginPixel(30);

            layoutParams.setMargins(rightLeftMargin, topBottomMargin, rightLeftMargin, topBottomMargin);
            textViewRecordAnc.setLayoutParams(layoutParams);
            textViewRecordAnc.setTag(PNC);
            textViewRecordAnc.setVisibility(View.VISIBLE);
        }

        if (pncVisitAlertRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.OVERDUE)) {
            textViewRecordAnc.setBackgroundResource(org.smartregister.chw.core.R.drawable.record_btn_selector_overdue);
        }

        if (pncVisitAlertRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.VISIT_DONE)) {
            visitDone.setVisibility(View.VISIBLE);
            textViewVisitDoneEdit.setVisibility(View.GONE);
        }
    }

    private int getMarginPixel(int dp) {
        final float scale = context().applicationContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private DateTime getPncLastVisitDate(String motherBaseID) {
        Long lastVisitDate = PncLibrary.getInstance().profileRepository().getLastVisit(motherBaseID);
        if (lastVisitDate != null) {
            return new DateTime(lastVisitDate);
        }
        return null;
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        if (memberObject.getMalariaTestDate() != null) updateMalariaFormConfigurationView();
    }

    private void updateMalariaFormConfigurationView() {
        rlMalariaPositiveDate.setVisibility(View.VISIBLE);
    }
}