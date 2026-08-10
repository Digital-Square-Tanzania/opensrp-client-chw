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
import org.smartregister.chw.model.NcdReferralInputs;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.Task;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
     * Opens the referral follow-up form for the referral behind the given task.
     *
     * <p>The client is read from the task's {@code forEntity} and NOT from the register row: the
     * Referral register selects {@code ec_referral.id AS _id}, so {@code client.getCaseId()} is the
     * referral row's id, not a person. Events written against that id belong to no client, so the
     * client processor silently drops them and nothing reaches the {@code ec_} tables.
     */
    public static void startReferralFollowUpForm(Activity activity, String taskId) {
        Task task = resolveTask(taskId);
        if (task == null || StringUtils.isBlank(task.getForEntity())) {
            Timber.e("Referral task %s could not be resolved; not opening the follow-up form", taskId);
            return;
        }
        String baseEntityId = task.getForEntity();

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
        Task task = resolveTask(taskId);
        if (task == null) {
            Timber.e("Referral task %s could not be resolved, leaving the referral open", taskId);
            return false;
        }
        CoreReferralUtils.completeTask(task, true);
        return true;
    }

    private static Task resolveTask(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            Timber.e("Referral follow-up attempted without a referral task id");
            return null;
        }
        return ChwApplication.getInstance().getTaskRepository().getTaskByIdentifier(taskId);
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

    /**
     * Opens the short referral form used when the follow-up shows the client's condition is still
     * unresolved. Only the referral reason, facility and emergency flag are asked — everything else
     * the referral needs is derived.
     */
    public static void startUnresolvedConditionReferralForm(Activity activity, String baseEntityId) {
        try {
            JSONObject form = new FormUtils().getFormJsonFromRepositoryOrAssets(
                    activity, Constants.JsonForm.getReferralFollowUpReferralForm()
            );
            if (form == null) {
                Timber.e("Referral form %s could not be loaded",
                        Constants.JsonForm.getReferralFollowUpReferralForm());
                return;
            }
            form.put(ENTITY_ID, baseEntityId);
            JsonFormUtilsFlv.overwriteQuestionOptions(
                    Constants.ReferralFollowUp.REFERRAL_HF,
                    LocationUtils.INSTANCE.getFacilitiesKeyAndName(),
                    form
            );
            activity.startActivityForResult(
                    org.smartregister.chw.core.utils.FormUtils.getStartFormActivity(
                            form, activity.getString(R.string.referral_unresolved_condition), activity
                    ),
                    JsonFormUtils.REQUEST_CODE_GET_JSON
            );
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    /**
     * Turns the submitted referral form into a Referral Registration event plus its task, so the
     * client reappears in the Referral register as a fresh open referral.
     *
     * @return true when the referral was created
     */
    public static boolean createUnresolvedConditionReferral(JSONArray fields, String baseEntityId) {
        List<String> problemKeys = new ArrayList<>();
        List<String> problemLabels = new ArrayList<>();
        collectSelectedOptions(fields, Constants.ReferralFollowUp.PROBLEM, problemKeys, problemLabels);

        if (problemKeys.isEmpty()) {
            Timber.e("Referral for %s submitted with no reason selected", baseEntityId);
            return false;
        }

        String otherReason = getFieldValue(fields, Constants.ReferralFollowUp.PROBLEM_OTHER);
        if (StringUtils.isNotBlank(otherReason)) {
            // Keep the coded "other" key, but show what the CHW actually typed.
            int otherIndex = problemKeys.indexOf("other");
            if (otherIndex >= 0) {
                problemLabels.set(otherIndex, otherReason.trim());
            }
        }

        String facilityId = getFieldValue(fields, Constants.ReferralFollowUp.REFERRAL_HF);
        NcdReferralInputs inputs = new NcdReferralInputs(
                getFieldValue(fields, Constants.ReferralFollowUp.IS_EMERGENCY_CASE),
                null, null, null, null,
                facilityId,
                optionLabel(fields, Constants.ReferralFollowUp.REFERRAL_HF, facilityId)
        );

        return ReferralTaskFactory.createReferral(
                baseEntityId,
                Constants.ReferralFollowUp.FOCUS_UNRESOLVED_REFERRAL,
                Constants.ReferralFollowUp.TASK_CODE,
                Constants.ReferralFollowUp.TASK_PRIORITY,
                StringUtils.join(problemLabels, ", "),
                problemKeys,
                problemLabels,
                inputs
        );
    }

    /**
     * Reads a {@code combine_checkbox_option_values} field. The widget writes the selection back as
     * a bracketed CSV of option keys on the parent field; older widgets instead flag each option, so
     * both are handled. Labels come from the form's own options, which keeps them in the CHW's
     * language.
     */
    private static void collectSelectedOptions(JSONArray fields, String key,
                                               List<String> keysOut, List<String> labelsOut) {
        JSONObject field = getFieldJSONObject(fields, key);
        if (field == null) {
            return;
        }
        JSONArray options = field.optJSONArray("options");
        if (options == null) {
            return;
        }

        Set<String> selected = new LinkedHashSet<>();
        String combined = field.optString(VALUE);
        if (StringUtils.isNotBlank(combined)) {
            for (String token : StringUtils.strip(combined, "[]").split(",")) {
                if (StringUtils.isNotBlank(token)) {
                    selected.add(token.trim());
                }
            }
        }

        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option == null) {
                continue;
            }
            String optionKey = option.optString("key");
            if (selected.contains(optionKey) || "true".equalsIgnoreCase(option.optString(VALUE))) {
                if (!keysOut.contains(optionKey)) {
                    keysOut.add(optionKey);
                    labelsOut.add(option.optString("text", optionKey));
                }
            }
        }
    }

    /** Resolves a spinner option's display text from its key, falling back to the key itself. */
    private static String optionLabel(JSONArray fields, String fieldKey, String optionKey) {
        JSONObject field = getFieldJSONObject(fields, fieldKey);
        if (field == null || StringUtils.isBlank(optionKey)) {
            return null;
        }
        JSONArray options = field.optJSONArray("options");
        for (int i = 0; options != null && i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null && optionKey.equals(option.optString("key"))) {
                return option.optString("text", optionKey);
            }
        }
        return null;
    }
}
