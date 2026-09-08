package org.smartregister.chw.interactor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class NacpVaccineAssetTest {

    @Test
    public void testNacpMrDoseDefinitionsUseDistinctOpenMrsDoseCalculations() throws Exception {
        JSONObject mrOne = findVaccine(readJsonArray("vaccines.json"), "MR 1");
        JSONObject mrTwo = findVaccine(readJsonArray("vaccines.json"), "MR 2");

        assertEquals("MR", mrOne.getString("type"));
        assertEquals("MR", mrTwo.getString("type"));
        assertEquals(1, mrOne.getJSONObject("openmrs_calculate").getInt("calculation"));
        assertEquals(2, mrTwo.getJSONObject("openmrs_calculate").getInt("calculation"));

        JSONObject mrTwoDue = mrTwo.getJSONObject("schedule").getJSONArray("due").getJSONObject(0);
        assertEquals("prerequisite", mrTwoDue.getString("reference"));
        assertEquals("MR 1", mrTwoDue.getString("prerequisite"));
    }

    @Test
    public void testNacpVaccineTypeAssetStaysAlignedWithScheduleAssetForMrDoseTwo() throws Exception {
        JSONObject mrTwo = findVaccine(readJsonArray("vaccine_type.json"), "MR 2");

        assertEquals("MR", mrTwo.getString("type"));
        assertEquals(2, mrTwo.getJSONObject("openmrs_calculate").getInt("calculation"));
    }

    @Test
    public void testNacpRotaThreeUsesFourWeekPrerequisiteWindow() throws Exception {
        JSONObject rotaThree = findVaccine(readJsonArray("vaccines.json"), "Rota 3");
        JSONObject rotaThreeVaccineType = findVaccine(readJsonArray("vaccine_type.json"), "Rota 3");

        assertRotaThreeDefinition(rotaThree);
        assertRotaThreeDefinition(rotaThreeVaccineType);
    }

    @Test
    public void testNacpVaccineTypeAssetContainsAllScheduleVaccines() throws Exception {
        Set<String> scheduleVaccines = getVaccineNames(readJsonArray("vaccines.json"));
        Set<String> vaccineTypeVaccines = getVaccineNames(readJsonArray("vaccine_type.json"));

        assertEquals(scheduleVaccines, vaccineTypeVaccines);
    }

    @Test
    public void testEcClientVaccinePersistsVaccineNameFromFormSubmissionField() throws Exception {
        JSONObject ecClientVaccine = readJsonObject("ec_client_vaccine.json");
        assertEquals("vaccines", ecClientVaccine.getString("name"));

        JSONObject nameColumn = findColumn(ecClientVaccine, "name");
        JSONObject nameMapping = nameColumn.getJSONObject("json_mapping");

        assertEquals("obs.fieldCode", nameMapping.getString("field"));
        assertEquals("1410AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", nameMapping.getString("concept"));
        assertEquals("formSubmissionField", nameMapping.getString("value_field"));
    }

    private JSONArray readJsonArray(String assetName) throws Exception {
        return new JSONArray(readAsset(assetName));
    }

    private JSONObject readJsonObject(String assetName) throws Exception {
        return new JSONObject(readAsset(assetName));
    }

    private String readAsset(String assetName) throws Exception {
        return new String(Files.readAllBytes(resolveAsset(assetName).toPath()), StandardCharsets.UTF_8);
    }

    private File resolveAsset(String assetName) {
        File moduleRelative = new File("src/nacp/assets", assetName);
        if (moduleRelative.exists()) {
            return moduleRelative;
        }

        return new File("opensrp-chw/src/nacp/assets", assetName);
    }

    private JSONObject findVaccine(JSONArray groups, String vaccineName) throws Exception {
        for (int groupIndex = 0; groupIndex < groups.length(); groupIndex++) {
            JSONArray vaccines = groups.getJSONObject(groupIndex).getJSONArray("vaccines");
            for (int vaccineIndex = 0; vaccineIndex < vaccines.length(); vaccineIndex++) {
                JSONObject vaccine = vaccines.getJSONObject(vaccineIndex);
                if (vaccineName.equals(vaccine.getString("name"))) {
                    return vaccine;
                }
            }
        }
        throw new AssertionError("Missing NACP vaccine definition: " + vaccineName);
    }

    private void assertRotaThreeDefinition(JSONObject rotaThree) throws Exception {
        assertEquals("Rota", rotaThree.getString("type"));
        assertEquals(3, rotaThree.getJSONObject("openmrs_calculate").getInt("calculation"));

        JSONObject due = rotaThree.getJSONObject("schedule").getJSONArray("due").getJSONObject(0);
        assertEquals("prerequisite", due.getString("reference"));
        assertEquals("Rota 2", due.getString("prerequisite"));
        assertEquals("+28d", due.getString("offset"));
        assertEquals("+14d", due.getString("window"));

        JSONObject expiry = rotaThree.getJSONObject("schedule").getJSONArray("expiry").getJSONObject(0);
        assertEquals("dob", expiry.getString("reference"));
        assertEquals("+2y", expiry.getString("offset"));
    }

    private Set<String> getVaccineNames(JSONArray groups) throws Exception {
        Set<String> vaccineNames = new LinkedHashSet<>();
        for (int groupIndex = 0; groupIndex < groups.length(); groupIndex++) {
            JSONArray vaccines = groups.getJSONObject(groupIndex).getJSONArray("vaccines");
            for (int vaccineIndex = 0; vaccineIndex < vaccines.length(); vaccineIndex++) {
                vaccineNames.add(vaccines.getJSONObject(vaccineIndex).getString("name"));
            }
        }
        return vaccineNames;
    }

    private JSONObject findColumn(JSONObject table, String columnName) throws Exception {
        JSONArray columns = table.getJSONArray("columns");
        for (int columnIndex = 0; columnIndex < columns.length(); columnIndex++) {
            JSONObject column = columns.getJSONObject(columnIndex);
            if (columnName.equals(column.getString("column_name"))) {
                return column;
            }
        }
        throw new AssertionError("Missing column: " + columnName);
    }
}
