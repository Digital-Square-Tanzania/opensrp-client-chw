package org.smartregister.chw.util;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.util.Iterator;
import java.util.Map;

public final class MotherMentorChildEidFormUtils {
    public static final String FORM = "mothermentor_enroll_child_eid";
    public static final String EVENT = "Mother Mentor Enroll Child Eid";
    private static final String BIRTH_ORDER_PATTERN = "(?:0?[1-9]|1[0-9]|20)";
    private static final String CTC_NUMBER_PATTERN = "[0-9]{2}-[0-9]{2}-[0-9]{4}-[0-9]{6}";

    private MotherMentorChildEidFormUtils() {}

    public static boolean isEligibleChild(String dob) {
        return isEligibleChildOn(dob, LocalDate.now());
    }

    public static boolean isEligibleChild(String dob, String approximate) {
        return !"true".equalsIgnoreCase(approximate)
                && !"1".equals(approximate)
                && !DBConstants.KEY.DOB_UNKNOWN.equalsIgnoreCase(approximate)
                && isEligibleChild(dob);
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

    public static void configureNewClientForm(JSONObject form, String ageError)
            throws JSONException {
        LocalDate today = LocalDate.now();
        LocalDate earliestBirthDate = today.minusMonths(18).plusDays(1);
        while (!isEligibleChildOn(earliestBirthDate.toString(), today)) {
            earliestBirthDate = earliestBirthDate.plusDays(1);
        }
        JSONObject dob = field(form, DBConstants.KEY.DOB);
        dob.put("min_date", earliestBirthDate.toString("dd-MM-yyyy"));
        dob.put("max_date", "today");
        dob.remove("relevance");
        dob.put("v_required", new JSONObject().put("value", true).put("err", ageError));
        // An age rounded to years cannot establish the under-18-month boundary.
        JSONObject unknown = field(form, DBConstants.KEY.DOB_UNKNOWN);
        unknown.put("type", "hidden");
        unknown.put("value", "false");
        unknown.remove("options");
        unknown.remove("combine_checkbox_option_values");
        JSONObject age = field(form, DBConstants.KEY.AGE);
        age.put("type", "hidden");
        age.put("value", "");
        age.remove("v_required");
        age.remove("relevance");
    }

    public static JSONObject prepare(
            JSONObject form,
            Map<String, String> mother,
            Map<String, String> child,
            Map<String, String> previous)
            throws JSONException {
        for (String key :
                new String[] {
                    DBConstants.KEY.HEI_REGISTERED,
                    DBConstants.KEY.HEI_NUMBER,
                    DBConstants.KEY.BREASTFEEDING_STATUS,
                    DBConstants.KEY.WEANING_DATE,
                    DBConstants.KEY.LAST_TEST,
                    DBConstants.KEY.LAST_TEST_RESULT
                }) {
            if (previous.get(key) != null) set(form, key, previous.get(key), false);
        }
        bindIdentity(form, mother, child);
        String birthOrder = previous.get(DBConstants.KEY.HEI_FOLLOW_UP_NUMBER);
        String motherCtc = mother.get(DBConstants.KEY.CTC_NUMBER);
        String previousHei = previous.get(DBConstants.KEY.HEI_NUMBER);
        if ((birthOrder == null || !birthOrder.matches(BIRTH_ORDER_PATTERN))
                && motherCtc != null
                && motherCtc.trim().matches(CTC_NUMBER_PATTERN)
                && previousHei != null) {
            String prefix = motherCtc.trim() + "-C";
            if (previousHei.startsWith(prefix)) {
                birthOrder = previousHei.substring(prefix.length());
                // Records created before the birth-order format used an extra hyphen.
                if (birthOrder.startsWith("-")) {
                    birthOrder = birthOrder.substring(1);
                }
                if (birthOrder.length() != 2) {
                    birthOrder = null;
                }
            }
        }
        if (birthOrder != null && birthOrder.matches(BIRTH_ORDER_PATTERN)) {
            set(form, DBConstants.KEY.HEI_FOLLOW_UP_NUMBER, birthOrder, false);
        }
        return form;
    }

    public static void bindIdentity(
            JSONObject form, Map<String, String> mother, Map<String, String> child)
            throws JSONException {
        String motherId = mother.get(DBConstants.KEY.BASE_ENTITY_ID);
        String childId = child.get(DBConstants.KEY.BASE_ENTITY_ID);
        if (motherId == null || childId == null || motherId.equals(childId)) {
            throw new IllegalArgumentException("Invalid child identity");
        }
        form.put(DBConstants.KEY.ENTITY_ID, childId);
        form.put("encounter_type", EVENT);
        set(form, DBConstants.KEY.RELATIONAL_ID, child.get(DBConstants.KEY.RELATIONAL_ID), false);
        set(form, DBConstants.KEY.MOTHER_BASE_ENTITY_ID, motherId, false);
        set(form, DBConstants.KEY.CHILD_LINKING, "yes", false);
        set(form, DBConstants.KEY.MOTHER_NAME, name(mother), true);
        String motherCtc = mother.get(DBConstants.KEY.CTC_NUMBER);
        set(
                form,
                DBConstants.KEY.MOTHER_CTC_NUMBER,
                motherCtc == null ? "" : motherCtc.trim(),
                true);
        set(form, DBConstants.KEY.CHILD_NAME, name(child), true);
        set(form, DBConstants.KEY.CHILD_SEX, child.get(DBConstants.KEY.GENDER), true);
        String dob = child.get(DBConstants.KEY.DOB);
        set(
                form,
                DBConstants.KEY.CHILD_DOB,
                dob != null && dob.length() >= 10 ? dob.substring(0, 10) : dob,
                true);
    }

    public static void bindHeiNumber(JSONObject form, Map<String, String> mother)
            throws JSONException {
        String heiNumber = "";
        if ("yes".equals(field(form, DBConstants.KEY.HEI_REGISTERED).optString("value"))) {
            String motherCtc = mother.get(DBConstants.KEY.CTC_NUMBER);
            String birthOrder = field(form, DBConstants.KEY.HEI_FOLLOW_UP_NUMBER).optString("value");
            if (motherCtc == null
                    || !motherCtc.trim().matches(CTC_NUMBER_PATTERN)
                    || !birthOrder.matches(BIRTH_ORDER_PATTERN)) {
                throw new IllegalArgumentException(
                        "A valid mother's CTC ID and a child's birth order from 1 to 20 are required");
            }
            heiNumber =
                    motherCtc.trim() + "-C" + (birthOrder.length() == 1 ? "0" : "") + birthOrder;
        }
        set(form, DBConstants.KEY.HEI_NUMBER, heiNumber, true);
    }

    public static String name(Map<String, String> person) {
        return MotherMentorPartnerFormUtils.name(person);
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
                if (key.equals(field.optString("key"))) return field;
            }
        }
        throw new JSONException("Missing child form field: " + key);
    }
}
