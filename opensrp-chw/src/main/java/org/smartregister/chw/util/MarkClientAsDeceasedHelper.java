package org.smartregister.chw.util;

import android.content.Context;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.joda.time.format.ISODateTimeFormat;
import org.smartregister.AllConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.PersonDao;
import org.smartregister.chw.ncd.domain.MemberObject;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MarkClientAsDeceasedHelper {

    private static final String REMOVE_REASON_DEATH = "Death";
    private static final String DATE_FORMAT_DISPLAY = "dd-MM-yyyy";
    private static final String DATE_FORMAT_ISO = "yyyy-MM-dd";

    private MarkClientAsDeceasedHelper() {
    }

    public static void markAsDeceased(Context context, MemberObject memberObject,
                                      String dateOfDeath) throws Exception {
        if (memberObject == null || StringUtils.isBlank(memberObject.getBaseEntityId())) {
            throw new IllegalArgumentException("NCD client is required");
        }

        String normalizedDateOfDeath = normalizeDateOfDeath(dateOfDeath);
        String normalizedDateOfBirth = normalizeDateOfBirth(
                PersonDao.getDob(memberObject.getBaseEntityId()));
        JSONObject form = (new FormUtils()).getFormJsonFromRepositoryOrAssets(
                context, CoreConstants.JSON_FORM.FAMILY_DETAILS_REMOVE_MEMBER);
        if (form == null) {
            throw new IllegalStateException("Unable to load Mark as Deceased form");
        }

        org.smartregister.chw.anc.util.JsonFormUtils.getRegistrationForm(
                form,
                memberObject.getBaseEntityId(),
                org.smartregister.Context.getInstance().allSharedPreferences()
                        .getPreference(AllConstants.CURRENT_LOCATION_ID));
        populateForm(form, memberObject, normalizedDateOfBirth, normalizedDateOfDeath);

        String eventType = Utils.removeUser(
                null,
                form,
                Utils.context().allSharedPreferences().fetchRegisteredANM());
        if (!CoreConstants.EventType.REMOVE_MEMBER.equalsIgnoreCase(eventType)) {
            throw new IllegalStateException("Mark as Deceased did not create Remove Family Member event");
        }
    }

    static void populateForm(JSONObject form, MemberObject memberObject,
                             String normalizedDateOfBirth,
                             String normalizedDateOfDeath) throws Exception {
        JSONArray fields = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1)
                .getJSONArray(org.smartregister.family.util.JsonFormUtils.FIELDS);

        updateField(fields, "first_name", memberObject.getFirstName());
        updateField(fields, "middle_name", memberObject.getMiddleName());
        updateField(fields, "last_name", memberObject.getLastName());
        updateField(fields, "sex", memberObject.getGender());
        updateField(fields, "dob", normalizedDateOfBirth);
        updateField(fields, "remove_reason", REMOVE_REASON_DEATH);
        updateField(fields, "date_died", normalizedDateOfDeath);
        updateField(fields, "dod", normalizedDateOfDeath);
    }

    static String normalizeDateOfDeath(String dateOfDeath) throws ParseException {
        return normalizeDate(dateOfDeath, "Date of death is required",
                "Unsupported date of death: ");
    }

    static String normalizeDateOfBirth(String dateOfBirth) throws ParseException {
        return normalizeDate(dateOfBirth, "Client date of birth is missing",
                "Unsupported client date of birth: ");
    }

    private static String normalizeDate(String value, String missingMessage,
                                        String unsupportedMessage) throws ParseException {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(missingMessage);
        }

        Date parsedDate = parseStrictly(value, DATE_FORMAT_DISPLAY);
        if (parsedDate == null) {
            parsedDate = parseStrictly(value, DATE_FORMAT_ISO);
        }
        if (parsedDate != null) {
            return new SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.ENGLISH).format(parsedDate);
        }

        if (!value.matches("^\\d{4}-\\d{2}-\\d{2}T.+$")) {
            throw new ParseException(unsupportedMessage + value, 0);
        }

        try {
            return ISODateTimeFormat.dateOptionalTimeParser()
                    .withOffsetParsed()
                    .parseDateTime(value)
                    .toString(DATE_FORMAT_DISPLAY, Locale.ENGLISH);
        } catch (IllegalArgumentException e) {
            throw new ParseException(unsupportedMessage + value, 0);
        }
    }

    private static Date parseStrictly(String value, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date parsedDate = format.parse(value, position);
        return parsedDate != null && position.getIndex() == value.length() ? parsedDate : null;
    }

    private static void updateField(JSONArray fields, String key, String value) {
        org.smartregister.chw.core.utils.FormUtils.updateFormField(fields, key,
                StringUtils.defaultString(value));
    }
}
