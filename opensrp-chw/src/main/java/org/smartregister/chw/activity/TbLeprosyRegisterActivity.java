package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreTbLeprosyRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.TbLeprosyContactRegisterFragment;
import org.smartregister.chw.fragment.TbLeprosyMobilizationFragment;
import org.smartregister.chw.fragment.TbLeprosyRegisterFragment;
import org.smartregister.chw.tbleprosy.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != Constants.REQUEST_CODE_GET_JSON || resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        try {
            String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
            if (StringUtils.isBlank(jsonString)) {
                return;
            }

            JSONObject form = new JSONObject(jsonString);
            String encounterType = form.optString(JsonFormUtils.ENCOUNTER_TYPE);
            if (!StringUtils.equals(encounterType, Constants.EVENT_TYPE.TB_LEPROSY_MOBILIZATION)) {
                return;
            }

            String sessionId = form.optString(org.smartregister.util.JsonFormUtils.ENTITY_ID);
            if (StringUtils.isBlank(sessionId)) {
                sessionId = form.optString("entity_id");
            }

            if (StringUtils.isNotBlank(sessionId)) {
                TbLeprosyMobilizationDetailsActivity.startMe(this, sessionId);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
