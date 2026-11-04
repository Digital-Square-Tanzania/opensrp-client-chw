package org.smartregister.chw.sync;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.repository.MotherMentorHouseholdClosureRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherDeliveryRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherChildRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import java.util.concurrent.atomic.AtomicBoolean;

/** Restored linked enrollments retain the closure projected from finalized mother evidence. */
public class MotherMentorMotherEnrollmentProcessingTest extends BaseUnitTest {
    @Test
    public void motherAndIitEnrollmentsRebuildAfterTheirGenericProjection() throws Exception {
        assertProjection(Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLLMENT, "mother");
        assertProjection(Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_IIT, "mother");
    }

    @Test
    public void childAndHouseholdEnrollmentsRebuildTheirLinkedMothers() throws Exception {
        assertProjection(Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_CHILD_EID, "child");
        assertProjection(Constants.EVENT_TYPE.MAMA_KINARA_HOUSEHOLD_ENROLMENT, "household");
    }

    @Test
    public void partnerEnrollmentKeepsItsIndependentProjection() throws Exception {
        assertProjection(Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_PARTNER, "none");
    }

    private void assertProjection(String eventType, String expected) throws Exception {
        ChwClientProcessor processor = spy((ChwClientProcessor) ChwClientProcessor.getInstance(
                ApplicationProvider.getApplicationContext()));
        Event event = new Event();
        event.setBaseEntityId("simulated-client"); event.setEventType(eventType);
        Client client = mock(Client.class);
        ClientClassification classification = mock(ClientClassification.class);
        EventClient eventClient = mock(EventClient.class);
        when(eventClient.getEvent()).thenReturn(event);
        when(eventClient.getClient()).thenReturn(client);
        AtomicBoolean projected = new AtomicBoolean();
        doAnswer(call -> { projected.set(true); return true; }).when(processor)
                .processEvent(event, client, classification);
        try (MockedStatic<CoreLibrary> core = mockStatic(CoreLibrary.class);
                MockedStatic<MotherMentorHouseholdClosureRepository> households =
                        mockStatic(MotherMentorHouseholdClosureRepository.class);
                MockedStatic<MotherMentorMotherDeliveryRepository> delivery = mockStatic(MotherMentorMotherDeliveryRepository.class);
                MockedStatic<MotherMentorMotherChildRepository> childClinical = mockStatic(MotherMentorMotherChildRepository.class);
                MockedStatic<MotherMentorChildEidRepository> childTracing = mockStatic(MotherMentorChildEidRepository.class);
                MockedStatic<MotherMentorMotherClosureRepository> closure =
                        mockStatic(MotherMentorMotherClosureRepository.class)) {
            CoreLibrary library = mock(CoreLibrary.class);
            core.when(CoreLibrary::getInstance).thenReturn(library);
            when(library.isPeerToPeerProcessing()).thenReturn(true);
            closure.when(() -> MotherMentorMotherClosureRepository.rebuild(any(), eq("simulated-client")))
                    .thenAnswer(call -> { assertTrue(projected.get()); return null; });
            closure.when(() -> MotherMentorMotherClosureRepository.rebuildForChild(any(), eq("simulated-client")))
                    .thenAnswer(call -> { assertTrue(projected.get()); return null; });
            closure.when(() -> MotherMentorMotherClosureRepository.rebuildForHousehold(any(), eq("simulated-client")))
                    .thenAnswer(call -> { assertTrue(projected.get()); return null; });
            delivery.when(() -> MotherMentorMotherDeliveryRepository.rebuild(any(), eq("simulated-client")))
                    .thenAnswer(call -> { assertTrue(projected.get()); return null; });
            childClinical.when(() -> MotherMentorMotherChildRepository.rebuild(any(), eq("simulated-client")))
                    .thenAnswer(call -> { assertTrue(projected.get()); return null; });
            childClinical.when(() -> MotherMentorMotherChildRepository.rebuildForChild(any(), eq("simulated-client")))
                    .thenAnswer(call -> { assertTrue(projected.get()); return null; });
            childTracing.when(() -> MotherMentorChildEidRepository.rebuild(any(), eq("simulated-client")))
                    .thenAnswer(call -> { assertTrue(projected.get()); return null; });
            processor.processEvents(classification, null, null, eventClient, event, eventType);
            assertTrue(projected.get());
            if (Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLLMENT.equals(eventType)) {
                delivery.verify(() -> MotherMentorMotherDeliveryRepository.rebuild(any(), eq("simulated-client")));
                childClinical.verify(() -> MotherMentorMotherChildRepository.rebuild(any(), eq("simulated-client")));
            }
            if (Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_CHILD_EID.equals(eventType)) {
                childTracing.verify(() -> MotherMentorChildEidRepository.rebuild(any(), eq("simulated-client")));
            }
            if ("mother".equals(expected)) {
                closure.verify(() -> MotherMentorMotherClosureRepository.rebuild(any(), eq("simulated-client")));
            } else if ("child".equals(expected)) {
                closure.verify(() -> MotherMentorMotherClosureRepository.rebuildForChild(any(), eq("simulated-client")));
            } else if ("household".equals(expected)) {
                closure.verify(() -> MotherMentorMotherClosureRepository.rebuildForHousehold(any(), eq("simulated-client")));
            } else closure.verifyNoInteractions();
        }
    }
}
