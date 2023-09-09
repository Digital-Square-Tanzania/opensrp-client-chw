package org.smartregister.chw.activity;

import org.smartregister.chw.core.activity.BaseReferralRegister;
import org.smartregister.chw.core.presenter.BaseReferralPresenter;
import org.smartregister.chw.fragment.LTFUReferralsRegisterFragment;
import org.smartregister.family.R;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class LTFURegisterActivity extends BaseReferralRegister {
    @Override
    protected void initializePresenter() {
        presenter = new BaseReferralPresenter();
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new LTFUReferralsRegisterFragment();
    }


    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);
        FamilyRegisterActivity.registerBottomNavigation(bottomNavigationHelper, bottomNavigationView, this);
        bottomNavigationView.getMenu().removeItem(R.id.action_register);
    }
}
