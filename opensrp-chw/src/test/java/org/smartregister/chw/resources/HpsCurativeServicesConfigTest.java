package org.smartregister.chw.resources;

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
    private static final String HPS_CURATIVE_RULES_PATH = "src/nacp/assets/rule/hps_curative_services.yml";
    private static final String HPS_REPOSITORY_FLV_PATH = "src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java";

    @Test
    public void ecHpsClientServicesShouldMapMalariaDrugsTreatment() throws Exception {
        JSONArray tables = new JSONArray(Files.readString(Paths.get(EC_CLIENT_FIELDS_PATH), StandardCharsets.UTF_8));
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
        String rules = Files.readString(Paths.get(HPS_CURATIVE_RULES_PATH), StandardCharsets.UTF_8);

        Assert.assertTrue(
                "malaria_drugs_treatment relevance should keep the field visible when a saved value exists",
                rules.contains("step1_malaria_mrdt_result.equalsIgnoreCase('positive_mrdt') || !step1_malaria_drugs_treatment.isEmpty()")
        );
    }

    @Test
    public void hpsCurativeServicesMigrationShouldAddMalariaDrugsTreatmentColumn() throws Exception {
        String repositoryFlv = Files.readString(Paths.get(HPS_REPOSITORY_FLV_PATH), StandardCharsets.UTF_8);
        String buildGradle = Files.readString(Paths.get(BUILD_GRADLE_PATH), StandardCharsets.UTF_8);

        Assert.assertTrue(
                "The NACP repository migration should add malaria_drugs_treatment to ec_hps_client_services",
                repositoryFlv.contains("ALTER TABLE ec_hps_client_services ADD COLUMN malaria_drugs_treatment VARCHAR;")
        );
        Assert.assertTrue(
                "DATABASE_VERSION should be bumped so the new migration runs on upgrade",
                buildGradle.contains("buildConfigField \"int\", \"DATABASE_VERSION\", '40'")
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
}
