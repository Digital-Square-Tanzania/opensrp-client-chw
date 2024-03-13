package org.smartregister.chw.sync;

import android.app.IntentService;
import android.content.Intent;

import org.smartregister.chw.asrh.dao.AsrhDao;
import org.smartregister.chw.asrh.domain.MemberObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by chriss on 2023-10-05.
 *
 * @author chrissdisigale https://github.com/ChrissDisigale
 */
public class CloseAsrhMembershipIntentService extends IntentService {

    private static final String TAG = CloseAsrhMembershipIntentService.class.getSimpleName();

    public CloseAsrhMembershipIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        List<MemberObject> memberObjects = AsrhDao.getMembers();

        try {
            for (MemberObject memberObject : memberObjects) {
                if (memberObject.getAge() > 24) {
                    AsrhDao.closeAsrhMemberFromRegister(memberObject.getBaseEntityId());
                }
            }
        }catch (Exception e){
            Timber.e(e);
        }
    }


}
