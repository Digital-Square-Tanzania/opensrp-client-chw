package org.smartregister.chw.job;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.job.ChwIndicatorGeneratingJob;
import org.smartregister.chw.core.job.HomeVisitServiceJob;
import org.smartregister.chw.core.job.StockUsageReportJob;
import org.smartregister.chw.core.job.SyncTaskWithClientEventsServiceJob;
import org.smartregister.chw.core.job.VaccineRecurringServiceJob;
import org.smartregister.chw.core.sync.intent.SyncClientEventsPerTaskIntentService;
import org.smartregister.chw.sync.ChwSyncIntentService;
import org.smartregister.chw.sync.intent.ChwSyncTaskIntentService;
import org.smartregister.job.DocumentConfigurationServiceJob;
import org.smartregister.job.ExtendedSyncServiceJob;
import org.smartregister.job.ImageUploadServiceJob;
import org.smartregister.job.P2pServiceJob;
import org.smartregister.job.PullUniqueIdsServiceJob;
import org.smartregister.job.SyncLocationsByLevelAndTagsServiceJob;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.job.SyncTaskServiceJob;
import org.smartregister.job.ValidateSyncDataServiceJob;
import org.smartregister.sync.intent.DocumentConfigurationIntentService;

import timber.log.Timber;

/**
 * Created by keyman on 27/11/2018.
 */
public class ChwJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case SyncServiceJob.TAG:
                return new SyncServiceJob(ChwSyncIntentService.class);
            case ExtendedSyncServiceJob.TAG:
                return new ExtendedSyncServiceJob();
            case PullUniqueIdsServiceJob.TAG:
                return new PullUniqueIdsServiceJob();
            case ValidateSyncDataServiceJob.TAG:
                return new ValidateSyncDataServiceJob();
            case VaccineRecurringServiceJob.TAG:
                return new VaccineRecurringServiceJob();
            case ImageUploadServiceJob.TAG:
                return new ImageUploadServiceJob();
            case P2pServiceJob.TAG:
                return new P2pServiceJob();
            case HomeVisitServiceJob.TAG:
                return new HomeVisitServiceJob();
            case ChwIndicatorGeneratingJob.TAG:
                return new ChwIndicatorGeneratingJob();
            case BasePncCloseJob.TAG:
                return new BasePncCloseJob();
            case SyncTaskServiceJob.TAG:
                return (ChwApplication.getApplicationFlavor().hasTasks()) ? new SyncTaskServiceJob(ChwSyncTaskIntentService.class) : null;
            case ScheduleJob.TAG:
                return new ScheduleJob();
            case SyncLocationsByLevelAndTagsServiceJob.TAG:
                return new SyncLocationsByLevelAndTagsServiceJob();
            case StockUsageReportJob.TAG:
                return new StockUsageReportJob();
            case SyncTaskWithClientEventsServiceJob.TAG:
                return new SyncTaskWithClientEventsServiceJob(SyncClientEventsPerTaskIntentService.class);
            case DocumentConfigurationServiceJob.TAG:
                return new DocumentConfigurationServiceJob(DocumentConfigurationIntentService.class);
            case PncCloseDateServiceJob.TAG:
                return new PncCloseDateServiceJob();
            case CloseAsrhMemberServiceJob.TAG:
                return new CloseAsrhMemberServiceJob();
            //TODO uncomment to enable plans
            /*case PlanIntentServiceJob.TAG:
                return new PlanIntentServiceJob();*/
            default:
                Timber.d("Looks like you tried to create a job " + tag + " that is not declared in the Chw Job Creator");
                return null;
        }
    }
}
