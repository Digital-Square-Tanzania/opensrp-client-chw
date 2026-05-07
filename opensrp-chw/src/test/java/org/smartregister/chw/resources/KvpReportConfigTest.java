package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class KvpReportConfigTest {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String EC_CLIENT_FIELDS_PATH = "src/nacp/assets/ec_client_fields.json";
    private static final String KVP_REPORT_CONFIG_PATH = "src/nacp/assets/config/kvp-monthly-report.yml";
    private static final String HPS_REPOSITORY_FLV_PATH = "src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java";

    @Test
    public void ecKvpPrepFollowupShouldMapKvpVisitDateFromEventDate() throws Exception {
        JSONArray tables = new JSONObject(readFile(EC_CLIENT_FIELDS_PATH)).getJSONArray("bindobjects");
        JSONObject kvpFollowup = findTable(tables, "ec_kvp_prep_followup");
        Assert.assertNotNull("Missing ec_kvp_prep_followup table definition", kvpFollowup);

        JSONObject kvpVisitDate = findColumn(kvpFollowup.getJSONArray("columns"), "kvp_visit_date");
        Assert.assertNotNull("ec_kvp_prep_followup must include kvp_visit_date for KVP monthly reports", kvpVisitDate);
        Assert.assertEquals("eventDate", kvpVisitDate.getJSONObject("json_mapping").getString("field"));
    }

    @Test
    public void kvpMonthlyReportShouldUseIsoVisitDateParsing() throws Exception {
        String kvpReportConfig = readFile(KVP_REPORT_CONFIG_PATH);

        Assert.assertFalse(
                "KVP monthly indicators should not parse kvp_visit_date as dd-MM-yyyy",
                kvpReportConfig.contains("substr(ekpf.kvp_visit_date, 7, 4)")
                        || kvpReportConfig.contains("substr(ekpf.kvp_visit_date, 4, 2)")
        );
        Assert.assertTrue(
                "KVP monthly indicators should parse the ISO eventDate-backed kvp_visit_date",
                kvpReportConfig.contains("substr(ekpf.kvp_visit_date, 1, 4)")
                        && kvpReportConfig.contains("substr(ekpf.kvp_visit_date, 6, 2)")
        );
        Assert.assertFalse(
                "KVP referral indicators should not contain an unmatched 10-14 age predicate",
                kvpReportConfig.contains("WHERE\n                      date(efm.dob, '+10 years') <= date('now'))")
        );
    }

    @Test
    public void kvpMigrationShouldAddBackfillAndReloadReportConfig() throws Exception {
        String repositoryFlv = readFile(HPS_REPOSITORY_FLV_PATH);
        String buildGradle = readFile(BUILD_GRADLE_PATH);

        Assert.assertTrue(repositoryFlv.contains("ALTER TABLE ec_kvp_prep_followup ADD COLUMN kvp_visit_date VARCHAR;"));
        Assert.assertTrue(repositoryFlv.contains("case 44:"));
        Assert.assertTrue(repositoryFlv.contains("event.formSubmissionId = ec_kvp_prep_followup.base_entity_id"));
        Assert.assertTrue(repositoryFlv.contains("DELETE FROM indicator_queries WHERE indicator_code LIKE 'kvp-%';"));
        Assert.assertTrue(repositoryFlv.contains("DELETE FROM indicator_daily_tally WHERE indicator_code LIKE 'kvp-%';"));
        Assert.assertTrue(repositoryFlv.contains("reportingLibrary.readConfigFile(\"config/kvp-monthly-report.yml\", db)"));
        Assert.assertTrue(
                "DATABASE_VERSION should be bumped so the KVP migration runs on upgrade",
                buildGradle.contains("buildConfigField \"int\", \"DATABASE_VERSION\", '44'")
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

    private JSONObject findColumn(JSONArray columns, String columnName) {
        for (int i = 0; i < columns.length(); i++) {
            JSONObject column = columns.optJSONObject(i);
            if (column != null && columnName.equals(column.optString("column_name"))) {
                return column;
            }
        }
        return null;
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
