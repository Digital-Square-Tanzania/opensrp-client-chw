package org.smartregister.chw.actionhelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.smartregister.chw.dao.ChwKvpDao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class KvpPrepReportingFieldsTest {
    private static final String[] SHARED = {"prep_type", "prep_facility", "currently_on_prep", "linked_to_prep_services"};

    @Test
    public void saveCalculatesSharedAnswersFromEitherBranchInBothLanguages() throws Exception {
        for (String language : new String[]{"json.form", "json.form-sw"}) {
            for (String screener : new String[]{"yes", "no"}) {
                for (String type : new String[]{"oral_prep", "injectable_prep"}) {
                    JSONObject form = form(language, screener);
                    String suffix = "yes".equals(screener) ? "a" : "b";
                    set(form, "prep_type_" + suffix, type);
                    JSONObject saved = save(form, false);

                    assertShared(saved, type, "Facility " + suffix, "yes", "");
                    assertEquals(type, value(saved, "prep_type_" + suffix));
                    assertEquals("", value(saved, "prep_type_" + ("a".equals(suffix) ? "b" : "a")));
                }
            }
        }
    }

    @Test
    public void saveCombinesLinkageWhenNotCurrentlyUsingPrep() throws Exception {
        for (String screener : new String[]{"yes", "no"}) {
            for (String linkage : new String[]{"yes", "no"}) {
                JSONObject form = form("json.form", screener);
                set(form, "yes".equals(screener) ? "on_prep" : "prep_follow_up", "no");
                set(form, "yes".equals(screener) ? "linked_to_prep_recent" : "linked_to_prep", linkage);
                assertShared(save(form, false), "", "", "no", linkage);
            }
        }
    }

    @Test
    public void upstreamChangesRemoveStaleSharedAnswers() throws Exception {
        String[][] cases = {
                {"yes", "hiv_tested_within_last_3_months", ""},
                {"yes", "hiv_result_recent", "positive"},
                {"yes", "hiv_result_recent", ""},
                {"yes", "on_prep", ""},
                {"no", "referred_for_hiv_test", "no"},
                {"no", "tested_for_hiv", "no"},
                {"no", "hiv_result", "positive"},
                {"no", "hiv_result", ""},
                {"no", "prep_follow_up", ""}
        };
        for (String[] scenario : cases) {
            JSONObject form = form("json.form", scenario[0]);
            set(form, scenario[1], scenario[2]);
            assertShared(save(form, false), "", "", "", "");
        }
    }

    @Test
    public void knownPositiveClientHasNoSharedPrepAnswers() throws Exception {
        assertShared(save(form("json.form", "yes"), true), "", "", "", "");
    }

    @Test
    public void missingActiveAnswerDoesNotUseTheOtherBranch() throws Exception {
        JSONObject form = form("json.form", "yes");
        set(form, "prep_type_a", "");
        assertShared(save(form, false), "", "Facility a", "yes", "");
    }

    @Test
    public void editingAndSwitchingBranchesRecalculatesTheSharedAnswers() throws Exception {
        JSONObject saved = save(form("json.form", "yes"), false);
        assertShared(saved, "oral_prep", "Facility a", "yes", "");
        set(saved, "hiv_tested_within_last_3_months", "no");
        set(saved, "referred_for_hiv_test", "yes");
        set(saved, "tested_for_hiv", "yes");
        set(saved, "hiv_result", "negative");
        set(saved, "prep_follow_up", "yes");
        set(saved, "prep_type_b", "injectable_prep");
        set(saved, "prep_facility_b", "New facility");
        saved = save(saved, false);
        assertShared(saved, "injectable_prep", "New facility", "yes", "");
        set(saved, "prep_follow_up", "no");
        set(saved, "linked_to_prep", "no");
        assertShared(save(saved, false), "", "", "no", "no");
    }

    private JSONObject save(JSONObject form, boolean hivPositive) throws Exception {
        try (MockedStatic<ChwKvpDao> dao = Mockito.mockStatic(ChwKvpDao.class)) {
            dao.when(() -> ChwKvpDao.isClientHivPositive("client-id")).thenReturn(hivPositive);
            return new JSONObject(new KvpPrEPPreventiveServicesActionHelper("client-id", new HashMap<>())
                    .postProcess(form.toString()));
        }
    }

    private JSONObject form(String language, String screener) throws Exception {
        Path path = Paths.get("src/nacp/assets/" + language + "/kvp_prep_preventive_services.json");
        if (!Files.exists(path)) {
            path = Paths.get("opensrp-chw").resolve(path);
        }
        JSONObject form = new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        set(form, "hiv_tested_within_last_3_months", screener);
        set(form, "hiv_result_recent", "negative");
        set(form, "on_prep", "yes");
        set(form, "prep_type_a", "oral_prep");
        set(form, "prep_facility_a", "Facility a");
        set(form, "linked_to_prep_recent", "no");
        set(form, "referred_for_hiv_test", "yes");
        set(form, "tested_for_hiv", "yes");
        set(form, "hiv_result", "negative");
        set(form, "prep_follow_up", "yes");
        set(form, "prep_type_b", "injectable_prep");
        set(form, "prep_facility_b", "Facility b");
        set(form, "linked_to_prep", "yes");
        for (String key : SHARED) {
            set(form, key, "stale");
        }
        return form;
    }

    private void assertShared(JSONObject form, String type, String facility, String currentUse, String linkage) throws Exception {
        String[] expected = {type, facility, currentUse, linkage};
        for (int i = 0; i < SHARED.length; i++) {
            assertEquals(SHARED[i], expected[i], value(form, SHARED[i]));
        }
    }

    private JSONObject field(JSONObject form, String key) throws Exception {
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            if (key.equals(fields.getJSONObject(i).optString("key"))) {
                return fields.getJSONObject(i);
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private void set(JSONObject form, String key, String value) throws Exception {
        field(form, key).put("value", value);
    }

    private String value(JSONObject form, String key) throws Exception {
        return field(form, key).optString("value");
    }
}
