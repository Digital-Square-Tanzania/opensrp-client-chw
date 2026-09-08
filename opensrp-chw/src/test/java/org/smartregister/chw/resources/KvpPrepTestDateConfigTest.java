package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KvpPrepTestDateConfigTest {

    private static final String[] FORM_PATHS = {
            "src/nacp/assets/json.form/kvp_prep_preventive_services.json",
            "src/nacp/assets/json.form-sw/kvp_prep_preventive_services.json"
    };

    @Test
    public void branchBTestDateCannotBeWithinLastNinetyDays() throws Exception {
        for (String formPath : FORM_PATHS) {
            JSONObject testDate = getField(form(formPath), "test_date");

            assertEquals(formPath, "date_picker", testDate.getString("type"));
            assertEquals(formPath, "today-90d", testDate.getString("max_date"));
            assertTrue(formPath, testDate.getJSONObject("v_required").getBoolean("value"));
        }
    }

    private JSONObject form(String formPath) throws Exception {
        return new JSONObject(new String(Files.readAllBytes(resolvePath(formPath)),
                StandardCharsets.UTF_8));
    }

    private JSONObject getField(JSONObject form, String key) throws Exception {
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        throw new AssertionError("Missing field: " + key);
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
