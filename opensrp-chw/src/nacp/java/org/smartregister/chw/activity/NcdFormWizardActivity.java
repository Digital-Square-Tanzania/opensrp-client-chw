package org.smartregister.chw.activity;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.smartregister.chw.fragment.NcdJsonWizardFormFragment;
import org.smartregister.chw.rules.ChwRulesEngineFactory;
import org.smartregister.family.activity.FamilyWizardFormActivity;

public class NcdFormWizardActivity extends FamilyWizardFormActivity {

    @Override
    public void initializeFormFragment() {
        NcdJsonWizardFormFragment jsonWizardFormFragment = NcdJsonWizardFormFragment.getFormFragment("step1");
        getSupportFragmentManager().beginTransaction()
                .add(com.vijay.jsonwizard.R.id.container, jsonWizardFormFragment).commit();
    }

    @Override
    public void init(String json) {
        super.init(json);
        rulesEngineFactory = new ChwRulesEngineFactory(this, globalValues);
        setRulesEngineFactory(rulesEngineFactory);

        confirmCloseTitle = getString(com.vijay.jsonwizard.R.string.confirm_form_close);
        confirmCloseMessage = getString(com.vijay.jsonwizard.R.string.confirm_form_close_explanation);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }
}
