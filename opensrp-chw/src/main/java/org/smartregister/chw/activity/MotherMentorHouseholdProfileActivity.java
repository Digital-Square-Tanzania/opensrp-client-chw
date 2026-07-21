package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreMotherMentorProfileActivity;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitsUtil;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import timber.log.Timber;

public class MotherMentorHouseholdProfileActivity extends CoreMotherMentorProfileActivity {
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
}
