package org.smartregister.chw.util;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository;
import org.smartregister.domain.db.EventClient;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildEidAutoEnrollmentTest {
    @Test
    public void persistedRecoveryProcessesTheStoredPayloadAndRealChildOnly() throws Exception {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        ECSyncHelper sync = mock(ECSyncHelper.class);
        ClientProcessorForJava processor = mock(ClientProcessorForJava.class);
        MotherMentorChildEidRepository.EnrollmentPlan plan = mock(MotherMentorChildEidRepository.EnrollmentPlan.class);
        when(plan.getChildId()).thenReturn("real-child");
        when(sync.getClient("real-child")).thenReturn(new JSONObject("{\"baseEntityId\":\"real-child\",\"firstName\":\"Simulated\"}"));
        when(plan.getStoredEventJson(db)).thenReturn(event("original-snapshot"));
        try (MockedStatic<MotherMentorChildEidRepository> repo = mockStatic(MotherMentorChildEidRepository.class)) {
            repo.when(() -> MotherMentorChildEidRepository.getPendingEnrollmentPlan(db, "mother")).thenReturn(plan);
            MotherMentorChildEidAutoEnrollment.process(db, "mother", sync, processor);
        }
        ArgumentCaptor<List> batch = ArgumentCaptor.forClass(List.class);
        verify(processor).processClient(batch.capture());
        assertEquals(1, batch.getValue().size());
        EventClient processed = (EventClient) batch.getValue().get(0);
        assertEquals("original-snapshot", processed.getEvent().getFormSubmissionId());
        assertEquals("real-child", processed.getClient().getBaseEntityId());
        verify(plan, never()).toEvent();
        verify(sync, never()).addEvent(anyString(), any(JSONObject.class));
        verify(sync).getClient("real-child");
        verifyNoMoreInteractions(sync);
    }

    @Test
    public void newEnrollmentIsPersistedBeforeItsSpecificEventIsProcessed() throws Exception {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        ECSyncHelper sync = mock(ECSyncHelper.class);
        ClientProcessorForJava processor = mock(ClientProcessorForJava.class);
        MotherMentorChildEidRepository.EnrollmentPlan plan = mock(MotherMentorChildEidRepository.EnrollmentPlan.class);
        when(plan.getChildId()).thenReturn("real-child");
        when(sync.getClient("real-child")).thenReturn(new JSONObject("{\"baseEntityId\":\"real-child\"}"));
        when(plan.getStoredEventJson(db)).thenReturn(null, event("new-source"));
        when(plan.toEvent()).thenReturn(org.smartregister.chw.core.utils.CoreJsonFormUtils.gson.fromJson(
                event("new-source").toString(), org.smartregister.clientandeventmodel.Event.class));
        try (MockedStatic<MotherMentorChildEidRepository> repo = mockStatic(MotherMentorChildEidRepository.class)) {
            repo.when(() -> MotherMentorChildEidRepository.getPendingEnrollmentPlan(db, "mother")).thenReturn(plan);
            MotherMentorChildEidAutoEnrollment.process(db, "mother", sync, processor);
        }
        org.mockito.InOrder order = inOrder(sync, processor);
        order.verify(sync).getClient("real-child");
        order.verify(sync).addEvent(eq("real-child"), any(JSONObject.class));
        order.verify(processor).processClient(argThat(batch -> batch.size() == 1
                && "new-source".equals(batch.get(0).getEvent().getFormSubmissionId())));
        verifyNoMoreInteractions(sync);
    }

    @Test
    public void failedRawEventWriteCannotCreateAnEnrollmentProjection() throws Exception {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        ECSyncHelper sync = mock(ECSyncHelper.class);
        ClientProcessorForJava processor = mock(ClientProcessorForJava.class);
        MotherMentorChildEidRepository.EnrollmentPlan plan = mock(MotherMentorChildEidRepository.EnrollmentPlan.class);
        when(plan.getChildId()).thenReturn("real-child");
        when(sync.getClient("real-child")).thenReturn(new JSONObject("{\"baseEntityId\":\"real-child\"}"));
        when(plan.toEvent()).thenReturn(org.smartregister.chw.core.utils.CoreJsonFormUtils.gson.fromJson(
                event("new-source").toString(), org.smartregister.clientandeventmodel.Event.class));
        try (MockedStatic<MotherMentorChildEidRepository> repo = mockStatic(MotherMentorChildEidRepository.class)) {
            repo.when(() -> MotherMentorChildEidRepository.getPendingEnrollmentPlan(db, "mother")).thenReturn(plan);
            try {
                MotherMentorChildEidAutoEnrollment.process(db, "mother", sync, processor);
                fail("A missing durable event must stop projection processing");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("not persisted"));
            }
        }
        verify(sync).addEvent(eq("real-child"), any(JSONObject.class));
        verify(plan, times(2)).getStoredEventJson(db);
        verifyNoInteractions(processor);
    }

    @Test
    public void missingOrMismatchedRegistrationNeverCreatesAnIdentityOrEnrollment() throws Exception {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        ECSyncHelper sync = mock(ECSyncHelper.class);
        ClientProcessorForJava processor = mock(ClientProcessorForJava.class);
        MotherMentorChildEidRepository.EnrollmentPlan plan = mock(MotherMentorChildEidRepository.EnrollmentPlan.class);
        when(plan.getChildId()).thenReturn("real-child");
        try (MockedStatic<MotherMentorChildEidRepository> repo = mockStatic(MotherMentorChildEidRepository.class)) {
            repo.when(() -> MotherMentorChildEidRepository.getPendingEnrollmentPlan(db, "mother")).thenReturn(plan);
            MotherMentorChildEidAutoEnrollment.process(db, "mother", sync, processor);
            when(sync.getClient("real-child")).thenReturn(new JSONObject("{\"baseEntityId\":\"other-child\"}"));
            MotherMentorChildEidAutoEnrollment.process(db, "mother", sync, processor);
        }
        verifyNoInteractions(processor);
        verify(plan, never()).toEvent();
        verify(sync, never()).addEvent(anyString(), any(JSONObject.class));
    }

    private JSONObject event(String id) throws Exception {
        return new JSONObject("{\"baseEntityId\":\"real-child\",\"formSubmissionId\":\"" + id
                + "\",\"eventType\":\"" + org.smartregister.chw.mothermentor.util.Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_CHILD_EID
                + "\",\"eventDate\":\"2026-11-04T15:00:00.000Z\",\"obs\":[]}");
    }
}
