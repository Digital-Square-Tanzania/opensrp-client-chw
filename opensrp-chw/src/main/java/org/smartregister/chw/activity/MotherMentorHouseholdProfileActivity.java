package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreMotherMentorProfileActivity;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.repository.MotherMentorHouseholdClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorHouseholdClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorHouseholdClosureLabels;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitsUtil;
import org.smartregister.chw.mothermentor.util.MotherMentorHouseholdVisitUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.Utils;

import timber.log.Timber;

import java.util.Calendar;
import java.util.Date;

public class MotherMentorHouseholdProfileActivity extends CoreMotherMentorProfileActivity {
    private final Handler visitAvailabilityHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshVisitAvailability =
            () -> {
                if (memberObject != null && !isFinishing()) {
                    setupButtons();
                    scheduleVisitAvailabilityRefresh();
                }
            };
    private final BroadcastReceiver visitDateChangedReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    refreshVisitAvailability.run();
                }
            };


    public static void startMe(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, MotherMentorHouseholdProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(
                Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE,
                Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return null;
        }
        MemberObject memberObject = getHouseholdMember(baseEntityId);
        return memberObject != null ? memberObject : MotherMentorDao.getContact(baseEntityId);
    }

    private MemberObject getHouseholdMember(String baseEntityId) {
        try {
            CommonRepository repository =
                    Utils.context().commonrepository(Utils.metadata().familyRegister.tableName);
            if (repository == null) {
                return null;
            }

            CommonPersonObject personObject = repository.findByBaseEntityId(baseEntityId);
            if (personObject != null) {
                MemberObject memberObject = new MemberObject();
                memberObject.setBaseEntityId(personObject.getCaseId());
                memberObject.setFamilyBaseEntityId(personObject.getCaseId());
                memberObject.setFirstName(
                        Utils.getValue(
                                personObject.getColumnmaps(), DBConstants.KEY.FIRST_NAME, false));
                memberObject.setLastName(
                        Utils.getValue(
                                personObject.getColumnmaps(), DBConstants.KEY.LAST_NAME, false));
                memberObject.setUniqueId(
                        Utils.getValue(
                                personObject.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false));
                memberObject.setAddress(
                        Utils.getValue(
                                personObject.getColumnmaps(), DBConstants.KEY.VILLAGE_TOWN, false));
                memberObject.setFamilyHead(
                        Utils.getValue(
                                personObject.getColumnmaps(), DBConstants.KEY.FAMILY_HEAD, false));
                memberObject.setPrimaryCareGiver(
                        Utils.getValue(
                                personObject.getColumnmaps(),
                                DBConstants.KEY.PRIMARY_CARE_GIVER,
                                false));
                memberObject.setPhoneNumber(
                        Utils.getValue(
                                personObject.getColumnmaps(), DBConstants.KEY.PHONE_NUMBER, false));
                return memberObject;
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to load Mother Mentor household profile");
        }
        return null;
    }

    @Override
    protected void setupButtons() {
        super.setupButtons();
        MotherMentorHouseholdClosure closure = getHouseholdClosureState();
        if (textViewRecordMotherMentor != null) {
            textViewRecordMotherMentor.setVisibility(View.VISIBLE);
            textViewRecordMotherMentor.setText(R.string.mothermentor_household_record);
            boolean canStartVisit = !closure.isClosed() && !hasCompletedHouseholdVisitToday();
            textViewRecordMotherMentor.setEnabled(canStartVisit);
            textViewRecordMotherMentor.setAlpha(canStartVisit ? 1f : 0.5f);
        }
        updateClosureBanner(closure);
        enforceProcessVisitVisibility();
        refreshMedicalHistory(true);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.textview_record_mothermentor) {
            openRecordClientVisit();
            return;
        }
        super.onClick(view);
    }

    @Override
    public void setProfileViewWithData() {
        if (memberObject == null) {
            return;
        }

        String name = Utils.getName(memberObject.getFirstName(), memberObject.getLastName());
        textViewName.setText(TextUtils.isEmpty(name) ? getString(R.string.mothermentor) : name);
        textViewGender.setText("");
        textViewLocation.setText(memberObject.getAddress());
        textViewUniqueID.setText(memberObject.getUniqueId());
        findViewById(R.id.primary_mothermentor_caregiver).setVisibility(View.GONE);
        findViewById(R.id.family_mothermentor_head).setVisibility(View.GONE);
    }

    @Override
    public void openRecordClientVisit() {
        // Re-read saved visits so a stale profile cannot start a duplicate on the same day.
        if (getHouseholdClosureState().isClosed() || hasCompletedHouseholdVisitToday()) {
            setupButtons();
            return;
        }
        if (isVisitOnProgress(getServiceVisit())) {
            continueService();
            return;
        }
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(
                this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openFollowupVisit() {
        openRecordClientVisit();
    }

    @Override
    public void openRecordTbContactVisit() {
        openRecordClientVisit();
    }

    @Override
    public void openClientObservationResults() {
        // Individual client observation results are not available on household profiles.
    }

    @Override
    public void observationResults() {
        // Individual client observation results are not available on household profiles.
    }

    @Override
    public void openObservationResults() {
        // Individual client observation results are not available on household profiles.
    }

    @Override
    public void startServiceForm() {
        // Household visits are started through openRecordClientVisit().
    }

    @Override
    public void continueService() {
        if (getHouseholdClosureState().isClosed()) {
            setupButtons();
            return;
        }
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(
                this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueContactVisit() {
        // Household visits are resumed through continueService().
    }

    @Override
    public void openTbContactFollowUpVisit() {
        openRecordClientVisit();
    }

    @Override
    public void openMedicalHistory() {
        MotherMentorMedicalHistoryActivity.startMe(this, memberObject);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        boolean showHistory = hasProcessedHouseholdVisitHistory();
        if (showHistory) {
            rlLastVisit.setVisibility(View.VISIBLE);
            view_last_visit_row.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
            ((TextView) findViewById(org.smartregister.chw.mothermentor.R.id.ivViewHistoryArrow))
                    .setText(getString(R.string.view_visits_history));
            rlLastVisit.setOnClickListener(view -> openMedicalHistory());
            ivViewHistoryArrow.setOnClickListener(view -> openMedicalHistory());
        } else {
            rlLastVisit.setVisibility(View.GONE);
            view_last_visit_row.setVisibility(View.GONE);
        }
    }

    @Override
    public void startHivstRegistration() {
        // HIV self-testing registration is not available from household profiles.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
        memberObject =
                getMemberObject(
                        getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID));
        IntentFilter dateChanges = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        dateChanges.addAction(Intent.ACTION_TIME_CHANGED);
        dateChanges.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(visitDateChangedReceiver, dateChanges);
        refreshVisitAvailability.run();
    }

    @Override
    protected void onPause() {
        visitAvailabilityHandler.removeCallbacks(refreshVisitAvailability);
        unregisterReceiver(visitDateChangedReceiver);
        super.onPause();
    }

    private void scheduleVisitAvailabilityRefresh() {
        visitAvailabilityHandler.removeCallbacks(refreshVisitAvailability);
        Date now = getVisitAvailabilityDate();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTime(now);
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        visitAvailabilityHandler.postDelayed(
                refreshVisitAvailability, tomorrow.getTimeInMillis() - now.getTime());
    }

    Date getVisitAvailabilityDate() {
        return new Date();
    }

    private boolean hasCompletedHouseholdVisitToday() {
        return memberObject != null
                && MotherMentorDao.hasCompletedHouseholdVisitToday(
                        memberObject.getBaseEntityId(), getVisitAvailabilityDate());
    }

    private void enforceProcessVisitVisibility() {
        if (getHouseholdClosureState().isClosed()) {
            if (manualProcessVisit != null) manualProcessVisit.setVisibility(View.GONE);
            if (mothermentorServiceInProgress != null) mothermentorServiceInProgress.setVisibility(View.GONE);
            if (textViewContinueMotherMentorService != null) textViewContinueMotherMentorService.setVisibility(View.GONE);
            if (visitInProgress != null) visitInProgress.setVisibility(View.GONE);
            if (textViewVisitDoneEdit != null) textViewVisitDoneEdit.setVisibility(View.GONE);
            return;
        }
        Visit latestVisit = getServiceVisit();
        if (latestVisit == null) {
            manualProcessVisit.setVisibility(View.GONE);
            mothermentorServiceInProgress.setVisibility(View.GONE);
            if (textViewContinueMotherMentorService != null) {
                textViewContinueMotherMentorService.setVisibility(View.GONE);
            }
            return;
        }

        if (!Boolean.TRUE.equals(latestVisit.getProcessed())) {
            textViewRecordMotherMentor.setVisibility(View.GONE);
            mothermentorServiceInProgress.setVisibility(View.VISIBLE);
            if (textViewContinueMotherMentorService != null) {
                textViewContinueMotherMentorService.setVisibility(View.VISIBLE);
            }
        } else {
            mothermentorServiceInProgress.setVisibility(View.GONE);
            if (textViewContinueMotherMentorService != null) {
                textViewContinueMotherMentorService.setVisibility(View.GONE);
            }
        }

        boolean shouldShowProcessVisit =
                !Boolean.TRUE.equals(latestVisit.getProcessed())
                        && hasCompletedHouseholdServiceSections(latestVisit);

        manualProcessVisit.setVisibility(shouldShowProcessVisit ? View.VISIBLE : View.GONE);
        if (shouldShowProcessVisit) {
            manualProcessVisit.setOnClickListener(
                    view -> {
                        if (getHouseholdClosureState().isClosed()) {
                            setupButtons();
                            return;
                        }
                        try {
                            MotherMentorVisitsUtil.manualProcessVisit(latestVisit);
                            Toast.makeText(
                                            this,
                                            R.string.mothermentor_visit_conducted,
                                            Toast.LENGTH_SHORT)
                                    .show();
                            setupViews();
                            refreshMedicalHistory(true);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    });
        }
    }

    protected Visit getServiceVisit() {
        Visit visit = MotherMentorLibrary.getInstance()
                .visitRepository()
                .getLatestVisit(
                        memberObject.getBaseEntityId(),
                        Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        return visit != null && isCurrentHouseholdVisit(visit) ? visit : null;
    }

    protected boolean isCurrentHouseholdVisit(Visit visit) {
        return MotherMentorHouseholdClosureRepository.belongsToCurrentEnrollment(
                ChwApplication.getInstance().getRepository().getReadableDatabase(),
                memberObject.getBaseEntityId(), visit.getJson());
    }

    protected MotherMentorHouseholdClosure getHouseholdClosureState() {
        return memberObject == null ? MotherMentorHouseholdClosure.active()
                : MotherMentorHouseholdClosureRepository.getState(
                        ChwApplication.getInstance().getRepository().getReadableDatabase(),
                        memberObject.getBaseEntityId(), null);
    }

    private void updateClosureBanner(MotherMentorHouseholdClosure closure) {
        TextView banner = findViewById(
                org.smartregister.chw.mothermentor.R.id.mothermentor_household_closed_banner);
        if (banner == null) return;
        banner.setVisibility(closure.isClosed() ? View.VISIBLE : View.GONE);
        if (closure.isClosed()) {
            banner.setText(getString(
                    org.smartregister.chw.mothermentor.R.string.mothermentor_household_closed_banner,
                    MotherMentorHouseholdClosureLabels.reason(this, closure.getClosureReason()),
                    closure.getClosedDate()));
        }
    }

    protected boolean isVisitOnProgress(Visit visit) {
        return visit != null && !Boolean.TRUE.equals(visit.getProcessed());
    }

    private boolean hasProcessedHouseholdVisitHistory() {
        try {
            return !MotherMentorLibrary.getInstance()
                    .visitRepository()
                    .getAllVisitsProcessed(
                            Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES,
                            memberObject.getBaseEntityId())
                    .isEmpty();
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    private boolean hasCompletedHouseholdServiceSections(Visit visit) {
        if (visit == null || TextUtils.isEmpty(visit.getJson())) return false;
        try {
            return MotherMentorHouseholdVisitUtils.isComplete(
                    MotherMentorHouseholdVisitUtils.observationValues(
                            new JSONObject(visit.getJson()).optJSONArray("obs")));
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }
}
