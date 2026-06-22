package org.smartregister.chw.activity;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.BaseUnitTest;

public class HarmReductionMatClientsVisitHistoryActivityTest extends BaseUnitTest {

    @Test
    public void visitHistoryParamsShouldIncludeMethadoneTreatmentStatus() {
        Assert.assertTrue(HarmReductionMatClientsVisitHistoryActivity.isVisitHistoryParam(
                HarmReductionMatClientsVisitHistoryActivity.METHADONE_TREATMENT_STATUS
        ));
    }
}
