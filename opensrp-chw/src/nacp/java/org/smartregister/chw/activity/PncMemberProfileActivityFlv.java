package org.smartregister.chw.activity;

import android.view.Menu;

import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.MalariaFollowUpStatusTaskUtil;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.chw.util.UtilsFlv;
import org.smartregister.util.Utils;

public class PncMemberProfileActivityFlv implements PncMemberProfileActivity.Flavor {

    @Override
    public Boolean onCreateOptionsMenu(Menu menu, String baseEntityId) {
        menu.findItem(R.id.action_malaria_diagnosis).setVisible(false);
        menu.findItem(R.id.action_fp_initiation_pnc).setVisible(false);
        menu.findItem(R.id.action_fp_change).setVisible(false);
        if (MalariaDao.isRegisteredForMalaria(baseEntityId)) {
            Utils.startAsyncTask(new MalariaFollowUpStatusTaskUtil(menu, baseEntityId), null);
        } else {
            menu.findItem(org.smartregister.chw.core.R.id.action_malaria_registration).setVisible(true);
        }
        UtilsFlv.updateHivMenuItems(baseEntityId, menu);
        return true;
    }
}
