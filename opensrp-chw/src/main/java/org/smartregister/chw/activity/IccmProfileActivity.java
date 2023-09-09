package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient;
import static org.smartregister.chw.core.utils.Utils.isMemberOfReproductiveAge;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID;
import static org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.ICCM_SERVICES_VISIT;
import static org.smartregister.chw.util.Constants.ICCM_REFERRAL_FORM;
import static org.smartregister.chw.util.NotificationsUtil.handleNotificationRowClick;
import static org.smartregister.chw.util.NotificationsUtil.handleReceivedNotifications;
import static org.smartregister.util.Utils.getAgeFromDate;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.contract.MalariaProfileContract;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreMalariaProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.custom_views.CoreMalariaFloatingMenu;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.interactor.CoreMalariaProfileInteractor;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.custom_view.MalariaFloatingMenu;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.malaria.MalariaLibrary;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.domain.Visit;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.chw.presenter.IccmProfilePresenter;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.IccmVisitUtils;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.AlertStatus;
import org.smartregister.family.model.BaseFamilyOtherMemberProfileActivityModel;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.util.FormUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class IccmProfileActivity extends CoreMalariaProfileActivity implements MalariaProfileContract.View {

    private static String baseEntityId;

    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();

    private final NotificationListAdapter notificationListAdapter = new NotificationListAdapter();

    private FormUtils formUtils;

    private RelativeLayout processVisitLayout;

    public static void startMalariaActivity(Activity activity, String baseEntityId) {
        IccmProfileActivity.baseEntityId = baseEntityId;
        Intent intent = new Intent(activity, IccmProfileActivity.class);
        intent.putExtra(BASE_ENTITY_ID, baseEntityId);
        passToolbarTitle(activity, intent);
        activity.startActivity(intent);
    }

    private List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
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
        setupViews();
        refreshMedicalHistory(true);
        notificationListAdapter.canOpen = true;
        ChwNotificationUtil.retrieveNotifications(ChwApplication.getApplicationFlavor().hasReferrals(), baseEntityId, this);
    }

    @Override
    protected void initializePresenter() {
        String baseEntityId = getIntent().getStringExtra(BASE_ENTITY_ID);
        memberObject = IccmDao.getMember(baseEntityId);
        showProgressBar(true);
        profilePresenter = new IccmProfilePresenter(this, new CoreMalariaProfileInteractor(), memberObject);
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        if (((ChwApplication) ChwApplication.getInstance()).hasReferrals()) {
            addIccmReferralTypes();
        }
    }

    private void addIccmReferralTypes() {
        getReferralTypeModels().add(new ReferralTypeModel(getString(R.string.iccm_referral), ICCM_REFERRAL_FORM, CoreConstants.TASKS_FOCUS.ICCM_REFERRAL));
    }

    @Override
    public void referToFacility() {
        if (getReferralTypeModels().size() == 1) {
            try {
                if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
                    JSONObject formJson = new FormUtils(IccmProfileActivity.this).getFormJson(ICCM_REFERRAL_FORM);
                    formJson.put(Constants.REFERRAL_TASK_FOCUS, referralTypeModels.get(0).getFocus());


                    CommonPersonObjectClient commonPersonObjectClient = getCommonPersonObjectClient(memberObject.getBaseEntityId());
                    boolean isFemaleOfReproductiveAge = isMemberOfReproductiveAge(commonPersonObjectClient, 10, 49) && Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.GENDER, false).equalsIgnoreCase("Female");

                    JSONArray steps = formJson.getJSONArray("steps");
                    JSONObject step = steps.getJSONObject(0);
                    JSONArray referralFormFields = step.getJSONArray("fields");

                    int age = getAgeFromDate(memberObject.getAge());
                    boolean removePneumoniaAndDiarrheSigns = age > 5;
                    boolean removeRectalArtesunate = age > 6;

                    updateProblemsAndServicesBeforeReferral(referralFormFields, removePneumoniaAndDiarrheSigns, removeRectalArtesunate, isFemaleOfReproductiveAge);

                    ReferralRegistrationActivity.startGeneralReferralFormActivityForResults(this, memberObject.getBaseEntityId(), formJson, false);
                } else {
                    startFormActivity(getFormUtils().getFormJson(getReferralTypeModels().get(0).getFormName()));
                }
            } catch (Exception ex) {
                Timber.e(ex);
            }
        } else {
            Utils.launchClientReferralActivity(this, getReferralTypeModels(), memberObject.getBaseEntityId());
        }
    }


    private void updateProblemsAndServicesBeforeReferral(JSONArray fields, boolean removePneumoniaAndDiarrheSigns, boolean removeRectalArtesunate, boolean isFemaleOfReproductiveAge) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field.getString("name").equals("problem")) {
                JSONArray options = field.getJSONArray("options");
                if (!isFemaleOfReproductiveAge) {
                    options.remove(options.length() - 1);
                }

                if (removePneumoniaAndDiarrheSigns) {
                    for (int j = options.length() - 1; j >= 0; j--) {
                        JSONObject option = options.getJSONObject(j);
                        if (option.getString("name").equalsIgnoreCase("sever_pneumonia") ||
                                option.getString("name").equalsIgnoreCase("diarrhea_with_signs_of_dehydration")) {
                            options.remove(j);
                        }
                    }
                }

            } else if (field.getString("name").equals("service_before_referral")) {
                JSONArray options = field.getJSONArray("options");
                if (removePneumoniaAndDiarrheSigns) {
                    for (int j = options.length() - 1; j >= 0; j--) {
                        JSONObject option = options.getJSONObject(j);
                        if (option.getString("name").equalsIgnoreCase("ors") ||
                                option.getString("name").equalsIgnoreCase("ors_zinc_co_pack")) {
                            options.remove(j);
                        }

                    }
                }

                if (removeRectalArtesunate) {
                    for (int j = options.length() - 1; j >= 0; j--) {
                        JSONObject option = options.getJSONObject(j);
                        if (option.getString("name").equalsIgnoreCase("rectal_artesunate")) {
                            options.remove(j);
                        }
                    }
                }
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_registration:
                if (UpdateDetailsUtil.isIndependentClient(memberObject.getBaseEntityId())) {
                    startFormForEdit(org.smartregister.chw.core.R.string.registration_info,
                            CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
                } else {
                    startFormForEdit(org.smartregister.chw.core.R.string.edit_member_form_title,
                            CoreConstants.JSON_FORM.getFamilyMemberRegister());
                }
                return true;
            case R.id.action_remove_member:
                IndividualProfileRemoveActivity.startIndividualProfileActivity(IccmProfileActivity.this, getClientDetailsByBaseEntityID(memberObject.getBaseEntityId()), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), MalariaRegisterActivity.class.getCanonicalName());
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
        IndividualProfileRemoveActivity.startIndividualProfileActivity(this, getClientDetailsByBaseEntityID(memberObject.getBaseEntityId()), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), FpRegisterActivity.class.getCanonicalName());
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
            IccmServicesActivity.startIccmServicesActivity(this, ((IccmMemberObject) memberObject).getIccmEnrollmentFormSubmissionId(), false);
        } else if (id == R.id.textview_edit || id == R.id.textview_undo) {
            Visit lastVisit = getVisit(ICCM_SERVICES_VISIT);
            if (lastVisit != null) {
                IccmServicesActivity.startIccmServicesActivity(this, ((IccmMemberObject) memberObject).getIccmEnrollmentFormSubmissionId(), true);
            }
        }
        handleNotificationRowClick(this, view, notificationListAdapter, baseEntityId);
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
    public void updateHasPhone(boolean hasPhone) {
        //implement
    }

    @Override
    public void setFamilyServiceStatus(String status) {
        //implement
    }

    @Override
    public void refreshFamilyStatus(AlertStatus status) {
        //implement
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void openUpcomingService() {
        executeOnLoaded(memberType -> MalariaUpcomingServicesActivity.startMe(IccmProfileActivity.this, memberType.getMemberObject()));
    }

    @Override
    public void openFamilyDueServices() {
        //implement
    }

    @Override
    public void openMedicalHistory() {
        IccmMedicalHistoryActivity.startMe(this, (IccmMemberObject) memberObject);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        showProgressBar(false);
        Visit lastIccmVisit = getVisit(ICCM_SERVICES_VISIT);
        if (lastIccmVisit != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.vViewHistory)).setText(R.string.visits_history_profile_title);
            ((TextView) findViewById(R.id.ivViewHistoryArrow)).setText(getString(R.string.view_visits_history));
        } else {
            rlLastVisit.setVisibility(View.GONE);
        }
    }

    private Visit getVisit(String eventType) {
        return MalariaLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return FamilyProfileActivity.class;
    }

    @Override
    public void verifyHasPhone() {
        //TODO implement check if has phone number
    }

    @Override
    public void notifyHasPhone(boolean b) {
        //TODO notify if it has phone number
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
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseMalariaFloatingMenu, linearLayoutParams);
    }

    @Override
    public void onReceivedNotifications(List<Pair<String, String>> notifications) {
        handleReceivedNotifications(this, notifications, notificationListAdapter);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        ((TextView) findViewById(R.id.toolbar_title)).setText(R.string.return_to_previous_iccm_page);
        textViewRecordMalaria.setText(R.string.record_iccm_services);

        Visit lastIccmVisit = getVisit(ICCM_SERVICES_VISIT);
        if (lastIccmVisit != null) {
            checkVisitStatus(lastIccmVisit);
        }

    }

    @Override
    public void setProfileViewWithData() {
        super.setProfileViewWithData();
        findViewById(R.id.family_malaria_head).setVisibility(View.GONE);
        findViewById(R.id.primary_malaria_caregiver).setVisibility(View.GONE);

        String clientAge = (org.smartregister.chw.core.utils.Utils.getTranslatedDate(org.smartregister.chw.core.utils.Utils.getDuration(memberObject.getAge()), getBaseContext()));
        textViewName.setText(String.format("%s %s %s, %s", memberObject.getFirstName(),
                memberObject.getMiddleName(), memberObject.getLastName(), clientAge));
    }

    private void checkVisitStatus(Visit visit) {
        processVisitLayout = findViewById(R.id.rlProcessVisitBtn);
        processVisitLayout.setVisibility(View.GONE);
        boolean visitDone = visit.getProcessed();
        boolean formsCompleted = IccmVisitUtils.isIccmVisitComplete(visit);
        if (!visitDone) {
            showVisitInProgress();
            if (formsCompleted) {
                showCompleteVisit(visit);
            }
        } else {
            Date updatedAtDate = new Date(visit.getDate().getTime());
            //            int daysDiff = TimeUtils.getElapsedDays(updatedAtDate);

            Calendar today = Calendar.getInstance();
            Calendar updatedAt = Calendar.getInstance();
            updatedAt.setTime(updatedAtDate);

            // Compare the date parts of the two Calendar instances
            boolean isToday = today.get(Calendar.YEAR) == updatedAt.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == updatedAt.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) == updatedAt.get(Calendar.DAY_OF_MONTH);


            if (isToday) {
                hideView();
            }
            textViewVisitDoneEdit.setVisibility(View.GONE);
            visitStatus.setVisibility(View.GONE);
        }
    }

    private void showVisitInProgress() {
        textViewRecordMalaria.setVisibility(View.GONE);
        textViewVisitDoneEdit.setVisibility(View.VISIBLE);
        visitStatus.setVisibility(View.VISIBLE);
        textviewNotVisitThisMonth.setText(getContext().getString(R.string.visit_in_progress, "iCCM"));
        imageViewCross.setImageResource(R.drawable.activityrow_visit_in_progress);
    }


    private void showCompleteVisit(Visit visit) {
        TextView processVisitBtn = findViewById(R.id.textview_process_visit);
        processVisitBtn.setOnClickListener(v -> {
            try {
                IccmVisitUtils.manualProcessVisit(visit);
                if (!memberObject.getBaseEntityId().isEmpty()) {
                    memberObject = IccmDao.getMember(baseEntityId);
                }
                //reload views after visit is processed
                setupViews();
            } catch (Exception e) {
                Timber.e(e);
            }
        });
        processVisitLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void startFormForEdit(Integer title_resource, String formName) {
        try {
            JSONObject form = null;
            boolean isPrimaryCareGiver = memberObject.getPrimaryCareGiver().equals(memberObject.getBaseEntityId());
            String titleString = title_resource != null ? getResources().getString(title_resource) : null;

            if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {

                String eventName = Utils.metadata().familyMemberRegister.updateEventType;

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new FamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString, eventName, memberObject.getUniqueId()));

                form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getFamilyMemberRegister());
            } else if (formName.equals(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
                String eventName = Utils.metadata().familyMemberRegister.updateEventType;
                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new FamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString, eventName, memberObject.getUniqueId()));

                form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            }

            startActivityForResult(org.smartregister.chw.util.JsonFormUtils.getAncPncStartFormIntent(form, this), JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}