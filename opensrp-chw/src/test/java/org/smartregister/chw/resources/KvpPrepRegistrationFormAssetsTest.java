package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KvpPrepRegistrationFormAssetsTest {
    private static final String[] FORM_PATHS = {
            "src/nacp/assets/json.form/kvp_prep_registration.json",
            "src/nacp/assets/json.form-sw/kvp_prep_registration.json"
    };

    @Test
    public void ctcNumberIsRequiredOnlyForHivPositiveClients() throws Exception {
        for (String formPath : FORM_PATHS) {
            JSONObject form = new JSONObject(readText(formPath));
            JSONObject ctcNumber = getField(form.getJSONObject("step1").getJSONArray("fields"), "ctc_number");

            Assert.assertTrue("CTC number must be required in " + formPath,
                    ctcNumber.getJSONObject("v_required").getBoolean("value"));
            Assert.assertFalse("CTC validation message must not be blank in " + formPath,
                    ctcNumber.getJSONObject("v_required").getString("err").trim().isEmpty());

            JSONObject hivStatusRelevance = ctcNumber.getJSONObject("relevance")
                    .getJSONObject("step1:hiv_status");
            Assert.assertEquals("string", hivStatusRelevance.getString("type"));
            Assert.assertEquals("equalTo(., \"positive\")", hivStatusRelevance.getString("ex"));
        }
    }

    @Test
    public void enrollmentUsesLocalizedTitlesAndKeepsTheEncounterType() throws Exception {
        JSONObject english = new JSONObject(readText(FORM_PATHS[0]));
        JSONObject swahili = new JSONObject(readText(FORM_PATHS[1]));

        Assert.assertEquals("MARP Enrollment", english.getJSONObject("step1").getString("title"));
        Assert.assertEquals("Usajili wa MARP", swahili.getJSONObject("step1").getString("title"));
        for (JSONObject form : Arrays.asList(english, swahili)) {
            Assert.assertEquals("KVP PrEP Registration", form.getString("encounter_type"));
        }
    }

    @Test
    public void bothLocalesUseTheSameGroupRulesAndStoredOptions() throws Exception {
        for (String formPath : FORM_PATHS) {
            JSONObject form = new JSONObject(readText(formPath));
            JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");

            assertGroupOptions(fields, "client_group_male", "msm", false);
            assertGroupOptions(fields, "client_group_female", "fsw", false);
            assertGroupOptions(fields, "client_group_female_10_24", "fsw", true);
            Assert.assertEquals("kvp_prep_registration_calculation.yml",
                    ruleFile(getField(fields, "client_group"), "calculation"));
        }
    }

    private static void assertGroupOptions(JSONArray fields, String fieldKey, String groupKey,
                                           boolean includesAgyw) throws Exception {
        JSONObject field = getField(fields, fieldKey);
        Assert.assertEquals("kvp_prep_registration_relevance.yml", ruleFile(field, "relevance"));
        JSONArray options = field.getJSONArray("options");
        List<String> actualKeys = new ArrayList<>();
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            String key = option.getString("key");
            Assert.assertEquals(key, option.getString("openmrs_entity_id"));
            actualKeys.add(key);
        }

        List<String> expectedKeys = new ArrayList<>(Arrays.asList(groupKey, "pwid", "prisoners", "inmate"));
        if (includesAgyw) {
            expectedKeys.add("agyw");
        }
        expectedKeys.addAll(Arrays.asList("discordant_couples", "fisherman", "truck_drivers", "miners", "other"));
        Assert.assertEquals(expectedKeys, actualKeys);
    }

    private static String ruleFile(JSONObject field, String ruleType) throws Exception {
        return field.getJSONObject(ruleType).getJSONObject("rules-engine")
                .getJSONObject("ex-rules").getString("rules-file");
    }

    private static JSONObject getField(JSONArray fields, String key) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private static String readText(String relativePath) throws Exception {
        return new String(Files.readAllBytes(resolvePath(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path resolvePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }

        Path modulePath = Paths.get("opensrp-chw").resolve(relativePath);
        if (Files.exists(modulePath)) {
            return modulePath;
        }

        throw new AssertionError("Could not resolve path: " + relativePath);
    }
}
