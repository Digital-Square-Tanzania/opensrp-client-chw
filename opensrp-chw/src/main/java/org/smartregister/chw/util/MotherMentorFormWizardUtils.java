package org.smartregister.chw.util;

import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.text.SpannedString;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.activity.MotherMentorVisitWizardActivity;
import org.smartregister.chw.core.R;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorDeliveryChildren;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUtils;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Shared wizard navigation for the library forms opened by Mother Mentor screens. */
public final class MotherMentorFormWizardUtils {
    private static final int SPECIFY_MAX_LENGTH = 100;
    private static final InputFilter.LengthFilter SPECIFY_LENGTH_FILTER =
            new InputFilter.LengthFilter(SPECIFY_MAX_LENGTH);
    private static final SpannedString EMPTY_TEXT = new SpannedString("");
    private static final Set<String> SPECIFY_FIELDS = new HashSet<>(Arrays.asList(
            "mama_sababu_nyingine_text",
            "mama_pregnancy_outcome_other",
            "other_reasons_art_no_reason",
            "reason_client_not_found",
            "reasons_for_not_returning_to_services_while_not_facing_challenges",
            "education_other",
            "referral_other",
            "iit_sababu_nyingine",
            "iit_sababu_kutorudi_other",
            "service_interruption_reason_other",
            "mtoto_not_found_reason_other",
            "mtoto_sababu_kutofika_other",
            "mtoto_sababu_kutopimwa_other",
            "child_eid_non_attendance_reason_other",
            "partner_not_found_reason_other",
            "partner_non_attendance_reason_other",
            "partner_hepatitis_b_not_tested_reason_other",
            "partner_hiv_not_tested_reason_other",
            "partner_syphilis_not_tested_reason_other",
            "household_not_found_reason_other",
            "other_purpose",
            "other_topic",
            "household_referral_other"));

    private MotherMentorFormWizardUtils() {}

    public static Form getFormConfig(Context context) {
        return configureForm(context, new Form());
    }

    public static Intent getFormIntent(Context context, JSONObject jsonForm) {
        String encounterType = jsonForm.optString(JsonFormConstants.ENCOUNTER_TYPE);
        Map<String, String> values = MotherMentorMotherChildUtils.values(jsonForm);
        boolean motherVisit = "Mother Mentor Visit Type".equals(encounterType)
                && (MotherMentorDeliveryChildren.hasSections(values)
                        || MotherMentorMotherChildUtils.field(jsonForm, DBConstants.KEY.MAMA_CHILD_STATUS) != null);
        boolean childHei = "Mother Mentor Child EID HEI".equals(encounterType)
                && "3".equals(values.get(DBConstants.KEY.CHILD_EID_FOLLOW_UP_SCHEMA));
        Intent intent = new Intent(context, motherVisit || childHei ? MotherMentorVisitWizardActivity.class
                : Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, withSpecifyFieldLimits(context, jsonForm, encounterType));
        return configureIntent(context, intent);
    }

    private static String withSpecifyFieldLimits(
            Context context, JSONObject jsonForm, String encounterType) {
        if (!encounterType.startsWith("Mother Mentor ") && !encounterType.startsWith("MotherMentor ")) {
            return jsonForm.toString();
        }

        try {
            JSONObject launchForm = new JSONObject(jsonForm.toString());
            JSONObject limit = new JSONObject()
                    .put(JsonFormConstants.VALUE, SPECIFY_MAX_LENGTH)
                    .put(JsonFormConstants.IS_FIXED_SIZE, true)
                    .put(JsonFormConstants.ERR, context.getString(
                            org.smartregister.chw.R.string.mothermentor_specify_max_length, SPECIFY_MAX_LENGTH));
            boolean householdCounselling =
                    "Mother Mentor Household Visit Received Counselling".equals(encounterType);
            for (Iterator<String> steps = launchForm.keys(); steps.hasNext(); ) {
                JSONObject step = launchForm.optJSONObject(steps.next());
                JSONArray fields = step == null ? null : step.optJSONArray(JsonFormConstants.FIELDS);
                if (fields == null) continue;

                for (int i = 0; i < fields.length(); i++) {
                    JSONObject field = fields.optJSONObject(i);
                    if (field == null || !"edit_text".equals(field.optString(JsonFormConstants.TYPE))) continue;

                    String key = field.optString(JsonFormConstants.KEY);
                    if (!SPECIFY_FIELDS.contains(key) && !(householdCounselling && "other".equals(key))) continue;

                    // Normalize a legacy prefill in the launch copy even if its widget stays hidden.
                    String value = field.optString(JsonFormConstants.VALUE, "");
                    if (value.length() > SPECIFY_MAX_LENGTH) {
                        CharSequence clipped = SPECIFY_LENGTH_FILTER.filter(
                                value, 0, value.length(), EMPTY_TEXT, 0, 0);
                        field.put(JsonFormConstants.VALUE, clipped.toString());
                    }
                    field.put(JsonFormConstants.V_MAX_LENGTH, limit);
                }
            }
            return launchForm.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Could not set Mother Mentor specify field limits", e);
        }
    }

    public static Intent configureIntent(Context context, Intent intent) {
        Form form = (Form) intent.getSerializableExtra(JsonFormConstants.JSON_FORM_KEY.FORM);
        if (form == null) form = new Form();
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, configureForm(context, form));
        return intent;
    }

    private static Form configureForm(Context context, Form form) {
        form.setWizard(true);
        form.setActionBarBackground(R.color.family_actionbar);
        form.setNavigationBackground(R.color.family_navigation);
        form.setHomeAsUpIndicator(R.mipmap.ic_cross_white);
        form.setNextLabel(context.getString(R.string.next));
        form.setPreviousLabel(context.getString(R.string.back));
        form.setSaveLabel(context.getString(R.string.save));
        return form;
    }
}
