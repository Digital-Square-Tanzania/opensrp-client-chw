package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.util.IccmReferralFormUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IccmRegisterActivityTest {

    @Test
    public void shouldKeepChildReferralReasonsAndServices() throws Exception {
        JSONArray fields = buildFields();

        IccmRegisterActivity.updateFieldsWithDangerSignsAndPreReferralServices(
                fields,
                new JSONArray(Arrays.asList("convulsions", "sever_pneumonia")),
                new JSONArray(Arrays.asList("anti_pyretic", "ors")),
                true,
                false
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
                        "sever_pneumonia",
                        "diarrhea_with_signs_of_dehydration",
                        "other_reasons"
                ),
                optionNames(problem.getJSONArray("options"))
        );
        assertEquals("Hawezi kunywa au kunyonya", findOption(problem.getJSONArray("options"), "inability_to_drink_or_breastfeed").getString("text"));
        assertTrue(findOption(problem.getJSONArray("options"), "convulsions").getJSONObject("properties").getBoolean("checked"));
        assertTrue(findOption(problem.getJSONArray("options"), "sever_pneumonia").getJSONObject("properties").getBoolean("checked"));

        JSONObject serviceBeforeReferral = findField(fields, "service_before_referral");
        assertEquals(
                Arrays.asList("anti_pyretic", "ors", "ors_zinc_co_pack", "none"),
                optionNames(serviceBeforeReferral.getJSONArray("options"))
        );
        assertTrue(findOption(serviceBeforeReferral.getJSONArray("options"), "anti_pyretic").getJSONObject("properties").getBoolean("checked"));
        assertTrue(findOption(serviceBeforeReferral.getJSONArray("options"), "ors").getJSONObject("properties").getBoolean("checked"));
    }

    @Test
    public void shouldRemoveChildOnlyProblemsAndOrsForReproductiveAgeWomen() throws Exception {
        JSONArray fields = buildFields();

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
        assertTrue(findOption(problem.getJSONArray("options"), "pregnant_client").getJSONObject("properties").getBoolean("checked"));
        assertFalse(containsOption(problem.getJSONArray("options"), "sever_pneumonia"));
        assertFalse(containsOption(problem.getJSONArray("options"), "diarrhea_with_signs_of_dehydration"));

        JSONObject serviceBeforeReferral = findField(fields, "service_before_referral");
        assertEquals(Arrays.asList("anti_pyretic", "none"), optionNames(serviceBeforeReferral.getJSONArray("options")));
        assertTrue(findOption(serviceBeforeReferral.getJSONArray("options"), "anti_pyretic").getJSONObject("properties").getBoolean("checked"));
    }

    @Test
    public void shouldRemovePregnancyAndChildOnlyOptionsForNonChildNonReproductiveClients() throws Exception {
        JSONArray fields = buildFields();

        IccmRegisterActivity.updateFieldsWithDangerSignsAndPreReferralServices(
                fields,
                new JSONArray(Arrays.asList("negative_mrdt_results")),
                new JSONArray(),
                false,
                false
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
                        "other_reasons"
                ),
                optionNames(problem.getJSONArray("options"))
        );
        assertEquals("Hawezi kunywa", findOption(problem.getJSONArray("options"), "inability_to_drink_or_breastfeed").getString("text"));
        assertFalse(containsOption(problem.getJSONArray("options"), "pregnant_client"));

        JSONObject serviceBeforeReferral = findField(fields, "service_before_referral");
        assertEquals(Arrays.asList("anti_pyretic", "none"), optionNames(serviceBeforeReferral.getJSONArray("options")));
    }

    @Test
    public void shouldRemoveAdultIneligibleOptionsFromNativeReferralForm() throws Exception {
        JSONArray fields = buildNativeFields();

        IccmReferralFormUtils.updateNativeReferralFields(fields, false, false);

        JSONObject problem = findFieldByKey(fields, "problem");
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
                        "other_reasons"
                ),
                optionKeys(problem.getJSONArray("options"))
        );
        assertEquals("Inability to drink", findOptionByKey(problem.getJSONArray("options"), "inability_to_drink_or_breastfeed").getString("text"));

        JSONObject serviceBeforeReferral = findFieldByKey(fields, "service_before_referral");
        assertEquals(Arrays.asList("anti_pyretic", "none"), optionKeys(serviceBeforeReferral.getJSONArray("options")));
    }

    private JSONArray buildFields() throws Exception {
        JSONArray fields = new JSONArray();
        fields.put(new JSONObject()
                .put("name", "problem")
                .put("options", new JSONArray()
                        .put(option("extreme_weakness", "Amelegea sana"))
                        .put(option("impaired_consciousness", "Kupoteza fahamu"))
                        .put(option("change_of_behaviour", "Kuchanganyikiwa"))
                        .put(option("convulsions", "Degedege"))
                        .put(option("respiratory_distress", "Kupumua kwa shida"))
                        .put(option("bleeding_tendency", "Mwelekeo wa kutokwa damu (Bleeding tendency/DIC)"))
                        .put(option("shock", "Mshtuko/kuporomoka kwa mzunguko wa damu mwilini (Shock/Circulatory collapse)"))
                        .put(option("vomiting_everything", "Anatapika kila kitu"))
                        .put(option("inability_to_drink_or_breastfeed", "Hawezi kunywa au kunyonya"))
                        .put(option("negative_mrdt_results", "Majibu hasi ya kipimo cha mRDT"))
                        .put(option("sever_pneumonia", "Nimonia"))
                        .put(option("diarrhea_with_signs_of_dehydration", "Kuharisha pamoja na dalili za upungufu wa maji mwilini"))
                        .put(option("pregnant_client", "Mama Mjamzito"))
                        .put(option("other_reasons", "Magonjwa mengine"))));
        fields.put(new JSONObject()
                .put("name", "service_before_referral")
                .put("options", new JSONArray()
                        .put(option("anti_pyretic", "Dawa ya kushusha homa"))
                        .put(option("ors", "ORS"))
                        .put(option("ors_zinc_co_pack", "ORS Zinc Co-pack"))
                        .put(option("none", "Hajapewa dawa yoyote"))));
        return fields;
    }

    private JSONArray buildNativeFields() throws Exception {
        JSONArray fields = new JSONArray();
        fields.put(new JSONObject()
                .put("key", "problem")
                .put("options", new JSONArray()
                        .put(nativeOption("extreme_weakness", "Prostration/extreme weakness"))
                        .put(nativeOption("impaired_consciousness", "Impaired consciousness"))
                        .put(nativeOption("change_of_behaviour", "Change of behaviour"))
                        .put(nativeOption("convulsions", "Convulsions"))
                        .put(nativeOption("respiratory_distress", "Respiratory distress"))
                        .put(nativeOption("bleeding_tendency", "Bleeding tendency/DIC"))
                        .put(nativeOption("shock", "Shock/Circulatory collapse"))
                        .put(nativeOption("vomiting_everything", "Vomiting everything"))
                        .put(nativeOption("inability_to_drink_or_breastfeed", "Inability to drink or breastfeed"))
                        .put(nativeOption("negative_mrdt_results", "Negative mRDT Results"))
                        .put(nativeOption("other_reasons", "Non iCCM Condition"))
                        .put(nativeOption("sever_pneumonia", "Pneumonia"))
                        .put(nativeOption("diarrhea_with_signs_of_dehydration", "Diarrhea with signs of dehydration"))
                        .put(nativeOption("pregnant_client", "Pregnant Client"))));
        fields.put(new JSONObject()
                .put("key", "service_before_referral")
                .put("options", new JSONArray()
                        .put(nativeOption("anti_pyretic", "Anti-pyretic"))
                        .put(nativeOption("ors", "ORS"))
                        .put(nativeOption("ors_zinc_co_pack", "ORS Zinc Co-pack"))
                        .put(nativeOption("none", "None"))));
        return fields;
    }

    private JSONObject option(String name, String text) throws Exception {
        return new JSONObject().put("name", name).put("text", text);
    }

    private JSONObject nativeOption(String key, String text) throws Exception {
        return new JSONObject().put("key", key).put("text", text);
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

    private JSONObject findFieldByKey(JSONArray fields, String key) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.getString("key"))) {
                return field;
            }
        }
        throw new AssertionError("Field not found: " + key);
    }

    private JSONObject findOptionByKey(JSONArray options, String key) throws Exception {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (key.equals(option.getString("key"))) {
                return option;
            }
        }
        throw new AssertionError("Option not found: " + key);
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

    private List<String> optionKeys(JSONArray options) throws Exception {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < options.length(); i++) {
            names.add(options.getJSONObject(i).getString("key"));
        }
        return names;
    }
}
