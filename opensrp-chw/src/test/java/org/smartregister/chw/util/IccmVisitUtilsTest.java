package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;

public class IccmVisitUtilsTest {

    @Test
    public void shouldDetectMalariaPhysicalExaminationFindings() {
        Assert.assertTrue(IccmVisitUtils.hasMalariaPhysicalExaminationFinding("pallor"));
        Assert.assertTrue(IccmVisitUtils.hasMalariaPhysicalExaminationFinding("[\"enlarged_spleen\"]"));
        Assert.assertTrue(IccmVisitUtils.hasMalariaPhysicalExaminationFinding("[\"pallor\",\"enlarged_spleen\"]"));

        Assert.assertFalse(IccmVisitUtils.hasMalariaPhysicalExaminationFinding(""));
        Assert.assertFalse(IccmVisitUtils.hasMalariaPhysicalExaminationFinding("[]"));
        Assert.assertFalse(IccmVisitUtils.hasMalariaPhysicalExaminationFinding("none"));
        Assert.assertFalse(IccmVisitUtils.hasMalariaPhysicalExaminationFinding("[\"none\"]"));
    }

    @Test
    public void shouldPopulateMalariaActionWhenPhysicalExamSuggestsMalariaAndNoRecentTreatment() {
        Assert.assertTrue(IccmVisitUtils.shouldPopulateMalariaActionAfterPhysicalExamination(
                "false",
                "[\"pallor\"]",
                "false",
                "no"
        ));
        Assert.assertTrue(IccmVisitUtils.shouldPopulateMalariaActionAfterPhysicalExamination(
                "false",
                "[\"enlarged_spleen\"]",
                "",
                "no"
        ));
        Assert.assertTrue(IccmVisitUtils.shouldPopulateMalariaActionAfterPhysicalExamination(
                "false",
                "[\"pallor\",\"enlarged_spleen\"]",
                "",
                "no"
        ));
    }

    @Test
    public void shouldPopulateMalariaActionWhenCalculatedSuspectFlagIsTrue() {
        Assert.assertTrue(IccmVisitUtils.shouldPopulateMalariaActionAfterPhysicalExamination(
                "false",
                "",
                "true",
                "no"
        ));
        Assert.assertTrue(IccmVisitUtils.shouldPopulateMalariaActionAfterPhysicalExamination(
                "true",
                "[\"none\"]",
                "false",
                ""
        ));
    }

    @Test
    public void shouldNotPopulateMalariaActionWhenNoFindingOrRecentTreatmentExists() {
        Assert.assertFalse(IccmVisitUtils.shouldPopulateMalariaActionAfterPhysicalExamination(
                "false",
                "[\"none\"]",
                "false",
                "no"
        ));
        Assert.assertFalse(IccmVisitUtils.shouldPopulateMalariaActionAfterPhysicalExamination(
                "false",
                "[\"pallor\"]",
                "true",
                "yes"
        ));
        Assert.assertFalse(IccmVisitUtils.shouldPopulateMalariaActionAfterPhysicalExamination(
                "",
                "",
                "",
                ""
        ));
    }
}
