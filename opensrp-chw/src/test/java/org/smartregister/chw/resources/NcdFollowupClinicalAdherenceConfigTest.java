package org.smartregister.chw.resources;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NcdFollowupClinicalAdherenceConfigTest {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String EC_CLIENT_FIELDS_PATH = "src/nacp/assets/ec_client_fields.json";
    private static final String FORM_PATH = "src/nacp/assets/json.form/ncd_followup_clinical_adherence.json";
    private static final String FORM_SW_PATH = "src/nacp/assets/json.form-sw/ncd_followup_clinical_adherence.json";
    private static final String RELEVANCE_RULES_PATH = "src/nacp/assets/rule/ncd_followup_clinical_adherence_relevance.yml";
    private static final String CALCULATION_RULES_PATH = "src/nacp/assets/rule/ncd_followup_clinical_adherence_calculation.yml";
    private static final String REPOSITORY_FLV_PATH = "src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java";

    @Test
    public void medicationSideEffectsShouldBeMandatoryStandaloneQuestion() throws Exception {
        assertMedicationSideEffectsQuestion(FORM_PATH);
        assertMedicationSideEffectsQuestion(FORM_SW_PATH);
    }

    @Test
    public void medicationSideEffectsShouldDriveCounselingAndYellowAlert() throws Exception {
        String relevanceRules = readText(RELEVANCE_RULES_PATH);
        String calculationRules = readText(CALCULATION_RULES_PATH);

        Assert.assertTrue(
                "side-effect counseling should use the standalone medication_side_effects answer",
                relevanceRules.contains("condition: \"step1_medication_side_effects == 'yes' || step1_no_adherence_reason == 'side_effects'\"")
        );
        Assert.assertTrue(
                "side-effect alert should use the standalone medication_side_effects answer",
                calculationRules.contains("calculation = (step1_medication_side_effects == 'yes' || (step1_medication_adherence == 'no' && step1_no_adherence_reason == 'side_effects')) ? 'true' : 'false'")
        );
        Assert.assertTrue(
                "yellow alert should use the standalone medication_side_effects answer",
                calculationRules.contains("calculation = (step1_clinic_attendance == 'no' || step1_medication_side_effects == 'yes' || step1_no_adherence_reason == 'side_effects') ? 'true' : 'false'")
        );
    }

    @Test
    public void medicationSideEffectsShouldBeMappedAndMigrated() throws Exception {
        JSONArray tables = new JSONObject(readText(EC_CLIENT_FIELDS_PATH)).getJSONArray("bindobjects");
        JSONObject followup = findBindObject(tables, "ec_ncd_case_management_followup");
        JSONArray columns = followup.getJSONArray("columns");

        Assert.assertTrue(
                "ec_ncd_case_management_followup should persist medication_side_effects",
                hasColumn(columns, "medication_side_effects")
        );

        String repositoryFlv = readText(REPOSITORY_FLV_PATH);
        String buildGradle = readText(BUILD_GRADLE_PATH);
        Assert.assertTrue(
                "NACP migration should add medication_side_effects for upgraded installations",
                repositoryFlv.contains("ALTER TABLE ec_ncd_case_management_followup ADD COLUMN medication_side_effects VARCHAR;")
        );
        Assert.assertTrue(
                "DATABASE_VERSION should be bumped so the medication_side_effects migration runs",
                buildGradle.contains("buildConfigField \"int\", \"DATABASE_VERSION\", '44'")
        );
    }

    private void assertMedicationSideEffectsQuestion(String formPath) throws Exception {
        JSONObject form = new JSONObject(readText(formPath));
        JSONArray fields = form.getJSONObject("step1").getJSONArray(JsonFormConstants.FIELDS);
        JSONObject sideEffects = findField(fields, "medication_side_effects");

        Assert.assertTrue(
                "medication_side_effects should appear immediately after medication_adherence",
                indexOf(fields, "medication_side_effects") == indexOf(fields, "medication_adherence") + 1
        );
        Assert.assertEquals("native_radio", sideEffects.getString(JsonFormConstants.TYPE));
        Assert.assertEquals("medication_side_effects", sideEffects.getString("openmrs_entity_id"));
        Assert.assertEquals("true", sideEffects.getJSONObject("v_required").getString(JsonFormConstants.VALUE));
        Assert.assertFalse(
                "medication_side_effects should be standalone and not hidden behind relevance logic",
                sideEffects.has("relevance")
        );
        Assert.assertEquals(2, sideEffects.getJSONArray("options").length());
    }

    private JSONObject findBindObject(JSONArray bindObjects, String name) throws Exception {
        for (int i = 0; i < bindObjects.length(); i++) {
            JSONObject bindObject = bindObjects.getJSONObject(i);
            if (name.equals(bindObject.optString("name"))) {
                return bindObject;
            }
        }
        throw new AssertionError("Missing bind object: " + name);
    }

    private boolean hasColumn(JSONArray columns, String columnName) throws Exception {
        for (int i = 0; i < columns.length(); i++) {
            if (columnName.equals(columns.getJSONObject(i).optString("column_name"))) {
                return true;
            }
        }
        return false;
    }

    private JSONObject findField(JSONArray fields, String key) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString(JsonFormConstants.KEY))) {
                return field;
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private int indexOf(JSONArray fields, String key) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString(JsonFormConstants.KEY))) {
                return i;
            }
        }
        return -1;
    }

    private String readText(String relativePath) throws Exception {
        return new String(Files.readAllBytes(resolvePath(relativePath)), StandardCharsets.UTF_8);
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
}
