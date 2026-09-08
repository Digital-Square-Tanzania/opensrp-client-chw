package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HarmReductionNicknameConfigTest {

    @Test
    public void nicknameIsMappedAndMigratedForRiskAssessment() throws Exception {
        JSONObject clientFields = new JSONObject(readText("src/nacp/assets/ec_client_fields.json"));
        JSONObject riskAssessment = findTable(clientFields.getJSONArray("bindobjects"),
                "ec_harm_reduction_risk_assessment");

        Assert.assertTrue(hasColumn(riskAssessment.getJSONArray("columns"), "nickname"));

        String repository = readText("src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java");
        Assert.assertTrue(repository.contains(
                "addColumnIfMissing(db, \"ec_harm_reduction_risk_assessment\", \"nickname\")"
        ));
        Assert.assertTrue(readText("build.gradle").contains(
                "buildConfigField \"int\", \"DATABASE_VERSION\", '47'"
        ));
    }

    private static JSONObject findTable(JSONArray tables, String name) throws Exception {
        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.getJSONObject(i);
            if (name.equals(table.optString("name"))) {
                return table;
            }
        }
        throw new AssertionError("Missing table: " + name);
    }

    private static boolean hasColumn(JSONArray columns, String name) throws Exception {
        for (int i = 0; i < columns.length(); i++) {
            if (name.equals(columns.getJSONObject(i).optString("column_name"))) {
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
