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

public class HarmReductionMatVisitHistoryStringTranslationsTest {

    private static final String DEFAULT_STRINGS_XML_PATH = "src/nacp/res/values/strings.xml";
    private static final String SW_STRINGS_XML_PATH = "src/nacp/res/values-sw/strings.xml";

    @Test
    public void shouldContainExpectedEnglishStringsForMatVisitHistoryOptions() throws Exception {
        Map<String, String> defaultStrings = readStrings(DEFAULT_STRINGS_XML_PATH);
        Map<String, String> expectedStrings = expectedEnglishStrings();

        for (Map.Entry<String, String> expectedEntry : expectedStrings.entrySet()) {
            Assert.assertEquals(
                    "Unexpected value for key: " + expectedEntry.getKey(),
                    expectedEntry.getValue(),
                    defaultStrings.get(expectedEntry.getKey())
            );
        }
    }

    @Test
    public void shouldContainExpectedSwahiliStringsForMatVisitHistoryOptions() throws Exception {
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

    private Map<String, String> expectedEnglishStrings() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("harm_reduction_mat_clients_drug_education", "Drug education");
        values.put("harm_reduction_mat_clients_drug_use_related_diseases", "Diseases associated with drug use");
        values.put("harm_reduction_mat_clients_epidemic_diseases", "Epidemic diseases");
        values.put("harm_reduction_mat_clients_treatment_adherence", "Treatment adherence");
        values.put("harm_reduction_mat_clients_life_and_job_skills", "Life and job skills");
        return values;
    }

    private Map<String, String> expectedSwahiliStrings() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("harm_reduction_mat_clients_drug_education", "Dawa za kulevya");
        values.put("harm_reduction_mat_clients_drug_use_related_diseases", "Magonjwa yanayoambatana na matumizi ya dawa za kulevya");
        values.put("harm_reduction_mat_clients_epidemic_diseases", "Magonjwa ya mlipuko");
        values.put("harm_reduction_mat_clients_treatment_adherence", "Ufuasi mzuri wa tiba");
        values.put("harm_reduction_mat_clients_life_and_job_skills", "Stadi za maisha na stadi za kazi");
        return values;
    }
}
