package org.smartregister.chw.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.sync.ChwSyncIntentService.LIMIT;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.AllConstants;
import org.smartregister.SyncConfiguration;
import org.smartregister.SyncFilter;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.domain.Response;
import org.smartregister.domain.ResponseStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ChwSyncIntentServiceTest extends BaseUnitTest {
    private static final List<String> TEAM_SCOPED_EVENT_TYPES = Arrays.asList(
            "Hiv Index Contact Registration",
            "HIV Index Contact Community Followup Referral",
            "LTFU Feedback",
            "HIV Index Contact CHW Followup"
    );
    private static final String TEAM_SCOPED_EVENT_TYPES_PARAM =
            "teamId:Hiv Index Contact Registration,HIV Index Contact Community Followup Referral,"
                    + "LTFU Feedback,HIV Index Contact CHW Followup";
    private static final String ENCODED_TEAM_SCOPED_EVENT_TYPES_PARAM =
            "eventType=teamId%3AHiv+Index+Contact+Registration%2CHIV+Index+Contact+Community+"
                    + "Followup+Referral%2CLTFU+Feedback%2CHIV+Index+Contact+CHW+Followup";

    @Test
    public void buildTeamScopedSyncParamsShouldIncludeLocationTeamAndWhitelist() {
        TestableChwSyncIntentService service = new TestableChwSyncIntentService("test-location-id", "test-team-id",
                TEAM_SCOPED_EVENT_TYPES);

        Map<String, String> params = service.buildTeamScopedSyncParams(25L, true);

        assertEquals("test-location-id", params.get("locationId"));
        assertEquals("test-team-id", params.get("teamId"));
        assertEquals(TEAM_SCOPED_EVENT_TYPES_PARAM, params.get("eventType"));
        assertEquals("25", params.get(AllConstants.SERVER_VERSION));
        assertEquals("500", params.get(LIMIT));
        assertEquals("true", params.get(AllConstants.RETURN_COUNT));
    }

    @Test
    public void buildTeamScopedSyncRequestPayloadShouldUseTypedValues() throws Exception {
        TestableChwSyncIntentService service = new TestableChwSyncIntentService("test-location-id", "test-team-id",
                TEAM_SCOPED_EVENT_TYPES);

        JSONObject payload = service.buildTeamScopedSyncRequestPayload(42L, false);

        assertEquals("test-location-id", payload.getString("locationId"));
        assertEquals("test-team-id", payload.getString("teamId"));
        assertEquals(TEAM_SCOPED_EVENT_TYPES_PARAM, payload.getString("eventType"));
        assertEquals(42L, payload.getLong(AllConstants.SERVER_VERSION));
        assertEquals(500, payload.getInt(LIMIT));
        assertEquals(false, payload.getBoolean(AllConstants.RETURN_COUNT));
    }

    @Test
    public void buildTeamScopedSyncRequestUrlShouldEncodeEventTypes() {
        TestableChwSyncIntentService service = new TestableChwSyncIntentService("test-location-id", "test-team-id",
                TEAM_SCOPED_EVENT_TYPES);

        String requestUrl = service.buildTeamScopedSyncRequestUrl("https://example.org/rest/event/sync", 7L, true);

        assertTrue(requestUrl.contains("locationId=test-location-id"));
        assertTrue(requestUrl.contains("teamId=test-team-id"));
        assertTrue(requestUrl.contains(ENCODED_TEAM_SCOPED_EVENT_TYPES_PARAM));
        assertTrue(requestUrl.contains("serverVersion=7"));
        assertTrue(requestUrl.contains("return_count=true"));
    }

    @Test
    public void extractBaseEntityIdsShouldReturnUniqueEventAndClientIds() throws Exception {
        TestableChwSyncIntentService service = new TestableChwSyncIntentService("test-location-id", "test-team-id",
                TEAM_SCOPED_EVENT_TYPES);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("events", new JSONArray()
                .put(new JSONObject().put("baseEntityId", "event-1"))
                .put(new JSONObject().put("baseEntityId", "shared-id"))
                .put(new JSONObject())
                .put(new JSONObject().put("baseEntityId", "")));
        jsonObject.put("clients", new JSONArray()
                .put(new JSONObject().put("baseEntityId", "client-1"))
                .put(new JSONObject().put("baseEntityId", "shared-id")));

        List<String> baseEntityIds = new ArrayList<>(service.extractBaseEntityIds(jsonObject));

        assertEquals(Arrays.asList("event-1", "shared-id", "client-1"), baseEntityIds);
    }

    @Test
    public void syncRelatedClientEventsByBaseEntityIdsShouldFetchAndProcessUniqueBatches() throws Exception {
        RecordingChwSyncIntentService service = new RecordingChwSyncIntentService("test-location-id", "test-team-id",
                TEAM_SCOPED_EVENT_TYPES);
        service.setBaseEntityBackfillBatchSize(2);
        service.queueResponse(successResponse());
        service.queueResponse(successResponse());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("events", new JSONArray()
                .put(new JSONObject().put("baseEntityId", "event-1"))
                .put(new JSONObject().put("baseEntityId", "event-2")));
        jsonObject.put("clients", new JSONArray()
                .put(new JSONObject().put("baseEntityId", "event-2"))
                .put(new JSONObject().put("baseEntityId", "client-1")));

        boolean isSuccessful = service.syncRelatedClientEventsByBaseEntityIds(jsonObject);

        assertTrue(isSuccessful);
        assertEquals(Arrays.asList("event-1", "event-2"), service.getFetchedBaseEntityIdBatches().get(0));
        assertEquals(Arrays.asList("client-1"), service.getFetchedBaseEntityIdBatches().get(1));
        assertEquals(2, service.getProcessedRelatedPayloads().size());
    }

    @Test
    public void syncRelatedClientEventsByBaseEntityIdsShouldReturnFalseWhenBatchFetchFails() throws Exception {
        RecordingChwSyncIntentService service = new RecordingChwSyncIntentService("test-location-id", "test-team-id",
                TEAM_SCOPED_EVENT_TYPES);
        service.queueResponse(new Response<>(ResponseStatus.failure, null));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("events", new JSONArray().put(new JSONObject().put("baseEntityId", "event-1")));

        assertFalse(service.syncRelatedClientEventsByBaseEntityIds(jsonObject));
    }

    private Response<String> successResponse() {
        return new Response<>(ResponseStatus.success, "{\"events\":[],\"clients\":[],\"no_of_events\":0}");
    }

    private static class TestableChwSyncIntentService extends ChwSyncIntentService {
        private final SyncConfiguration syncConfiguration;
        private final String teamId;
        private final List<String> teamScopedEventTypes;

        private TestableChwSyncIntentService(String locationId, String teamId, List<String> teamScopedEventTypes) {
            this.syncConfiguration = mock(SyncConfiguration.class);
            when(syncConfiguration.getSyncFilterParam()).thenReturn(SyncFilter.LOCATION);
            when(syncConfiguration.getSyncFilterValue()).thenReturn(locationId);
            this.teamId = teamId;
            this.teamScopedEventTypes = teamScopedEventTypes;
        }

        @Override
        protected SyncConfiguration getSyncConfiguration() {
            return syncConfiguration;
        }

        @Override
        protected String getDefaultTeamId() {
            return teamId;
        }

        @Override
        protected List<String> getTeamIdScopedEventTypes() {
            return teamScopedEventTypes;
        }
    }

    private static class RecordingChwSyncIntentService extends TestableChwSyncIntentService {
        private final List<Response<String>> queuedResponses = new ArrayList<>();
        private final List<List<String>> fetchedBaseEntityIdBatches = new ArrayList<>();
        private final List<JSONObject> processedRelatedPayloads = new ArrayList<>();
        private int baseEntityBackfillBatchSize = 1000;

        private RecordingChwSyncIntentService(String locationId, String teamId, List<String> teamScopedEventTypes) {
            super(locationId, teamId, teamScopedEventTypes);
        }

        @Override
        protected Response<String> fetchClientEventsByBaseEntityIds(JSONArray baseEntityIds) {
            List<String> fetchedBaseEntityIds = new ArrayList<>();
            for (int i = 0; i < baseEntityIds.length(); i++) {
                fetchedBaseEntityIds.add(baseEntityIds.optString(i));
            }
            fetchedBaseEntityIdBatches.add(fetchedBaseEntityIds);
            return queuedResponses.remove(0);
        }

        @Override
        protected boolean processRelatedClientEvents(JSONObject jsonObject) {
            processedRelatedPayloads.add(jsonObject);
            return true;
        }

        @Override
        protected int getBaseEntityBackfillBatchSize() {
            return baseEntityBackfillBatchSize;
        }

        private void queueResponse(Response<String> response) {
            queuedResponses.add(response);
        }

        private void setBaseEntityBackfillBatchSize(int baseEntityBackfillBatchSize) {
            this.baseEntityBackfillBatchSize = baseEntityBackfillBatchSize;
        }

        private List<List<String>> getFetchedBaseEntityIdBatches() {
            return fetchedBaseEntityIdBatches;
        }

        private List<JSONObject> getProcessedRelatedPayloads() {
            return processedRelatedPayloads;
        }
    }
}
