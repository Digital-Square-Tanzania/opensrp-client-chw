package org.smartregister.chw.resources;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

public class TbLeprosyStringTranslationsTest {

    private static final String DEFAULT_STRINGS_XML_PATH = "src/main/res/values/strings.xml";
    private static final String SW_STRINGS_XML_PATH = "src/main/res/values-sw/strings.xml";

    @Test
    public void shouldContainAllTbLeprosyKeysFromDefaultInSwahiliFile() throws Exception {
        Map<String, String> defaultTbLeprosyStrings = readStringsWithPrefix(DEFAULT_STRINGS_XML_PATH, "tb_leprosy_");
        Map<String, String> swahiliTbLeprosyStrings = readStringsWithPrefix(SW_STRINGS_XML_PATH, "tb_leprosy_");

        Assert.assertFalse(
                "No tb_leprosy_ keys were found in default strings.xml",
                defaultTbLeprosyStrings.isEmpty()
        );
        Assert.assertEquals(
                "Swahili strings.xml is missing tb_leprosy_ keys from default strings.xml",
                defaultTbLeprosyStrings.keySet(),
                swahiliTbLeprosyStrings.keySet()
        );
    }

    @Test
    public void shouldContainExpectedSwahiliTbLeprosyStringsInSwFile() throws Exception {
        Map<String, String> swahiliTbLeprosyStrings = readStringsWithPrefix(SW_STRINGS_XML_PATH, "tb_leprosy_");
        Map<String, String> expectedSwahiliTbLeprosyStrings = expectedSwahiliTbLeprosyStrings();

        Assert.assertEquals(
                "Unexpected tb_leprosy_ key set in Swahili strings.xml",
                expectedSwahiliTbLeprosyStrings.keySet(),
                swahiliTbLeprosyStrings.keySet()
        );

        for (Map.Entry<String, String> expectedEntry : expectedSwahiliTbLeprosyStrings.entrySet()) {
            Assert.assertEquals(
                    "Unexpected value for key: " + expectedEntry.getKey(),
                    expectedEntry.getValue(),
                    swahiliTbLeprosyStrings.get(expectedEntry.getKey())
            );
        }
    }

    @Test
    public void shouldContainAllTbleprosyKeysFromDefaultInSwahiliFile() throws Exception {
        Map<String, String> defaultTbleprosyStrings = readStringsWithPrefix(DEFAULT_STRINGS_XML_PATH, "tbleprosy_");
        Map<String, String> swahiliTbleprosyStrings = readStringsWithPrefix(SW_STRINGS_XML_PATH, "tbleprosy_");

        Assert.assertFalse(
                "No tbleprosy_ keys were found in default strings.xml",
                defaultTbleprosyStrings.isEmpty()
        );
        Assert.assertEquals(
                "Swahili strings.xml is missing tbleprosy_ keys from default strings.xml",
                defaultTbleprosyStrings.keySet(),
                swahiliTbleprosyStrings.keySet()
        );
    }

    @Test
    public void shouldContainExpectedSwahiliTbleprosyStringsInSwFile() throws Exception {
        Map<String, String> swahiliTbleprosyStrings = readStringsWithPrefix(SW_STRINGS_XML_PATH, "tbleprosy_");
        Map<String, String> expectedSwahiliTbleprosyStrings = expectedSwahiliTbleprosyStrings();

        Assert.assertEquals(
                "Unexpected tbleprosy_ key set in Swahili strings.xml",
                expectedSwahiliTbleprosyStrings.keySet(),
                swahiliTbleprosyStrings.keySet()
        );

        for (Map.Entry<String, String> expectedEntry : expectedSwahiliTbleprosyStrings.entrySet()) {
            Assert.assertEquals(
                    "Unexpected value for key: " + expectedEntry.getKey(),
                    expectedEntry.getValue(),
                    swahiliTbleprosyStrings.get(expectedEntry.getKey())
            );
        }
    }

    private Map<String, String> readStringsWithPrefix(String path, String prefix) throws Exception {
        File stringsFile = new File(path);
        Assert.assertTrue("Missing strings.xml at " + path, stringsFile.exists());

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile);
        NodeList stringNodes = document.getElementsByTagName("string");
        Map<String, String> values = new HashMap<>();

