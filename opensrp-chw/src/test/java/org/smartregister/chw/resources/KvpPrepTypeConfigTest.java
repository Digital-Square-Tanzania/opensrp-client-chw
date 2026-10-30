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

public class KvpPrepTypeConfigTest {

    @Test
    public void prepTypeIsRequiredImmediatelyBeforeFacilityInBothLanguagesAndBranches() throws Exception {
        for (String language : new String[]{"json.form", "json.form-sw"}) {
            JSONObject form = readJson("src/nacp/assets/" + language + "/kvp_prep_preventive_services.json");
            JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
            for (String branch : new String[]{"a", "b"}) {
                String typeKey = "prep_type_" + branch;
                int index = fieldIndex(fields, typeKey);
                JSONObject type = fields.getJSONObject(index);
                JSONObject facility = fields.getJSONObject(index + 1);

                assertEquals("prep_facility_" + branch, facility.getString("key"));
                assertEquals("native_radio", type.getString("type"));
                assertEquals("concept", type.getString("openmrs_entity"));
                assertEquals(typeKey, type.getString("openmrs_entity_id"));
                assertTrue(type.getJSONObject("v_required").getBoolean("value"));
                assertEquals(facility.getJSONObject("relevance").toString(),
                        type.getJSONObject("relevance").toString());
                assertEquals(language.equals("json.form")
                                ? "What type of PrEP did the RoC receive?"
                                : "Aina ya dawa kinga (PrEP) anayotumia",
                        type.getString("label"));

                JSONArray options = type.getJSONArray("options");
                assertEquals(2, options.length());
                assertOption(options.getJSONObject(0), "oral_prep", "Oral PrEP (Vidonge)");
                assertOption(options.getJSONObject(1), "injectable_prep", "Injectable PrEP (Sindano)");
            }
        }
    }

    @Test
    public void prepTypeObservationsAreMappedForFollowupReporting() throws Exception {
        JSONArray tables = readJson("src/nacp/assets/ec_client_fields.json").getJSONArray("bindobjects");
        JSONObject followup = tables.getJSONObject(fieldIndex(tables, "ec_kvp_prep_followup", "name"));
        JSONArray columns = followup.getJSONArray("columns");
        for (String key : new String[]{"prep_type_a", "prep_type_b"}) {
            JSONObject column = columns.getJSONObject(fieldIndex(columns, key, "column_name"));
            assertEquals("Event", column.getString("type"));
            assertEquals("obs.fieldCode", column.getJSONObject("json_mapping").getString("field"));
            assertEquals(key, column.getJSONObject("json_mapping").getString("concept"));
        }
    }

    private void assertOption(JSONObject option, String key, String text) throws Exception {
        assertEquals(key, option.getString("key"));
        assertEquals(key, option.getString("openmrs_entity_id"));
        assertEquals(text, option.getString("text"));
    }

    private int fieldIndex(JSONArray fields, String key) throws Exception {
        return fieldIndex(fields, key, "key");
    }

    private int fieldIndex(JSONArray fields, String key, String property) throws Exception {
        int match = -1;
        for (int i = 0; i < fields.length(); i++) {
            if (key.equals(fields.getJSONObject(i).optString(property))) {
                assertEquals("Duplicate " + key, -1, match);
                match = i;
            }
        }
        assertTrue("Missing " + key, match >= 0);
        return match;
    }

    private JSONObject readJson(String relativePath) throws Exception {
        Path path = Paths.get(relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("opensrp-chw").resolve(relativePath);
        }
        return new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }
}
