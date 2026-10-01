package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HarmReductionSoberHouseDiseaseScreeningConfigTest {
    private static final String TABLE_NAME = "ec_harm_reduction_sober_house_services";
    private static final String[] SCREENING_FIELDS = {
            "screening_tests_done",
            "other_conditions_specify",
            "mental_health_result",
            "mental_health_treatment_after_screening",
            "diabetes_result",
            "diabetes_treatment_after_screening",
            "other_conditions_result",
            "other_conditions_treatment_after_screening"
    };

    @Test
    public void diseaseScreeningFieldsAreMappedMigratedAndIncludedInHistory() throws Exception {
        JSONObject clientFields = new JSONObject(readText("src/nacp/assets/ec_client_fields.json"));
        JSONObject soberHouseServices = findTable(clientFields.getJSONArray("bindobjects"), TABLE_NAME);
        JSONArray columns = soberHouseServices.getJSONArray("columns");
        String repository = readText("src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java");
        String historyActivity = readText("src/main/java/org/smartregister/chw/activity/HarmReductionSoberHouseVisitHistoryActivity.java");

        for (String field : SCREENING_FIELDS) {
            Assert.assertTrue("Missing mapped column: " + field, hasColumn(columns, field));
            Assert.assertTrue("Missing database migration: " + field,
                    repository.contains("addColumnIfMissing(db, tableName, \"" + field + "\")"));
            Assert.assertTrue("Missing history field: " + field,
                    historyActivity.contains("\"" + field + "\""));
        }

        Assert.assertTrue(repository.contains("case 48:"));
        Assert.assertTrue(repository.contains("upgradeToVersion48(db)"));
        Assert.assertTrue(readText("build.gradle").contains(
                "buildConfigField \"int\", \"DATABASE_VERSION\", '48'"
        ));
    }

    @Test
    public void diseaseScreeningHistoryLabelsAreTranslated() throws Exception {
        String englishStrings = readText("src/nacp/res/values/strings.xml");
        String swahiliStrings = readText("src/nacp/res/values-sw/strings.xml");

        for (String field : SCREENING_FIELDS) {
            String resourceName = "name=\"harm_reduction_sober_house_" + field + "\"";
            Assert.assertTrue("Missing English history label: " + field, englishStrings.contains(resourceName));
            Assert.assertTrue("Missing Swahili history label: " + field, swahiliStrings.contains(resourceName));
        }

        Assert.assertTrue(englishStrings.contains("name=\"harm_reduction_sober_house_mental_health_conditions\""));
        Assert.assertTrue(englishStrings.contains("name=\"harm_reduction_sober_house_diabetes\""));
        Assert.assertTrue(englishStrings.contains("name=\"harm_reduction_sober_house_other_conditions\""));
        Assert.assertTrue(swahiliStrings.contains("name=\"harm_reduction_sober_house_mental_health_conditions\""));
        Assert.assertTrue(swahiliStrings.contains("name=\"harm_reduction_sober_house_diabetes\""));
        Assert.assertTrue(swahiliStrings.contains("name=\"harm_reduction_sober_house_other_conditions\""));
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
        int occurrences = 0;
        for (int i = 0; i < columns.length(); i++) {
            if (name.equals(columns.getJSONObject(i).optString("column_name"))) {
                occurrences++;
            }
        }
        return occurrences == 1;
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
