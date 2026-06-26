package org.smartregister.chw.model;

import org.junit.Assert;
import org.junit.Test;

public class NcdReferralInputsTest {

    @Test
    public void gettersReturnSuppliedValues() {
        NcdReferralInputs inputs = new NcdReferralInputs(
                "Yes", "Yes", "Chausiku Kibange", "0788994488", "Friend");

        Assert.assertEquals("Yes", inputs.getIsEmergencyCase());
        Assert.assertEquals("Yes", inputs.getHasTreatmentSupporter());
        Assert.assertEquals("Chausiku Kibange", inputs.getSupporterName());
        Assert.assertEquals("0788994488", inputs.getSupporterPhone());
        Assert.assertEquals("Friend", inputs.getSupporterRelationship());
    }

    @Test
    public void gateYesIsCaseInsensitive() {
        Assert.assertTrue(new NcdReferralInputs("No", "Yes", null, null, null)
                .isTreatmentSupporterGateYes());
        Assert.assertTrue(new NcdReferralInputs("No", "yes", null, null, null)
                .isTreatmentSupporterGateYes());
    }

    @Test
    public void gateNotYesWhenNoOrNull() {
        Assert.assertFalse(new NcdReferralInputs("Yes", "No", null, null, null)
                .isTreatmentSupporterGateYes());
        Assert.assertFalse(new NcdReferralInputs("Yes", null, null, null, null)
                .isTreatmentSupporterGateYes());
    }
}
