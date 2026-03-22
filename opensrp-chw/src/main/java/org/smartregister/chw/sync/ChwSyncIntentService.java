package org.smartregister.chw.sync;

import android.content.Intent;
import android.util.Pair;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.CoreLibrary;
import org.smartregister.SyncConfiguration;
import org.smartregister.SyncFilter;
import org.smartregister.chw.application.ChwSyncConfiguration;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.Response;
import org.smartregister.domain.SyncEntity;
import org.smartregister.domain.SyncProgress;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.sync.intent.SyncIntentService;
import org.smartregister.util.Utils;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ChwSyncIntentService extends SyncIntentService {
    private static final String EVENT_TYPE = "eventType";
    private static final String TEAM_ID_SCOPED_EVENT_TYPES_PREFIX = "teamId:";
    private static final String UTF_8 = "UTF-8";
    public static final String LIMIT = "limit";
    private long totalRecords;
    private int fetchedRecords;
    private boolean teamScopedSyncActive;
    private boolean retryReturnCount = true;

    @Override
    protected void pullECFromServer() {
        if (!shouldUseTeamIdScopedEventSync()) {
            super.pullECFromServer();
            return;
        }

        if (StringUtils.isBlank(getDefaultTeamId())) {
            FetchStatus.fetchedFailed.setDisplayValue("Missing teamId for CHW sync");
            complete(FetchStatus.fetchedFailed);
            resetTeamScopedSyncState();
            return;
        }

        resetTeamScopedSyncState();
        teamScopedSyncActive = true;
        fetchTeamScopedEvents(0, true);
    }

    private synchronized void fetchTeamScopedEvents(int count, boolean returnCount) {
        retryReturnCount = returnCount;

        try {
            SyncConfiguration configs = getSyncConfiguration();
            if (configs.getSyncFilterParam() == null || StringUtils.isBlank(configs.getSyncFilterValue())) {
                complete(FetchStatus.fetchedFailed);
                resetTeamScopedSyncState();
                return;
            }

            ECSyncHelper ecSyncUpdater = ECSyncHelper.getInstance(getContext());
            Long lastSyncDatetime = ecSyncUpdater.getLastSyncTimeStamp();
            Response<String> response = getTeamScopedSyncResponse(getFormattedBaseUrl() + SYNC_URL, lastSyncDatetime,
                    returnCount);

            if (response == null) {
                FetchStatus.fetchedFailed.setDisplayValue("Empty response");
                complete(FetchStatus.fetchedFailed);
                resetTeamScopedSyncState();
                return;
            }

            if (response.isUrlError() || response.isTimeoutError()) {
                FetchStatus.fetchedFailed.setDisplayValue(response.status().displayValue());
                complete(FetchStatus.fetchedFailed);
                resetTeamScopedSyncState();
                return;
            }

            if (response.isFailure()) {
                fetchFailed(count);
                return;
            }

            if (returnCount && response.getTotalRecords() != null) {
                totalRecords = response.getTotalRecords();
            }

            processFetchedTeamScopedEvents(response, ecSyncUpdater, count);
        } catch (Exception e) {
            Timber.e(e, "Fetch Retry Exception: %s", e.getMessage());
            fetchFailed(count);
        }
    }

    protected Response<String> getTeamScopedSyncResponse(String requestUrl, long lastSyncDatetime, boolean returnCount)
            throws JSONException {
        if (getHttpAgent() == null) {
            return null;
        }

        if (getSyncConfiguration().isSyncUsingPost()) {
            return getHttpAgent().postWithJsonResponse(requestUrl,
                    buildTeamScopedSyncRequestPayload(lastSyncDatetime, returnCount).toString());
        }

        String requestUrlWithParams = buildTeamScopedSyncRequestUrl(requestUrl, lastSyncDatetime, returnCount);
        Timber.i("URL: %s", requestUrlWithParams);
        return getHttpAgent().fetch(requestUrlWithParams);
    }

    private void processFetchedTeamScopedEvents(Response<String> response, ECSyncHelper ecSyncUpdater, int count)
            throws JSONException {
        int eventCount;
        JSONObject jsonObject = new JSONObject();
        if (response.payload() == null) {
            eventCount = 0;
        } else {
            jsonObject = new JSONObject(response.payload());
            eventCount = fetchNumberOfEvents(jsonObject);
        }

        if (eventCount == 0) {
            complete(FetchStatus.nothingFetched);
            sendSyncProgressBroadcast(0);
            resetTeamScopedSyncState();
            return;
        }

        if (eventCount < 0) {
            fetchFailed(count);
            return;
        }

        Pair<Long, Long> serverVersionPair = getMinMaxServerVersions(jsonObject);
        long lastServerVersion = serverVersionPair.second - 1;
        if (eventCount < getEventPullLimit()) {
            lastServerVersion = serverVersionPair.second;
        }

        boolean isSaved = ecSyncUpdater.saveAllClientsAndEvents(jsonObject);
        if (isSaved) {
            processClient(serverVersionPair);
            ecSyncUpdater.updateLastSyncTimeStamp(lastServerVersion);
        }

        sendSyncProgressBroadcast(eventCount);
        fetchTeamScopedEvents(0, false);
    }

    @Override
    public void fetchFailed(int count) {
        if (!teamScopedSyncActive) {
            super.fetchFailed(count);
            return;
        }

        if (count < getSyncConfiguration().getSyncMaxRetries()) {
            fetchTeamScopedEvents(count + 1, retryReturnCount);
        } else {
            complete(FetchStatus.fetchedFailed);
            resetTeamScopedSyncState();
        }
    }

    @Override
    protected void sendSyncProgressBroadcast(int eventCount) {
        if (!teamScopedSyncActive) {
            super.sendSyncProgressBroadcast(eventCount);
            return;
        }

        fetchedRecords += eventCount;
        SyncProgress syncProgress = new SyncProgress();
        syncProgress.setSyncEntity(SyncEntity.EVENTS);
        syncProgress.setTotalRecords(totalRecords);
        syncProgress.setPercentageSynced(Utils.calculatePercentage(totalRecords, fetchedRecords));

        Intent intent = new Intent();
        intent.setAction(AllConstants.SyncProgressConstants.ACTION_SYNC_PROGRESS);
        intent.putExtra(AllConstants.SyncProgressConstants.SYNC_PROGRESS_DATA, syncProgress);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    protected JSONObject buildTeamScopedSyncRequestPayload(long lastSyncDatetime, boolean returnCount)
            throws JSONException {
        Map<String, String> params = buildTeamScopedSyncParams(lastSyncDatetime, returnCount);
        JSONObject requestPayload = new JSONObject();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            switch (entry.getKey()) {
                case AllConstants.SERVER_VERSION:
                    requestPayload.put(entry.getKey(), Long.parseLong(entry.getValue()));
                    break;
                case LIMIT:
                    requestPayload.put(entry.getKey(), Integer.parseInt(entry.getValue()));
                    break;
                case AllConstants.RETURN_COUNT:
                    requestPayload.put(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
                    break;
                default:
                    requestPayload.put(entry.getKey(), entry.getValue());
                    break;
            }
        }

        return requestPayload;
    }

    protected String buildTeamScopedSyncRequestUrl(String requestUrl, long lastSyncDatetime, boolean returnCount) {
        return requestUrl + "?" + encodeQueryString(buildTeamScopedSyncParams(lastSyncDatetime, returnCount));
    }

    protected Map<String, String> buildTeamScopedSyncParams(long lastSyncDatetime, boolean returnCount) {
        LinkedHashMap<String, String> requestParams = new LinkedHashMap<>();
        SyncConfiguration syncConfiguration = getSyncConfiguration();
        requestParams.put(syncConfiguration.getSyncFilterParam().value(), syncConfiguration.getSyncFilterValue());
        requestParams.put(SyncFilter.TEAM_ID.value(), getDefaultTeamId());
        requestParams.put(EVENT_TYPE, getTeamScopedEventTypeFilter());
        requestParams.put(AllConstants.SERVER_VERSION, String.valueOf(lastSyncDatetime));
        requestParams.put(LIMIT, String.valueOf(getEventPullLimit()));
        requestParams.put(AllConstants.RETURN_COUNT, String.valueOf(returnCount));
        return requestParams;
    }

    protected SyncConfiguration getSyncConfiguration() {
        return CoreLibrary.getInstance().getSyncConfiguration();
    }

    protected List<String> getTeamIdScopedEventTypes() {
        SyncConfiguration syncConfiguration = getSyncConfiguration();
        if (syncConfiguration instanceof ChwSyncConfiguration) {
            return ((ChwSyncConfiguration) syncConfiguration).getTeamIdScopedEventTypes();
        }
        return Collections.emptyList();
    }

    protected String getTeamScopedEventTypeFilter() {
        return TEAM_ID_SCOPED_EVENT_TYPES_PREFIX + String.join(",", getTeamIdScopedEventTypes());
    }

    protected String getDefaultTeamId() {
        AllSharedPreferences allSharedPreferences = CoreLibrary.getInstance().context().allSharedPreferences();
        return allSharedPreferences.fetchDefaultTeamId(allSharedPreferences.fetchRegisteredANM());
    }

    protected boolean shouldUseTeamIdScopedEventSync() {
        return !getTeamIdScopedEventTypes().isEmpty();
    }

    private String encodeQueryString(Map<String, String> requestParams) {
        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append('&');
            }

            queryBuilder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }

        return queryBuilder.toString();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode sync parameter", e);
        }
    }

    private void resetTeamScopedSyncState() {
        teamScopedSyncActive = false;
        retryReturnCount = true;
        totalRecords = 0;
        fetchedRecords = 0;
    }

    @Override
    public int getEventPullLimit() {
        return 500;
    }
}
