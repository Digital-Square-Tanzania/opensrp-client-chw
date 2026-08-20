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
        JSONObject kitsDistributed = fields.getJSONObject(1);
        JSONObject hivstPrompt = fields.getJSONObject(2);

        assertEquals("hidden", hivResult.getString("type"));
        assertFalse(hivResult.has("relevance"));
        assertEquals("native_radio", kitsDistributed.getString("type"));
        assertEquals("toaster_notes", hivstPrompt.getString("type"));
        assertTrue(hivstPrompt.has("relevance"));
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
