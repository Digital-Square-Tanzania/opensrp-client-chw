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

    @Test
    public void facilityGettersReturnSuppliedValues() {
        NcdReferralInputs inputs = new NcdReferralInputs(
                "Yes", "No", null, null, null, "facility-123", "Sinza Hospital");

        Assert.assertEquals("facility-123", inputs.getReferralFacilityId());
        Assert.assertEquals("Sinza Hospital", inputs.getReferralFacilityName());
    }

    @Test
    public void facilityIsNullViaFiveArgConstructor() {
        NcdReferralInputs inputs = new NcdReferralInputs("Yes", "No", null, null, null);

        Assert.assertNull(inputs.getReferralFacilityId());
        Assert.assertNull(inputs.getReferralFacilityName());
    }
}
