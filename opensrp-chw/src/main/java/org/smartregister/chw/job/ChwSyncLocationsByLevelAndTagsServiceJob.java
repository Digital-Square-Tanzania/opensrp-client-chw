package org.smartregister.chw.job;

import android.content.Intent;

import com.evernote.android.job.Job;

import org.smartregister.chw.sync.intent.ChwSyncLocationsByLevelAndTagsIntentService;
import org.smartregister.job.BaseJob;
import org.smartregister.job.SyncLocationsByLevelAndTagsServiceJob;

public class ChwSyncLocationsByLevelAndTagsServiceJob extends BaseJob {

    private static final String TO_RESCHEDULE = "to_reschedule";

    @Override
    protected Job.Result onRunJob(Job.Params params) {
        Intent intent = new Intent(getApplicationContext(), ChwSyncLocationsByLevelAndTagsIntentService.class);
        getApplicationContext().startService(intent);

        if (params != null && params.getExtras().getBoolean(TO_RESCHEDULE, false)) {
            return Job.Result.RESCHEDULE;
        }

        return Job.Result.SUCCESS;
    }

    public static void scheduleJobImmediately() {
        SyncLocationsByLevelAndTagsServiceJob.scheduleJobImmediately(SyncLocationsByLevelAndTagsServiceJob.TAG);
    }
}
