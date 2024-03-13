package org.smartregister.chw.hf.sync.intent;

import android.app.IntentService;
import android.content.Intent;

import org.smartregister.chw.vmmc.dao.VmmcDao;
import org.smartregister.chw.vmmc.domain.MemberObject;
import org.smartregister.chw.vmmc.util.VmmcUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by chriss on 2023-10-05.
 *
 * @author chrissdisigale https://github.com/ChrissDisigale
 */
public class CloseVmmcMembershipIntentService extends IntentService {

    private static final String TAG = CloseVmmcMembershipIntentService.class.getSimpleName();

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public CloseVmmcMembershipIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        List<MemberObject> memberObjects = VmmcDao.getMembers();

        for (MemberObject memberObject : memberObjects) {
            String enrollmentDate = memberObject.getEnrollmentDate();
            Calendar expiredCalendar = Calendar.getInstance();

            if (enrollmentDate != null && !enrollmentDate.isEmpty()) {

                Date date = null;
                try {
                    date = simpleDateFormat.parse(enrollmentDate);
                } catch (ParseException e) {
                    Timber.e(e);
                }

                if (date != null) {
                    expiredCalendar.setTime(date);
                    expiredCalendar.add(Calendar.DAY_OF_MONTH, 30);
                    if (checkIfExpired(expiredCalendar)) {
                        VmmcUtil.closeVmmcService(memberObject.getBaseEntityId());
                    }
                }

            }

        }
    }

    public boolean checkIfExpired(Calendar expiredCalendar) {
        return Calendar.getInstance().getTime().after(expiredCalendar.getTime());
    }

}
