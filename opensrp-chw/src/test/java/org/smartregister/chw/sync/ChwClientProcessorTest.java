package org.smartregister.chw.sync;

import org.junit.Assert;
import org.junit.Test;

public class ChwClientProcessorTest {

    @Test
    public void isCompletedMethadoneTreatmentReturnsTrueForCompletedValue() {
        Assert.assertTrue(ChwClientProcessor.isCompletedMethadoneTreatment("completed_methadone_treatment"));
    }

    @Test
    public void isCompletedMethadoneTreatmentHandlesSerializedArrayValue() {
        Assert.assertTrue(ChwClientProcessor.isCompletedMethadoneTreatment("[completed_methadone_treatment]"));
    }

    @Test
    public void isCompletedMethadoneTreatmentReturnsFalseForOtherStatuses() {
        Assert.assertFalse(ChwClientProcessor.isCompletedMethadoneTreatment("continuing_methadone_treatment"));
        Assert.assertFalse(ChwClientProcessor.isCompletedMethadoneTreatment("stopped_using_methadone"));
        Assert.assertFalse(ChwClientProcessor.isCompletedMethadoneTreatment("not_completed_methadone_treatment"));
        Assert.assertFalse(ChwClientProcessor.isCompletedMethadoneTreatment(""));
        Assert.assertFalse(ChwClientProcessor.isCompletedMethadoneTreatment(null));
    }
}
