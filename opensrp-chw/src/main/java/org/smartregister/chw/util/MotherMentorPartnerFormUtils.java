package org.smartregister.chw.util;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.util.Iterator;
import java.util.Map;

public final class MotherMentorPartnerFormUtils {
    public static final String FORM = "mothermentor_enroll_partner";
    public static final String EVENT = "Mother Mentor Enroll Partner";

    private MotherMentorPartnerFormUtils() {}

    public static boolean isEligiblePartner(String gender, String dob) {
        if (gender == null
                || !"Male".equalsIgnoreCase(gender.trim())
                || dob == null
                || dob.length() < 10) return false;
        try {
            LocalDate birthDate = LocalDate.parse(dob.substring(0, 10));
            return !birthDate.isAfter(LocalDate.now().minusYears(15));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void configureNewClientForm(JSONObject form, String ageError)
            throws JSONException {
        JSONObject sex = field(form, "sex");
        JSONArray keys = sex.getJSONArray("keys");
        JSONArray labels = sex.getJSONArray("values");
        String maleLabel = "Male";
        for (int i = 0; i < keys.length(); i++)
            if ("Male".equals(keys.getString(i))) maleLabel = labels.getString(i);
        sex.put("keys", new JSONArray().put("Male"));
        sex.put("values", new JSONArray().put(maleLabel));
        sex.put("value", "Male");
        sex.put("read_only", true);
        sex.put("editable", false);
        field(form, DBConstants.KEY.DOB).put("max_date", "today-15y");
        field(form, DBConstants.KEY.AGE)
                .put("v_min", new JSONObject().put("value", "15").put("err", ageError));
    }

    public static JSONObject prepare(
            JSONObject form,
            Map<String, String> mother,
            Map<String, String> partner,
            Map<String, String> previous)
            throws JSONException {
        for (String key :
                new String[] {
                    DBConstants.KEY.HIV_DISCLOSURE,
                    DBConstants.KEY.HIV_TESTED,
                    DBConstants.KEY.TEST_RESULT,
                    DBConstants.KEY.ON_CTC,
                    DBConstants.KEY.PARTNER_CTC
                }) {
            if (previous.get(key) != null) set(form, key, previous.get(key), false);
        }
        bindIdentity(form, mother, partner);
        set(form, DBConstants.KEY.PARTNER_LINKAGE_ID, java.util.UUID.randomUUID().toString(), false);
        return form;
    }

    public static void bindIdentity(
            JSONObject form, Map<String, String> mother, Map<String, String> partner)
            throws JSONException {
        String motherId = mother.get(DBConstants.KEY.BASE_ENTITY_ID);
        String partnerId = partner.get(DBConstants.KEY.BASE_ENTITY_ID);
        if (motherId == null || partnerId == null || motherId.equals(partnerId))
            throw new IllegalArgumentException("Invalid partner identity");
        form.put(DBConstants.KEY.ENTITY_ID, partnerId);
        form.put("encounter_type", EVENT);
        set(form, DBConstants.KEY.RELATIONAL_ID, partner.get(DBConstants.KEY.RELATIONAL_ID), false);
        set(form, DBConstants.KEY.MOTHER_BASE_ENTITY_ID, motherId, false);
        set(form, DBConstants.KEY.PARTNER_LINKING, "yes", false);
        set(form, DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID,
                mother.get(DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID), false);
        set(form, DBConstants.KEY.PARTNER_MOTHER_PREGNANT,
                "pregnant".equals(mother.get(DBConstants.KEY.PREGNANCY_STATUS)) ? "yes"
                        : ("not_pregnant".equals(mother.get(DBConstants.KEY.PREGNANCY_STATUS))
                                || "breast_feeding".equals(mother.get(DBConstants.KEY.PREGNANCY_STATUS)))
                                ? "no" : "unknown", false);
        set(form, DBConstants.KEY.MOTHER_NAME, name(mother), true);
        String motherCtc = mother.get(DBConstants.KEY.CTC_NUMBER);
        set(
                form,
                DBConstants.KEY.MOTHER_CTC_NUMBER,
                motherCtc == null ? "" : motherCtc.trim(),
                true);
        set(form, DBConstants.KEY.PARTNER_NAME, name(partner), true);
        set(form, DBConstants.KEY.PARTNER_SEX, partner.get(DBConstants.KEY.GENDER), true);
        set(form, DBConstants.KEY.PARTNER_DOB, dateOfBirth(partner.get(DBConstants.KEY.DOB)), true);
        set(
                form,
                DBConstants.KEY.PARTNER_PHONE_NUMBER,
                partner.get(DBConstants.KEY.PHONE_NUMBER),
                true);
    }

    private static String dateOfBirth(String value) {
        // Family registration stores an ISO timestamp; show the calendar date on the form.
        if (value != null && value.matches("\\d{4}-\\d{2}-\\d{2}.*")) return value.substring(0, 10);
        return value;
    }

    public static String name(Map<String, String> person) {
        StringBuilder name = new StringBuilder();
        for (String key :
                new String[] {
                    DBConstants.KEY.FIRST_NAME,
                    DBConstants.KEY.MIDDLE_NAME,
                    DBConstants.KEY.LAST_NAME
                }) {
            String value = person.get(key);
            if (value != null && !value.trim().isEmpty()) {
                if (name.length() > 0) name.append(' ');
                name.append(value.trim());
            }
        }
        return name.toString();
    }

    private static void set(JSONObject form, String key, String value, boolean readOnly)
            throws JSONException {
        JSONObject field = field(form, key);
        field.put("value", value == null ? "" : value);
        if (readOnly) {
            field.put("read_only", true);
            // Native forms give editable precedence over read_only, even when it is false.
            field.remove("editable");
        }
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
