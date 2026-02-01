package org.smartregister.chw.util;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.dao.AbstractDao;

import java.util.ArrayList;

import timber.log.Timber;

public class DatabaseMigrationUtils extends AbstractDao {

    public static void fillFamilyMemberLocationTableWithProviderIds(SQLiteDatabase db) {
        ArrayList<String> submissionsIds = getFormSubmissionsIds(db);
        ArrayList<String> jsonLists = getJSONLists(db, submissionsIds);
        if (db != null && db.isOpen())
            db.rawExecSQL(buildUpdateQuery(jsonLists));
    }

    public static String buildUpdateQuery(ArrayList<String> jsonLists) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("UPDATE ec_family_member_location SET provider_id = (case ");
        for (String item : jsonLists) {
            try {
                JSONObject jsonObject = new JSONObject(item);
                String formSubmissionId = jsonObject.getString("formSubmissionId");
                String providerId = jsonObject.getString("providerId");

                queryBuilder.append("when form_submission_id = '")
                        .append(formSubmissionId)
                        .append("' then '").append(providerId).append("'");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        queryBuilder.append(" end)");
        return queryBuilder.toString();
    }

    public static ArrayList<String> getJSONLists(SQLiteDatabase db, ArrayList<String> submissionsIds) {
        String _SubmissionsIds = "('" + StringUtils.join(submissionsIds, "','") + "')";
        ArrayList<String> JSONLists = new ArrayList<>();
        try {
            String query = "SELECT json FROM event WHERE formSubmissionId IN " + _SubmissionsIds;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONLists.add(cursor.getString(cursor.getColumnIndex("json")));
                }
            }
            return JSONLists;
        } catch (Exception ex) {
            Timber.e(ex);
        }
        return JSONLists;
    }

    public static ArrayList<String> getFormSubmissionsIds(SQLiteDatabase db) {
        ArrayList<String> formSubmissionIds = new ArrayList<>();
        try {
            String query = "SELECT form_submission_id FROM ec_family_member_location";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    formSubmissionIds.add(cursor.getString(cursor.getColumnIndex("form_submission_id")));
                }
            }
            return formSubmissionIds;
        } catch (Exception ex) {
            Timber.e(ex);
        }
        return formSubmissionIds;
    }
}
