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

public class HarmReductionTerminologySwahiliStringsTest {

    private static final String SW_STRINGS_XML_PATH = "src/nacp/res/values-sw/strings.xml";

    @Test
    public void shouldUseUpdatedClientTerminologyForHarmReductionStrings() throws Exception {
        Map<String, String> strings = readStrings(SW_STRINGS_XML_PATH);
        Map<String, String> expectedStrings = new LinkedHashMap<>();

        expectedStrings.put("harm_reduction_new_client_registration", "Usajili wa Mpokea Huduma Mpya");
        expectedStrings.put("harm_reduction_existing_client_registration", "Usajili wa Mpokea Huduma Aliyekuwepo");
        expectedStrings.put("harm_reduction_mat_clients", "Wapokea Huduma wa MAT");
        expectedStrings.put("record_services_provided_to_mat_clients", "Rekodi huduma zilizotolewa kwa wapokea huduma wa MAT");
        expectedStrings.put("harm_reduction_mat_clients_followup_visit", "Hudhurio la ufuatiliaji kwa wapokea huduma wa MAT");
        expectedStrings.put("harm_reduction_sober_house_client_current_status", "Hali ya sasa ya mpokea huduma");

        for (Map.Entry<String, String> expectedEntry : expectedStrings.entrySet()) {
            String actual = strings.get(expectedEntry.getKey());

            Assert.assertEquals("Unexpected value for key: " + expectedEntry.getKey(), expectedEntry.getValue(), actual);
            Assert.assertFalse("Found deprecated singular client term in key: " + expectedEntry.getKey(), actual.toLowerCase().contains("mteja"));
            Assert.assertFalse("Found deprecated plural client term in key: " + expectedEntry.getKey(), actual.toLowerCase().contains("wateja"));
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
}
