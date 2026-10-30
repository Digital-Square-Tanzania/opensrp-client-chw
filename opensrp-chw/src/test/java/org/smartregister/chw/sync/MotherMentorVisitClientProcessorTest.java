package org.smartregister.chw.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.util.JsonFormUtils;

/** Exercises the app dispatch, leaving conversion/persistence to the existing visit processor. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorVisitClientProcessorTest {
    @Test
    public void motherAndHouseholdServicesUseTheExistingVisitProcessor() throws Exception {
        assertVisitRouting(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
    }

    @Test
    public void motherIitFollowUpUsesTheExistingVisitProcessor() throws Exception {
        assertVisitRouting(MotherMentorFollowUpType.MOTHER_IIT.getEventType());
    }

    @Test
    public void partnerFollowUpUsesTheExistingVisitProcessor() throws Exception {
        assertVisitRouting(MotherMentorFollowUpType.PARTNER.getEventType());
    }

    @Test
    public void childEidFollowUpUsesTheExistingVisitProcessor() throws Exception {
        assertVisitRouting(MotherMentorFollowUpType.CHILD_EID.getEventType());
    }

    @Test
    public void enrollmentDoesNotBecomeAServiceVisit() throws Exception {
        try (MockedStatic<CoreLibrary> core = peerProcessing();
                MockedStatic<NCUtils> visits = mockStatic(NCUtils.class)) {
            process(processor(), Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_PARTNER);
            visits.verifyNoInteractions();
        }
    }

    private void assertVisitRouting(String eventType) throws Exception {
        try (MockedStatic<CoreLibrary> core = peerProcessing();
                MockedStatic<NCUtils> visits = mockStatic(NCUtils.class)) {
            ChwClientProcessor processor = processor();
            EventClient eventClient = eventClient(eventType);
            // The existing persistence implementation owns replay deduplication, so the
            // dispatcher must hand both deliveries of the canonical event to that implementation.
            for (int delivery = 0; delivery < 2; delivery++) {
                processor.processEvents(null, null, null, eventClient, eventClient.getEvent(), eventType);
            }
            visits.verify(() -> NCUtils.processHomeVisit(eventClient), times(2));
        }
    }

    private void process(ChwClientProcessor processor, String eventType) throws Exception {
        EventClient eventClient = eventClient(eventType);
        processor.processEvents(null, null, null, eventClient, eventClient.getEvent(), eventType);
    }

    private ChwClientProcessor processor() throws Exception {
        ChwClientProcessor processor = mock(ChwClientProcessor.class, CALLS_REAL_METHODS);
        doReturn(true).when(processor).processEvent(any(Event.class), isNull(), isNull());
        return processor;
    }

    private MockedStatic<CoreLibrary> peerProcessing() {
        MockedStatic<CoreLibrary> core = mockStatic(CoreLibrary.class);
        CoreLibrary library = mock(CoreLibrary.class);
        when(library.isPeerToPeerProcessing()).thenReturn(true);
        core.when(CoreLibrary::getInstance).thenReturn(library);
        return core;
    }

    private EventClient eventClient(String eventType) {
        Event event = JsonFormUtils.gson.fromJson("{\"baseEntityId\":\"synthetic-client\","
                + "\"formSubmissionId\":\"synthetic-submission\",\"eventType\":\"" + eventType
                + "\",\"eventDate\":\"2026-10-29T12:00:00.000Z\",\"obs\":[]}", Event.class);
        return new EventClient(event);
    }
}
