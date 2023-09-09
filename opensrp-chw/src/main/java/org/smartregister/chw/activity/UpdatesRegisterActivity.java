package org.smartregister.chw.activity;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;
import org.smartregister.R;
import org.smartregister.chw.core.activity.BaseChwNotificationRegister;
import org.smartregister.chw.core.presenter.BaseChwNotificationPresenter;
import org.smartregister.chw.fragment.UpdatesRegisterFragment;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.List;

public class UpdatesRegisterActivity extends BaseChwNotificationRegister {

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void initializePresenter() {
        presenter = new BaseChwNotificationPresenter();
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new UpdatesRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[0];
    }

    @Override
    public void startFormActivity(String formName, String entityId, String metaData) {
        //Overridden not needed
    }

    @Override
    public void startFormActivity(JSONObject form) {
        //Overridden not needed
    }

    @Override
    protected void onActivityResultExtended(int requestCode, int resultCode, Intent data) {
        //Overridden not needed
    }

    @Override
    public List<String> getViewIdentifiers() {
        return null;
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        FamilyRegisterActivity.registerBottomNavigation(bottomNavigationHelper, bottomNavigationView, this);
        bottomNavigationView.getMenu().removeItem(R.id.action_register);
    }

    @Override
    public void startRegistration() {
        //Overridden not needed
    }
}
