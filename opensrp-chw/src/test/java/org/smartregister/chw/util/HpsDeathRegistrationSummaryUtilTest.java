package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class HpsDeathRegistrationSummaryUtilTest {

    @Test
    public void getDisplayValuesShouldShowSpecifiedCauseWhenOtherWasSelected() {
        LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
        visitDetails.put("dod", "2026-04-17");
        visitDetails.put("cause_of_death", "other");
        visitDetails.put("cause_of_death_specify", "kuumwa");
        visitDetails.put("comments", "comment");

        Map<String, String> displayValues = HpsDeathRegistrationSummaryUtil.getDisplayValues(visitDetails, "Sababu nyinginezo");

        Assert.assertEquals("kuumwa", displayValues.get("cause_of_death"));
        Assert.assertFalse(displayValues.containsKey("cause_of_death_specify"));

        Iterator<String> keys = displayValues.keySet().iterator();
        Assert.assertEquals("dod", keys.next());
        Assert.assertEquals("cause_of_death", keys.next());
        Assert.assertEquals("comments", keys.next());
    }

    @Test
    public void getDisplayValuesShouldFallbackToOtherLabelWhenSpecifyIsBlank() {
        LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
        visitDetails.put("cause_of_death", "other");
        visitDetails.put("cause_of_death_specify", " ");

        Map<String, String> displayValues = HpsDeathRegistrationSummaryUtil.getDisplayValues(visitDetails, "Sababu nyinginezo");

        Assert.assertEquals("Sababu nyinginezo", displayValues.get("cause_of_death"));
        Assert.assertFalse(displayValues.containsKey("cause_of_death_specify"));
    }

    @Test
    public void getDisplayValuesShouldKeepNonOtherCauseUntouched() {
        LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
        visitDetails.put("cause_of_death", "accidents_injuries");
        visitDetails.put("cause_of_death_specify", "legacy value");

        Map<String, String> displayValues = HpsDeathRegistrationSummaryUtil.getDisplayValues(visitDetails, "Sababu nyinginezo");

        Assert.assertEquals("accidents_injuries", displayValues.get("cause_of_death"));
        Assert.assertFalse(displayValues.containsKey("cause_of_death_specify"));
    }
}
