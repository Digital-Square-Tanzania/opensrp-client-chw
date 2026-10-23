package org.smartregister.chw.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.asrh.dao.AsrhDao;
import org.smartregister.chw.asrh.domain.MemberObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class CloseAsrhMembershipIntentServiceTest {

    @Test
    public void nullMemberListFinishesWithoutClosingMembersOrLoggingAnError() {
        try (MockedStatic<AsrhDao> dao = mockStatic(AsrhDao.class);
                MockedStatic<Timber> logs = mockStatic(Timber.class)) {
            dao.when(AsrhDao::getMembers).thenReturn(null);
            List<String> closedMembers = captureClosedMembers(dao);
            List<Throwable> errors = captureErrors(logs);

            runService();

            assertTrue(closedMembers.isEmpty());
            assertTrue(errors.isEmpty());
            dao.verify(() -> AsrhDao.closeAsrhMemberFromRegister(anyString()), never());
            logs.verifyNoInteractions();
        }
    }

    @Test
    public void emptyMemberListFinishesWithoutClosingMembersOrLoggingAnError() {
        try (MockedStatic<AsrhDao> dao = mockStatic(AsrhDao.class);
                MockedStatic<Timber> logs = mockStatic(Timber.class)) {
            dao.when(AsrhDao::getMembers).thenReturn(Collections.emptyList());
            List<String> closedMembers = captureClosedMembers(dao);
            List<Throwable> errors = captureErrors(logs);

            runService();

            assertTrue(closedMembers.isEmpty());
            assertTrue(errors.isEmpty());
            dao.verify(() -> AsrhDao.closeAsrhMemberFromRegister(anyString()), never());
            logs.verifyNoInteractions();
        }
    }

    @Test
    public void closesOnlyMembersOlderThanTwentyFour() {
        MemberObject adolescent = member("adolescent", 19);
        MemberObject eligibleBoundary = member("age-24", 24);
        MemberObject olderMember = member("age-25", 25);
        try (MockedStatic<AsrhDao> dao = mockStatic(AsrhDao.class);
                MockedStatic<Timber> logs = mockStatic(Timber.class)) {
            dao.when(AsrhDao::getMembers)
                    .thenReturn(Arrays.asList(adolescent, eligibleBoundary, olderMember));
            List<String> closedMembers = captureClosedMembers(dao);
            List<Throwable> errors = captureErrors(logs);

            runService();

            assertEquals(Collections.singletonList("age-25"), closedMembers);
            assertTrue(errors.isEmpty());
            dao.verify(() -> AsrhDao.closeAsrhMemberFromRegister("age-25"));
            dao.verify(() -> AsrhDao.closeAsrhMemberFromRegister("adolescent"), never());
            dao.verify(() -> AsrhDao.closeAsrhMemberFromRegister("age-24"), never());
        }
    }

    @Test
    public void memberLookupFailureRemainsLogged() {
        IllegalStateException failure = new IllegalStateException("Member lookup failed");
        try (MockedStatic<AsrhDao> dao = mockStatic(AsrhDao.class);
                MockedStatic<Timber> logs = mockStatic(Timber.class)) {
            dao.when(AsrhDao::getMembers).thenThrow(failure);
            List<String> closedMembers = captureClosedMembers(dao);
            List<Throwable> errors = captureErrors(logs);

            runService();

            assertTrue(closedMembers.isEmpty());
            assertEquals(1, errors.size());
            assertSame(failure, errors.get(0));
            logs.verify(() -> Timber.e(failure));
            dao.verify(() -> AsrhDao.closeAsrhMemberFromRegister(anyString()), never());
        }
    }

    @Test
    public void memberClosureFailureRemainsLogged() {
        IllegalStateException failure = new IllegalStateException("Member closure failed");
        MemberObject olderMember = member("age-25", 25);
        try (MockedStatic<AsrhDao> dao = mockStatic(AsrhDao.class);
                MockedStatic<Timber> logs = mockStatic(Timber.class)) {
            List<String> attemptedMembers = new ArrayList<>();
            dao.when(AsrhDao::getMembers).thenReturn(Collections.singletonList(olderMember));
            dao.when(() -> AsrhDao.closeAsrhMemberFromRegister("age-25"))
                    .thenAnswer(invocation -> {
                        attemptedMembers.add(invocation.getArgument(0));
                        throw failure;
                    });
            List<Throwable> errors = captureErrors(logs);

            runService();

            assertEquals(Collections.singletonList("age-25"), attemptedMembers);
            assertEquals(1, errors.size());
            assertSame(failure, errors.get(0));
            logs.verify(() -> Timber.e(failure));
        }
    }

    private List<String> captureClosedMembers(MockedStatic<AsrhDao> dao) {
        List<String> closedMembers = new ArrayList<>();
        dao.when(() -> AsrhDao.closeAsrhMemberFromRegister(anyString()))
                .thenAnswer(invocation -> {
                    closedMembers.add(invocation.getArgument(0));
                    return null;
                });
        return closedMembers;
    }

    private List<Throwable> captureErrors(MockedStatic<Timber> logs) {
        List<Throwable> errors = new ArrayList<>();
        logs.when(() -> Timber.e(any(Throwable.class))).thenAnswer(invocation -> {
            errors.add(invocation.getArgument(0));
            return null;
        });
        return errors;
    }

    private MemberObject member(String baseEntityId, int age) {
        MemberObject member = mock(MemberObject.class);
        when(member.getBaseEntityId()).thenReturn(baseEntityId);
        when(member.getAge()).thenReturn(age);
        return member;
    }

    private void runService() {
        CloseAsrhMembershipIntentService service =
                mock(CloseAsrhMembershipIntentService.class, CALLS_REAL_METHODS);
        service.onHandleIntent(null);
    }
}
