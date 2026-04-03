package org.smartregister.chw.resources;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

public class HarmReductionVisitHistoryStringTranslationsTest {

    private static final String SW_STRINGS_XML_PATH = "src/nacp/res/values-sw/strings.xml";

    @Test
    public void shouldContainExpectedSwahiliStringsForHrVisitHistory() throws Exception {
        Map<String, String> swahiliStrings = readStrings(SW_STRINGS_XML_PATH);
        Map<String, String> expectedStrings = expectedSwahiliStrings();

        for (Map.Entry<String, String> expectedEntry : expectedStrings.entrySet()) {
            Assert.assertEquals(
                    "Unexpected value for key: " + expectedEntry.getKey(),
                    expectedEntry.getValue(),
                    swahiliStrings.get(expectedEntry.getKey())
            );
        }
    }

    private Map<String, String> readStrings(String path) throws Exception {
        File stringsFile = new File(path);
        Assert.assertTrue("Missing strings.xml at " + path, stringsFile.exists());

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile);
        NodeList stringNodes = document.getElementsByTagName("string");
        Map<String, String> values = new LinkedHashMap<>();

        for (int i = 0; i < stringNodes.getLength(); i++) {
            Element element = (Element) stringNodes.item(i);
            values.put(element.getAttribute("name"), element.getTextContent().trim());
        }

        return values;
    }

    private Map<String, String> expectedSwahiliStrings() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("harm_reduction_health_education_given", "Je, elimu imetolewa?");
        values.put("harm_reduction_health_education_provided", "Elimu ya afya iliyotolewa");
        values.put("harm_reduction_health_education_other_specify", "Bainisha elimu nyingine ya afya iliyotolewa");
        values.put("harm_reduction_iec_materials_provided", "Je, vipeperushi vimetolewa?");
        values.put("harm_reduction_iec_materials_type", "Aina ya ujumbe katika vipeperushi vilivyotolewa");
        values.put("harm_reduction_iec_materials_other_specify", "Bainisha aina nyingine ya vipeperushi iliyotolewa");
        values.put("harm_reduction_referrals_provided", "Rufaa zilizotolewa");
        values.put("harm_reduction_referrals_other_specify", "Ikiwa ni nyinginezo, taja");
        values.put("harm_reduction_yes", "Ndiyo");
        values.put("harm_reduction_no", "Hapana");
        values.put("harm_reduction_hiv_aids", "VVU na UKIMWI");
        values.put("harm_reduction_epidemic_diseases", "Magonjwa ya mlipuko");
        values.put("harm_reduction_communicable_diseases", "Magonjwa ya kuambukiza");
        values.put("harm_reduction_tb_leprosy", "Kifua Kikuu/Ukoma");
        values.put("harm_reduction_ntds", "Magonjwa ya Kitropiki Yaliyopuuzwa");
        values.put("harm_reduction_family_planning", "Uzazi wa mpango");
        values.put("harm_reduction_reproductive_health", "Afya ya uzazi, baba, mama, mtoto na vijana");
        values.put("harm_reduction_nutrition", "Lishe");
        values.put("harm_reduction_prep", "PrEP");
        values.put("harm_reduction_mental_health", "Afya ya akili");
        values.put("harm_reduction_substance_abuse", "Matumizi ya dawa za kulevya");
        values.put("harm_reduction_gbv_vac", "Ukatili wa Kijinsia/Watoto");
        values.put("harm_reduction_psychological_assistance", "Msaada wa kisaikolojia");
        values.put("harm_reduction_rights_vulnerable", "Haki stahiki kwa makundi maalumu");
        values.put("harm_reduction_health_services_advocacy", "Uhamasishaji wa huduma za afya");
        values.put("harm_reduction_hepatitis_bc", "Homa ya Ini");
        values.put("harm_reduction_stis_stds", "Magonjwa ya Ngono na Via vya Uzazi");
        values.put("harm_reduction_overdose_management", "Kuzuia na kumudu Overdose");
        values.put("harm_reduction_legal_issues", "Masuala ya kisheria");
        values.put("harm_reduction_safe_injection", "Udungaji salama");
        values.put("harm_reduction_methadone_use", "Matumizi ya Methadone");
        values.put("harm_reduction_lowering_infection_risk", "Kupunguza hatari ya maambukizi");
        values.put("harm_reduction_social_services", "Huduma za kijamii");
        values.put("harm_reduction_income_generating", "Shughuli za kujikwamua kiuchumi (IGA)");
        values.put("harm_reduction_methadone_services", "Huduma ya Methadone (MAT)");
        values.put("harm_reduction_hiv_testing", "Upimaji wa VVU");
        values.put("harm_reduction_sober_house", "Nyumba ya Upataji Nafuu");
        values.put("harm_reduction_wounds_abscess_treatment", "Matibabu ya vidonda na mabunyufu (majipu)");
        values.put("harm_reduction_other", "Nyinginezo (Taja)");
        values.put("harm_reduction_none", "Hakuna");
        values.put("harm_reduction_iec_hiv_aids", "VVU na UKIMWI");
        values.put("harm_reduction_iec_epidemic_diseases", "Magonjwa ya mlipuko");
        values.put("harm_reduction_iec_communicable_diseases", "Magonjwa ya kuambukiza");
        values.put("harm_reduction_iec_tb_leprosy", "Kifua Kikuu/Ukoma");
        values.put("harm_reduction_iec_ntds", "Magonjwa ya Kitropiki Yaliyopuuzwa");
        values.put("harm_reduction_iec_family_planning", "Uzazi wa mpango");
        values.put("harm_reduction_iec_reproductive_health", "Afya ya uzazi, baba, mama, mtoto na vijana");
        values.put("harm_reduction_iec_nutrition", "Lishe");
        values.put("harm_reduction_iec_prep", "PrEP");
        values.put("harm_reduction_iec_mental_health", "Afya ya akili");
        values.put("harm_reduction_iec_substance_abuse", "Matumizi ya dawa za kulevya");
        values.put("harm_reduction_iec_gbv_vac", "Ukatili wa Kijinsia/Watoto");
        values.put("harm_reduction_iec_psychological_assistance", "Msaada wa kisaikolojia");
        values.put("harm_reduction_iec_rights_vulnerable", "Haki stahiki kwa makundi maalumu");
        values.put("harm_reduction_iec_health_services_advocacy", "Uhamasishaji wa huduma za afya");
        values.put("harm_reduction_iec_hepatitis_bc", "Homa ya Ini");
        values.put("harm_reduction_iec_stis_stds", "Magonjwa ya Ngono na Via vya Uzazi");
        values.put("harm_reduction_iec_overdose_management", "Kuzuia na kumudu Overdose");
        values.put("harm_reduction_iec_legal_issues", "Masuala ya kisheria");
        values.put("harm_reduction_iec_safe_injection", "Udungaji salama");
        values.put("harm_reduction_iec_methadone_use", "Matumizi ya Methadone");
        values.put("harm_reduction_iec_lowering_infection_risk", "Kupunguza hatari ya maambukizi");
        values.put("harm_reduction_iec_social_services", "Huduma za kijamii");
        values.put("harm_reduction_iec_income_generating", "Shughuli za kujikwamua kiuchumi (IGA)");
        values.put("harm_reduction_iec_other", "Nyinginezo (Taja)");
        return values;
    }
}
