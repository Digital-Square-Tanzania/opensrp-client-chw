package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.smartregister.chw.core.activity.CoreTbLeprosyRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.TbLeprosyContactRegisterFragment;
import org.smartregister.chw.fragment.TbLeprosyMobilizationFragment;
import org.smartregister.chw.fragment.TbLeprosyRegisterFragment;
import org.smartregister.chw.tbleprosy.util.Constants;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class TbLeprosyRegisterActivity extends CoreTbLeprosyRegisterActivity {
    public static void startRegistration(Activity activity, String memberBaseEntityID) {
        Intent intent = new Intent(activity, TbLeprosyRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.TB_LEPROSY_FORM_NAME, Constants.FORMS.TB_LEPROSY_SCREENING);

        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new TbLeprosyRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{
                new TbLeprosyContactRegisterFragment(),
                new TbLeprosyMobilizationFragment()
        };
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.TBLEPROSY);
        }
    }
}
