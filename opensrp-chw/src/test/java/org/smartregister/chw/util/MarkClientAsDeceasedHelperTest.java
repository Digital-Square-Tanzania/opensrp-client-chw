package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.ncd.domain.MemberObject;

public class MarkClientAsDeceasedHelperTest {

    @Test
    public void normalizesIsoDateForRemoveMemberProcessor() throws Exception {
        assertEquals("10-06-2026",
                MarkClientAsDeceasedHelper.normalizeDateOfDeath("2026-06-10"));
    }

    @Test
    public void preservesDisplayFormattedDate() throws Exception {
        assertEquals("10-06-2026",
                MarkClientAsDeceasedHelper.normalizeDateOfDeath("10-06-2026"));
    }

    @Test
    public void normalizesFamilyMemberDateOfBirth() throws Exception {
        assertEquals("14-02-1985",
                MarkClientAsDeceasedHelper.normalizeDateOfBirth("1985-02-14"));
    }

    @Test
    public void normalizesFamilyMemberIsoTimestampDateOfBirth() throws Exception {
        assertEquals("20-06-1994", MarkClientAsDeceasedHelper.normalizeDateOfBirth(
                "1994-06-20T03:00:00.000+03:00"));
    }

    @Test(expected = java.text.ParseException.class)
    public void rejectsNumericAgeAsDateOfBirth() throws Exception {
        MarkClientAsDeceasedHelper.normalizeDateOfBirth("41");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingDateOfBirth() throws Exception {
        MarkClientAsDeceasedHelper.normalizeDateOfBirth("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingDate() throws Exception {
        MarkClientAsDeceasedHelper.normalizeDateOfDeath("");
    }

    @Test
    public void populatesRemoveMemberWorkflowFields() throws Exception {
        JSONObject form = new JSONObject("{\"step1\":{\"fields\":["
                + field("first_name") + "," + field("middle_name") + ","
                + field("last_name") + "," + field("sex") + ","
                + field("dob") + "," + field("remove_reason") + ","
                + field("date_died") + "," + field("dod") + "]}}");
        MemberObject member = new MemberObject();
        member.setFirstName("Asha");
        member.setMiddleName("Juma");
        member.setLastName("Moshi");
        member.setDob("41");
        member.setGender("Female");

        MarkClientAsDeceasedHelper.populateForm(
                form, member, "14-02-1985", "10-06-2026");

        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        assertEquals("Asha", value(fields, "first_name"));
        assertEquals("Juma", value(fields, "middle_name"));
        assertEquals("Moshi", value(fields, "last_name"));
        assertEquals("Female", value(fields, "sex"));
        assertEquals("14-02-1985", value(fields, "dob"));
        assertEquals("Death", value(fields, "remove_reason"));
        assertEquals("10-06-2026", value(fields, "date_died"));
        assertEquals("10-06-2026", value(fields, "dod"));
    }

    private String field(String key) {
        return "{\"key\":\"" + key + "\",\"value\":\"\"}";
    }

    private String value(JSONArray fields, String key) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString("key"))) {
                return field.optString("value");
            }
        }
        return null;
    }
}
