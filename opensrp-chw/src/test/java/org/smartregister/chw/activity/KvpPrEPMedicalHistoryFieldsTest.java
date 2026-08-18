package org.smartregister.chw.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KvpPrEPMedicalHistoryFieldsTest {

    private static final Set<String> EXPECTED_HIV_PREP_FIELDS = new HashSet<>(Arrays.asList(
            "hiv_tested_within_last_3_months", "hiv_result_recent", "ctc_number_a",
            "on_prep", "prep_facility_a", "linked_to_prep_recent",
            "referred_for_hiv_test", "tested_for_hiv", "testing_location",
            "facility_name", "test_date", "hiv_result", "ctc_number_b",
            "prep_follow_up", "prep_facility_b", "linked_to_prep"));

    @Test
    public void historyIncludesAllHivPrepAndScheduleFields() {
        assertEquals(EXPECTED_HIV_PREP_FIELDS,
                new HashSet<>(Arrays.asList(KvpPrEPMedicalHistoryActivity.HIV_PREP_HISTORY_FIELDS)));
        assertEquals(new HashSet<>(Arrays.asList("next_visit_date")),
                new HashSet<>(Arrays.asList(KvpPrEPMedicalHistoryActivity.SCHEDULE_HISTORY_FIELDS)));
    }

    @Test
    public void historyFieldsHaveEnglishAndSwahiliLabels() throws Exception {
        String english = readText("src/nacp/res/values/strings.xml");
        String swahili = readText("src/nacp/res/values-sw/strings.xml");

        for (String field : EXPECTED_HIV_PREP_FIELDS) {
            assertHasLabel(english, field);
            assertHasLabel(swahili, field);
        }
        assertHasLabel(english, "next_visit_date");
        assertHasLabel(swahili, "next_visit_date");
    }

    @Test
    public void hivstDistributionAppearsOnceWithBooleanHistoryLabels() throws Exception {
        assertEquals(1, Collections.frequency(
                Arrays.asList(KvpPrEPMedicalHistoryActivity.PROTECTIVE_SERVICES_HISTORY_FIELDS),
                "kits_distributed"));

        String english = readText("src/nacp/res/values/strings.xml");
        String swahili = readText("src/main/res/values-sw/strings.xml");
        assertTrue(english.contains(
                "<string name=\"kvp_kits_distributed\">HIVST kits distributed:</string>"));
        assertTrue(swahili.contains(
                "<string name=\"kvp_kits_distributed\">Vitepe vya JIPIME vilitolewa:</string>"));
    }

    private void assertHasLabel(String stringsXml, String field) {
        assertTrue("Missing history label for " + field,
                stringsXml.contains("name=\"kvp_" + field + "\""));
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
