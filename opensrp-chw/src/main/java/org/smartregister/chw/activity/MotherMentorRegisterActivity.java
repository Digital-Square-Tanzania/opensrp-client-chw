package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.isMultiPartForm;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.view.Menu;

import androidx.annotation.MenuRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

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
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class MotherMentorRegisterActivity extends CoreMotherMentorRegisterActivity {
    private static final String EVENT_MOTHER_MENTOR_ENROLL_IIT = "Mother Mentor Enroll IIT";
    private static final String EVENT_MOTHER_MENTOR_ENROLL_PARTNER = "Mother Mentor Enroll Partner";
    private static final String EVENT_MOTHER_MENTOR_ENROLL_CHILD_EID = "Mother Mentor Enroll Child Eid";
    private static final String EVENT_MOTHER_MENTOR_MOBILIZATION = "MotherMentor Mobilization Session";
    private static final String TABLE_MOTHERMENTOR_ENROLL_IIT = "ec_mothermentor_enroll_it";
    private static final String TABLE_MOTHERMENTOR_ENROLL_PARTNER = "ec_mothermentor_enroll_partner";
    private static final String TABLE_MOTHERMENTOR_ENROLL_CHILD_EID = "ec_mothermentor_enroll_child_eid";
    private static final String TABLE_MOTHERMENTOR_MOBILIZATION = "ec_mothermentor_mobilization";
    private static final String FIELD_SH_TAREHE = "sh_tarehe";
    private static final String FIELD_SH_AINA = "sh_aina";
    private static final String FIELD_SH_AINA_OTHER = "sh_aina_other";
    private static final String FIELD_GPS = "gps";
    private static final String FIELD_SH_MADA = "sh_mada";
    private static final String FIELD_SH_MADA_OTHER = "sh_mada_other";
    private static final String FIELD_ELIM_M = "elim_m";
    private static final String FIELD_ELIM_F = "elim_f";
    private static final String FIELD_ELIM_TOTAL = "elim_total";
    private static final String FIELD_MAONI = "maoni";

    public static void startRegistration(Activity activity, String baseEntityId, String gender,int age) {
        startRegistration(activity, baseEntityId, null, gender, age);
    }

    public static void startRegistration(Activity activity, String baseEntityId, String familyBaseEntityId, String gender, int age) {
        startRegistration(activity, baseEntityId, familyBaseEntityId, gender, age, Constants.FORMS.MOTHER_MENTOR_ENROLLMENT);
    }

    public static void startRegistration(Activity activity, String baseEntityId, String familyBaseEntityId, String gender, int age, String formName) {
        Intent intent = new Intent(activity, MotherMentorRegisterActivity.class);
        intent.putExtra(org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD.FAMILY_BASE_ENTITY_ID, familyBaseEntityId);
        intent.putExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        intent.putExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.AGE, age);
        intent.putExtra(org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD.MOTHER_MENTOR_FORM_NAME, formName);
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
        form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));
        form.setSaveLabel(this.getResources().getString(org.smartregister.chw.core.R.string.save));
        return form;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new MotherMentorRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{
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
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_clients);
            bottomNavigationView.getMenu().removeItem(org.smartregister.chw.mothermentor.R.id.action_register);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_search);
            bottomNavigationView.getMenu().removeItem(org.smartregister.R.id.action_library);
            bottomNavigationView.inflateMenu(getMenuResource());
            bottomNavigationView.getMenu()
                    .add(Menu.NONE, R.id.action_mothermentor_secondary_enrollments, Menu.NONE, R.string.mothermentor_secondary_enrollments)
                    .setIcon(org.smartregister.chw.mothermentor.R.mipmap.ic_jobaids);
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                if (item.getItemId() == org.smartregister.chw.mothermentor.R.id.action_home) {
                    switchToBaseFragment();
                } else if (item.getItemId() == org.smartregister.chw.mothermentor.R.id.action_contact) {
                    switchToFragment(1);
                } else if (item.getItemId() == org.smartregister.chw.mothermentor.R.id.action_mobilization) {
                    switchToFragment(2);
                } else if (item.getItemId() == R.id.action_mothermentor_secondary_enrollments) {
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
        Form form = new Form();
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);

        String gender = getIntent().getStringExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.GENDER);
        int age = getIntent().getIntExtra(org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.AGE, -1);

        try {
            if (jsonForm.getString("encounter_type").equals("Mother Mentor Enrollment")) {

                JSONObject global = jsonForm.getJSONObject("global");

                if (gender != null) {
                    global.put("age", age);
                    global.put("gender", gender);
                }

            }
        } catch (Exception e) {
            Timber.e(e);
        }

        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setHomeAsUpIndicator(org.smartregister.chw.core.R.mipmap.ic_cross_white);
        form.setSaveLabel(getResources().getString(org.smartregister.chw.core.R.string.save));

        if (isMultiPartForm(jsonForm)) {
            form.setWizard(true);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
            form.setName(null);
            form.setNextLabel(getResources().getString(org.smartregister.chw.core.R.string.next));
            form.setPreviousLabel(getResources().getString(org.smartregister.chw.core.R.string.back));
        }
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && isJsonFormRequest(requestCode) && data != null) {
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                String tableName = getMotherMentorSecondaryEnrollmentTable(form.optString(Constants.ENCOUNTER_TYPE));
                if (tableName != null) {
                    saveMotherMentorSecondaryEnrollment(form, tableName);
                    startClientProcessing();
                    return;
                }
            } catch (Exception e) {
                Timber.e(e);
                displayToast(getString(org.smartregister.chw.mothermentor.R.string.error_unable_to_save_form));
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isJsonFormRequest(int requestCode) {
        return requestCode == Constants.REQUEST_CODE_GET_JSON || requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON;
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

    private void saveMotherMentorSecondaryEnrollment(JSONObject form, String tableName) throws Exception {
        updateRelationalId(form);
        org.smartregister.repository.AllSharedPreferences allSharedPreferences =
                org.smartregister.chw.mothermentor.MotherMentorLibrary.getInstance().context().allSharedPreferences();
        Event event = org.smartregister.chw.mothermentor.util.JsonFormUtils.processJsonForm(allSharedPreferences, form.toString(), tableName);
        org.smartregister.chw.mothermentor.util.MotherMentorUtil.processEvent(allSharedPreferences, event);
        if (TABLE_MOTHERMENTOR_MOBILIZATION.equals(tableName)) {
            saveMotherMentorMobilizationSession(event);
        }
    }

    private void saveMotherMentorMobilizationSession(Event event) {
        if (event == null) {
            return;
        }

        ContentValues values = new ContentValues();
        String shTarehe = getObsValue(event, FIELD_SH_TAREHE, false);
        String shAina = getObsValue(event, FIELD_SH_AINA, false);
        String shMada = getObsValue(event, FIELD_SH_MADA, true);
        String elimM = getObsValue(event, FIELD_ELIM_M, false);
        String elimF = getObsValue(event, FIELD_ELIM_F, false);

        values.put("id", event.getBaseEntityId());
        values.put("base_entity_id", event.getBaseEntityId());
        values.put(FIELD_SH_TAREHE, shTarehe);
        values.put(FIELD_SH_AINA, shAina);
        values.put(FIELD_SH_AINA_OTHER, getObsValue(event, FIELD_SH_AINA_OTHER, false));
        values.put(FIELD_GPS, getObsValue(event, FIELD_GPS, false));
        values.put(FIELD_SH_MADA, shMada);
        values.put(FIELD_SH_MADA_OTHER, getObsValue(event, FIELD_SH_MADA_OTHER, false));
        values.put(FIELD_ELIM_M, elimM);
        values.put(FIELD_ELIM_F, elimF);
        values.put(FIELD_ELIM_TOTAL, getObsValue(event, FIELD_ELIM_TOTAL, false));
        values.put(FIELD_MAONI, getObsValue(event, FIELD_MAONI, false));
        values.put("mobilization_date", shTarehe);
        values.put("mobilization_area", shAina);
        values.put("other_mobilization_area", getObsValue(event, FIELD_SH_AINA_OTHER, false));
        values.put("mobilization_topics", shMada);
        values.put("male_clients_reached", elimM);
        values.put("female_clients_reached", elimF);
        long lastInteractedWith = event.getVersion() > 0 ? event.getVersion() : System.currentTimeMillis();
        values.put("last_interacted_with", lastInteractedWith);

        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
            ensureMotherMentorMobilizationColumns(db);
            db.insertWithOnConflict(TABLE_MOTHERMENTOR_MOBILIZATION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Timber.e(e, "Unable to save Mother Mentor mobilization session row");
        }
    }

    private void ensureMotherMentorMobilizationColumns(SQLiteDatabase db) {
        addColumnIfMissing(db, FIELD_SH_TAREHE);
        addColumnIfMissing(db, FIELD_SH_AINA);
        addColumnIfMissing(db, FIELD_SH_AINA_OTHER);
        addColumnIfMissing(db, FIELD_SH_MADA);
        addColumnIfMissing(db, FIELD_SH_MADA_OTHER);
        addColumnIfMissing(db, FIELD_ELIM_M);
        addColumnIfMissing(db, FIELD_ELIM_F);
        addColumnIfMissing(db, FIELD_ELIM_TOTAL);
        addColumnIfMissing(db, FIELD_MAONI);
    }

    private void addColumnIfMissing(SQLiteDatabase db, String column) {
        if (hasColumn(db, column)) {
            return;
        }
        db.execSQL("ALTER TABLE " + TABLE_MOTHERMENTOR_MOBILIZATION + " ADD COLUMN " + column + " VARCHAR;");
    }

    private boolean hasColumn(SQLiteDatabase db, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_MOTHERMENTOR_MOBILIZATION + ")", null)) {
            while (cursor.moveToNext()) {
                String columnName = cursor.getString(cursor.getColumnIndex("name"));
                if (column.equals(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getObsValue(Event event, String key, boolean preferValues) {
        if (event.getObs() == null) {
            return null;
        }

        for (Obs obs : event.getObs()) {
            if (!key.equals(obs.getFormSubmissionField())) {
                continue;
            }
            if (preferValues && obs.getValues() != null && !obs.getValues().isEmpty()) {
                return obs.getValues().toString();
            }
            Object value = obs.getValue();
            return value == null ? null : String.valueOf(value);
        }
        return null;
    }

    private void updateRelationalId(JSONObject form) throws JSONException {
        if (FAMILY_BASE_ENTITY_ID == null) {
            return;
        }

        JSONArray fields = org.smartregister.chw.mothermentor.util.MotherMentorJsonFormUtils.fields(form, Constants.STEP_ONE);
        JSONObject relationalId = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, org.smartregister.chw.mothermentor.util.DBConstants.KEY.RELATIONAL_ID);
        if (relationalId != null) {
            relationalId.remove(org.smartregister.util.JsonFormUtils.VALUE);
            relationalId.put(org.smartregister.util.JsonFormUtils.VALUE, FAMILY_BASE_ENTITY_ID);
        }
    }
}
