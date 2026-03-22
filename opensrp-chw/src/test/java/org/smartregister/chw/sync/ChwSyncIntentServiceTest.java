package org.smartregister.chw.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.sync.ChwSyncIntentService.LIMIT;

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
