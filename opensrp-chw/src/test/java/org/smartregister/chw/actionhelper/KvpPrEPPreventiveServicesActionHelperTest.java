package org.smartregister.chw.actionhelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KvpPrEPPreventiveServicesActionHelperTest {

    @Test
    public void hivPositiveSuppressionKeepsHivstQuestionAndPromptVisible() throws Exception {
        JSONObject form = formWithFields(
                field("hiv_result", "native_radio", true),
                field("test_date", "date_picker", true),
                field("kits_distributed", "native_radio", false),
                field("prompt_for_hivst", "toaster_notes", true));

        KvpPrEPPreventiveServicesActionHelper helper =
                new KvpPrEPPreventiveServicesActionHelper("client-id", new HashMap<>());
        Method suppressFields = KvpPrEPPreventiveServicesActionHelper.class
                .getDeclaredMethod("suppressHivPrepFields", JSONObject.class);
        suppressFields.setAccessible(true);
        suppressFields.invoke(helper, form);

        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        JSONObject hivResult = fields.getJSONObject(0);
        JSONObject recentTestDate = fields.getJSONObject(1);
        JSONObject kitsDistributed = fields.getJSONObject(2);
        JSONObject hivstPrompt = fields.getJSONObject(3);

        assertEquals("hidden", hivResult.getString("type"));
        assertFalse(hivResult.has("relevance"));
        assertEquals("hidden", recentTestDate.getString("type"));
        assertFalse(recentTestDate.has("relevance"));
        assertEquals("native_radio", kitsDistributed.getString("type"));
        assertEquals("toaster_notes", hivstPrompt.getString("type"));
        assertTrue(hivstPrompt.has("relevance"));
    }

    @Test
    public void hivRetestQuestionCanReplaceFirstVisitQuestion() throws Exception {
        JSONObject form = formWithFields(field("hiv_tested_within_last_3_months", "native_radio", false));
        KvpPrEPPreventiveServicesActionHelper helper =
                new KvpPrEPPreventiveServicesActionHelper("client-id", new HashMap<>());
        Method setQuestion = KvpPrEPPreventiveServicesActionHelper.class
                .getDeclaredMethod("setHivTestQuestion", JSONObject.class, String.class);
        setQuestion.setAccessible(true);

        setQuestion.invoke(helper, form, "Has the client undergone a repeat HIV/AIDS test?");

        assertEquals("Has the client undergone a repeat HIV/AIDS test?",
                form.getJSONObject("step1").getJSONArray("fields").getJSONObject(0).getString("label"));
    }

    @Test
    public void hivTestingVisibilityHonorsVisitAndRetestState() {
        assertTrue(KvpPrEPPreventiveServicesActionHelper.shouldShowHivTestingFields(false, false, false));
        assertTrue(KvpPrEPPreventiveServicesActionHelper.shouldShowHivTestingFields(false, true, true));
        assertFalse(KvpPrEPPreventiveServicesActionHelper.shouldShowHivTestingFields(false, true, false));
        assertFalse(KvpPrEPPreventiveServicesActionHelper.shouldShowHivTestingFields(true, false, false));
        assertFalse(KvpPrEPPreventiveServicesActionHelper.shouldShowHivTestingFields(true, true, true));
    }

    private JSONObject formWithFields(JSONObject... formFields) throws Exception {
        JSONArray fields = new JSONArray();
        for (JSONObject field : formFields) {
            fields.put(field);
        }
        return new JSONObject().put("step1", new JSONObject().put("fields", fields));
    }

    private JSONObject field(String key, String type, boolean withRelevance) throws Exception {
        JSONObject field = new JSONObject().put("key", key).put("type", type);
        if (withRelevance) {
            field.put("relevance", new JSONObject());
        }
        return field;
    }
}
