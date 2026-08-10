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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guards the manual referral follow-up form (WAJA "Manual Closure of Referrals") against the three
 * ways it silently breaks: the Swahili form drifting out of parity with English, an answer losing
 * its column mapping, and the migration not running because DATABASE_VERSION was not bumped.
 */
public class ReferralFollowUpFormConfigTest {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String EC_CLIENT_FIELDS_PATH = "src/nacp/assets/ec_client_fields.json";
    private static final String EC_CLIENT_CLASSIFICATION_PATH = "src/nacp/assets/ec_client_classification.json";
    private static final String FORM_PATH = "src/nacp/assets/json.form/referral_followup_form.json";
    private static final String FORM_SW_PATH = "src/nacp/assets/json.form-sw/referral_followup_form.json";
    private static final String REFERRAL_FORM_PATH = "src/nacp/assets/json.form/referral_followup_referral_form.json";
    private static final String REFERRAL_FORM_SW_PATH = "src/nacp/assets/json.form-sw/referral_followup_referral_form.json";
    private static final String REPOSITORY_FLV_PATH = "src/nacp/java/org/smartregister/chw/repository/ChwRepositoryFlv.java";

    private static final String TABLE = "ec_referral_followup";
    private static final String ENCOUNTER_TYPE = "Referral Followup Registration";

    private static final List<String> ANSWER_KEYS = Arrays.asList(
            "referral_task_id",
            "client_attended_referral",
            "reason_not_attended",
            "attended_assigned_facility",
            "reason_different_facility",
            "facility_attended",
            "services_received",
            "client_condition",
            "client_satisfied"
    );

    @Test
    public void bothFormsShouldDeclareTheReferralFollowUpEncounter() throws Exception {
        for (String formPath : Arrays.asList(FORM_PATH, FORM_SW_PATH)) {
            JSONObject form = new JSONObject(readText(formPath));
            Assert.assertEquals(
                    formPath + " should emit the encounter type routed by ec_client_classification.json",
                    ENCOUNTER_TYPE,
                    form.getString("encounter_type")
            );
        }
    }

    @Test
    public void swahiliFormShouldMirrorEnglishStructure() throws Exception {
        JSONArray english = fields(FORM_PATH);
        JSONArray swahili = fields(FORM_SW_PATH);

        Assert.assertEquals("Swahili form should have the same number of questions", english.length(), swahili.length());
        Assert.assertEquals("Swahili form should keep the English question order", keys(english), keys(swahili));

        for (int i = 0; i < english.length(); i++) {
            JSONObject en = english.getJSONObject(i);
            JSONObject sw = swahili.getJSONObject(i);
            String key = en.getString(JsonFormConstants.KEY);

            Assert.assertEquals(key + " should keep its widget type", en.getString(JsonFormConstants.TYPE), sw.getString(JsonFormConstants.TYPE));
            Assert.assertEquals(key + " should keep its option keys", optionKeys(en), optionKeys(sw));
            Assert.assertEquals(key + " should keep its relevance rule", String.valueOf(en.opt("relevance")), String.valueOf(sw.opt("relevance")));

            if (en.has(JsonFormConstants.LABEL)) {
                Assert.assertTrue(key + " should carry a Swahili label", sw.has(JsonFormConstants.LABEL));
                Assert.assertNotEquals(
                        key + " should be translated rather than left in English",
                        en.getString(JsonFormConstants.LABEL),
                        sw.getString(JsonFormConstants.LABEL)
                );
            }
        }
    }

    @Test
    public void skipLogicShouldFollowTheClosureFlowchart() throws Exception {
        for (String formPath : Arrays.asList(FORM_PATH, FORM_SW_PATH)) {
            JSONArray fields = fields(formPath);

            Assert.assertFalse(
                    formPath + ": the first question must always be asked",
                    findField(fields, "client_attended_referral").has("relevance")
            );
            assertRelevance(formPath, fields, "reason_not_attended", "client_attended_referral", "no");
            assertRelevance(formPath, fields, "attended_assigned_facility", "client_attended_referral", "yes");
            assertRelevance(formPath, fields, "reason_different_facility", "attended_assigned_facility", "no");
            assertRelevance(formPath, fields, "facility_attended", "attended_assigned_facility", "no");
            assertRelevance(formPath, fields, "services_received", "client_attended_referral", "yes");
            assertRelevance(formPath, fields, "client_satisfied", "client_condition", "no_danger_signs");

            Assert.assertFalse(
                    formPath + ": both branches of the flowchart converge on the client's condition",
                    findField(fields, "client_condition").has("relevance")
            );
        }
    }

