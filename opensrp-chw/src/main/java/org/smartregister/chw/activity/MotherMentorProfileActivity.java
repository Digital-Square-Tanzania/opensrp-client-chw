package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreMotherMentorProfileActivity;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitsUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class MotherMentorProfileActivity extends CoreMotherMentorProfileActivity {
    private static final String FORM_MOTHERMENTOR_ENROLL_IIT = "mothermentor_enroll_iit";
    private static final String FORM_MOTHERMENTOR_ENROLL_PARTNER = "mothermentor_enroll_partner";
    private static final String FORM_MOTHERMENTOR_ENROLL_CHILD_EID = "mothermentor_enroll_child_eid";
    private static final String ATTENDANCE_TYPE = "attendance_type";
    private static final String PSYCHOSOCIAL_GROUP_LINKAGE = "has_been_linked_to_psychosocial_support_group";
    private static final String IGA_GROUP_LINKAGE = "has_been_linked_to_iga_group";
    private static final String EDUCATION_PROVIDED = "education_provided";
    private static final String REFERRAL_GIVEN = "referral_given";
    private static final String NEXT_APPOINTMENT_DATE = "next_appointment_date";
    private static final String COMMENTS = "comments";

    private static final Set<String> REQUIRED_MOTHER_MENTOR_SERVICE_FIELDS = new HashSet<>(Arrays.asList(
            ATTENDANCE_TYPE,
            PSYCHOSOCIAL_GROUP_LINKAGE,
            IGA_GROUP_LINKAGE,
            EDUCATION_PROVIDED,
            REFERRAL_GIVEN,
            NEXT_APPOINTMENT_DATE,
            COMMENTS
    ));

    public static void startMe(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, MotherMentorProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        MemberObject memberObject = super.getMemberObject(baseEntityId);
        if (memberObject != null) {
            return memberObject;
        }

        return getSecondaryEnrollmentMember(baseEntityId);
    }

    private MemberObject getSecondaryEnrollmentMember(String baseEntityId) {
        MemberObject memberObject = getSecondaryEnrollmentMember(baseEntityId, Constants.TABLES.MOTHERMENTOR_ENROLL_IIT);
        if (memberObject != null) {
            return memberObject;
        }

        memberObject = getSecondaryEnrollmentMember(baseEntityId, Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER);
        if (memberObject != null) {
            return memberObject;
        }

        return getSecondaryEnrollmentMember(baseEntityId, Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID);
    }

    private MemberObject getSecondaryEnrollmentMember(String baseEntityId, String tableName) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return null;
        }

        String sql = "select " +
                "m.base_entity_id, m.unique_id, m.relational_id, m.dob, m.first_name, m.middle_name, " +
                "m.last_name, m.gender, m.marital_status, m.phone_number, f.base_entity_id as family_base_entity_id, " +
                "f.first_name as family_name, f.primary_caregiver, f.family_head, f.village_town, " +
                "fh.first_name as family_head_first_name, fh.middle_name as family_head_middle_name, " +
                "fh.last_name as family_head_last_name, fh.phone_number as family_head_phone_number, " +
                "pcg.first_name as pcg_first_name, pcg.middle_name as pcg_middle_name, " +
                "pcg.last_name as pcg_last_name, pcg.phone_number as pcg_phone_number " +
                "from ec_family_member m " +
                "inner join ec_family f on m.relational_id = f.base_entity_id " +
                "inner join " + tableName + " mr on mr.base_entity_id = m.base_entity_id " +
                "left join ec_family_member fh on fh.base_entity_id = f.family_head " +
                "left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver " +
                "where mr.is_closed = 0 AND m.is_closed = 0 AND f.is_closed = 0 " +
                "AND m.base_entity_id = ? " +
                "ORDER BY mr.last_interacted_with DESC LIMIT 1";

        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
            try (Cursor cursor = db.rawQuery(sql, new String[]{baseEntityId})) {
                if (cursor.moveToFirst()) {
                    return cursorToSecondaryEnrollmentMember(cursor);
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to load Mother Mentor secondary enrollment profile from %s", tableName);
        }
        return null;
    }

    private MemberObject cursorToSecondaryEnrollmentMember(Cursor cursor) {
        MemberObject memberObject = new MemberObject();
        memberObject.setFirstName(getCursorValue(cursor, "first_name"));
        memberObject.setMiddleName(getCursorValue(cursor, "middle_name"));
        memberObject.setLastName(getCursorValue(cursor, "last_name"));
        memberObject.setAddress(getCursorValue(cursor, "village_town"));
        memberObject.setGender(getCursorValue(cursor, "gender"));
        memberObject.setMartialStatus(getCursorValue(cursor, "marital_status"));
        memberObject.setUniqueId(getCursorValue(cursor, "unique_id"));
        memberObject.setDob(getCursorValue(cursor, "dob"));
        memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "family_base_entity_id"));
        memberObject.setRelationalId(getCursorValue(cursor, "relational_id"));
        memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
        memberObject.setFamilyName(getCursorValue(cursor, "family_name"));
        memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number"));
        memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id"));
        memberObject.setFamilyHead(getCursorValue(cursor, "family_head"));
        memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number"));

        String familyHeadName = (getCursorValue(cursor, "family_head_first_name") + " "
                + getCursorValue(cursor, "family_head_middle_name")).trim();
        memberObject.setFamilyHeadName((familyHeadName + " " + getCursorValue(cursor, "family_head_last_name")).trim());

        String primaryCareGiverName = (getCursorValue(cursor, "pcg_first_name") + " "
                + getCursorValue(cursor, "pcg_middle_name")).trim();
        memberObject.setPrimaryCareGiverName((primaryCareGiverName + " " + getCursorValue(cursor, "pcg_last_name")).trim());

        return memberObject;
    }

    private String getCursorValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex == -1 || cursor.isNull(columnIndex)) {
            return "";
        }
        return cursor.getString(columnIndex);
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
        enforceProcessVisitVisibility();
        refreshMedicalHistory(true);
    }

    private void enforceProcessVisitVisibility() {
        Visit latestVisit = getServiceVisit();
        if (latestVisit == null) {
            manualProcessVisit.setVisibility(View.GONE);
            mothermentorServiceInProgress.setVisibility(View.GONE);
            return;
        }

        if (isVisitOnProgress(latestVisit)) {
            textViewRecordMotherMentor.setVisibility(View.GONE);
            mothermentorServiceInProgress.setVisibility(View.VISIBLE);
        } else {
            mothermentorServiceInProgress.setVisibility(View.GONE);
        }

        boolean shouldShowProcessVisit =
                !Boolean.TRUE.equals(latestVisit.getProcessed()) &&
                        hasCompletedMotherMentorServiceSections(latestVisit);

        manualProcessVisit.setVisibility(shouldShowProcessVisit ? View.VISIBLE : View.GONE);
        if (shouldShowProcessVisit) {
            manualProcessVisit.setOnClickListener(view -> {
                try {
                    MotherMentorVisitsUtil.manualProcessVisit(latestVisit);
                    displayToast(org.smartregister.chw.mothermentor.R.string.mothermentor_visit_conducted);
                    setupViews();
                    refreshMedicalHistory(true);
                } catch (Exception e) {
                    Timber.e(e);
                }
            });
        }
    }

    private boolean hasCompletedMotherMentorServiceSections(Visit visit) {
        if (visit == null || TextUtils.isEmpty(visit.getJson())) {
            return false;
        }

        try {
            JSONArray obsArray = new JSONObject(visit.getJson()).optJSONArray("obs");
            return obsArray != null && getCompletedMotherMentorServiceFields(obsArray)
                    .containsAll(REQUIRED_MOTHER_MENTOR_SERVICE_FIELDS);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    private Set<String> getCompletedMotherMentorServiceFields(JSONArray obsArray) {
        Set<String> completedFields = new HashSet<>();
        for (int i = 0; i < obsArray.length(); i++) {
            JSONObject obs = obsArray.optJSONObject(i);
            if (!hasRequiredFieldValue(obs)) {
                continue;
            }

            String fieldCode = obs.optString("fieldCode").toLowerCase(Locale.US);
            if (REQUIRED_MOTHER_MENTOR_SERVICE_FIELDS.contains(fieldCode)) {
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
        if (hasProcessedMotherMentorVisitHistory()) {
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
    public void openMedicalHistory() {
        MotherMentorMedicalHistoryActivity.startMe(this, memberObject);
    }

    private boolean hasProcessedMotherMentorVisitHistory() {
        try {
            return !MotherMentorLibrary.getInstance().visitRepository()
                    .getAllVisitsProcessed(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, memberObject.getBaseEntityId())
                    .isEmpty()
                    || !MotherMentorLibrary.getInstance().visitRepository()
                    .getAllVisitsProcessed(Constants.EVENT_TYPE.MOTHERMENTOR_CONTACT_VISIT, memberObject.getBaseEntityId())
                    .isEmpty();
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
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
