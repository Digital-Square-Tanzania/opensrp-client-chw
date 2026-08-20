package org.smartregister.chw.actionhelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.smartregister.chw.ayp.domain.MemberObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class AypOutSchoolMedicalServicesActionHelperTest {

    private MemberObject memberObject;

    @Before
    public void setUp() {
        memberObject = Mockito.mock(MemberObject.class);
        Mockito.when(memberObject.getBaseEntityId()).thenReturn("ayp-client-id");
        Mockito.when(memberObject.getGender()).thenReturn("Female");
        Mockito.when(memberObject.getAge()).thenReturn(19);
    }

    @Test
    public void previousPositiveResultSuppressesHivPrepFieldsButKeepsHpvVisible() throws Exception {
        TestActionHelper helper = helper(true);
        JSONObject form = loadForm();
        helper.onJsonFormLoaded(form.toString(), null, null);

        JSONObject processed = new JSONObject(helper.getPreProcessed());

        assertEquals("native_radio", getField(processed, "received_hpv_vaccine").getString("type"));
        assertEquals("hidden", getField(processed, "hiv_tested_within_last_3_months").getString("type"));
        assertEquals("hidden", getField(processed, "hiv_result_recent").getString("type"));
        assertEquals("hidden", getField(processed, "hiv_result").getString("type"));
        assertEquals("hidden", getField(processed, "linked_to_prep").getString("type"));
    }

    @Test
    public void recentPositiveResultClearsPrepAndFullFlowValues() throws Exception {
        TestActionHelper helper = helper(false);
        JSONObject form = loadForm();
        setValue(form, "hiv_tested_within_last_3_months", "yes");
        setValue(form, "hiv_result_recent", "positive");
        setValue(form, "ctc_number_a", "CTC-123");
        setValue(form, "on_prep", "yes");
        setValue(form, "prep_facility_a", "Stale facility");
        setValue(form, "referred_for_hiv_test", "yes");
        setValue(form, "hiv_result", "negative");

        JSONObject processed = new JSONObject(helper.postProcess(form.toString()));

        assertEquals("", value(processed, "on_prep"));
        assertEquals("", value(processed, "prep_facility_a"));
        assertEquals("", value(processed, "referred_for_hiv_test"));
        assertEquals("", value(processed, "hiv_result"));
        assertEquals("positive", value(processed, "client_hiv_status"));
        assertEquals("true", value(processed, "hiv_positive"));
        assertEquals("CTC-123", value(processed, "ctc_number"));
        assertEquals("yes", value(processed, "tested_hiv"));
    }

    @Test
    public void fullFlowPositiveResultClearsPrepAndStoresCtcNumber() throws Exception {
        TestActionHelper helper = helper(false);
        JSONObject form = loadForm();
        setValue(form, "hiv_tested_within_last_3_months", "no");
        setValue(form, "referred_for_hiv_test", "yes");
        setValue(form, "tested_for_hiv", "yes");
        setValue(form, "testing_location", "facility");
        setValue(form, "hiv_result", "positive");
        setValue(form, "ctc_number_b", "CTC-456");
        setValue(form, "prep_follow_up", "no");
        setValue(form, "linked_to_prep", "yes");

        JSONObject processed = new JSONObject(helper.postProcess(form.toString()));

        assertEquals("", value(processed, "prep_follow_up"));
        assertEquals("", value(processed, "linked_to_prep"));
        assertEquals("positive", value(processed, "client_hiv_status"));
        assertEquals("true", value(processed, "hiv_positive"));
        assertEquals("CTC-456", value(processed, "ctc_number"));
        assertEquals("facility", value(processed, "loc_test_conducted"));
    }

    @Test
    public void negativeResultDoesNotSetPositiveFlagAndPreservesActiveLinkage() throws Exception {
        TestActionHelper helper = helper(false);
        JSONObject form = loadForm();
        setValue(form, "hiv_tested_within_last_3_months", "yes");
        setValue(form, "hiv_result_recent", "negative");
        setValue(form, "on_prep", "no");
        setValue(form, "linked_to_prep_recent", "no");
        setValue(form, "ctc_number_a", "stale");

        JSONObject processed = new JSONObject(helper.postProcess(form.toString()));

        assertEquals("", value(processed, "ctc_number_a"));
        assertEquals("negative", value(processed, "client_hiv_status"));
        assertEquals("false", value(processed, "hiv_positive"));
        assertEquals("no", value(processed, "linked_to_prep_services"));
    }

    private TestActionHelper helper(boolean hivPositive) {
        return new TestActionHelper(memberObject, hivPositive);
    }

    private static JSONObject loadForm() throws Exception {
        return new JSONObject(new String(Files.readAllBytes(resolvePath(
                "src/nacp/assets/json.form/ayp_out_school_medical_services.json")), StandardCharsets.UTF_8));
    }

    private static JSONObject getField(JSONObject form, String key) throws Exception {
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private static void setValue(JSONObject form, String key, String value) throws Exception {
        getField(form, key).put("value", value);
    }

    private static String value(JSONObject form, String key) throws Exception {
        return getField(form, key).optString("value");
    }

    private static Path resolvePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        return Files.exists(direct) ? direct : Paths.get("opensrp-chw").resolve(relativePath);
    }

    private static class TestActionHelper extends AypOutSchoolMedicalServicesActionHelper {
        private final boolean hivPositive;

        TestActionHelper(MemberObject memberObject, boolean hivPositive) {
            super(null, memberObject);
            this.hivPositive = hivPositive;
        }

        @Override
        protected boolean isClientHivPositive() {
            return hivPositive;
        }
    }
}
