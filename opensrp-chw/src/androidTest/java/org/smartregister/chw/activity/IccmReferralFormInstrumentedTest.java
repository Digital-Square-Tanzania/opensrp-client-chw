package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class IccmReferralFormInstrumentedTest {

    @Test
    public void shouldLoadSwahiliReferralAssetWithExpectedOrderAndPhrasing() throws Exception {
        JSONObject form = loadAssetJson("json.form-sw/referrals/iccm_referral_form.json");
        JSONArray options = firstStepFields(form).getJSONObject(0).getJSONArray("options");

        assertEquals(
                Arrays.asList(
                        "extreme_weakness",
                        "impaired_consciousness",
                        "change_of_behaviour",
                        "convulsions",
                        "respiratory_distress",
                        "bleeding_tendency",
                        "shock",
                        "vomiting_everything",
                        "inability_to_drink_or_breastfeed",
                        "negative_mrdt_results",
                        "sever_pneumonia",
                        "diarrhea_with_signs_of_dehydration",
                        "pregnant_client",
                        "other_reasons"
                ),
                optionNames(options)
        );
        assertEquals("Mwelekeo wa kutokwa damu (Bleeding tendency/DIC)", findOption(options, "bleeding_tendency").getString("text"));
        assertEquals("Mshtuko/kuporomoka kwa mzunguko wa damu mwilini (Shock/Circulatory collapse)", findOption(options, "shock").getString("text"));
        assertEquals("Kuharisha pamoja na dalili za upungufu wa maji mwilini", findOption(options, "diarrhea_with_signs_of_dehydration").getString("text"));
    }

    @Test
    public void shouldFilterAdultReferralOptionsOnDevice() throws Exception {
        JSONObject form = loadAssetJson("json.form-sw/referrals/iccm_referral_form.json");
        JSONArray fields = firstStepFields(form);

        IccmRegisterActivity.updateFieldsWithDangerSignsAndPreReferralServices(
                fields,
                new JSONArray(Arrays.asList("pregnant_client", "inability_to_drink_or_breastfeed")),
                new JSONArray(Arrays.asList("anti_pyretic", "ors_zinc_co_pack")),
                false,
                true
        );

        JSONObject problem = findField(fields, "problem");
        assertEquals(
                Arrays.asList(
                        "extreme_weakness",
                        "impaired_consciousness",
                        "change_of_behaviour",
                        "convulsions",
                        "respiratory_distress",
                        "bleeding_tendency",
                        "shock",
                        "vomiting_everything",
                        "inability_to_drink_or_breastfeed",
                        "negative_mrdt_results",
                        "pregnant_client",
                        "other_reasons"
                ),
                optionNames(problem.getJSONArray("options"))
        );
        assertEquals("Hawezi kunywa", findOption(problem.getJSONArray("options"), "inability_to_drink_or_breastfeed").getString("text"));
        assertFalse(containsOption(problem.getJSONArray("options"), "sever_pneumonia"));
        assertFalse(containsOption(problem.getJSONArray("options"), "diarrhea_with_signs_of_dehydration"));
        assertTrue(findOption(problem.getJSONArray("options"), "pregnant_client").getJSONObject("properties").getBoolean("checked"));

        JSONObject serviceBeforeReferral = findField(fields, "service_before_referral");
        assertEquals(Arrays.asList("anti_pyretic", "none"), optionNames(serviceBeforeReferral.getJSONArray("options")));
        assertTrue(findOption(serviceBeforeReferral.getJSONArray("options"), "anti_pyretic").getJSONObject("properties").getBoolean("checked"));
    }

    private JSONObject loadAssetJson(String path) throws Exception {
        try (InputStream inputStream = InstrumentationRegistry.getInstrumentation().getTargetContext().getAssets().open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return new JSONObject(builder.toString());
        }
    }

    private JSONArray firstStepFields(JSONObject form) throws Exception {
        return form.getJSONArray("steps").getJSONObject(0).getJSONArray("fields");
    }

    private JSONObject findField(JSONArray fields, String name) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (name.equals(field.getString("name"))) {
                return field;
            }
        }
        throw new AssertionError("Field not found: " + name);
    }

    private JSONObject findOption(JSONArray options, String name) throws Exception {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (name.equals(option.getString("name"))) {
                return option;
            }
        }
        throw new AssertionError("Option not found: " + name);
    }

    private boolean containsOption(JSONArray options, String name) throws Exception {
        for (int i = 0; i < options.length(); i++) {
            if (name.equals(options.getJSONObject(i).getString("name"))) {
                return true;
            }
        }
        return false;
    }

    private List<String> optionNames(JSONArray options) throws Exception {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < options.length(); i++) {
            names.add(options.getJSONObject(i).getString("name"));
        }
        return names;
    }
}
