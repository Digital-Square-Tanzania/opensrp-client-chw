package org.smartregister.chw.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.joda.time.LocalDate;

import java.util.Iterator;
import java.util.Map;

public final class MotherMentorPartnerFormUtils {
    public static final String FORM = "mothermentor_enroll_partner";
    public static final String EVENT = "Mother Mentor Enroll Partner";
    private MotherMentorPartnerFormUtils() { }

    public static boolean isEligiblePartner(String gender, String dob) {
        if (gender == null || !"Male".equalsIgnoreCase(gender.trim()) || dob == null || dob.length() < 10) return false;
        try {
            LocalDate birthDate = LocalDate.parse(dob.substring(0, 10));
            return !birthDate.isAfter(LocalDate.now().minusYears(15));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void configureNewClientForm(JSONObject form, String ageError) throws JSONException {
        JSONObject sex = field(form, "sex");
        JSONArray keys = sex.getJSONArray("keys");
        JSONArray labels = sex.getJSONArray("values");
        String maleLabel = "Male";
        for (int i = 0; i < keys.length(); i++) if ("Male".equals(keys.getString(i))) maleLabel = labels.getString(i);
        sex.put("keys", new JSONArray().put("Male"));
        sex.put("values", new JSONArray().put(maleLabel));
        sex.put("value", "Male");
        sex.put("read_only", true);
        sex.put("editable", false);
        field(form, "dob").put("max_date", "today-15y");
        field(form, "age").put("v_min", new JSONObject().put("value", "15").put("err", ageError));
    }

    public static JSONObject prepare(JSONObject form, Map<String, String> mother, Map<String, String> partner,
                                     Map<String, String> previous) throws JSONException {
        for (String key : new String[]{"relationship_to_mother", "other_relationship", "hiv_disclosure", "hiv_tested",
                "test_result", "on_ctc", "partner_ctc"}) {
            if (previous.get(key) != null) set(form, key, previous.get(key), false);
        }
        bindIdentity(form, mother, partner);
        return form;
    }

    public static void bindIdentity(JSONObject form, Map<String, String> mother, Map<String, String> partner) throws JSONException {
        String motherId = mother.get("base_entity_id");
        String partnerId = partner.get("base_entity_id");
        if (motherId == null || partnerId == null || motherId.equals(partnerId)) throw new IllegalArgumentException("Invalid partner identity");
        form.put("entity_id", partnerId);
        form.put("encounter_type", EVENT);
        set(form, "relational_id", partner.get("relational_id"), false);
        set(form, "mother_base_entity_id", motherId, false);
        set(form, "partner_linking", "yes", false);
        set(form, "mother_name", name(mother), true);
        // An absent CTC number can still be entered in the existing required field.
        if (mother.get("ctc_number") != null && !mother.get("ctc_number").trim().isEmpty()) {
            set(form, "mother_ctc_number", mother.get("ctc_number"), true);
        }
        set(form, "partner_name", name(partner), true);
        set(form, "partner_sex", partner.get("gender"), true);
        set(form, "partner_dob", dateOfBirth(partner.get("dob")), true);
        set(form, "partner_phone_number", partner.get("phone_number"), true);
    }

    private static String dateOfBirth(String value) {
        // Family registration stores an ISO timestamp; show the calendar date on the form.
        if (value != null && value.matches("\\d{4}-\\d{2}-\\d{2}.*")) return value.substring(0, 10);
        return value;
    }

    public static String name(Map<String, String> person) {
        StringBuilder name = new StringBuilder();
        for (String key : new String[]{"first_name", "middle_name", "last_name"}) {
            String value = person.get(key);
            if (value != null && !value.trim().isEmpty()) {
                if (name.length() > 0) name.append(' ');
                name.append(value.trim());
            }
        }
        return name.toString();
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
                if (key.equals(field.optString("key"))) {
                    return field;
                }
            }
        }
        throw new JSONException("Missing partner form field: " + key);
    }
}