    @Test
    public void facilityAttendedShouldBePopulatedAtRuntime() throws Exception {
        for (String formPath : Arrays.asList(FORM_PATH, FORM_SW_PATH)) {
            JSONObject facility = findField(fields(formPath), "facility_attended");
            Assert.assertEquals(formPath + ": facility list is a searchable spinner", "spinner", facility.getString(JsonFormConstants.TYPE));
            Assert.assertTrue(formPath + ": facility list must be searchable", facility.optBoolean("searchable"));
            Assert.assertEquals(
                    formPath + ": options are filled from the synced locations, so none may be hardcoded",
                    0,
                    facility.getJSONArray("options").length()
            );
        }
    }

    @Test
    public void answersShouldBeMappedRoutedAndMigrated() throws Exception {
        JSONArray bindObjects = new JSONObject(readText(EC_CLIENT_FIELDS_PATH)).getJSONArray("bindobjects");
        JSONArray columns = findBindObject(bindObjects, TABLE).getJSONArray("columns");

        for (String key : ANSWER_KEYS) {
            Assert.assertTrue(TABLE + " should persist " + key, hasColumn(columns, key));
        }

        Assert.assertTrue(
                ENCOUNTER_TYPE + " should create a case in " + TABLE,
                readText(EC_CLIENT_CLASSIFICATION_PATH).contains(ENCOUNTER_TYPE)
        );

        String repositoryFlv = readText(REPOSITORY_FLV_PATH);
        for (String key : ANSWER_KEYS) {
            Assert.assertTrue(
                    "NACP migration should add " + key + " for upgraded installations",
                    repositoryFlv.contains("addColumnIfMissing(db, tableName, \"" + key + "\")")
            );
        }
        Assert.assertTrue(
                "the referral follow-up migration should be reachable from onUpgrade",
                repositoryFlv.contains("upgradeToVersion49(db)")
        );
        Assert.assertTrue(
                "DATABASE_VERSION should be bumped so the referral follow-up migration runs",
                extractDatabaseVersion(readText(BUILD_GRADLE_PATH)) >= 49
        );
    }

    @Test
    public void referralFormShouldRaiseAReferralRegistration() throws Exception {
        for (String formPath : Arrays.asList(REFERRAL_FORM_PATH, REFERRAL_FORM_SW_PATH)) {
            JSONObject form = new JSONObject(readText(formPath));
            Assert.assertEquals(
                    formPath + " should emit the event type routed to ec_referral",
                    "Referral Registration",
                    form.getString("encounter_type")
            );
        }
        Assert.assertTrue(
                "Referral Registration should create a case in ec_referral",
                readText(EC_CLIENT_CLASSIFICATION_PATH).contains("Referral Registration")
        );
    }

    @Test
    public void referralFormShouldAskReasonFacilityAndEmergency() throws Exception {
        for (String formPath : Arrays.asList(REFERRAL_FORM_PATH, REFERRAL_FORM_SW_PATH)) {
            JSONArray fields = fields(formPath);
            Assert.assertEquals(
                    formPath + " should stay short — reason, other, facility, emergency",
                    Arrays.asList("problem", "problem_other", "chw_referral_hf", "is_emergency_case"),
                    keys(fields)
            );

            JSONObject problem = findField(fields, "problem");
            Assert.assertEquals("check_box", problem.getString(JsonFormConstants.TYPE));
            Assert.assertEquals(
                    formPath + ": the agreed reason list",
                    Arrays.asList("condition_not_improved", "condition_worsened",
                            "did_not_receive_services", "new_danger_signs", "other"),
                    optionKeys(problem)
            );

            JSONObject other = findField(fields, "problem_other");
            Assert.assertEquals(
                    formPath + ": the free-text reason should only appear when Other is ticked",
                    "{\"or\":[\"other\"]}",
                    other.getJSONObject("relevance").getJSONObject("step1:problem")
                            .getJSONArray("ex-checkbox").getJSONObject(0).toString()
            );

            JSONObject facility = findField(fields, "chw_referral_hf");
            Assert.assertTrue(formPath + ": facility list must be searchable", facility.optBoolean("searchable"));
            Assert.assertEquals(
                    formPath + ": facilities are injected at runtime, so none may be hardcoded",
                    0,
                    facility.getJSONArray("options").length()
            );

            Assert.assertEquals(
                    formPath + ": emergency flag uses the Yes/No keys the event builder expects",
                    Arrays.asList("Yes", "No"),
                    optionKeys(findField(fields, "is_emergency_case"))
            );
        }
    }

