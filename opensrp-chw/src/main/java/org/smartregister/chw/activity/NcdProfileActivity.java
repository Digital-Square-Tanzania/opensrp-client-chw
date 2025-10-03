package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.ncd.activity.BaseNcdProfileActivity;
import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.util.Locale;

import net.sqlcipher.database.SQLiteDatabase;

import timber.log.Timber;

/**
 * Concrete implementation of BaseNcdProfileActivity.
 * Register this activity in AndroidManifest.xml and use it to launch NCD profile screens.
 */
public class NcdProfileActivity extends BaseNcdProfileActivity {
    private static final String EXTRA_IS_CONFIRMED_NCD = "extra_is_confirmed_ncd";
    private static final String CONFIRMATION_TABLE = "ec_diabetes_hypertension_confirmation";
    private static final String COLUMN_DIABETES_RESULT = "diabetes_result";
    private static final String COLUMN_HYPERTENSION_RESULT = "hypertension_result";
    private static final String COLUMN_LAST_INTERACTED_WITH = "last_interacted_with";
    private static final String COLUMN_VISIT_DATE = "visit_date";
    boolean isConfirmedNcd = false;

    /**
     * Use this method to start the NcdProfileActivity.
     * @param activity The calling activity
     * @param baseEntityId The base entity id to pass
     */
    public static void startProfileActivity(Activity activity, String baseEntityId, boolean isConfirmedNcd) {
        Intent intent = new Intent(activity, NcdProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(EXTRA_IS_CONFIRMED_NCD, isConfirmedNcd);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isConfirmedNcd = getIntent().getBooleanExtra(EXTRA_IS_CONFIRMED_NCD, false);
    }

    @Override
    public void startServiceForm() {

    }

    @Override
    public void continueService() {

    }

    @Override
    public void continueDischarge() {

    }

    @Override
    public void openFollowupVisit() {
        if (memberObject == null) {
            Timber.w("Member object is null, cannot continue with NCD visit");
            return;
        }

        if (shouldOpenNcdVisit(memberObject.getBaseEntityId())) {
            NcdVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
            return;
        }

        try {
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(
                    NcdProfileActivity.this,
                    org.smartregister.chw.util.Constants.JsonForm.getDiabetesFollowupForm()
            );
            if (formJsonObject != null) {
                formJsonObject.put("entity_id", memberObject.getBaseEntityId());
                startFormActivity(formJsonObject);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void startFormActivity(JSONObject jsonForm) {

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());


        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean shouldOpenNcdVisit(String baseEntityId) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return false;
        }

        if (isConfirmedNcd) {
            return true;
        }

        return hasConfirmedDiagnosis(baseEntityId);
    }

    private boolean hasConfirmedDiagnosis(String baseEntityId) {
        Cursor cursor = null;
        try {
            SQLiteDatabase database = ChwApplication.getInstance().getRepository().getReadableDatabase();
            String query = "SELECT " + COLUMN_DIABETES_RESULT + ", " + COLUMN_HYPERTENSION_RESULT +
                    " FROM " + CONFIRMATION_TABLE +
                    " WHERE base_entity_id = ? ORDER BY CASE WHEN " + COLUMN_LAST_INTERACTED_WITH +
                    " IS NOT NULL THEN " + COLUMN_LAST_INTERACTED_WITH + " ELSE " + COLUMN_VISIT_DATE +
                    " END DESC LIMIT 1";

            cursor = database.rawQuery(query, new String[]{baseEntityId});
            if (cursor != null && cursor.moveToFirst()) {
                String diabetesResult = getValue(cursor, COLUMN_DIABETES_RESULT);
                String hypertensionResult = getValue(cursor, COLUMN_HYPERTENSION_RESULT);
                return isConfirmedResult(diabetesResult) || isConfirmedResult(hypertensionResult);
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    private String getValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex >= 0 ? cursor.getString(columnIndex) : null;
    }

    private boolean isConfirmedResult(String result) {
        if (TextUtils.isEmpty(result)) {
            return false;
        }

        String normalized = result.trim().toLowerCase(Locale.US);
        return "positive".equals(normalized)
                || "confirmed".equals(normalized)
                || "yes".equals(normalized)
                || "true".equals(normalized);
    }
}
