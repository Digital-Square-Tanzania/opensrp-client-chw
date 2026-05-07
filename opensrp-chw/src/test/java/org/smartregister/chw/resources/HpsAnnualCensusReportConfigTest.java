package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HpsAnnualCensusReportConfigTest {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String EC_CLIENT_FIELDS_PATH = "src/nacp/assets/ec_client_fields.json";
    private static final String HPS_ANNUAL_REPORT_PATH = "src/nacp/assets/config/hps-annual-report.yml";
    private static final String HPS_ANNUAL_REPORT_OBJECT_PATH = "src/main/java/org/smartregister/chw/domain/hps_reports/HpsAnnualReportObject.java";
    private static final String HPS_REPOSITORY_FLV_PATH = "src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java";

    @Test
    public void ecHpsAnnualCensusRegisterShouldMapAllQuarterAttendanceFields() throws Exception {
        JSONArray tables = new JSONObject(readFile(EC_CLIENT_FIELDS_PATH)).getJSONArray("bindobjects");
        JSONObject annualCensusRegister = findTable(tables, "ec_hps_annual_census_register");
        Assert.assertNotNull("Missing ec_hps_annual_census_register table definition", annualCensusRegister);

        JSONArray columns = annualCensusRegister.getJSONArray("columns");
        Assert.assertTrue(hasColumn(columns, "number_of_committee_members_attended_first_quarter"));
        Assert.assertTrue(hasColumn(columns, "number_of_committee_members_attended_second_quarter"));
        Assert.assertTrue(hasColumn(columns, "number_of_committee_members_attended_third_quarter"));
        Assert.assertTrue(hasColumn(columns, "number_of_committee_members_attended_fourth_quarter"));
    }

    @Test
    public void hpsAnnualReportShouldAggregateAllQuarterAttendanceFields() throws Exception {
        String annualReportConfig = readFile(HPS_ANNUAL_REPORT_PATH);
        String annualReportObject = readFile(HPS_ANNUAL_REPORT_OBJECT_PATH);

        Assert.assertTrue(
                "The reporting-library query should sum all quarter attendance values for hps-x-2",
                annualReportConfig.contains("number_of_committee_members_attended_first_quarter")
                        && annualReportConfig.contains("number_of_committee_members_attended_second_quarter")
                        && annualReportConfig.contains("number_of_committee_members_attended_third_quarter")
                        && annualReportConfig.contains("number_of_committee_members_attended_fourth_quarter")
        );
        Assert.assertTrue(
                "The web annual report payload should aggregate all quarter attendance fields for hps-x-2",
                annualReportObject.contains("TOTAL_COMMITTEE_MEMBERS_ATTENDED_QUARTERS_SELECTOR")
                        && annualReportObject.contains("number_of_committee_members_attended_second_quarter")
                        && annualReportObject.contains("number_of_committee_members_attended_third_quarter")
                        && annualReportObject.contains("number_of_committee_members_attended_fourth_quarter")
        );
    }

    @Test
    public void annualCensusMigrationShouldAddAndBackfillQuarterAttendanceColumns() throws Exception {
        String repositoryFlv = readFile(HPS_REPOSITORY_FLV_PATH);
        String buildGradle = readFile(BUILD_GRADLE_PATH);

        Assert.assertTrue(repositoryFlv.contains("number_of_committee_members_attended_second_quarter"));
        Assert.assertTrue(repositoryFlv.contains("number_of_committee_members_attended_third_quarter"));
        Assert.assertTrue(repositoryFlv.contains("number_of_committee_members_attended_fourth_quarter"));
        Assert.assertTrue(
                "The migration should backfill missing quarter attendance values from visit_details",
                repositoryFlv.contains("COALESCE(NULLIF(vd.human_readable_details, ''), NULLIF(vd.details, ''), '') AS value")
        );
        Assert.assertTrue(
                "DATABASE_VERSION should be bumped so the annual census migration runs on upgrade",
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

    private boolean hasColumn(JSONArray columns, String columnName) {
        for (int i = 0; i < columns.length(); i++) {
            JSONObject column = columns.optJSONObject(i);
            if (column != null && columnName.equals(column.optString("column_name"))) {
                return true;
            }
        }
        return false;
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
