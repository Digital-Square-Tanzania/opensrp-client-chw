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

public class KvpPrepPreventiveServicesConfigTest {

    private static final String[] FORM_PATHS = {
            "src/nacp/assets/json.form/kvp_prep_preventive_services.json",
            "src/nacp/assets/json.form-sw/kvp_prep_preventive_services.json"
    };

    @Test
    public void branchACtcNumberIsRequiredForRecentPositiveResult() throws Exception {
        for (String formPath : FORM_PATHS) {
            JSONObject field = getField(form(formPath), "ctc_number_a");

            assertTrue(formPath, field.getJSONObject("v_required").getBoolean("value"));
            JSONObject relevance = field.getJSONObject("relevance")
                    .getJSONObject("step1:hiv_result_recent");
            assertEquals("equalTo(., \"positive\")", relevance.getString("ex"));
        }
    }

    @Test
    public void firstVisitHivQuestionUsesApprovedWording() throws Exception {
        assertEquals("Have you been tested for HIV in the last three months?",
                getField(form(FORM_PATHS[0]), "hiv_tested_within_last_3_months").getString("label"));
        assertEquals("Je, umepima VVU ndani ya miezi mitatu iliyopita?",
                getField(form(FORM_PATHS[1]), "hiv_tested_within_last_3_months").getString("label"));
    }

    @Test
    public void recentHivTestBranchCapturesDateUsingDedicatedBranchConcept() throws Exception {
        for (String formPath : FORM_PATHS) {
            JSONObject testDate = getField(form(formPath), "branch_a_test_date");

            assertEquals(formPath, "date_picker", testDate.getString("type"));
            assertEquals(formPath, "branch_a_test_date", testDate.getString("openmrs_entity_id"));
            assertEquals(formPath, "today", testDate.getString("max_date"));
            assertTrue(formPath, testDate.getJSONObject("v_required").getBoolean("value"));
            assertEquals(formPath, "equalTo(., \"yes\")", testDate.getJSONObject("relevance")
                    .getJSONObject("step1:hiv_tested_within_last_3_months").getString("ex"));

            JSONObject calculatedTestDate = getField(form(formPath), "test_date");
            assertEquals(formPath, "hidden", calculatedTestDate.getString("type"));
            assertEquals(formPath, "test_date", calculatedTestDate.getString("openmrs_entity_id"));
        }
    }

    @Test
    public void branchTestDatesAreMappedAndMigratedAtDatabaseVersion50() throws Exception {
        JSONObject clientFields = new JSONObject(readText("src/nacp/assets/ec_client_fields.json"));
        JSONArray tables = clientFields.getJSONArray("bindobjects");
        JSONArray columns = null;
        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.getJSONObject(i);
            if ("ec_kvp_prep_followup".equals(table.optString("name"))) {
                columns = table.getJSONArray("columns");
                break;
            }
        }
        if (columns == null) {
            throw new AssertionError("Missing table: ec_kvp_prep_followup");
        }

        assertEquals(1, columnCount(columns, "branch_a_test_date"));
        assertEquals(1, columnCount(columns, "branch_b_test_date"));
        assertEquals(1, columnCount(columns, "test_date"));

        String repository = readText("src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java");
        assertTrue(repository.contains("case 50:"));
        assertTrue(repository.contains("upgradeToVersion50(db)"));
        assertTrue(repository.contains("addColumnIfMissing(db, \"ec_kvp_prep_followup\", \"branch_a_test_date\")"));
        assertTrue(repository.contains("addColumnIfMissing(db, \"ec_kvp_prep_followup\", \"branch_b_test_date\")"));
        assertTrue(readText("build.gradle").contains(
                "buildConfigField \"int\", \"DATABASE_VERSION\", '50'"));
    }

    private JSONObject form(String formPath) throws Exception {
        return new JSONObject(readText(formPath));
    }

    private JSONObject getField(JSONObject form, String key) throws Exception {
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private Path resolvePath(String relativePath) {
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

    private String readText(String relativePath) throws Exception {
        return new String(Files.readAllBytes(resolvePath(relativePath)), StandardCharsets.UTF_8);
    }

    private int columnCount(JSONArray columns, String name) throws Exception {
        int count = 0;
        for (int i = 0; i < columns.length(); i++) {
            if (name.equals(columns.getJSONObject(i).optString("column_name"))) {
                count++;
            }
        }
        return count;
    }
}
