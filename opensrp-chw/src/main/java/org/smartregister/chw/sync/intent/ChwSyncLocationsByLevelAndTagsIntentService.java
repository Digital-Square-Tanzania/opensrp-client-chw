package org.smartregister.chw.sync.intent;

import android.content.Intent;

import org.greenrobot.eventbus.EventBus;
import org.smartregister.chw.event.LocationSyncCompleteEvent;
import org.smartregister.sync.helper.LocationServiceHelper;
import org.smartregister.sync.intent.BaseSyncIntentService;

import timber.log.Timber;

public class ChwSyncLocationsByLevelAndTagsIntentService extends BaseSyncIntentService {

    public ChwSyncLocationsByLevelAndTagsIntentService() {
        super("ChwSyncLocationsByLevelAndTagsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            prepareSync(intent);
            syncLocationsByLevelAndTags();
            notifyLocationSyncComplete();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void prepareSync(Intent intent) {
        super.onHandleIntent(intent);
    }

    protected void syncLocationsByLevelAndTags() throws Exception {
        LocationServiceHelper.getInstance().fetchLocationsByLevelAndTags();
    }

    protected void notifyLocationSyncComplete() {
        EventBus.getDefault().post(new LocationSyncCompleteEvent());
    }
}
