package org.smartregister.chw.sync;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.util.JsonFormUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChwClientProcessorTest extends BaseUnitTest {

    @Test
    public void testBuildVaccineFromEventUsesFormSubmissionFieldInsteadOfAdministeredDate() throws Exception {
        String eventJson = "{\"baseEntityId\":\"28bd4ea2-1016-4b66-b37f-3b635633e607\",\"details\":{\"program_client_id\":\"bd2351a0-e8d3-4110-9193-be5003e67d3e\"},\"duration\":0,\"entityType\":\"vaccination\",\"eventDate\":\"2026-06-04T00:00:00.000Z\",\"eventId\":\"event-id\",\"eventType\":\"Vaccination\",\"formSubmissionId\":\"8fb98195-eed6-4f0d-9440-d4a77456394c\",\"locationId\":\"fbdd93f1-2045-4744-ae38-133f78a049c0\",\"obs\":[{\"fieldCode\":\"1410AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"fieldDataType\":\"date\",\"fieldType\":\"concept\",\"formSubmissionField\":\"opv_1\",\"humanReadableValues\":[],\"parentCode\":\"783AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"saveObsAsArray\":false,\"values\":[\"2026-06-04\"]},{\"fieldCode\":\"1418AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"fieldDataType\":\"calculate\",\"fieldType\":\"concept\",\"formSubmissionField\":\"opv_1_dose\",\"humanReadableValues\":[],\"parentCode\":\"783AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"saveObsAsArray\":false,\"values\":[\"1\"]}],\"providerId\":\"markchw\",\"team\":\"Kia - 102557-6\",\"teamId\":\"8b0ad916-115b-410c-9dd2-0b9fc00ca84f\",\"dateCreated\":\"2026-07-30T12:00:17.988Z\",\"clientApplicationVersion\":40,\"clientDatabaseVersion\":37}";

        Event event = JsonFormUtils.gson.fromJson(eventJson, Event.class);
        EventClient eventClient = new EventClient(event);

        ChwClientProcessor processor = (ChwClientProcessor) ChwClientProcessor.getInstance(ApplicationProvider.getApplicationContext());
        org.smartregister.immunization.domain.Vaccine vaccine = processor.buildVaccineFromEvent(eventClient, false);

        Assert.assertNotNull(vaccine);
        Assert.assertEquals("opv_1", vaccine.getName());
        Assert.assertEquals(Integer.valueOf(1), vaccine.getCalculation());
        Assert.assertEquals("bd2351a0-e8d3-4110-9193-be5003e67d3e", vaccine.getProgramClientId());
        Assert.assertEquals("markchw", vaccine.getAnmId());
        Assert.assertEquals("fbdd93f1-2045-4744-ae38-133f78a049c0", vaccine.getLocationId());
        Assert.assertEquals("8fb98195-eed6-4f0d-9440-d4a77456394c", vaccine.getFormSubmissionId());
        Assert.assertEquals("2026-06-04", new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(vaccine.getDate()));
    }
}
