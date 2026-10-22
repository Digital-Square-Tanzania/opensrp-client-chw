package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.isMultiPartForm;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.view.Menu;

import androidx.annotation.MenuRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreMotherMentorRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.MotherMentorContactRegisterFragment;
import org.smartregister.chw.fragment.MotherMentorMobilizationFragment;
import org.smartregister.chw.fragment.MotherMentorRegisterFragment;
import org.smartregister.chw.fragment.MotherMentorSecondaryEnrollmentsFragment;
import org.smartregister.chw.mothermentor.repository.MotherMentorReportingRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MotherMentorRegisterActivity extends CoreMotherMentorRegisterActivity {
    public static final String EXTRA_SECONDARY_ENROLLMENT_TAB_INDEX =
            "secondary_enrollment_tab_index";
    public static final int TAB_INDEX_IIT = 0;
    public static final int TAB_INDEX_PARTNER = 1;
    public static final int TAB_INDEX_CHILD_EID = 2;

    private static final String EVENT_MOTHER_MENTOR_ENROLLMENT = "Mother Mentor Enrollment";
    private static final String EVENT_MOTHER_MENTOR_ENROLL_IIT = "Mother Mentor Enroll IIT";
    private static final String EVENT_MOTHER_MENTOR_ENROLL_PARTNER = "Mother Mentor Enroll Partner";
    private static final String EVENT_MOTHER_MENTOR_ENROLL_CHILD_EID =
            "Mother Mentor Enroll Child Eid";
    private static final String EVENT_MOTHER_MENTOR_MOBILIZATION =
            "MotherMentor Mobilization Session";
    private static final String EVENT_MAMA_KINARA_HOUSEHOLD_ENROLMENT =
            "mama_kinara_household_enrolment";
    private static final String TABLE_MOTHERMENTOR_ENROLL_IIT =
            org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_IIT;
    private static final String TABLE_MOTHERMENTOR_ENROLL_PARTNER =
            org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER;
    private static final String TABLE_MOTHERMENTOR_ENROLL_CHILD_EID =
            org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID;
    private static final String TABLE_MOTHERMENTOR_MOBILIZATION =
            org.smartregister.chw.mothermentor.util.Constants.TABLES.MOTHERMENTOR_MOBILIZATION;
    private static final String TABLE_MAMA_KINARA_HOUSEHOLD_ENROLMENT =
            org.smartregister.chw.mothermentor.util.Constants.TABLES
                    .MAMA_KINARA_HOUSEHOLD_ENROLMENT;
    private static final String CONSENT_YES = "ndiyo";
    private static final String CHILD_OVER_18_MONTHS_MESSAGE =
            "Mother not enrolled — child is over 18 months of follow-up";
    private static final DateTimeFormatter[] CHILD_DOB_FORMATTERS =
            new DateTimeFormatter[] {
                DateTimeFormat.forPattern("dd-MM-yyyy"), DateTimeFormat.forPattern("yyyy-MM-dd")
            };

    public static void startRegistration(
            Activity activity, String baseEntityId, String gender, int age) {
        startRegistration(activity, baseEntityId, null, gender, age);
    }

    public static void startRegistration(
            Activity activity,
            String baseEntityId,
            String familyBaseEntityId,
            String gender,
            int age) {
        startRegistration(
                activity,
                baseEntityId,
                familyBaseEntityId,
                gender,
                age,
                Constants.FORMS.MOTHER_MENTOR_ENROLLMENT);
    }

    public static void startRegistration(
            Activity activity,
            String baseEntityId,
            String familyBaseEntityId,
            String gender,
            int age,
            String formName) {
        Intent intent = new Intent(activity, MotherMentorRegisterActivity.class);
        intent.putExtra(
                org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID,
                baseEntityId);
        intent.putExtra(
                org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD
                        .FAMILY_BASE_ENTITY_ID,
                familyBaseEntityId);
        intent.putExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        intent.putExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.AGE, age);
        intent.putExtra(
                org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD
                        .MOTHER_MENTOR_FORM_NAME,
                formName);
        activity.startActivity(intent);
    }

    public static void startHouseholdEnrollment(Activity activity, String householdBaseEntityId) {
        Intent intent = new Intent(activity, MotherMentorRegisterActivity.class);
        intent.putExtra(
                org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID,
                householdBaseEntityId);
        intent.putExtra(
                org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD
                        .FAMILY_BASE_ENTITY_ID,
                householdBaseEntityId);
        intent.putExtra(
                org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD
                        .MOTHER_MENTOR_FORM_NAME,
                EVENT_MAMA_KINARA_HOUSEHOLD_ENROLMENT);
        activity.startActivity(intent);
    }

    @Override
    public Form getFormConfig() {
        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setName(getString(R.string.mother_mentor_registration));
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
        form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
        form.setPreviousLabel(
                this.getResources().getString(org.smartregister.chw.core.R.string.back));
        form.setSaveLabel(this.getResources().getString(org.smartregister.chw.core.R.string.save));
        return form;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new MotherMentorRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[] {
            new MotherMentorContactRegisterFragment(),
            new MotherMentorMobilizationFragment(),
            new MotherMentorSecondaryEnrollmentsFragment()
        };
    }

    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_mothermentor;
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(
                    LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_clients);
            bottomNavigationView
                    .getMenu()
                    .removeItem(org.smartregister.chw.mothermentor.R.id.action_register);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_search);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_library);
            bottomNavigationView.inflateMenu(getMenuResource());
            bottomNavigationView
                    .getMenu()
                    .add(
                            Menu.NONE,
                            R.id.action_mothermentor_secondary_enrollments,
                            Menu.NONE,
                            R.string.mothermentor_secondary_enrollments)
                    .setIcon(org.smartregister.chw.mothermentor.R.mipmap.ic_jobaids);
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);
            bottomNavigationView.setOnNavigationItemSelectedListener(
                    item -> {
                        if (item.getItemId()
                                == org.smartregister.chw.mothermentor.R.id.action_home) {
                            switchToBaseFragment();
                        } else if (item.getItemId()
                                == org.smartregister.chw.mothermentor.R.id.action_contact) {
                            switchToFragment(1);
                        } else if (item.getItemId()
                                == org.smartregister.chw.mothermentor.R.id.action_mobilization) {
                            switchToFragment(2);
                        } else if (item.getItemId()
                                == R.id.action_mothermentor_secondary_enrollments) {
                            switchToFragment(3);
                        }
                        return true;
                    });
        }
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.MOTHER_MENTOR);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        if (EVENT_MOTHER_MENTOR_ENROLL_PARTNER.equals(jsonForm.optString(Constants.ENCOUNTER_TYPE))
                && !canEnrollPartner(jsonForm.optString(DBConstants.KEY.ENTITY_ID))) {
            displayToast(getString(R.string.mothermentor_partner_enrollment_unavailable));
            return;
        }
        if (EVENT_MOTHER_MENTOR_ENROLL_CHILD_EID.equals(
                        jsonForm.optString(Constants.ENCOUNTER_TYPE))
                && !canEnrollChild(jsonForm.optString(DBConstants.KEY.ENTITY_ID))) {
            displayToast(getString(R.string.mothermentor_child_eid_enrollment_unavailable));
            return;
        }
        Form form = new Form();
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);

        String gender =
                getIntent()
                        .getStringExtra(
                                org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.GENDER);
        int age =
                getIntent()
                        .getIntExtra(
                                org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.AGE, -1);

        try {
            if (jsonForm.getString("encounter_type").equals("Mother Mentor Enrollment")) {

                JSONObject global = jsonForm.getJSONObject("global");

                if (gender != null) {
                    global.put(DBConstants.KEY.AGE, age);
                    global.put(DBConstants.KEY.GENDER, gender);
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        intent.putExtra(
                org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setHomeAsUpIndicator(org.smartregister.chw.core.R.mipmap.ic_cross_white);
        form.setSaveLabel(getResources().getString(org.smartregister.chw.core.R.string.save));

        if (isMultiPartForm(jsonForm)) {
            form.setWizard(true);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
            form.setName(null);
            form.setNextLabel(getResources().getString(org.smartregister.chw.core.R.string.next));
            form.setPreviousLabel(
                    getResources().getString(org.smartregister.chw.core.R.string.back));
        }
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        Form formConfig = getFormConfig();
        if (formConfig != null) {
            if (Constants.EVENT_TYPE.MOTHER_MENTOR_MOBILIZATION.equals(
                    jsonForm.optString(Constants.ENCOUNTER_TYPE))) {
                // Let the community activity form supply its localized toolbar title.
                formConfig.setName(null);
            }
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, formConfig);
        }
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && isJsonFormRequest(requestCode) && data != null) {
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                if (isMotherMentorEnrollment(form) && isChildOver18Months(form)) {
                    displayToast(CHILD_OVER_18_MONTHS_MESSAGE);
                    return;
                }
                if (EVENT_MAMA_KINARA_HOUSEHOLD_ENROLMENT.equals(
                        form.optString(Constants.ENCOUNTER_TYPE))) {
                    boolean hasConsent = saveHouseholdEnrollment(form);
                    displayToast(
                            getString(
                                    hasConsent
                                            ? R.string.mama_kinara_household_enrolled
                                            : R.string.mama_kinara_household_no_consent));
                    startClientProcessing();
                    if (!hasConsent) {
                        finish();
                    } else {
                        openHouseholdTab();
                    }
                    return;
                }
                String tableName =
                        getMotherMentorSecondaryEnrollmentTable(
                                form.optString(Constants.ENCOUNTER_TYPE));
                if (tableName != null) {
                    if (TABLE_MOTHERMENTOR_ENROLL_PARTNER.equals(tableName)
                            && !canEnrollPartner(form.optString(DBConstants.KEY.ENTITY_ID))) {
                        displayToast(
                                getString(R.string.mothermentor_partner_enrollment_unavailable));
                        return;
                    }
                    if (TABLE_MOTHERMENTOR_ENROLL_CHILD_EID.equals(tableName)
                            && !canEnrollChild(form.optString(DBConstants.KEY.ENTITY_ID))) {
                        displayToast(
                                getString(R.string.mothermentor_child_eid_enrollment_unavailable));
                        return;
                    }
                    saveMotherMentorSecondaryEnrollment(form, tableName);
                    startClientProcessing();
                    openSecondaryEnrollmentsTab(tableName);
                    return;
                }
            } catch (Exception e) {
                Timber.e(e);
                displayToast(
                        getString(
                                org.smartregister.chw.mothermentor.R.string
                                        .error_unable_to_save_form));
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isJsonFormRequest(int requestCode) {
        return requestCode == Constants.REQUEST_CODE_GET_JSON
                || requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON;
    }

    private boolean isMotherMentorEnrollment(JSONObject form) {
        String encounterType = form.optString(Constants.ENCOUNTER_TYPE);
        return Constants.FORMS.MOTHER_MENTOR_ENROLLMENT.equals(FORM_NAME)
                || EVENT_MOTHER_MENTOR_ENROLLMENT.equals(encounterType)
                || Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLLMENT.equals(encounterType);
    }

    private boolean isChildOver18Months(JSONObject form) {
        String childDob =
                org.smartregister.chw.mothermentor.util.JsonFormUtils.getValue(
                        form, DBConstants.KEY.CHILD_DOB);
        if (StringUtils.isBlank(childDob)) {
            return false;
        }

        LocalDate childDobDate = parseChildDob(childDob);
        return childDobDate != null
                && Months.monthsBetween(childDobDate, LocalDate.now()).getMonths() > 18;
    }

    private LocalDate parseChildDob(String childDob) {
        for (DateTimeFormatter formatter : CHILD_DOB_FORMATTERS) {
            try {
                return formatter.parseLocalDate(childDob);
            } catch (IllegalArgumentException e) {
                // Try the next supported native-form date format.
            }
        }
        Timber.w("Unable to parse Mother Mentor child DOB: %s", childDob);
        return null;
    }

    private boolean saveHouseholdEnrollment(JSONObject form) throws Exception {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        JSONArray fields =
                org.smartregister.chw.mothermentor.util.MotherMentorJsonFormUtils.fields(
                        form, Constants.STEP_ONE);
        String consent = getFieldValue(fields, DBConstants.KEY.CONSENT);
        setFieldValue(fields, DBConstants.KEY.HOUSEHOLD_ID, BASE_ENTITY_ID);
        setFieldValue(fields, DBConstants.KEY.ENROLLMENT_DATE, today);
        form.put(org.smartregister.util.JsonFormUtils.ENTITY_ID, BASE_ENTITY_ID);

        org.smartregister.repository.AllSharedPreferences allSharedPreferences =
                org.smartregister.chw.mothermentor.MotherMentorLibrary.getInstance()
                        .context()
                        .allSharedPreferences();
        Event event =
                org.smartregister.chw.mothermentor.util.JsonFormUtils.processJsonForm(
                        allSharedPreferences,
                        form.toString(),
                        TABLE_MAMA_KINARA_HOUSEHOLD_ENROLMENT);
        org.smartregister.chw.mothermentor.util.MotherMentorUtil.processEvent(
                allSharedPreferences, event);
        return CONSENT_YES.equals(consent);
    }

    private String getFieldValue(JSONArray fields, String key) throws JSONException {
        JSONObject field = getField(fields, key);
        return field == null ? "" : field.optString(org.smartregister.util.JsonFormUtils.VALUE);
    }

    private void setFieldValue(JSONArray fields, String key, String value) throws JSONException {
        JSONObject field = getField(fields, key);
        if (field != null) {
            field.put(org.smartregister.util.JsonFormUtils.VALUE, value);
        }
    }

    private JSONObject getField(JSONArray fields, String key) throws JSONException {
        if (fields == null) {
            return null;
        }
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        return null;
    }

    private String getMotherMentorSecondaryEnrollmentTable(String encounterType) {
        if (EVENT_MOTHER_MENTOR_ENROLL_IIT.equals(encounterType)) {
            return TABLE_MOTHERMENTOR_ENROLL_IIT;
        } else if (EVENT_MOTHER_MENTOR_ENROLL_PARTNER.equals(encounterType)) {
            return TABLE_MOTHERMENTOR_ENROLL_PARTNER;
        } else if (EVENT_MOTHER_MENTOR_ENROLL_CHILD_EID.equals(encounterType)) {
            return TABLE_MOTHERMENTOR_ENROLL_CHILD_EID;
        } else if (EVENT_MOTHER_MENTOR_MOBILIZATION.equals(encounterType)) {
            return TABLE_MOTHERMENTOR_MOBILIZATION;
        }
        return null;
    }

    private void openSecondaryEnrollmentsTab(String tableName) {
        int tabIndex = getSecondaryEnrollmentTabIndex(tableName);
        if (tabIndex < 0) {
            return;
        }

        getIntent().putExtra(EXTRA_SECONDARY_ENROLLMENT_TAB_INDEX, tabIndex);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.action_mothermentor_secondary_enrollments);
        } else {
            switchToFragment(3);
        }
    }

    private void openHouseholdTab() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(
                    org.smartregister.chw.mothermentor.R.id.action_contact);
        } else {
            switchToFragment(1);
        }
    }

    private int getSecondaryEnrollmentTabIndex(String tableName) {
        if (TABLE_MOTHERMENTOR_ENROLL_IIT.equals(tableName)) {
            return TAB_INDEX_IIT;
        } else if (TABLE_MOTHERMENTOR_ENROLL_PARTNER.equals(tableName)) {
            return TAB_INDEX_PARTNER;
        } else if (TABLE_MOTHERMENTOR_ENROLL_CHILD_EID.equals(tableName)) {
            return TAB_INDEX_CHILD_EID;
        }
        return -1;
    }

    private boolean canEnrollPartner(String id) {
        return new org.smartregister.chw.repository.MotherMentorPartnerRepository(
                        ChwApplication.getInstance().getRepository().getReadableDatabase())
                .canEnrollPartner(id);
    }

    private boolean canEnrollChild(String id) {
        return new org.smartregister.chw.repository.MotherMentorChildEidRepository(
                        ChwApplication.getInstance().getRepository().getReadableDatabase())
                .canEnrollChild(id);
    }

    private void saveMotherMentorSecondaryEnrollment(JSONObject form, String tableName)
            throws Exception {
        updateRelationalId(form);
        org.smartregister.repository.AllSharedPreferences allSharedPreferences =
                org.smartregister.chw.mothermentor.MotherMentorLibrary.getInstance()
                        .context()
                        .allSharedPreferences();
        Event event =
                org.smartregister.chw.mothermentor.util.JsonFormUtils.processJsonForm(
                        allSharedPreferences, form.toString(), tableName);
        org.smartregister.chw.mothermentor.util.MotherMentorUtil.processEvent(
                allSharedPreferences, event);
        if (TABLE_MOTHERMENTOR_MOBILIZATION.equals(tableName)) {
            saveMotherMentorMobilizationSession(event);
        }
    }

    private void saveMotherMentorMobilizationSession(Event event) {
        if (event == null) {
            return;
        }

        ContentValues values = new ContentValues();
        String shTarehe = getObsValue(event, DBConstants.KEY.SH_TAREHE, false);
        String shAina = getObsValue(event, DBConstants.KEY.SH_AINA, false);
        String shMada = getObsValue(event, DBConstants.KEY.SH_MADA, true);
        String elimM = getObsValue(event, DBConstants.KEY.ELIM_M, false);
        String elimF = getObsValue(event, DBConstants.KEY.ELIM_F, false);

        values.put(DBConstants.KEY.ID, event.getBaseEntityId());
        values.put(DBConstants.KEY.BASE_ENTITY_ID, event.getBaseEntityId());
        values.put(DBConstants.KEY.SH_TAREHE, shTarehe);
        values.put(DBConstants.KEY.SH_AINA, shAina);
        values.put(
                DBConstants.KEY.SH_AINA_OTHER,
                getObsValue(event, DBConstants.KEY.SH_AINA_OTHER, false));
        values.put(DBConstants.KEY.GPS, getObsValue(event, DBConstants.KEY.GPS, false));
        values.put(DBConstants.KEY.SH_MADA, shMada);
        values.put(
                DBConstants.KEY.SH_MADA_OTHER,
                getObsValue(event, DBConstants.KEY.SH_MADA_OTHER, false));
        values.put(DBConstants.KEY.ELIM_M, elimM);
        values.put(DBConstants.KEY.ELIM_F, elimF);
        values.put(
                DBConstants.KEY.ELIM_TOTAL, getObsValue(event, DBConstants.KEY.ELIM_TOTAL, false));
        values.put(DBConstants.KEY.MAONI, getObsValue(event, DBConstants.KEY.MAONI, false));
        values.put(
                DBConstants.KEY.REFERRED_MALE,
                getObsValue(event, DBConstants.KEY.REFERRED_MALE, false));
        values.put(
                DBConstants.KEY.REFERRED_FEMALE,
                getObsValue(event, DBConstants.KEY.REFERRED_FEMALE, false));
        values.put(
                DBConstants.KEY.REFERRED_TOTAL,
                getObsValue(event, DBConstants.KEY.REFERRED_TOTAL, false));
        values.put(
                DBConstants.KEY.FACILITY_REFERRAL_SERVICES,
                getObsValue(event, DBConstants.KEY.FACILITY_REFERRAL_SERVICES, true));
        values.put(
                DBConstants.KEY.FACILITY_REFERRAL_OTHER,
                getObsValue(event, DBConstants.KEY.FACILITY_REFERRAL_OTHER, false));

        values.put(DBConstants.KEY.MOBILIZATION_DATE, shTarehe);
        values.put(DBConstants.KEY.MOBILIZATION_AREA, shAina);
        values.put(
                DBConstants.KEY.OTHER_MOBILIZATION_AREA,
                getObsValue(event, DBConstants.KEY.SH_AINA_OTHER, false));
        values.put(DBConstants.KEY.MOBILIZATION_TOPICS, shMada);
        values.put(DBConstants.KEY.MALE_CLIENTS_REACHED, elimM);
        values.put(DBConstants.KEY.FEMALE_CLIENTS_REACHED, elimF);
        long lastInteractedWith =
                event.getVersion() > 0 ? event.getVersion() : System.currentTimeMillis();
        values.put(DBConstants.KEY.LAST_INTERACTED_WITH, lastInteractedWith);

        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
            MotherMentorReportingRepository.ensureMobilizationColumns(db);
            db.insertWithOnConflict(
                    TABLE_MOTHERMENTOR_MOBILIZATION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Timber.e(e, "Unable to save Mother Mentor mobilization session row");
        }
    }

    private String getObsValue(Event event, String key, boolean preferValues) {
        if (event.getObs() == null) {
            return null;
        }

        for (Obs obs : event.getObs()) {
            if (key.equals(obs.getFormSubmissionField())) {
                if (preferValues && obs.getValues() != null && !obs.getValues().isEmpty()) {
                    return obs.getValues().toString();
                }
                Object value = obs.getValue();
                return value == null ? null : String.valueOf(value);
            }
        }
        return null;
    }

    private void updateRelationalId(JSONObject form) throws JSONException {
        if (FAMILY_BASE_ENTITY_ID == null) {
            return;
        }

        JSONArray fields =
                org.smartregister.chw.mothermentor.util.MotherMentorJsonFormUtils.fields(
                        form, Constants.STEP_ONE);
        JSONObject relationalId =
                org.smartregister.util.JsonFormUtils.getFieldJSONObject(
                        fields, DBConstants.KEY.RELATIONAL_ID);
        if (relationalId != null) {
            relationalId.remove(org.smartregister.util.JsonFormUtils.VALUE);
            relationalId.put(org.smartregister.util.JsonFormUtils.VALUE, FAMILY_BASE_ENTITY_ID);
        }
    }
}
