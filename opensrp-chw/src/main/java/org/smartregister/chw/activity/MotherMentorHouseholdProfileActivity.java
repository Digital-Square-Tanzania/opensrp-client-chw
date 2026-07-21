package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreMotherMentorProfileActivity;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitsUtil;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class MotherMentorHouseholdProfileActivity extends CoreMotherMentorProfileActivity {
    private static final Set<String> REQUIRED_HOUSEHOLD_SERVICE_FIELDS = new HashSet<>();

    static {
        REQUIRED_HOUSEHOLD_SERVICE_FIELDS.add("purpose_of_visit");
        REQUIRED_HOUSEHOLD_SERVICE_FIELDS.add("participants");
        REQUIRED_HOUSEHOLD_SERVICE_FIELDS.add("topics_taught");
        REQUIRED_HOUSEHOLD_SERVICE_FIELDS.add("comments");
    }

    public static void startMe(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, MotherMentorHouseholdProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
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
            CommonRepository repository = Utils.context().commonrepository(Utils.metadata().familyRegister.tableName);
            if (repository == null) {
                return null;
            }

            CommonPersonObject personObject = repository.findByBaseEntityId(baseEntityId);
            if (personObject != null) {
                MemberObject memberObject = new MemberObject();
                memberObject.setBaseEntityId(personObject.getCaseId());
                memberObject.setFamilyBaseEntityId(personObject.getCaseId());
                memberObject.setFirstName(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.FIRST_NAME, false));
                memberObject.setLastName(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.LAST_NAME, false));
                memberObject.setUniqueId(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.UNIQUE_ID, false));
                memberObject.setAddress(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.VILLAGE_TOWN, false));
                memberObject.setFamilyHead(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.FAMILY_HEAD, false));
                memberObject.setPrimaryCareGiver(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.PRIMARY_CAREGIVER, false));
                memberObject.setPhoneNumber(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.PHONE_NUMBER, false));
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
        if (textViewRecordMotherMentor != null) {
            textViewRecordMotherMentor.setVisibility(android.view.View.VISIBLE);
            textViewRecordMotherMentor.setText(R.string.mothermentor_household_record);
        }
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
        findViewById(R.id.primary_mothermentor_caregiver).setVisibility(android.view.View.GONE);
        findViewById(R.id.family_mothermentor_head).setVisibility(android.view.View.GONE);
    }

    @Override
    public void openRecordClientVisit() {
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openFollowupVisit() {
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openRecordTbContactVisit() {
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openClientObservationResults() {
    }

    @Override
    public void observationResults() {
    }

    @Override
    public void openObservationResults() {
    }

    @Override
    public void startServiceForm() {
    }

    @Override
    public void continueService() {
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueContactVisit() {
    }

    @Override
    public void openTbContactFollowUpVisit() {
        MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(this, memberObject.getBaseEntityId(), false);
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
    public void openMotherMentorContactRegister() {
    }

    @Override
    public void startHivstRegistration() {
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
        memberObject = getMemberObject(getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID));
    }

    private void enforceProcessVisitVisibility() {
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
                !Boolean.TRUE.equals(latestVisit.getProcessed()) &&
                        hasCompletedHouseholdServiceSections(latestVisit);

        manualProcessVisit.setVisibility(shouldShowProcessVisit ? View.VISIBLE : View.GONE);
        if (shouldShowProcessVisit) {
            manualProcessVisit.setOnClickListener(view -> {
                try {
                    MotherMentorVisitsUtil.manualProcessVisit(latestVisit);
                    Toast.makeText(this, R.string.mothermentor_visit_conducted, Toast.LENGTH_SHORT).show();
                    setupViews();
                    refreshMedicalHistory(true);
                } catch (Exception e) {
                    Timber.e(e);
                }
            });
        }
    }

    protected Visit getServiceVisit() {
        return MotherMentorLibrary.getInstance().visitRepository()
                .getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
    }

    protected boolean isVisitOnProgress(Visit visit) {
        return visit != null && TextUtils.isEmpty(visit.getVisitId());
    }

    private boolean hasProcessedHouseholdVisitHistory() {
        try {
            return !MotherMentorLibrary.getInstance().visitRepository()
                    .getAllVisitsProcessed(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, memberObject.getBaseEntityId())
                    .isEmpty();
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    private boolean hasCompletedHouseholdServiceSections(Visit visit) {
        if (visit == null || TextUtils.isEmpty(visit.getJson())) {
            return false;
        }

        try {
            JSONArray obsArray = new JSONObject(visit.getJson()).optJSONArray("obs");
            return obsArray != null && getCompletedHouseholdServiceFields(obsArray)
                    .containsAll(REQUIRED_HOUSEHOLD_SERVICE_FIELDS);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    private Set<String> getCompletedHouseholdServiceFields(JSONArray obsArray) {
        Set<String> completedFields = new HashSet<>();
        for (int i = 0; i < obsArray.length(); i++) {
            JSONObject obs = obsArray.optJSONObject(i);
            if (!hasRequiredFieldValue(obs)) {
                continue;
            }

            String fieldCode = obs.optString("fieldCode").toLowerCase(Locale.US);
            if (REQUIRED_HOUSEHOLD_SERVICE_FIELDS.contains(fieldCode)) {
                completedFields.add(fieldCode);
            }
        }
        return completedFields;
    }

    private boolean hasRequiredFieldValue(JSONObject obs) {
        if (obs == null) {
            return false;
        }

        JSONArray values = obs.optJSONArray("values");
        return values != null
                && values.length() > 0
                && !TextUtils.isEmpty(values.optString(0));
    }
}
