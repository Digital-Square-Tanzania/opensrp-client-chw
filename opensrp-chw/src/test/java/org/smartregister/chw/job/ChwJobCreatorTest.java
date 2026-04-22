package org.smartregister.chw.job;

import com.evernote.android.job.Job;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.job.SyncLocationsByLevelAndTagsServiceJob;

public class ChwJobCreatorTest {

    @Test
    public void createReturnsChwLocationSyncJobForLocationSyncTag() {
        Job job = new ChwJobCreator().create(SyncLocationsByLevelAndTagsServiceJob.TAG);

        Assert.assertTrue(job instanceof ChwSyncLocationsByLevelAndTagsServiceJob);
    }
}
