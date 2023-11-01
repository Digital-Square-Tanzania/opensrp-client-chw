package org.smartregister.chw.listener;

import android.app.Activity;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.MotherChampionRegisterActivity;
import org.smartregister.chw.pmtct.listener.PmtctBottomNavigationListener;

public class ChwMotherChampionBottomNavigationListener extends PmtctBottomNavigationListener {

    private final MotherChampionRegisterActivity baseRegisterActivity;

    public ChwMotherChampionBottomNavigationListener(Activity context) {
        super(context);
        this.baseRegisterActivity = (MotherChampionRegisterActivity) context;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_home) {
            baseRegisterActivity.switchToFragment(0);
            return true;
        } else if (item.getItemId() == R.id.action_received_referrals) {
            baseRegisterActivity.switchToFragment(1);
            return true;
        } else if (item.getItemId() == R.id.action_followup) {
            baseRegisterActivity.switchToFragment(2);
            return true;
        } else if (item.getItemId() == R.id.action_sbcc) {
//            JSONObject form;
//            try {
//                form = (new FormUtils()).getFormJsonFromRepositoryOrAssets(baseRegisterActivity, org.smartregister.chw.util.Constants.JsonForm.getMotherChampionSbccForm());
//                if (form != null) {
//                    String randomId = generateRandomUUIDString();
//                    form.put(ENTITY_ID,randomId);
//                    baseRegisterActivity.startActivityForResult(org.smartregister.chw.core.utils.FormUtils.getStartFormActivity(form, baseRegisterActivity.getString(R.string.sbcc), baseRegisterActivity), JsonFormUtils.REQUEST_CODE_GET_JSON);
//                }
//            } catch (JSONException e) {
//                Timber.e(e);
//            }
            baseRegisterActivity.switchToFragment(3);
            return true;
        } else
            return super.onNavigationItemSelected(item);
    }
}
