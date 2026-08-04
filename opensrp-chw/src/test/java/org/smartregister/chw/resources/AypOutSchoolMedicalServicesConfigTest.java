package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AypOutSchoolMedicalServicesConfigTest {

    private static final String TABLE_NAME = "ec_ayp_out_school_client_followup_visits";
    private static final String[] NEW_PERSISTED_FIELDS = {
            "hiv_positive", "client_hiv_status", "ctc_number",
            "hiv_tested_within_last_3_months", "hiv_result_recent", "ctc_number_a",
            "on_prep", "prep_facility_a", "linked_to_prep_recent", "tested_for_hiv",
            "testing_location", "facility_name", "test_date", "hiv_result", "ctc_number_b",
            "prep_follow_up", "prep_facility_b", "linked_to_prep"
    };

    @Test
    public void englishAndSwahiliFormsHaveMatchingWorkflowFields() throws Exception {
        JSONObject english = form("json.form");
        JSONObject swahili = form("json.form-sw");
        JSONArray englishFields = fields(english);
        JSONArray swahiliFields = fields(swahili);

        Assert.assertEquals(fieldKeys(englishFields), fieldKeys(swahiliFields));
        Assert.assertEquals(indexOf(englishFields, "received_hpv_vaccine") + 1,
                indexOf(englishFields, "hiv_tested_within_last_3_months"));
        Assert.assertEquals(indexOf(swahiliFields, "received_hpv_vaccine") + 1,
                indexOf(swahiliFields, "hiv_tested_within_last_3_months"));

        assertRequiredRadio(englishFields, "hiv_tested_within_last_3_months", "yes", "no");
        assertRequiredRadio(englishFields, "hiv_result_recent", "negative", "positive");
        assertRequiredRadio(englishFields, "hiv_result", "negative", "positive");
        assertRequiredRadio(swahiliFields, "hiv_tested_within_last_3_months", "yes", "no");
        assertRequiredRadio(swahiliFields, "hiv_result_recent", "negative", "positive");
        assertRequiredRadio(swahiliFields, "hiv_result", "negative", "positive");

        assertRequiredType(englishFields, "ctc_number_a", "edit_text");
        assertRequiredType(englishFields, "ctc_number_b", "edit_text");
        assertRequiredType(englishFields, "facility_name", "edit_text");
        assertRequiredType(englishFields, "test_date", "date_picker");
        assertRequiredType(englishFields, "prep_facility_a", "edit_text");
        assertRequiredType(englishFields, "prep_facility_b", "edit_text");
        assertRequiredType(swahiliFields, "ctc_number_a", "edit_text");
        assertRequiredType(swahiliFields, "ctc_number_b", "edit_text");
    }

    @Test
    public void promptsAndBranchRelevanceMatchRequirements() throws Exception {
        JSONArray englishFields = fields(form("json.form"));
        JSONArray swahiliFields = fields(form("json.form-sw"));

        assertWarningPrompt(englishFields, "referred_for_hiv_test_prompt",
                "Refer the client for HIV Testing.");
        assertWarningPrompt(englishFields, "linked_to_prep_recent_prompt",
                "Refer the client to PrEP services.");
        assertWarningPrompt(englishFields, "linked_to_prep_prompt",
                "Refer the client to PrEP services.");
        assertWarningPrompt(swahiliFields, "referred_for_hiv_test_prompt",
                "Mpe rufaa mteja kwenda kupima VVU.");
        assertWarningPrompt(swahiliFields, "linked_to_prep_recent_prompt",
                "Mpe rufaa mteja kwenda kwenye huduma za PrEP.");
        assertWarningPrompt(swahiliFields, "linked_to_prep_prompt",
                "Mpe rufaa mteja kwenda kwenye huduma za PrEP.");

        assertRelevance(englishFields, "hiv_result_recent", "step1:hiv_tested_within_last_3_months", "yes");
        assertRelevance(englishFields, "referred_for_hiv_test", "step1:hiv_tested_within_last_3_months", "no");
        assertRelevance(englishFields, "ctc_number_a", "step1:hiv_result_recent", "positive");
        assertRelevance(englishFields, "ctc_number_b", "step1:hiv_result", "positive");
        assertRelevance(englishFields, "prep_facility_a", "step1:on_prep", "yes");
        assertRelevance(englishFields, "prep_facility_b", "step1:prep_follow_up", "yes");
    }

    @Test
    public void newFieldsAreMappedAndMigratedAtDatabaseVersion49() throws Exception {
        JSONObject clientFields = new JSONObject(readText("src/nacp/assets/ec_client_fields.json"));
        JSONArray columns = findTable(clientFields.getJSONArray("bindobjects"), TABLE_NAME)
                .getJSONArray("columns");
        String repository = readText("src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java");

        for (String field : NEW_PERSISTED_FIELDS) {
            Assert.assertEquals("Mapped column must occur once: " + field, 1, columnCount(columns, field));
            Assert.assertTrue("Missing migration for " + field,
                    repository.contains("addColumnIfMissing(db, tableName, \"" + field + "\")"));
        }

        Assert.assertTrue(repository.contains("case 49:"));
        Assert.assertTrue(repository.contains("upgradeToVersion49(db)"));
        Assert.assertTrue(readText("build.gradle").contains(
                "buildConfigField \"int\", \"DATABASE_VERSION\", '49'"));
    }

    private static JSONObject form(String directory) throws Exception {
        return new JSONObject(readText("src/nacp/assets/" + directory
                + "/ayp_out_school_medical_services.json"));
    }

    private static JSONArray fields(JSONObject form) throws Exception {
        return form.getJSONObject("step1").getJSONArray("fields");
    }

    private static Set<String> fieldKeys(JSONArray fields) throws Exception {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < fields.length(); i++) {
            Assert.assertTrue("Duplicate form field: " + fields.getJSONObject(i).optString("key"),
                    keys.add(fields.getJSONObject(i).optString("key")));
        }
        return keys;
    }

    private static int indexOf(JSONArray fields, String key) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            if (key.equals(fields.getJSONObject(i).optString("key"))) {
                return i;
            }
        }
        return -1;
    }

    private static JSONObject field(JSONArray fields, String key) throws Exception {
        int index = indexOf(fields, key);
        if (index < 0) {
            throw new AssertionError("Missing field: " + key);
        }
        return fields.getJSONObject(index);
    }

    private static void assertRequiredRadio(JSONArray fields, String key, String... optionKeys)
            throws Exception {
        JSONObject field = field(fields, key);
        Assert.assertEquals("native_radio", field.getString("type"));
        Assert.assertTrue(Boolean.parseBoolean(field.getJSONObject("v_required").get("value").toString()));
        Set<String> actualOptions = new HashSet<>();
        JSONArray options = field.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            actualOptions.add(options.getJSONObject(i).getString("key"));
        }
        Assert.assertEquals(new HashSet<>(Arrays.asList(optionKeys)), actualOptions);
    }

    private static void assertRequiredType(JSONArray fields, String key, String type) throws Exception {
        JSONObject field = field(fields, key);
        Assert.assertEquals(type, field.getString("type"));
        Assert.assertTrue(Boolean.parseBoolean(field.getJSONObject("v_required").get("value").toString()));
    }

    private static void assertWarningPrompt(JSONArray fields, String key, String text) throws Exception {
        JSONObject prompt = field(fields, key);
        Assert.assertEquals("toaster_notes", prompt.getString("type"));
        Assert.assertEquals("warning", prompt.getString("toaster_type"));
        Assert.assertEquals(text, prompt.getString("text"));
    }

    private static void assertRelevance(JSONArray fields, String key, String source, String value)
            throws Exception {
        JSONObject relevance = field(fields, key).getJSONObject("relevance").getJSONObject(source);
        Assert.assertEquals("equalTo(., \"" + value + "\")", relevance.getString("ex"));
    }

    private static JSONObject findTable(JSONArray tables, String name) throws Exception {
        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.getJSONObject(i);
            if (name.equals(table.optString("name"))) {
                return table;
            }
        }
        throw new AssertionError("Missing table: " + name);
    }

    private static int columnCount(JSONArray columns, String name) throws Exception {
        int count = 0;
        for (int i = 0; i < columns.length(); i++) {
            if (name.equals(columns.getJSONObject(i).optString("column_name"))) {
                count++;
            }
        }
        return count;
    }

    private static String readText(String relativePath) throws Exception {
        return new String(Files.readAllBytes(resolvePath(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path resolvePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        return Files.exists(direct) ? direct : Paths.get("opensrp-chw").resolve(relativePath);
    }
}
