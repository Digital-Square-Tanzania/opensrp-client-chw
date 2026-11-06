package org.smartregister.chw.sync;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.repository.MotherMentorHouseholdClosureRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;

import java.util.concurrent.atomic.AtomicBoolean;

/** Keeps replayed enrollment projection from reopening a household with finalized closure evidence. */
public class MotherMentorHouseholdEnrollmentProcessingTest extends BaseUnitTest {
    @Test
    public void householdClosureIsRebuiltAfterTheGenericEnrollmentProjection() throws Exception {
        processEnrollment(Constants.EVENT_TYPE.MAMA_KINARA_HOUSEHOLD_ENROLMENT, true);
    }

    @Test
    public void unrelatedEnrollmentDoesNotRebuildHouseholdState() throws Exception {
        processEnrollment("Mother Mentor Enrollment", false);
    }

    private void processEnrollment(String eventType, boolean household) throws Exception {
        ChwClientProcessor processor = spy((ChwClientProcessor) ChwClientProcessor.getInstance(
                ApplicationProvider.getApplicationContext()));
        Event event = new Event();
        event.setBaseEntityId("household");
        event.setEventType(eventType);
        Client client = mock(Client.class);
        ClientClassification classification = mock(ClientClassification.class);
        EventClient eventClient = mock(EventClient.class);
        when(eventClient.getEvent()).thenReturn(event);
        when(eventClient.getClient()).thenReturn(client);
        AtomicBoolean projected = new AtomicBoolean();
        doAnswer(call -> { projected.set(true); return true; }).when(processor)
                .processEvent(event, client, classification);
        try (MockedStatic<CoreLibrary> core = Mockito.mockStatic(CoreLibrary.class);
             MockedStatic<MotherMentorMotherClosureRepository> mothers =
                     Mockito.mockStatic(MotherMentorMotherClosureRepository.class);
             MockedStatic<MotherMentorHouseholdClosureRepository> closure =
                     Mockito.mockStatic(MotherMentorHouseholdClosureRepository.class)) {
            CoreLibrary library = mock(CoreLibrary.class);
            core.when(CoreLibrary::getInstance).thenReturn(library);
            when(library.isPeerToPeerProcessing()).thenReturn(true);
            closure.when(() -> MotherMentorHouseholdClosureRepository.rebuild(any(), eq("household")))
                    .thenAnswer(call -> { assertTrue(projected.get()); return null; });
            processor.processEvents(classification, null, null, eventClient, event, eventType);
            assertTrue(projected.get());
            if (household) {
                closure.verify(() -> MotherMentorHouseholdClosureRepository.rebuild(any(), eq("household")));
            } else {
                closure.verifyNoInteractions();
            }
        }
    }
}
