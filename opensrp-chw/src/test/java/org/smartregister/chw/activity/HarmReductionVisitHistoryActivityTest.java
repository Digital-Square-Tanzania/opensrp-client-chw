package org.smartregister.chw.activity;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.BaseUnitTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HarmReductionVisitHistoryActivityTest extends BaseUnitTest {

    @Test
    public void parseHistoryValuesShouldHandleSingleBracketedValue() {
        List<String> values = HarmReductionVisitHistoryActivity.parseHistoryValues("[heroine]");

        Assert.assertEquals(1, values.size());
        Assert.assertEquals("heroine", values.get(0));
    }

    @Test
    public void parseHistoryValuesShouldHandleMultipleBracketedValues() {
        List<String> values = HarmReductionVisitHistoryActivity.parseHistoryValues("[heroine, cocaine]");

        Assert.assertEquals(2, values.size());
        Assert.assertEquals("heroine", values.get(0));
        Assert.assertEquals("cocaine", values.get(1));
    }

    @Test
    public void parseHistoryValuesShouldFlattenCommaSeparatedBracketedCheckboxGroups() {
        List<String> values = HarmReductionVisitHistoryActivity.parseHistoryValues("[heroine],[],[],[],[]");

        Assert.assertEquals(1, values.size());
        Assert.assertEquals("heroine", values.get(0));
    }

    @Test
    public void parseHistoryValuesShouldHandleMultipleUnbracketedIdentifierValues() {
        List<String> values = HarmReductionVisitHistoryActivity.parseHistoryValues("hiv_aids, epidemic_diseases, communicable_diseases");

        Assert.assertEquals(3, values.size());
        Assert.assertEquals("hiv_aids", values.get(0));
        Assert.assertEquals("epidemic_diseases", values.get(1));
        Assert.assertEquals("communicable_diseases", values.get(2));
    }

    @Test
    public void parseHistoryValuesShouldKeepPlainTextWithCommasAsSingleValue() {
        List<String> values = HarmReductionVisitHistoryActivity.parseHistoryValues("7. Reproductive health, father, mother, child and adolescents");

        Assert.assertEquals(1, values.size());
        Assert.assertEquals("Reproductive health, father, mother, child and adolescents", values.get(0));
    }

    @Test
    public void parseHistoryValuesShouldKeepBracketedTextWithCommasAsSingleValue() {
        List<String> values = HarmReductionVisitHistoryActivity.parseHistoryValues("[7. Reproductive health, father, mother, child and adolescents]");

        Assert.assertEquals(1, values.size());
        Assert.assertEquals("Reproductive health, father, mother, child and adolescents", values.get(0));
    }

    @Test
    public void shouldSkipHiddenAggregateFieldShouldSkipSubstancesUsedWhenSpecificFieldExists() {
        Map<String, String> values = new HashMap<>();
        values.put("substances_used_injecting_only", "[heroine]");
        values.put(HarmReductionVisitHistoryActivity.SUBSTANCES_USED, "[heroine]");

        Assert.assertTrue(HarmReductionVisitHistoryActivity.shouldSkipHiddenAggregateField(values, HarmReductionVisitHistoryActivity.SUBSTANCES_USED));
    }

    @Test
    public void shouldSkipHiddenAggregateFieldShouldSkipRiskyBehavioursWhenSpecificFieldExists() {
        Map<String, String> values = new HashMap<>();
        values.put("risky_behaviours_injecting_only", "[sharing_needles_syringes]");
        values.put(HarmReductionVisitHistoryActivity.RISKY_BEHAVIOURS, "[sharing_needles_syringes]");

        Assert.assertTrue(HarmReductionVisitHistoryActivity.shouldSkipHiddenAggregateField(values, HarmReductionVisitHistoryActivity.RISKY_BEHAVIOURS));
    }

    @Test
    public void shouldSkipHiddenAggregateFieldShouldKeepAggregateFieldWhenNoSpecificFieldExists() {
        Map<String, String> values = new HashMap<>();
        values.put(HarmReductionVisitHistoryActivity.SUBSTANCES_USED, "[heroine]");

        Assert.assertFalse(HarmReductionVisitHistoryActivity.shouldSkipHiddenAggregateField(values, HarmReductionVisitHistoryActivity.SUBSTANCES_USED));
    }
}
