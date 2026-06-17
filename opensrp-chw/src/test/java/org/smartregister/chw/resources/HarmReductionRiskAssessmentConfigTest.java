package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public class HarmReductionRiskAssessmentConfigTest {

    @Test
    public void testRiskAssessmentBindObjectMapsStressChallengeGate() throws Exception {
        JSONObject clientFields = readJson("src/nacp/assets/ec_client_fields.json");
        JSONArray columns = getBindObjectColumns(clientFields, "ec_harm_reduction_risk_assessment");
        Set<String> mappedColumns = mappedColumns(columns);

        Assert.assertTrue(mappedColumns.contains("stress_challenges_experienced"));
        Assert.assertTrue(mappedColumns.contains("stress_challenges"));
        Assert.assertTrue(mappedColumns.contains("help_received"));
    }

    private static JSONArray getBindObjectColumns(JSONObject clientFields, String bindObjectName) throws Exception {
        JSONArray bindObjects = clientFields.getJSONArray("bindobjects");
        for (int i = 0; i < bindObjects.length(); i++) {
            JSONObject bindObject = bindObjects.getJSONObject(i);
            if (bindObjectName.equals(bindObject.optString("name"))) {
                return bindObject.getJSONArray("columns");
            }
        }

        throw new AssertionError("Missing bindobject: " + bindObjectName);
    }

    private static Set<String> mappedColumns(JSONArray columns) throws Exception {
        Set<String> mappedColumns = new LinkedHashSet<>();
        for (int i = 0; i < columns.length(); i++) {
            mappedColumns.add(columns.getJSONObject(i).optString("column_name"));
        }
        return mappedColumns;
    }

    private static JSONObject readJson(String relativePath) throws Exception {
        return new JSONObject(readText(relativePath));
    }

    private static String readText(String relativePath) throws IOException {
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
