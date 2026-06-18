package org.smartregister.chw.activity;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.smartregister.chw.fragment.AncJsonWizardFormFragment;
import org.smartregister.family.activity.FamilyWizardFormActivity;

public class AncJsonWizardFormActivity extends FamilyWizardFormActivity {

    @Override
    public synchronized void initializeFormFragment() {
        AncJsonWizardFormFragment formFragment = AncJsonWizardFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, formFragment)
                .commit();
    }
}
