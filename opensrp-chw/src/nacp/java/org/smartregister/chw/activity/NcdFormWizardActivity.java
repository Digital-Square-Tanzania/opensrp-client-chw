package org.smartregister.chw.activity;

import org.smartregister.chw.fragment.NcdJsonWizardFormFragment;
import org.smartregister.family.activity.FamilyWizardFormActivity;

public class NcdFormWizardActivity extends FamilyWizardFormActivity {

    @Override
    public void initializeFormFragment() {
        NcdJsonWizardFormFragment jsonWizardFormFragment = NcdJsonWizardFormFragment.getFormFragment("step1");
        getSupportFragmentManager().beginTransaction()
                .add(com.vijay.jsonwizard.R.id.container, jsonWizardFormFragment).commit();
    }

}
