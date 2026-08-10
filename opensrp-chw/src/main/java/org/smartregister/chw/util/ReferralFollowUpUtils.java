package org.smartregister.chw.util;

import static org.smartregister.util.JsonFormUtils.VALUE;
import static org.smartregister.util.JsonFormUtils.getFieldJSONObject;

import android.app.Activity;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.Task;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * Launches and persists the community level referral follow-up form (WAJA "Manual Closure of
 * Referrals" flow). The CHW opens this form for a client who still has an open referral, records
 * whether the client attended and whether the services were received, and the referral task is
 * closed from the answers.
 */
public class ReferralFollowUpUtils {

    public static final String REFERRAL_TASK_ID = "referral_task_id";
    public static final String FOLLOWUP_DATE = "chw_followup_date";
    public static final String FACILITY_ATTENDED = "facility_attended";
    public static final String CLIENT_CONDITION = "client_condition";
    public static final String EXHIBITS_DANGER_SIGNS = "exhibits_danger_signs";

    private static final String ENTITY_ID = "entity_id";
    private static final String DATE_FORMAT = "dd-MM-yyyy";

    private ReferralFollowUpUtils() {
        // Utility class
    }

    /**
     * Opens the referral follow-up form for the given client, pre-filled with the referral task
     * that the answers will close.
     */
    public static void startReferralFollowUpForm(Activity activity, String baseEntityId, String taskId) {
        try {
            JSONObject form = new FormUtils().getFormJsonFromRepositoryOrAssets(
                    activity, Constants.JsonForm.getReferralFollowUpForm()
            );
            if (form == null) {
                Timber.e("Referral follow-up form %s could not be loaded", Constants.JsonForm.getReferralFollowUpForm());
                return;
            }
            prepopulate(form, baseEntityId, taskId);
            activity.startActivityForResult(
                    org.smartregister.chw.core.utils.FormUtils.getStartFormActivity(
                            form, activity.getString(R.string.referral_follow_up), activity
                    ),
                    JsonFormUtils.REQUEST_CODE_GET_JSON
            );
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static void prepopulate(JSONObject form, String baseEntityId, String taskId) throws Exception {
        form.put(ENTITY_ID, baseEntityId);

        JSONArray fields = org.smartregister.family.util.JsonFormUtils.fields(
                form, Constants.JsonFormConstants.STEP1
        );

        JSONObject taskField = getFieldJSONObject(fields, REFERRAL_TASK_ID);
        if (taskField != null) {
            taskField.put(VALUE, StringUtils.defaultString(taskId));
        }

        JSONObject dateField = getFieldJSONObject(fields, FOLLOWUP_DATE);
        if (dateField != null) {
            dateField.put(VALUE, new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date()));
        }

        // The facility the client actually attended is only known at runtime, so the spinner ships
        // with an empty option list that is filled from the locations the device has synced.
        Map<String, String> facilityOptions = LocationUtils.INSTANCE.getFacilitiesKeyAndName();
        JsonFormUtilsFlv.overwriteQuestionOptions(FACILITY_ATTENDED, facilityOptions, form);
    }

    /**
     * Persists the follow-up answers as a {@code Referral Followup Registration} event so they land
     * in {@code ec_referral_followup} and sync to the server.
     */
    public static void saveReferralFollowUp(String jsonString, String baseEntityId) throws Exception {
        AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
        Event baseEvent = org.smartregister.chw.anc.util.JsonFormUtils.processJsonForm(
                allSharedPreferences,
                CoreReferralUtils.setEntityId(jsonString, baseEntityId),
                Constants.TableName.REFERRAL_FOLLOWUP
        );
        org.smartregister.chw.anc.util.JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
        NCUtils.processEvent(
                baseEvent.getBaseEntityId(),
                new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(baseEvent))
        );
    }

    /**
     * Closes the referral task the follow-up was recorded against. Returns false when the task can
     * no longer be resolved so the caller can leave the referral open rather than silently drop it.
     */
    public static boolean completeReferralTask(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            Timber.e("Referral follow-up submitted without a referral task id");
            return false;
        }
        Task task = ChwApplication.getInstance().getTaskRepository().getTaskByIdentifier(taskId);
        if (task == null) {
            Timber.e("Referral task %s could not be resolved, leaving the referral open", taskId);
            return false;
        }
        CoreReferralUtils.completeTask(task, true);
        return true;
    }

    public static String getFieldValue(JSONArray fields, String key) {
        JSONObject field = getFieldJSONObject(fields, key);
        return field == null ? "" : field.optString(VALUE);
    }

    /**
     * QN6 — a client still exhibiting danger signs at follow-up needs a fresh referral. This must
     * run after the original task has been closed, otherwise the open-referral guard rejects it.
     */
    public static boolean exhibitsDangerSigns(JSONArray fields) {
        return EXHIBITS_DANGER_SIGNS.equalsIgnoreCase(getFieldValue(fields, CLIENT_CONDITION));
    }
}
