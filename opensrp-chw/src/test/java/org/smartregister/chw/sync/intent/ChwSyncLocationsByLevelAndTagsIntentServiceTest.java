package org.smartregister.chw.sync.intent;

import org.junit.Assert;
import org.junit.Test;

public class ChwSyncLocationsByLevelAndTagsIntentServiceTest {

    @Test
    public void onHandleIntentNotifiesAfterSuccessfulLocationSync() {
        TestSyncLocationsByLevelAndTagsIntentService service = new TestSyncLocationsByLevelAndTagsIntentService(false);

        service.runSync();

        Assert.assertTrue(service.syncStarted);
        Assert.assertTrue(service.notified);
    }

    @Test
    public void onHandleIntentDoesNotNotifyWhenLocationSyncFails() {
        TestSyncLocationsByLevelAndTagsIntentService service = new TestSyncLocationsByLevelAndTagsIntentService(true);

        service.runSync();

        Assert.assertTrue(service.syncStarted);
        Assert.assertFalse(service.notified);
    }

    private static class TestSyncLocationsByLevelAndTagsIntentService extends ChwSyncLocationsByLevelAndTagsIntentService {
        private final boolean failSync;
        private boolean syncStarted;
        private boolean notified;

        private TestSyncLocationsByLevelAndTagsIntentService(boolean failSync) {
            this.failSync = failSync;
        }

        private void runSync() {
            onHandleIntent(null);
        }

        @Override
        protected void prepareSync(android.content.Intent intent) {
            // no-op for unit test
        }

        @Override
        protected void syncLocationsByLevelAndTags() throws Exception {
            syncStarted = true;
            if (failSync) {
                throw new Exception("Location sync failed");
            }
        }

        @Override
        protected void notifyLocationSyncComplete() {
            notified = true;
        }
    }
}
