package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.domain.Event;
import org.smartregister.util.JsonFormUtils;

public class NcdAutoConfirmationHelperTest {

    @Test
    public void diagnosedConditionsShouldTriggerAutoConfirmation() {
        Event event = eventWithAnswers("diagnosed_diabetes", "Yes",
                "diagnosed_hypertension", "Ndio");

        Assert.assertTrue(NcdAutoConfirmationHelper.isDiabetesPositive(event));
        Assert.assertTrue(NcdAutoConfirmationHelper.isHypertensionPositive(event));
    }

    @Test
    public void legacyMedicationAnswersShouldStillTriggerAutoConfirmation() {
        Event event = eventWithAnswers("medicines_diabetes", "Yes",
                "medicines_hypertension", "Yes");

        Assert.assertTrue(NcdAutoConfirmationHelper.isDiabetesPositive(event));
        Assert.assertTrue(NcdAutoConfirmationHelper.isHypertensionPositive(event));
    }

    @Test
    public void negativeDiagnosisShouldNotTriggerAutoConfirmation() {
        Event event = eventWithAnswers("diagnosed_diabetes", "No",
                "diagnosed_hypertension", "Hapana");

        Assert.assertFalse(NcdAutoConfirmationHelper.isDiabetesPositive(event));
        Assert.assertFalse(NcdAutoConfirmationHelper.isHypertensionPositive(event));
    }

    private static Event eventWithAnswers(String firstField, String firstValue,
                                          String secondField, String secondValue) {
        String json = "{\"obs\":["
                + observation(firstField, firstValue) + ","
                + observation(secondField, secondValue) + "]}";
        return JsonFormUtils.gson.fromJson(json, Event.class);
    }

    private static String observation(String field, String value) {
        return "{\"formSubmissionField\":\"" + field
                + "\",\"humanReadableValues\":[\"" + value
                + "\"],\"values\":[\"" + value + "\"]}";
    }
}
