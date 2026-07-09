package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NcdInitialScreeningConfigTest {

    private static final String ENGLISH_FORM =
            "src/nacp/assets/json.form/diabetes_hypertension_screening_form.json";
    private static final String SWAHILI_FORM =
            "src/nacp/assets/json.form-sw/diabetes_hypertension_screening_form.json";
    private static final String RELEVANCE_RULES =
            "src/nacp/assets/rule/diabetes_hypertension_screening_relevance.yml";

    @Test
    public void screeningFormsShouldExcludeCurrentMedicationQuestionsAndKeepFlowConnected() throws Exception {
        assertFormUpdated(ENGLISH_FORM);
        assertFormUpdated(SWAHILI_FORM);
    }

    @Test
    public void screeningRulesShouldNotDependOnRemovedMedicationAnswers() throws Exception {
        String rules = readText(RELEVANCE_RULES);

        Assert.assertFalse(rules.contains("step2_medicines_diabetes"));
        Assert.assertFalse(rules.contains("step2_medicines_hypertension"));
        Assert.assertFalse(rules.contains("prescreening_client_has_condition_toaster_not_using_meds"));
    }

    private static void assertFormUpdated(String path) throws Exception {
        JSONObject form = new JSONObject(readText(path));
        JSONArray fields = form.getJSONObject("step2").getJSONArray("fields");

        Assert.assertNull(findField(fields, "medicines_diabetes"));
        Assert.assertNull(findField(fields, "medicines_hypertension"));
        assertRelevantAfter(fields, "screened_diabetes_12m", "diagnosed_diabetes");
        assertRelevantAfter(fields, "screened_hypertension_last12m", "diagnosed_hypertension");

        JSONArray resultFields = form.getJSONObject("step4").getJSONArray("fields");
        Assert.assertNotNull(findField(resultFields, "prescreening_client_has_condition_toaster"));
        Assert.assertNull(findField(resultFields,
                "prescreening_client_has_condition_toaster_not_using_meds"));
    }

    private static void assertRelevantAfter(JSONArray fields, String fieldKey, String dependencyKey)
            throws Exception {
        JSONObject field = findField(fields, fieldKey);
        Assert.assertNotNull(field);
        Assert.assertTrue(field.getJSONObject("relevance").has("step2:" + dependencyKey));
    }

    private static JSONObject findField(JSONArray fields, String key) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        return null;
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
