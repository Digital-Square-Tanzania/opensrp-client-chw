package org.smartregister.chw.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.AllConstants;
import org.smartregister.SyncConfiguration;
import org.smartregister.SyncFilter;
import org.smartregister.chw.BaseUnitTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ChwSyncIntentServiceTest extends BaseUnitTest {

    @Test
    public void buildTeamScopedSyncParamsShouldIncludeLocationTeamAndWhitelist() {
        TestableChwSyncIntentService service = new TestableChwSyncIntentService("test-location-id", "test-team-id",
                Arrays.asList("Close Referral", "LTFU Feedback"));

        Map<String, String> params = service.buildTeamScopedSyncParams(25L, true);

        assertEquals("test-location-id", params.get("locationId"));
        assertEquals("test-team-id", params.get("teamId"));
        assertEquals("teamId:Close Referral,LTFU Feedback", params.get("eventType"));
        assertEquals("25", params.get(AllConstants.SERVER_VERSION));
        assertEquals("500", params.get(AllConstants.LIMIT));
        assertEquals("true", params.get(AllConstants.RETURN_COUNT));
    }

    @Test
    public void buildTeamScopedSyncRequestPayloadShouldUseTypedValues() throws Exception {
        TestableChwSyncIntentService service = new TestableChwSyncIntentService("test-location-id", "test-team-id",
                Arrays.asList("Close Referral", "LTFU Feedback"));

        JSONObject payload = service.buildTeamScopedSyncRequestPayload(42L, false);

        assertEquals("test-location-id", payload.getString("locationId"));
        assertEquals("test-team-id", payload.getString("teamId"));
        assertEquals("teamId:Close Referral,LTFU Feedback", payload.getString("eventType"));
        assertEquals(42L, payload.getLong(AllConstants.SERVER_VERSION));
        assertEquals(500, payload.getInt(AllConstants.LIMIT));
        assertEquals(false, payload.getBoolean(AllConstants.RETURN_COUNT));
    }

    @Test
    public void buildTeamScopedSyncRequestUrlShouldEncodeEventTypes() {
        TestableChwSyncIntentService service = new TestableChwSyncIntentService("test-location-id", "test-team-id",
                Arrays.asList("Close Referral", "LTFU Feedback"));

        String requestUrl = service.buildTeamScopedSyncRequestUrl("https://example.org/rest/event/sync", 7L, true);

        assertTrue(requestUrl.contains("locationId=test-location-id"));
        assertTrue(requestUrl.contains("teamId=test-team-id"));
        assertTrue(requestUrl.contains("eventType=teamId%3AClose+Referral%2CLTFU+Feedback"));
        assertTrue(requestUrl.contains("serverVersion=7"));
        assertTrue(requestUrl.contains("return_count=true"));
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
}
