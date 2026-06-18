package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreMotherMentorProfileActivity;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;

import timber.log.Timber;

public class MotherMentorProfileActivity extends CoreMotherMentorProfileActivity {
    private static final String FORM_MOTHERMENTOR_ENROLL_IIT = "mothermentor_enroll_iit";
    private static final String FORM_MOTHERMENTOR_ENROLL_PARTNER = "mothermentor_enroll_partner";
    private static final String FORM_MOTHERMENTOR_ENROLL_CHILD_EID = "mothermentor_enroll_child_eid";

    public static void startMe(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, MotherMentorProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, R.id.action_mothermentor_enroll_iit, Menu.NONE, R.string.mothermentor_enroll_iit)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, R.id.action_mothermentor_enroll_partner, Menu.NONE, R.string.mothermentor_enroll_partner)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, R.id.action_mothermentor_enroll_child_eid, Menu.NONE, R.string.mothermentor_enroll_child_eid)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_mothermentor_enroll_iit) {
            startSecondaryEnrollment(FORM_MOTHERMENTOR_ENROLL_IIT);
            return true;
        } else if (itemId == R.id.action_mothermentor_enroll_partner) {
            startSecondaryEnrollment(FORM_MOTHERMENTOR_ENROLL_PARTNER);
            return true;
        } else if (itemId == R.id.action_mothermentor_enroll_child_eid) {
            startSecondaryEnrollment(FORM_MOTHERMENTOR_ENROLL_CHILD_EID);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void setupButtons() {
        super.setupButtons();
        if (textViewRecordMotherMentor != null) {
            textViewRecordMotherMentor.setVisibility(View.VISIBLE);
            textViewRecordMotherMentor.setText(org.smartregister.chw.mothermentor.R.string.record_mothermentor);
        }
        if (textViewRegisterMotherMentorContact != null) {
            textViewRegisterMotherMentorContact.setVisibility(View.VISIBLE);
        }
        refreshMedicalHistory(true);
    }

    @Override
    public void openFollowupVisit() {
        startForm(Constants.FORMS.MOTHERMENTOR_FOLLOWUP_VISIT);
    }

    @Override
    public void openRecordTbContactVisit() {
        MotherMentorVisitActivity.startMotherMentorVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openClientObservationResults() {
        openObservationResults();
    }

    @Override
    public void observationResults() {
        openObservationResults();
    }

    @Override
    public void openTbContactFollowUpVisit() {
        startForm(Constants.FORMS.MOTHERMENTOR_FOLLOWUP_VISIT);
    }

    @Override
    public void openRecordClientVisit() {
        MotherMentorVisitActivity.startMotherMentorVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit lastVisit = getLatestMotherMentorVisit();
        if (lastVisit != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
            view_last_visit_row.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
            ((TextView) findViewById(org.smartregister.chw.mothermentor.R.id.ivViewHistoryArrow))
                    .setText(getString(R.string.view_visits_history));
        } else {
            rlLastVisit.setVisibility(View.GONE);
            view_last_visit_row.setVisibility(View.GONE);
        }
    }

    @Override
    public void openMedicalHistory() {
        MotherMentorMedicalHistoryActivity.startMe(this, memberObject);
    }

    @Override
    public void startServiceForm() {
        MotherMentorVisitActivity.startMotherMentorVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void continueService() {
        MotherMentorVisitActivity.startMotherMentorVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueContactVisit() {
        MotherMentorVisitActivity.startMotherMentorVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void openObservationResults() {
        startForm(Constants.FORMS.OBSERVATION_RESULTS);
    }

    @Override
    public void openMotherMentorContactRegister() {
//        MotherMentorRegisterActivity.startRegistration(
//                this,
//                memberObject.getBaseEntityId(),
//                memberObject.getFamilyBaseEntityId()
//        );
    }

    private void startForm(String formName) {
        try {
            JSONObject form = FormUtils.getFormUtils().getFormJson(formName);
            form.put(org.smartregister.util.JsonFormUtils.ENTITY_ID, memberObject.getBaseEntityId());
            startFormActivity(form);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void startSecondaryEnrollment(String formName) {
        MotherMentorRegisterActivity.startRegistration(
                this,
                memberObject.getBaseEntityId(),
                memberObject.getFamilyBaseEntityId(),
                memberObject.getGender(),
                memberObject.getAge(),
                formName);
    }

    private Visit getLatestMotherMentorVisit() {
        Visit serviceVisit = MotherMentorLibrary.getInstance().visitRepository()
                .getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        Visit contactVisit = MotherMentorLibrary.getInstance().visitRepository()
                .getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.MOTHERMENTOR_CONTACT_VISIT);

        if (serviceVisit == null) {
            return contactVisit;
        }

        if (contactVisit == null || serviceVisit.getDate().after(contactVisit.getDate())) {
            return serviceVisit;
        }

        return contactVisit;
    }

    @Override
    public void startHivstRegistration() {

    }
}
