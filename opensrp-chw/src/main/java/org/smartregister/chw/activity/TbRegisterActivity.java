package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.jetbrains.annotations.NotNull;
import org.smartregister.R;
import org.smartregister.chw.core.activity.CoreTbRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.fragment.TbFollowupRegisterFragment;
import org.smartregister.chw.fragment.TbRegisterFragment;
import org.smartregister.chw.tb.fragment.BaseTbCommunityFollowupRegisterFragment;
import org.smartregister.chw.tb.fragment.BaseTbRegisterFragment;
import org.smartregister.chw.tb.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.listener.BottomNavigationListener;

public class TbRegisterActivity extends CoreTbRegisterActivity {

    public static void startTbFormActivity(Activity activity, String baseEntityID, String formName, String payloadType) {
        Intent intent = new Intent(activity, TbRegisterActivity.class);
        intent.putExtra(Constants.ActivityPayload.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ActivityPayload.ACTION, payloadType);
        intent.putExtra(Constants.ActivityPayload.TB_REGISTRATION_FORM_NAME, formName);
        activity.startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @NotNull
    @Override
    protected BaseTbRegisterFragment getRegisterFragment() {
        return new TbRegisterFragment();
    }

    @NotNull
    @Override
    protected BaseTbCommunityFollowupRegisterFragment[] getOtherFragments() {
        return new TbFollowupRegisterFragment[]{
                new TbFollowupRegisterFragment()};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavigationMenu.getInstance(this, null, null);
    }

    @Override
    protected void registerBottomNavigation() {
        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.getMenu().removeItem(R.id.action_clients);
            bottomNavigationView.getMenu().removeItem(org.smartregister.chw.tb.R.id.action_register);
            bottomNavigationView.getMenu().removeItem(R.id.action_search);
            bottomNavigationView.getMenu().removeItem(R.id.action_library);

            bottomNavigationView.inflateMenu(getMenuResource());
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);

            BottomNavigationListener tbBottomNavigationListener = getBottomNavigation(this);
            bottomNavigationView.setOnNavigationItemSelectedListener(tbBottomNavigationListener);

        }
    }

}
 