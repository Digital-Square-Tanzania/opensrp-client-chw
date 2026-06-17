package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HarmReductionFollowupConfigTest {

    @Test
    public void shouldMapSeparateHepatitisScreeningColumnsInNacpEcClientFields() throws Exception {
        JSONObject file = new JSONObject(readText("src/nacp/assets/ec_client_fields.json"));
        JSONObject bindObject = findBindObject(file.getJSONArray("bindobjects"), "ec_harm_reduction_followup_visit");
        JSONArray columns = bindObject.getJSONArray("columns");

        Assert.assertTrue(hasColumn(columns, "hepatitis_bc_screening"));
        Assert.assertTrue(hasColumn(columns, "hepatitis_b_screening"));
        Assert.assertTrue(hasColumn(columns, "hepatitis_c_screening"));
    }

    private static JSONObject findBindObject(JSONArray bindObjects, String name) throws Exception {
        for (int i = 0; i < bindObjects.length(); i++) {
            JSONObject bindObject = bindObjects.getJSONObject(i);
            if (name.equals(bindObject.optString("name"))) {
                return bindObject;
            }
        }

        throw new AssertionError("Missing bind object: " + name);
    }

    private static boolean hasColumn(JSONArray columns, String columnName) throws Exception {
        for (int i = 0; i < columns.length(); i++) {
            if (columnName.equals(columns.getJSONObject(i).optString("column_name"))) {
                return true;
            }
        }
        return false;
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
