package org.smartregister.chw.job;

import android.content.Intent;

import com.evernote.android.job.Job;

import org.smartregister.chw.core.job.HomeVisitServiceJob;
import org.smartregister.chw.intent.ChwHomeVisitIntentService;

import timber.log.Timber;

public class ChwHomeVisitServiceJob extends HomeVisitServiceJob {

    @Override
    protected Job.Result onRunJob(Job.Params params) {
        Timber.v("%s started", "ChwHomeVisitServiceJob");
        getApplicationContext().startService(new Intent(getApplicationContext(), ChwHomeVisitIntentService.class));
        return params.getExtras().getBoolean("to_reschedule", false) ? Job.Result.RESCHEDULE : Job.Result.SUCCESS;
    }
}
