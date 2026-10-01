package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class KvpPrepSwahiliOptionsTest {

    private static final String FORM_PATH =
            "src/nacp/assets/json.form-sw/kvp_prep_preventive_services.json";
    private static final String[] FIELDS = {
            "hiv_tested_within_last_3_months",
            "on_prep",
            "referred_for_hiv_test",
            "tested_for_hiv",
            "kits_distributed"
    };

    @Test
    public void selectedYesOptionsUseCorrectSwahiliSpelling() throws Exception {
        JSONArray fields = form().getJSONObject("step1").getJSONArray("fields");

        for (String fieldKey : FIELDS) {
            JSONObject field = getField(fields, fieldKey);
            assertEquals(fieldKey, "Ndiyo", getOption(field.getJSONArray("options"), "yes")
                    .getString("text"));
        }
    }

    @Test
    public void kitsDistributedUsesCanonicalSwahiliQuestion() throws Exception {
        JSONArray fields = form().getJSONObject("step1").getJSONArray("fields");

        assertEquals("Je, vitepe vya JIPIME vilitolewa?",
                getField(fields, "kits_distributed").getString("label"));
    }

    private JSONObject form() throws Exception {
        return new JSONObject(new String(Files.readAllBytes(resolvePath(FORM_PATH)),
                StandardCharsets.UTF_8));
    }

    private JSONObject getField(JSONArray fields, String key) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private JSONObject getOption(JSONArray options, String key) throws Exception {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (key.equals(option.optString("key"))) {
                return option;
            }
        }
        throw new AssertionError("Missing option: " + key);
    }

    private Path resolvePath(String relativePath) {
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
