package org.smartregister.chw.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(application = android.app.Application.class, sdk = 28, manifest = org.robolectric.annotation.Config.NONE)
public class MotherMentorPartnerFormUtilsTest {
    @Test public void newRegistrationRestrictsSexDobAndEstimatedAgeInBothLocales() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "all_clients_registration_form.json");
            MotherMentorPartnerFormUtils.configureNewClientForm(form, "15 or older");
            JSONArray fields = form.getJSONObject("step2").getJSONArray("fields");
            int checked = 0;
            for (int i = 0; i < fields.length(); i++) {
                JSONObject f = fields.getJSONObject(i);
                if ("sex".equals(f.optString("key"))) {
                    assertEquals("Male", f.getString("value")); assertEquals(1, f.getJSONArray("keys").length());
                    assertEquals(locale.equals("json.form") ? "Male" : "Me", f.getJSONArray("values").getString(0)); checked++;
                } else if ("dob".equals(f.optString("key"))) { assertEquals("today-15y", f.getString("max_date")); checked++; }
                else if ("age".equals(f.optString("key"))) { assertEquals("15", f.getJSONObject("v_min").getString("value")); checked++; }
            }
            assertEquals(3, checked);
        }
    }
    @Test public void eligibilityIncludesFifteenthBirthdayAndRejectsUnverifiedAge() {
        String cutoff = org.joda.time.LocalDate.now().minusYears(15).toString();
        assertTrue(MotherMentorPartnerFormUtils.isEligiblePartner("Male", cutoff + "T03:00:00.000+03:00"));
        assertFalse(MotherMentorPartnerFormUtils.isEligiblePartner("Female", "1990-01-01"));
        assertFalse(MotherMentorPartnerFormUtils.isEligiblePartner("Male", org.joda.time.LocalDate.parse(cutoff).plusDays(1).toString()));
        assertFalse(MotherMentorPartnerFormUtils.isEligiblePartner("Male", null));
        assertFalse(MotherMentorPartnerFormUtils.isEligiblePartner("Male", "2000-02-31"));
    }
    @Test public void bothLocalesBindPartnerIdentityAndRetainClinicalAnswers() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = form(locale);
            Map<String, String> mother = person("mother", "mother-family", "Asha");
            mother.put("ctc_number", "12-34-5678-123456");
            Map<String, String> partner = person("partner", "partner-family", "Juma");
            partner.put("gender", "Male"); partner.put("dob", "1990-01-01"); partner.put("phone_number", "0700000000");
            Map<String, String> previous = new HashMap<>();
            previous.put("relationship_to_mother", "husband_or_partner"); previous.put("hiv_tested", "no");
            MotherMentorPartnerFormUtils.prepare(form, mother, partner, previous);
            assertEquals("partner", form.getString("entity_id"));
            assertEquals("partner-family", value(form, "relational_id"));
            assertEquals("mother", value(form, "mother_base_entity_id"));
            assertEquals("Asha Mtemi", value(form, "mother_name"));
            assertEquals("12-34-5678-123456", value(form, "mother_ctc_number"));
            assertEquals("Juma Mtemi", value(form, "partner_name"));
            assertEquals("Male", value(form, "partner_sex"));
            assertEquals("1990-01-01", value(form, "partner_dob"));
            assertEquals("0700000000", value(form, "partner_phone_number"));
            assertEquals("husband_or_partner", value(form, "relationship_to_mother"));
            assertEquals("no", value(form, "hiv_tested"));
            assertTrue(field(form, "mother_name").getBoolean("read_only"));
            assertTrue(field(form, "partner_name").getBoolean("read_only"));
            // Restoring trusted identity on save cannot overwrite the selected person's clinical answer.
            field(form, "hiv_tested").put("value", "yes");
            form.put("entity_id", "mother"); field(form, "relational_id").put("value", "mother-family");
            MotherMentorPartnerFormUtils.bindIdentity(form, mother, partner);
            assertEquals("partner", form.getString("entity_id"));
            assertEquals("partner-family", value(form, "relational_id"));
            assertEquals("yes", value(form, "hiv_tested"));
        }
    }
    @Test public void missingMotherCtcAllowsEntryAndDoesNotEraseItOnSave() throws Exception {
        JSONObject form = form("json.form");
        Map<String, String> mother = person("mother", "family", "Asha");
        Map<String, String> partner = person("partner", "family", "Juma");
        MotherMentorPartnerFormUtils.bindIdentity(form, mother, partner);
        assertFalse(field(form, "mother_ctc_number").optBoolean("read_only"));
        field(form, "mother_ctc_number").put("value", "12-34-5678-123456");
        MotherMentorPartnerFormUtils.bindIdentity(form, mother, partner);
        assertEquals("12-34-5678-123456", value(form, "mother_ctc_number"));
    }
    @Test(expected = IllegalArgumentException.class) public void cannotEnrollMotherAsOwnPartner() throws Exception {
        Map<String, String> mother = person("mother", "family", "Asha");
        MotherMentorPartnerFormUtils.bindIdentity(form("json.form"), mother, mother);
    }
    @Test public void legacyEntryKeepsIdentityFieldsHiddenUntilLinking() throws Exception {
        for (String locale : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = form(locale);
            assertEquals("no", value(form, "partner_linking"));
            assertEquals("hidden", field(form, "mother_base_entity_id").getString("type"));
            for (String key : new String[]{"partner_name", "partner_sex", "partner_dob", "partner_phone_number"}) {
                assertEquals("equalTo(., \"yes\")", field(form, key).getJSONObject("relevance").getJSONObject("step1:partner_linking").getString("ex"));
            }
        }
    }
    private static Map<String, String> person(String id, String family, String name) {
        Map<String, String> values = new HashMap<>(); values.put("base_entity_id", id); values.put("relational_id", family);
        values.put("first_name", name); values.put("last_name", "Mtemi"); return values;
    }
    private JSONObject form(String locale) throws Exception {
        return MotherMentorTestAssets.form(locale, "mothermentor_enroll_partner.json");
    }
    private String value(JSONObject form, String key) throws Exception { return field(form, key).getString("value"); }
    private JSONObject field(JSONObject form, String key) throws Exception {
        for (int step = 1; step <= 4; step++) {
            JSONArray fields = form.getJSONObject("step" + step).getJSONArray("fields");
            for (int i = 0; i < fields.length(); i++) if (key.equals(fields.getJSONObject(i).optString("key"))) return fields.getJSONObject(i);
        }
        throw new AssertionError("Missing " + key);
    }
}
