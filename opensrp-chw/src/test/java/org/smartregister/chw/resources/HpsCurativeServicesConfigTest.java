package org.smartregister.chw.resources;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HpsCurativeServicesConfigTest {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String EC_CLIENT_FIELDS_PATH = "src/nacp/assets/ec_client_fields.json";
    private static final String HPS_CURATIVE_FORM_PATH = "src/nacp/assets/json.form/hps_curative_services.json";
    private static final String HPS_CURATIVE_FORM_SW_PATH = "src/nacp/assets/json.form-sw/hps_curative_services.json";
    private static final String HPS_CURATIVE_RULES_PATH = "src/nacp/assets/rule/hps_curative_services.yml";
    private static final String HPS_REPOSITORY_FLV_PATH = "src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java";

    @Test
    public void ecHpsClientServicesShouldMapMalariaDrugsTreatment() throws Exception {
        JSONArray tables = new JSONObject(readFile(EC_CLIENT_FIELDS_PATH)).getJSONArray("bindobjects");
        JSONObject hpsClientServices = findTable(tables, "ec_hps_client_services");
        Assert.assertNotNull("Missing ec_hps_client_services table definition", hpsClientServices);

        JSONArray columns = hpsClientServices.getJSONArray("columns");
        Assert.assertTrue(
                "ec_hps_client_services must include malaria_drugs_treatment so edit mode can restore it",
                hasColumn(columns, "malaria_drugs_treatment")
        );
    }

    @Test
    public void hpsCurativeServicesRuleShouldKeepMalariaTreatmentVisibleDuringEdit() throws Exception {
        String rules = readFile(HPS_CURATIVE_RULES_PATH);

        Assert.assertTrue(
                rules.contains("step1_malaria_mrdt_result.equalsIgnoreCase('positive_mrdt') || !step1_malaria_drugs_treatment.isEmpty()")
        );
    }

    @Test
    public void hpsCurativeServicesShouldValidateBloodPressurePairWithoutLiveDiastolicConstraint() throws Exception {
        assertBloodPressureValidationConfig(HPS_CURATIVE_FORM_PATH);
        assertBloodPressureValidationConfig(HPS_CURATIVE_FORM_SW_PATH);

        String rules = readFile(HPS_CURATIVE_RULES_PATH);
        Assert.assertTrue(
                "blood pressure guard should only pass when both values are present and diastolic is less than systolic",
                rules.contains("calculation = (!step1_systolic.isEmpty() && !step1_diastolic.isEmpty() && Float.parseFloat(step1_diastolic) < Float.parseFloat(step1_systolic)) ? 'valid' : ''")
        );
        Assert.assertTrue(
                "an invalid blood pressure pair should surface a visible warning",
                rules.contains("condition: \"!step1_systolic.isEmpty() && !step1_diastolic.isEmpty() && Float.parseFloat(step1_diastolic) >= Float.parseFloat(step1_systolic)\"")
        );
    }

    @Test
    public void hpsCurativeServicesMigrationShouldAddMalariaDrugsTreatmentColumn() throws Exception {
        String repositoryFlv = readFile(HPS_REPOSITORY_FLV_PATH);
        String buildGradle = readFile(BUILD_GRADLE_PATH);

        Assert.assertTrue(
                "The NACP repository migration should add malaria_drugs_treatment to ec_hps_client_services",
                repositoryFlv.contains("ALTER TABLE ec_hps_client_services ADD COLUMN malaria_drugs_treatment VARCHAR;")
        );
        Assert.assertTrue(
                "DATABASE_VERSION should be bumped so the new migration runs on upgrade",
                buildGradle.contains("buildConfigField \"int\", \"DATABASE_VERSION\", '41'")
        );
    }

    private JSONObject findTable(JSONArray tables, String tableName) {
        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.optJSONObject(i);
            if (table != null && tableName.equals(table.optString("name"))) {
                return table;
            }
        }
        return null;
    }

    private boolean hasColumn(JSONArray columns, String columnName) {
        for (int i = 0; i < columns.length(); i++) {
            JSONObject column = columns.optJSONObject(i);
            if (column != null && columnName.equals(column.optString("column_name"))) {
                return true;
            }
        }
        return false;
    }

    private void assertBloodPressureValidationConfig(String formPath) throws Exception {
        JSONObject form = new JSONObject(readFile(formPath));
        JSONArray fields = form.getJSONObject("step1").getJSONArray(JsonFormConstants.FIELDS);

        JSONObject diastolic = findField(fields, "diastolic");
        Assert.assertFalse(
                "diastolic should no longer use a live cross-field constraint that clears the value while typing",
                diastolic.has("constraints")
        );

        JSONObject invalidBloodPressureReading = findField(fields, "invalid_blood_pressure_reading");
        Assert.assertEquals("toaster_notes", invalidBloodPressureReading.getString(JsonFormConstants.TYPE));

        JSONObject bloodPressureGuard = findField(fields, "blood_pressure_guard");
        Assert.assertEquals("edit_text", bloodPressureGuard.getString(JsonFormConstants.TYPE));
        Assert.assertEquals("true", bloodPressureGuard.getString("hidden"));
        Assert.assertEquals("true", bloodPressureGuard.getJSONObject("v_required").getString(JsonFormConstants.VALUE));
        Assert.assertTrue(bloodPressureGuard.has("calculation"));
    }

    private JSONObject findField(JSONArray fields, String key) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString(JsonFormConstants.KEY))) {
                return field;
            }
        }
        Assert.fail("Missing field " + key);
        return null;
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