        for (int i = 0; i < stringNodes.getLength(); i++) {
            Element element = (Element) stringNodes.item(i);
            String key = element.getAttribute("name");
            if (key != null && key.startsWith(prefix)) {
                values.put(key, element.getTextContent().trim());
            }
        }
        return values;
    }

    private Map<String, String> expectedSwahiliTbLeprosyStrings() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("tb_leprosy_reports_title", "Ripoti za Kifua Kikuu/Ukoma");
        values.put("tb_leprosy_report_section_community", "Uchunguzi wa awali wa Kifua Kikuu na Ukoma katika jamii");
        values.put("tb_leprosy_report_section_community_desc", "Ripoti za uchunguzi katika jamii kwa umri na jinsia");
        values.put("tb_leprosy_report_section_household", "Uchunguzi kwa wanaoishi pamoja au karibu na mgonjwa wa Kifua Kikuu/Ukoma");
        values.put("tb_leprosy_report_section_household_desc", "Ripoti za uchunguzi kwa wanafamilia na kaya");
        values.put("tb_leprosy_report_section_special", "Uchunguzi katika maeneo maalumu");
        values.put("tb_leprosy_report_section_special_desc", "Ripoti za uchunguzi katika maeneo maalumu kama masokoni na migodini");
        values.put("tb_leprosy_report_section_treatment", "Waliokatiza au hawakuanza matibabu");
        values.put("tb_leprosy_report_section_treatment_desc", "Ripoti za ufuatiliaji wa waliokatiza au hawakuanza matibabu");
        values.put("tb_leprosy_report_section_challenges", "Changamoto za huduma za Kifua Kikuu na Ukoma");
        values.put("tb_leprosy_report_section_challenges_desc", "Ripoti za changamoto zilizobainika wakati wa huduma");
        values.put("tb_leprosy_report_section_merged", "Ripoti jumuishi ya Kifua Kikuu na Ukoma");
        values.put("tb_leprosy_report_section_merged_desc", "Ripoti moja inayounganisha sehemu zote tano za Kifua Kikuu/Ukoma");
        values.put("tb_leprosy_reports_subtitle", "Tazama ripoti ya Kifua Kikuu/Ukoma");
        values.put("tb_leprosy_monthly_report_title", "Ripoti ya mwezi ya Kifua Kikuu/Ukoma");
        return values;
    }

    private Map<String, String> expectedSwahiliTbleprosyStrings() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("tbleprosy_contact_registration_success", "Usajili wa mhisiwa wa Kifua Kikuu/Ukoma umehifadhiwa");
        values.put("tbleprosy_no_contacts_available", "Hakuna wateja wanaokidhi vigezo");
        values.put("tbleprosy_issue_referral_action", "Toa rufaa");
        values.put("tbleprosy_pending_issuing_of_referral", "Bado rufaa haijatolewa");
        values.put("tbleprosy_issue_referral_toast", "Toa rufaa");
        values.put("tbleprosy_contact_family_header", "Wanafamilia");
        values.put("tbleprosy_contact_other_header", "Wateja wengine");
        values.put("tbleprosy_contact_search_hint", "Tafuta jina au nambari ya utambulisho");
        values.put("tbleprosy_contact_no_match", "Hakuna mteja aliyelingana na utafutaji wako");
        values.put("tbleprosy_contact_family_dialog_title", "Chagua mwanafamilia");
        values.put("tbleprosy_contact_all_clients_dialog_title", "Chagua mteja");
        values.put("tbleprosy_visit_history_title", "Hudhurio za Kifua Kikuu/Ukoma");
        values.put("tbleprosy_observation_results_history", "Matokeo ya uchunguzi");
        values.put("tbleprosy_observation_results_visit_title", "Hudhurio la matokeo ya uchunguzi");
        values.put("tbleprosy_observation_results_investigation_type", "Aina ya uchunguzi");
        values.put("tbleprosy_observation_results_tb_preliminary_investigation_tests", "Chagua vipimo vya awali vya uchunguzi wa Kifua Kikuu");
        values.put("tbleprosy_observation_results_tb_preliminary_investigation_results", "Matokeo ya uchunguzi wa awali");
        values.put("tbleprosy_observation_results_tb_diagnostic_test_type", "Aina ya kipimo cha ugunduzi kilichotumika kupima Kifua Kikuu");
        values.put("tbleprosy_observation_results_tb_sample_test_results", "Matokeo ya sampuli ya Kifua Kikuu");
        values.put("tbleprosy_observation_results_clinical_decision", "Maamuzi ya kitabibu");
        values.put("tbleprosy_observation_results_leprosy_diagnostic_method", "Aina ya uchunguzi uliyotumika kupima ukoma");
        values.put("tbleprosy_observation_results_leprosy_investigation_results", "Matokeo ya uchunguzi wa Ukoma");
        values.put("tbleprosy_observation_results_tb_treatment_initiated", "Je, ameanza tiba ya TB/DR TB?");
        values.put("tbleprosy_observation_results_leprosy_treatment_initiated", "Je, ameanza tiba ya Ukoma?");
        values.put("tbleprosy_observation_results_eligible_for_tpt", "Je, mteja anastahili kuanzishiwa TPT?");
        values.put("tbleprosy_observation_results_tpt_initiation_date", "Tarehe ya kuanzishiwa TPT");
        values.put("tbleprosy_observation_results_tb_treatment_start_date", "Tarehe ya kuanza tiba ya Kifua Kikuu");
        values.put("tbleprosy_observation_results_leprosy_treatment_start_date", "Tarehe ya kuanza tiba ya Ukoma");
        values.put("tbleprosy_observation_results_hiv_tested", "Je, amepima VVU?");
        values.put("tbleprosy_observation_results_poor_sample_quality_prompt", "Kusanya sampuli ya mteja kwa ajili ya kurudia kufanya kipimo cha Kifua Kikuu.");
        values.put("tbleprosy_observation_results_tb", "Kifua Kikuu (TB)");
        values.put("tbleprosy_observation_results_leprosy", "Ukoma");
        values.put("tbleprosy_observation_results_screening_questionnaire", "Dodoso la uchunguzi");
        values.put("tbleprosy_observation_results_xray", "Mashine ya mionzi");
        values.put("tbleprosy_observation_results_scorechart", "Chati ya alama");
        values.put("tbleprosy_observation_results_treatment_decision_algorithm", "Kanuni ya uamuzi wa matibabu (TDA)");
        values.put("tbleprosy_observation_results_tb_indicative", "Ana viashiria vya Kifua Kikuu");
        values.put("tbleprosy_observation_results_no_tb_indications", "Hana viashiria vya Kifua Kikuu");
        values.put("tbleprosy_observation_results_genexpert", "GeneXpert");
        values.put("tbleprosy_observation_results_trunati", "Trunati");
        values.put("tbleprosy_observation_results_smear_microscopy", "Smear Microscopy");
        values.put("tbleprosy_observation_results_lflam", "LFLAM");
        values.put("tbleprosy_observation_results_pulmonary_tb", "Kifua kikuu cha mapafu");
        values.put("tbleprosy_observation_results_extra_pulmonary_tb", "Kifua kikuu nje ya mapafu");
        values.put("tbleprosy_observation_results_drug_resistant_tb", "Kifua Kikuu sugu (DR-TB)");
        values.put("tbleprosy_observation_results_tb_dr_tb_undetected", "TB, DR-TB haijagundulika");
        values.put("tbleprosy_observation_results_poor_quality_sample", "Sampuli haina ubora");
        values.put("tbleprosy_observation_results_suggestive", "Inaashiria");
        values.put("tbleprosy_observation_results_non_suggestive", "Haiashirii");
        values.put("tbleprosy_observation_results_hadubini", "Hadubini");
        values.put("tbleprosy_observation_results_clinical_decision_method", "Maamuzi ya kitabibu");
        values.put("tbleprosy_observation_results_leprosy_confirmed", "Amegundulika na Ukoma");
        values.put("tbleprosy_observation_results_no_leprosy_detected", "Hana Ukoma");
        values.put("tbleprosy_observation_results_yes", "Ndiyo");
        values.put("tbleprosy_observation_results_no", "Hapana");
        values.put("tbleprosy_observation_results_yes_hiv_tested", "Ndiyo");
        values.put("tbleprosy_observation_results_no_hiv_not_tested", "Hapana");
        values.put("tbleprosy_followup_visit_title", "Hudhurio la ufuatiliaji wa Kifua Kikuu na Ukoma");
        values.put("tbleprosy_followup_visit_follow_up_reason", "Sababu ya ufuatiliaji");
        values.put("tbleprosy_followup_visit_interrupted_treatment", "Amekatiza dawa");
        values.put("tbleprosy_followup_visit_never_started_treatment", "Hakuanza dawa");
        values.put("tbleprosy_followup_visit_follow_up_outcome", "Matokeo ya ufuatiliaji");
        values.put("tbleprosy_followup_visit_client_found", "Amepatikana");
        values.put("tbleprosy_followup_visit_client_not_found", "Hajapatikana");
        values.put("tbleprosy_followup_visit_client_deceased", "Amefariki");
        values.put("tbleprosy_followup_visit_reason_client_not_found", "Taja sababu ya kutopatikana mteja");
        values.put("tbleprosy_followup_visit_returned_to_treatment", "Amerudi kwenye matibabu?");
        values.put("tbleprosy_followup_visit_service_access_challenges", "Je, kuna changamoto anazokumbana nazo katika upatikanaji wa huduma?");
        values.put("tbleprosy_followup_visit_reasons_for_not_returning_to_services_while_not_facing_challenges", "Taja sababu ya kutokuanza/kukatiza dawa");
        values.put("tbleprosy_followup_visit_client_challenge_types", "Chagua aina ya changamoto mteja aliyopata");
        values.put("tbleprosy_followup_visit_family_challenges", "Katika familia");
        values.put("tbleprosy_followup_visit_health_facility_challenges", "Katika kituo cha kutolea huduma za afya");
        values.put("tbleprosy_followup_visit_community_challenges", "Katika jamii");
        values.put("tbleprosy_followup_visit_other", "Changamoto nyinginezo");
        values.put("tbleprosy_followup_visit_health_facility_challenges_detail", "Chagua changamoto katika kituo cha kutolea huduma za afya");
        values.put("tbleprosy_followup_visit_verbal_abuse", "Kutukanwa/Kufokewa");
        values.put("tbleprosy_followup_visit_denied_information_on_condition", "Kunyimwa taarifa juu ya ugonjwa na matibabu");
        values.put("tbleprosy_followup_visit_neglected_or_delayed", "Kutelekezwa au kucheleweshwa");
        values.put("tbleprosy_followup_visit_limited_service_access", "Upatikanaji mdogo wa huduma");
        values.put("tbleprosy_followup_visit_discriminated", "Kudharauliwa");
        values.put("tbleprosy_followup_visit_medication_denied", "Kunyimwa dawa");
        values.put("tbleprosy_followup_visit_return_to_treatment_prompt", "Mshauri mteja kurudi kwenye matibabu");
        values.put("tbleprosy_followup_visit_yes", "Ndiyo");
        values.put("tbleprosy_followup_visit_no", "Hapana");
        values.put("tbleprosy_record_visit_visit_title", "Hudhurio la mteja wa Kifua Kikuu/Ukoma");
        values.put("tbleprosy_record_visit_has_sample_been_collected", "Je, mteja amechukuliwa sampuli?");
        values.put("tbleprosy_record_visit_sample_collection_date", "Tarehe ya kuchukuliwa sampuli");
        values.put("tbleprosy_record_visit_sputum_container_provided", "Je, mteja amepewa kontena la makohozi?");
        values.put("tbleprosy_record_visit_extra_sputum_container_required", "Je, mteja anahitaji kontena la ziada la makohozi?");
        values.put("tbleprosy_record_visit_yes", "Ndiyo");
        values.put("tbleprosy_record_visit_no", "Hapana");
        return values;
    }
}
