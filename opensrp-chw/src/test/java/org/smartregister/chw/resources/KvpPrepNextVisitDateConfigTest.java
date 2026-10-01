package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KvpPrepNextVisitDateConfigTest {

    private static final String[] FORM_DIRECTORIES = {"json.form", "json.form-sw"};

    @Test
    public void nextVisitDateIsARequiredDedicatedForm() throws Exception {
        for (String directory : FORM_DIRECTORIES) {
            JSONObject nextVisitForm = form(directory, "kvp_prep_next_visit_date.json");
            JSONArray fields = nextVisitForm.getJSONObject("step1").getJSONArray("fields");

            assertEquals(1, fields.length());
            JSONObject date = fields.getJSONObject(0);
            assertEquals("next_visit_date", date.getString("key"));
            assertEquals("date_picker", date.getString("type"));
            assertEquals("today", date.getString("min_date"));
            assertTrue(date.getJSONObject("v_required").getBoolean("value"));

            JSONObject referralForm = form(directory, "kvp_prep_referral_services.json");
            assertFalse(hasField(referralForm.getJSONObject("step1").getJSONArray("fields"),
                    "next_visit_date"));
        }
    }

    private boolean hasField(JSONArray fields, String key) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            if (key.equals(fields.getJSONObject(i).optString("key"))) {
                return true;
            }
        }
        return false;
    }

    private JSONObject form(String directory, String fileName) throws Exception {
        String path = "src/nacp/assets/" + directory + "/" + fileName;
        return new JSONObject(new String(Files.readAllBytes(resolvePath(path)),
                StandardCharsets.UTF_8));
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
