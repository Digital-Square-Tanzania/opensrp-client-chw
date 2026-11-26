package org.smartregister.chw.sync;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.domain.Event;
import org.smartregister.util.JsonFormUtils;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorDeceasedClientProcessorTest {

    @Test
    public void programmaticDeathClosesOnlyTheRemovedClientsMotherMentorEnrollments() {
        assertDeathClosesEnrollments("Death", CoreConstants.EventType.REMOVE_MEMBER);
    }

    @Test
    public void nativeFormDiedChoiceClosesMotherMentorEnrollments() {
        assertDeathClosesEnrollments("Died", CoreConstants.EventType.REMOVE_MEMBER);
    }

    @Test
    public void codedChildDeathClosesMotherMentorEnrollments() {
        assertDeathClosesEnrollments(
                "160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", CoreConstants.EventType.REMOVE_CHILD);
    }

    @Test
    public void movedAwayDoesNotCloseMotherMentorEnrollmentsAsADeath() {
        assertRemovalDoesNotCloseEnrollments("Moved away");
    }

    @Test
    public void otherRemovalDoesNotCloseMotherMentorEnrollmentsAsADeath() {
        assertRemovalDoesNotCloseEnrollments("Other");
    }

    @Test
    public void dateOfDeathWithoutDeathReasonDoesNotCloseMotherMentorEnrollments() {
        assertRemovalDoesNotCloseEnrollments("");
    }

    @Test
    public void missingClientIdentityDoesNotAttemptMotherMentorClosure() {
        try (MockedStatic<CoreChwApplication> application = mockApplication();
                MockedStatic<MotherMentorDao> dao = mockStatic(MotherMentorDao.class)) {
            processor().processRemoveMember(null, removal("Death", CoreConstants.EventType.REMOVE_MEMBER));

            dao.verifyNoInteractions();
        }
    }

    @Test
    public void syncedDeathReplayKeepsTheSameClientClosedWithoutRemovingHistory() {
        try (MockedStatic<CoreChwApplication> application = mockApplication();
                MockedStatic<MotherMentorDao> dao = mockStatic(MotherMentorDao.class)) {
            ChwClientProcessor processor = processor();
            Event event = removal("Death", CoreConstants.EventType.REMOVE_MEMBER);

            processor.processRemoveMember("removed-client", event);
            processor.processRemoveMember("removed-client", event);

            dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("removed-client"), times(2));
            dao.verifyNoMoreInteractions();
        }
    }

    private void assertDeathClosesEnrollments(String reason, String eventType) {
        try (MockedStatic<CoreChwApplication> application = mockApplication();
                MockedStatic<MotherMentorDao> dao = mockStatic(MotherMentorDao.class)) {
            processor().processRemoveMember("removed-client", removal(reason, eventType));

            dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("removed-client"));
            dao.verifyNoMoreInteractions();
        }
    }

    private void assertRemovalDoesNotCloseEnrollments(String reason) {
        try (MockedStatic<CoreChwApplication> application = mockApplication();
                MockedStatic<MotherMentorDao> dao = mockStatic(MotherMentorDao.class)) {
            processor().processRemoveMember(
                    "removed-client", removal(reason, CoreConstants.EventType.REMOVE_MEMBER));

            dao.verifyNoInteractions();
        }
    }

    private MockedStatic<CoreChwApplication> mockApplication() {
        MockedStatic<CoreChwApplication> application = mockStatic(CoreChwApplication.class);
        CoreChwApplication app = mock(CoreChwApplication.class);
        application.when(CoreChwApplication::getInstance).thenReturn(app);
        return application;
    }

    private ChwClientProcessor processor() {
        return mock(ChwClientProcessor.class, CALLS_REAL_METHODS);
    }

    private Event removal(String reason, String eventType) {
        return JsonFormUtils.gson.fromJson(
                "{\"baseEntityId\":\"removed-client\",\"eventType\":\"" + eventType
                        + "\",\"eventDate\":\"2026-10-28T12:00:00.000Z\",\"obs\":["
                        + "{\"fieldCode\":\"160417AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\","
                        + "\"formSubmissionField\":\"remove_reason\",\"values\":[\"" + reason + "\"]},"
                        + "{\"fieldCode\":\"date_died\",\"formSubmissionField\":\"date_died\","
                        + "\"values\":[\"28-10-2026\"]}]}", Event.class);
    }
}
