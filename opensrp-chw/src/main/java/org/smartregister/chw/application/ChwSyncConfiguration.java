package org.smartregister.chw.application;

import org.smartregister.SyncConfiguration;
import org.smartregister.SyncFilter;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.core.utils.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by samuelgithengi on 10/19/18.
 */
public class ChwSyncConfiguration extends SyncConfiguration {
    private int connectTimeout = 900000;
    private int readTimeout = 900000;

    @Override
    public int getSyncMaxRetries() {
        return BuildConfig.MAX_SYNC_RETRIES;
    }

    @Override
    public SyncFilter getSyncFilterParam() {
        return SyncFilter.LOCATION;
    }

    @Override
    public String getSyncFilterValue() {
        return Utils.getSyncFilterValue();
    }

    @Override
    public int getUniqueIdSource() {
        return BuildConfig.OPENMRS_UNIQUE_ID_SOURCE;
    }

    @Override
    public int getUniqueIdBatchSize() {
        return BuildConfig.OPENMRS_UNIQUE_ID_BATCH_SIZE;
    }

    @Override
    public int getUniqueIdInitialBatchSize() {
        return BuildConfig.OPENMRS_UNIQUE_ID_INITIAL_BATCH_SIZE;
    }

    @Override
    public boolean isSyncSettings() {
        return BuildConfig.IS_SYNC_SETTINGS;
    }

    @Override
    public SyncFilter getEncryptionParam() {
        return SyncFilter.LOCATION;
    }

    @Override
    public boolean updateClientDetailsTable() {
        return false;
    }

    @Override
    public boolean isSyncUsingPost() {
        return !BuildConfig.DEBUG && ChwApplication.getApplicationFlavor().syncUsingPost();
    }

    @Override
    public List<String> getSynchronizedLocationTags() {
        return Arrays.asList("MOH Jhpiego Facility Name", "Health Facility", "Facility");
    }

    @Override
    public String getTopAllowedLocationLevel() {
        return "Council";
    }

    @Override
    public int getReadTimeout() {
        return connectTimeout;
    }

    @Override
    public int getConnectTimeout() {
        return readTimeout;
    }
}
