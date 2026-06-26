package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.model.NcdReferralInputs;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

/**
 * Unit tests for the emergency-case / treatment-supporter obs emission added to the
 * auto-generated Referral Registration event by
 * {@link NcdReferralTaskHelper#addReferralInputObs(Event, NcdReferralInputs)}.
 */
public class NcdReferralTaskHelperTest {

    private static String obsValue(Event event, String fieldCode) {
        for (Obs obs : event.getObs()) {
            if (fieldCode.equals(obs.getFieldCode())) {
                return obs.getValue() == null ? null : String.valueOf(obs.getValue());
            }
        }
        return null;
    }

    private static boolean hasObs(Event event, String fieldCode) {
        for (Obs obs : event.getObs()) {
            if (fieldCode.equals(obs.getFieldCode())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void nullInputsEmitsNoObs() {
        Event event = new Event();
        NcdReferralTaskHelper.addReferralInputObs(event, null);
        Assert.assertTrue(event.getObs() == null || event.getObs().isEmpty());
    }

    @Test
    public void gateYesEmitsEmergencyAndAllSupporterDetails() {
        Event event = new Event();
        NcdReferralTaskHelper.addReferralInputObs(event,
                new NcdReferralInputs("Yes", "Yes", "Chausiku Kibange", "0788994488", "Friend"));

        Assert.assertEquals("Yes", obsValue(event, Constants.NcdReferral.IS_EMERGENCY_CASE));
        Assert.assertEquals("Yes", obsValue(event, Constants.NcdReferral.HAS_TREATMENT_SUPPORTER));
        Assert.assertEquals("Chausiku Kibange", obsValue(event, Constants.NcdReferral.TREATMENT_SUPPORTER_NAME));
        Assert.assertEquals("0788994488", obsValue(event, Constants.NcdReferral.TREATMENT_SUPPORTER_PHONE));
        Assert.assertEquals("Friend", obsValue(event, Constants.NcdReferral.TREATMENT_SUPPORTER_RELATIONSHIP));
    }

    @Test
    public void gateNoOmitsSupporterDetailsButKeepsGateAndEmergency() {
        Event event = new Event();
        NcdReferralTaskHelper.addReferralInputObs(event,
                new NcdReferralInputs("No", "No", "Someone", "0700000000", "Friend"));

        Assert.assertEquals("No", obsValue(event, Constants.NcdReferral.IS_EMERGENCY_CASE));
        Assert.assertEquals("No", obsValue(event, Constants.NcdReferral.HAS_TREATMENT_SUPPORTER));
        Assert.assertFalse(hasObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_NAME));
        Assert.assertFalse(hasObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_PHONE));
        Assert.assertFalse(hasObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_RELATIONSHIP));
    }

    @Test
    public void blankSupporterDetailsAreSkippedWhenGateYes() {
        Event event = new Event();
        NcdReferralTaskHelper.addReferralInputObs(event,
                new NcdReferralInputs("Yes", "Yes", "  ", null, ""));

        Assert.assertTrue(hasObs(event, Constants.NcdReferral.HAS_TREATMENT_SUPPORTER));
        Assert.assertFalse(hasObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_NAME));
        Assert.assertFalse(hasObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_PHONE));
        Assert.assertFalse(hasObs(event, Constants.NcdReferral.TREATMENT_SUPPORTER_RELATIONSHIP));
    }

    @Test
    public void valuesAreTrimmed() {
        Event event = new Event();
        NcdReferralTaskHelper.addReferralInputObs(event,
                new NcdReferralInputs("Yes", "Yes", "  Jane Doe  ", "  0788  ", "Mother"));

        Assert.assertEquals("Jane Doe", obsValue(event, Constants.NcdReferral.TREATMENT_SUPPORTER_NAME));
        Assert.assertEquals("0788", obsValue(event, Constants.NcdReferral.TREATMENT_SUPPORTER_PHONE));
    }
}