    @Test
    public void referralFormSwahiliShouldMirrorEnglishStructure() throws Exception {
        assertParity(REFERRAL_FORM_PATH, REFERRAL_FORM_SW_PATH);
    }

    private void assertParity(String englishPath, String swahiliPath) throws Exception {
        JSONArray english = fields(englishPath);
        JSONArray swahili = fields(swahiliPath);

        Assert.assertEquals("Swahili form should have the same number of questions", english.length(), swahili.length());
        Assert.assertEquals("Swahili form should keep the English question order", keys(english), keys(swahili));

        for (int i = 0; i < english.length(); i++) {
            JSONObject en = english.getJSONObject(i);
            JSONObject sw = swahili.getJSONObject(i);
            String key = en.getString(JsonFormConstants.KEY);

            Assert.assertEquals(key + " should keep its widget type", en.getString(JsonFormConstants.TYPE), sw.getString(JsonFormConstants.TYPE));
            Assert.assertEquals(key + " should keep its option keys", optionKeys(en), optionKeys(sw));
            Assert.assertEquals(key + " should keep its relevance rule", String.valueOf(en.opt("relevance")), String.valueOf(sw.opt("relevance")));

            if (en.has(JsonFormConstants.LABEL)) {
                Assert.assertTrue(key + " should carry a Swahili label", sw.has(JsonFormConstants.LABEL));
                Assert.assertNotEquals(
                        key + " should be translated rather than left in English",
                        en.getString(JsonFormConstants.LABEL),
                        sw.getString(JsonFormConstants.LABEL)
                );
            }
        }
    }

    private void assertRelevance(String formPath, JSONArray fields, String key, String dependsOn, String value) throws Exception {
        JSONObject relevance = findField(fields, key).optJSONObject("relevance");
        Assert.assertNotNull(formPath + ": " + key + " should only be asked after " + dependsOn, relevance);

        JSONObject rule = relevance.optJSONObject("step1:" + dependsOn);
        Assert.assertNotNull(formPath + ": " + key + " should depend on " + dependsOn, rule);
        Assert.assertEquals(
                formPath + ": " + key + " should be shown when " + dependsOn + " is " + value,
                "equalTo(.,\"" + value + "\")",
                rule.getString("ex")
        );
    }

    private JSONArray fields(String formPath) throws Exception {
        return new JSONObject(readText(formPath)).getJSONObject("step1").getJSONArray(JsonFormConstants.FIELDS);
    }

    private List<String> keys(JSONArray fields) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < fields.length(); i++) {
            keys.add(fields.optJSONObject(i).optString(JsonFormConstants.KEY));
        }
        return keys;
    }

    private List<String> optionKeys(JSONObject field) {
        List<String> keys = new ArrayList<>();
        JSONArray options = field.optJSONArray("options");
        if (options != null) {
            for (int i = 0; i < options.length(); i++) {
                keys.add(options.optJSONObject(i).optString(JsonFormConstants.KEY));
            }
        }
        return keys;
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

    private int extractDatabaseVersion(String buildGradle) {
        Matcher matcher = Pattern.compile("buildConfigField\\s+\"int\",\\s+\"DATABASE_VERSION\",\\s+'(\\d+)'")
                .matcher(buildGradle);
        if (!matcher.find()) {
            throw new AssertionError("Could not find DATABASE_VERSION buildConfigField");
        }
        return Integer.parseInt(matcher.group(1));
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
