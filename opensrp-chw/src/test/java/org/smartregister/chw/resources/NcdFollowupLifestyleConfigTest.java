package org.smartregister.chw.resources;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NcdFollowupLifestyleConfigTest {

    private static final String ENGLISH_FORM_PATH =
            "src/nacp/assets/json.form/ncd_followup_lifestyle.json";
    private static final String SWAHILI_FORM_PATH =
            "src/nacp/assets/json.form-sw/ncd_followup_lifestyle.json";

    @Test
    public void alcoholGuidanceShouldRecommendCompleteAbstinenceInEnglish() throws Exception {
        JSONObject form = readForm(ENGLISH_FORM_PATH);
        JSONObject target = findField(form, "alcohol_intake_target_met");
        JSONObject prompt = findField(form, "alcohol_prompt");

        Assert.assertEquals(
                "Is the client completely abstaining from alcohol?",
                target.getString(JsonFormConstants.LABEL)
        );
        Assert.assertTrue(prompt.getString(JsonFormConstants.TEXT).contains("do not drink any alcohol"));
        Assert.assertEquals(
                "Complete Abstinence from Alcohol",
                prompt.getString("toaster_info_title")
        );
        Assert.assertTrue(
                prompt.getString("toaster_info_text").contains("Completely abstain from alcohol")
        );
        assertDoesNotContainSafeLimitGuidance(prompt, "safe limits", "standard drinks");
    }

    @Test
    public void alcoholGuidanceShouldRecommendCompleteAbstinenceInSwahili() throws Exception {
        JSONObject form = readForm(SWAHILI_FORM_PATH);
        JSONObject target = findField(form, "alcohol_intake_target_met");
        JSONObject prompt = findField(form, "alcohol_prompt");

        Assert.assertEquals(
                "Je, mteja anajiepusha kabisa na pombe?",
                target.getString(JsonFormConstants.LABEL)
        );
        Assert.assertTrue(prompt.getString(JsonFormConstants.TEXT).contains("usinywe pombe yoyote"));
        Assert.assertEquals("Kuacha Pombe Kabisa", prompt.getString("toaster_info_title"));
        Assert.assertTrue(
                prompt.getString("toaster_info_text").contains("Acha kabisa kutumia pombe")
        );
        assertDoesNotContainSafeLimitGuidance(prompt, "kiasi salama", "vinywaji 2");
    }

    private void assertDoesNotContainSafeLimitGuidance(
            JSONObject prompt,
            String safeLimitPhrase,
            String drinkingLimitPhrase
    ) throws Exception {
        String guidance = prompt.getString(JsonFormConstants.TEXT)
                + " "
                + prompt.getString("toaster_info_text");
        Assert.assertFalse(guidance.contains(safeLimitPhrase));
        Assert.assertFalse(guidance.contains(drinkingLimitPhrase));
    }

    private JSONObject readForm(String path) throws Exception {
        byte[] contents = Files.readAllBytes(Paths.get(path));
        return new JSONObject(new String(contents, StandardCharsets.UTF_8));
    }

    private JSONObject findField(JSONObject form, String key) throws Exception {
        JSONArray fields = form.getJSONObject("step1").getJSONArray(JsonFormConstants.FIELDS);
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString(JsonFormConstants.KEY))) {
                return field;
            }
        }
        Assert.fail("Missing field " + key);
        return null;
    }
}
