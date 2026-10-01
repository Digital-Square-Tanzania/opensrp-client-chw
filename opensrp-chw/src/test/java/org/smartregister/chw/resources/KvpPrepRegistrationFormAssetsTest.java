package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
