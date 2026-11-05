package org.smartregister.chw.activity;

import org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUi;
import org.smartregister.family.activity.FamilyWizardFormActivity;

/** Keeps mother HEI controls in the existing family wizard and delegates domain UI to the library. */
public class MotherMentorVisitWizardActivity extends FamilyWizardFormActivity {
    private final MotherMentorMotherChildUi childUi = new MotherMentorMotherChildUi();
    private boolean refreshingChild;

    @Override
    public void refreshCalculationLogic(String parentKey, String childKey, boolean popup,
            String stepName, boolean isForNextStep) {
        super.refreshCalculationLogic(parentKey, childKey, popup, stepName, isForNextStep);
        if (refreshingChild) return;
        refreshingChild = true;
        try { childUi.refresh(this); } finally { refreshingChild = false; }
    }
}
