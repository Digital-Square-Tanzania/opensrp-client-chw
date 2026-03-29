package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;

public class IccmReferralActionUtilsTest {

    @Test
    public void shouldKeepReferralFromDiarrheaWhenAnyTriggerRemainsTrue() {
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromDiarrhea("false", "yes", "none"));
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromDiarrhea("true", "no", "none"));
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromDiarrhea("false", "no", "dehydration"));
    }

    @Test
    public void shouldNotKeepReferralFromDiarrheaWhenAllTriggersAreFalse() {
        Assert.assertFalse(IccmReferralActionUtils.shouldKeepReferralFromDiarrhea("false", "no", "none"));
        Assert.assertFalse(IccmReferralActionUtils.shouldKeepReferralFromDiarrhea("", "", ""));
    }

    @Test
    public void shouldKeepReferralFromMalariaWhenAnyTriggerRemainsTrue() {
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromMalaria("true", "control", "none", "yes", "no"));
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromMalaria("false", "parasites_detected", "none", "yes", "no"));
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromMalaria("false", "control", "bloody_stool", "yes", "no"));
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromMalaria("false", "control", "none", "no", "no"));
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromMalaria("false", "control", "none", "yes", "yes"));
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromMalaria("false", "control", "none", "no", "yes"));
    }

    @Test
    public void shouldNotKeepReferralFromMalariaWhenAllTriggersAreFalse() {
        Assert.assertFalse(IccmReferralActionUtils.shouldKeepReferralFromMalaria("false", "control", "none", "yes", "no"));
        Assert.assertFalse(IccmReferralActionUtils.shouldKeepReferralFromMalaria("", "", "", "", ""));
        Assert.assertFalse(IccmReferralActionUtils.shouldKeepReferralFromMalaria("false", "control", "none", "", "yes"));
    }

    @Test
    public void shouldKeepReferralFromPhysicalExaminationWhenAnyTriggerRemainsTrue() {
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromPhysicalExamination(
                7,
                Constants.PneumoniaStatus.DISABLED,
                "false",
                "yes"
        ));
        Assert.assertTrue(IccmReferralActionUtils.shouldKeepReferralFromPhysicalExamination(
                5,
                Constants.PneumoniaStatus.DISABLED,
                "true",
                "no"
        ));
    }

    @Test
    public void shouldNotKeepReferralFromPhysicalExaminationWhenAllTriggersAreFalse() {
        Assert.assertFalse(IccmReferralActionUtils.shouldKeepReferralFromPhysicalExamination(
                5,
                Constants.PneumoniaStatus.DISABLED,
                "false",
                "no"
        ));
        Assert.assertFalse(IccmReferralActionUtils.shouldKeepReferralFromPhysicalExamination(
                5,
                Constants.PneumoniaStatus.ENABLED,
                "true",
                "no"
        ));
    }
}
