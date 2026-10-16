package org.smartregister.chw.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.joda.time.LocalDate;

import java.util.Iterator;
import java.util.Map;

public final class MotherMentorChildEidFormUtils {
    public static final String FORM = "mothermentor_enroll_child_eid";
    public static final String EVENT = "Mother Mentor Enroll Child Eid";

    private MotherMentorChildEidFormUtils() { }

    public static boolean isEligibleChild(String dob) {
        return isEligibleChildOn(dob, LocalDate.now());
    }

    public static boolean isEligibleChild(String dob, String approximate) {
        return !"true".equalsIgnoreCase(approximate) && !"1".equals(approximate)
                && !"dob_unknown".equalsIgnoreCase(approximate) && isEligibleChild(dob);
    }

    static boolean isEligibleChildOn(String dob, LocalDate today) {
        if (dob == null || dob.length() < 10) return false;
        try {
            LocalDate birthDate = LocalDate.parse(dob.substring(0, 10));
            // Compare the actual 18-month anniversary, including month-end birthdays.
            return !birthDate.isAfter(today) && today.isBefore(birthDate.plusMonths(18));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void configureNewClientForm(JSONObject form, String ageError) throws JSONException {
        LocalDate today = LocalDate.now();
        LocalDate earliestBirthDate = today.minusMonths(18).plusDays(1);
        while (!isEligibleChildOn(earliestBirthDate.toString(), today)) {
            earliestBirthDate = earliestBirthDate.plusDays(1);
        }
        JSONObject dob = field(form, "dob");
        dob.put("min_date", earliestBirthDate.toString("dd-MM-yyyy"));
        dob.put("max_date", "today");
        dob.remove("relevance");
        dob.put("v_required", new JSONObject().put("value", true).put("err", ageError));
        // An age rounded to years cannot establish the under-18-month boundary.
        JSONObject unknown = field(form, "dob_unknown");
        unknown.put("type", "hidden");
        unknown.put("value", "false");
        unknown.remove("options");
        unknown.remove("combine_checkbox_option_values");
        JSONObject age = field(form, "age");
        age.put("type", "hidden");
        age.put("value", "");
        age.remove("v_required");
        age.remove("relevance");
    }

    public static JSONObject prepare(JSONObject form, Map<String, String> mother, Map<String, String> child,
                                     Map<String, String> previous) throws JSONException {
        for (String key : new String[]{"hei_registered", "hei_number", "breastfeeding_status", "weaning_date",
                "last_test", "last_test_result"}) {
            if (previous.get(key) != null) set(form, key, previous.get(key), false);
        }
        bindIdentity(form, mother, child);
        return form;
    }

    public static void bindIdentity(JSONObject form, Map<String, String> mother, Map<String, String> child) throws JSONException {
        String motherId = mother.get("base_entity_id");
        String childId = child.get("base_entity_id");
        if (motherId == null || childId == null || motherId.equals(childId)) {
            throw new IllegalArgumentException("Invalid child identity");
        }
        form.put("entity_id", childId);
        form.put("encounter_type", EVENT);
        set(form, "relational_id", child.get("relational_id"), false);
        set(form, "mother_base_entity_id", motherId, false);
        set(form, "child_linking", "yes", false);
        set(form, "mother_name", name(mother), true);
        set(form, "child_name", name(child), true);
        set(form, "child_sex", child.get("gender"), true);
        String dob = child.get("dob");
        set(form, "child_dob", dob != null && dob.length() >= 10 ? dob.substring(0, 10) : dob, true);
    }

    public static String name(Map<String, String> person) {
        return MotherMentorPartnerFormUtils.name(person);
    }

    private static void set(JSONObject form, String key, String value, boolean readOnly) throws JSONException {
        JSONObject field = field(form, key);
        field.put("value", value == null ? "" : value);
        if (readOnly) { field.put("read_only", true); field.put("editable", false); }
    }

    private static JSONObject field(JSONObject form, String key) throws JSONException {
        Iterator<String> steps = form.keys();
        while (steps.hasNext()) {
            JSONObject step = form.optJSONObject(steps.next());
            JSONArray fields = step == null ? null : step.optJSONArray("fields");
            if (fields == null) continue;
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                if (key.equals(field.optString("key"))) return field;
            }
        }
        throw new JSONException("Missing child form field: " + key);
    }
}
