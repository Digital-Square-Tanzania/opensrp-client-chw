package org.smartregister.chw.activity;

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidDirectEnrollment;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidHeiUi;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUi;
import org.smartregister.family.activity.FamilyWizardFormActivity;

import timber.log.Timber;

/** Keeps mother HEI controls in the existing family wizard and delegates domain UI to the library. */
public class MotherMentorVisitWizardActivity extends FamilyWizardFormActivity {
    private final MotherMentorMotherChildUi childUi = new MotherMentorMotherChildUi();
    private final MotherMentorChildEidHeiUi childEidHeiUi = new MotherMentorChildEidHeiUi();
    private boolean refreshingChild;

    @Override
    public void refreshCalculationLogic(String parentKey, String childKey, boolean popup,
            String stepName, boolean isForNextStep) {
        super.refreshCalculationLogic(parentKey, childKey, popup, stepName, isForNextStep);
        if (refreshingChild) return;
        refreshingChild = true;
        try {
            MotherMentorChildEidDirectEnrollment.refreshLastTestOptions(
                    this, getmJSONObject(), LocalDate.now());
            childUi.refresh(this);
            childEidHeiUi.refresh(this);
        } catch (JSONException e) {
            Timber.e(e, "Unable to refresh direct HEI enrollment test options");
        } finally {
            refreshingChild = false;
        }
    }
}
