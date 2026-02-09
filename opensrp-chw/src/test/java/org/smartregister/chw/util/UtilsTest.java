package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.clientandeventmodel.Event;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.smartregister.chw.util.Utils.formatDateForVisual;
import static org.smartregister.chw.util.Utils.getClientName;

public class UtilsTest extends BaseUnitTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void firstCharacterUppercase_empty() {
        Assert.assertEquals("", Utils.firstCharacterUppercase(""));
    }

    @Test
    public void firstCharacterUppercase_with_one_character() {
        Assert.assertEquals("A", Utils.firstCharacterUppercase("a"));
    }

    @Test
    public void firstCharacterUppercase_with_two_word() {
        Assert.assertEquals("A b", Utils.firstCharacterUppercase("a b"));
    }


    @Test
    public void testFormatDateForVisual() {
        String date = "2020-06-23";
        String inputFormat = "yyyy-MM-dd";
        String formattedDate = formatDateForVisual(date, inputFormat);
        Assert.assertEquals(formattedDate, "23 Jun 2020");
    }

    @Test
    public void testGetClientName() {
        String name = getClientName("first_name", "middle_name", "last_name");
        if (ChwApplication.getApplicationFlavor().hasSurname())
            Assert.assertEquals("first_name middle_name last_name", name);
        else
            Assert.assertEquals("first_name middle_name", name);

    }

    @Test
    public void testUpdateFamilyRelationship_preservesOtherRelationships() {
        org.smartregister.clientandeventmodel.Client client = new TestClient();
        Map<String, java.util.List<String>> relationships = new HashMap<>();
        relationships.put("mother", Arrays.asList("mother-id"));
        relationships.put("family", Arrays.asList("old-family-id"));
        client.setRelationships(relationships);

        boolean updated = org.smartregister.chw.util.Utils.updateFamilyRelationship(client, "new-family-id");

        Assert.assertTrue(updated);
        Assert.assertEquals("new-family-id", client.getRelationships().get("family").get(0));
        Assert.assertEquals("mother-id", client.getRelationships().get("mother").get(0));
    }

    @Test
    public void testUpdateFamilyRelationship_createsRelationshipsMapWhenMissing() {
        org.smartregister.clientandeventmodel.Client client = new TestClient();

        boolean updated = org.smartregister.chw.util.Utils.updateFamilyRelationship(client, "new-family-id");

        Assert.assertTrue(updated);
        Assert.assertEquals("new-family-id", client.getRelationships().get("family").get(0));
    }

    @Test
    public void testUpdateFamilyRelationship_returnsFalseForInvalidInputs() {
        org.smartregister.clientandeventmodel.Client client = new TestClient();

        Assert.assertFalse(org.smartregister.chw.util.Utils.updateFamilyRelationship(null, "family-id"));
        Assert.assertFalse(org.smartregister.chw.util.Utils.updateFamilyRelationship(client, ""));
        Assert.assertFalse(org.smartregister.chw.util.Utils.updateFamilyRelationship(client, null));
    }

    @Test
    public void testExtractFormSubmissionIds_filtersInvalidValues() {
        Event validEvent = new Event();
        validEvent.setFormSubmissionId("form-id-1");

        Event emptyIdEvent = new Event();
        emptyIdEvent.setFormSubmissionId("");

        List<String> formSubmissionIds = org.smartregister.chw.util.Utils.extractFormSubmissionIds(
                Arrays.asList(validEvent, emptyIdEvent, null)
        );

        Assert.assertEquals(1, formSubmissionIds.size());
        Assert.assertEquals("form-id-1", formSubmissionIds.get(0));
    }

    @Test
    public void testExtractFormSubmissionIds_returnsEmptyForNullAndEmptyLists() {
        Assert.assertTrue(org.smartregister.chw.util.Utils.extractFormSubmissionIds(null).isEmpty());
        Assert.assertTrue(org.smartregister.chw.util.Utils.extractFormSubmissionIds(Collections.emptyList()).isEmpty());
    }

    private static class TestClient extends org.smartregister.clientandeventmodel.Client {
        public TestClient() {
            super();
        }
    }
}
