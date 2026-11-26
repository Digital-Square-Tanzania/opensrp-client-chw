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
                field("branch_a_test_date", "date_picker", true),
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

    @Test
    public void positiveClientCtcNumberUsesCurrentVisitBeforeStoredValue() {
        assertEquals("current-a", KvpPrEPPreventiveServicesActionHelper.resolvePersistedCtcNumber(
                true, "current-a", "current-b", "stored"));
        assertEquals("current-b", KvpPrEPPreventiveServicesActionHelper.resolvePersistedCtcNumber(
                true, "", "current-b", "stored"));
        assertEquals("stored", KvpPrEPPreventiveServicesActionHelper.resolvePersistedCtcNumber(
                true, null, null, "stored"));
    }

    @Test
    public void nonPositiveClientDoesNotPersistCtcNumber() {
        assertEquals("", KvpPrEPPreventiveServicesActionHelper.resolvePersistedCtcNumber(
                false, "current-a", "current-b", "stored"));
    }

    @Test
    public void calculatedTestDateUsesTheActiveBranchValue() {
        assertEquals("15-06-2026", KvpPrEPPreventiveServicesActionHelper.resolvePersistedTestDate(
                "15-06-2026", "01-01-2026"));
        assertEquals("01-01-2026", KvpPrEPPreventiveServicesActionHelper.resolvePersistedTestDate(
                "", "01-01-2026"));
        assertEquals("", KvpPrEPPreventiveServicesActionHelper.resolvePersistedTestDate(null, null));
    }

    @Test
    public void prepTypeIsRetainedOnlyForTheActivePrepBranch() throws Exception {
        String[][] cases = {
                {"yes", "negative", "yes", "", "", "", "", "oral_prep", ""},
                {"yes", "negative", "yes", "", "", "", "", "injectable_prep", ""},
                {"no", "", "", "yes", "yes", "negative", "yes", "", "oral_prep"},
                {"no", "", "", "yes", "yes", "negative", "yes", "", "injectable_prep"},
                {"yes", "negative", "no", "", "", "", "", "", ""},
                {"yes", "negative", "", "", "", "", "", "", ""},
                {"yes", "positive", "yes", "", "", "", "", "", ""},
                {"yes", "", "yes", "", "", "", "", "", ""},
                {"no", "", "", "no", "yes", "negative", "yes", "", ""},
                {"no", "", "", "yes", "no", "negative", "yes", "", ""},
                {"no", "", "", "yes", "yes", "positive", "yes", "", ""},
                {"no", "", "", "yes", "yes", "", "yes", "", ""},
                {"no", "", "", "yes", "yes", "negative", "no", "", ""},
                {"no", "", "", "yes", "yes", "negative", "", "", ""},
                {"", "negative", "yes", "yes", "yes", "negative", "yes", "", ""}
        };
        String[] keys = {"hiv_tested_within_last_3_months", "hiv_result_recent", "on_prep",
                "referred_for_hiv_test", "tested_for_hiv", "hiv_result", "prep_follow_up"};
        KvpPrEPPreventiveServicesActionHelper helper =
                new KvpPrEPPreventiveServicesActionHelper("client-id", new HashMap<>());
        Method clearInactive = KvpPrEPPreventiveServicesActionHelper.class
                .getDeclaredMethod("clearInactiveBranchValues", JSONArray.class);
        clearInactive.setAccessible(true);

        for (String[] values : cases) {
            JSONArray fields = new JSONArray();
            for (int i = 0; i < keys.length; i++) {
                fields.put(field(keys[i], "native_radio", false).put("value", values[i]));
            }
            String selectedType = "injectable_prep".equals(values[7]) || "injectable_prep".equals(values[8])
                    ? "injectable_prep" : "oral_prep";
            fields.put(field("prep_type_a", "native_radio", false).put("value", selectedType));
            fields.put(field("prep_type_b", "native_radio", false).put("value", selectedType));
            clearInactive.invoke(helper, fields);

            assertEquals(java.util.Arrays.toString(values), values[7],
                    fields.getJSONObject(keys.length).getString("value"));
            assertEquals(java.util.Arrays.toString(values), values[8],
                    fields.getJSONObject(keys.length + 1).getString("value"));
        }
    }

    @Test
    public void hivPositiveSuppressionHidesBothPrepTypeQuestions() throws Exception {
        JSONObject form = formWithFields(field("prep_type_a", "native_radio", true),
                field("prep_type_b", "native_radio", true));
        Method suppress = KvpPrEPPreventiveServicesActionHelper.class
                .getDeclaredMethod("suppressHivPrepFields", JSONObject.class);
        suppress.setAccessible(true);
        suppress.invoke(new KvpPrEPPreventiveServicesActionHelper("client-id", new HashMap<>()), form);

        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            assertEquals("hidden", fields.getJSONObject(i).getString("type"));
            assertFalse(fields.getJSONObject(i).has("relevance"));
        }
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
