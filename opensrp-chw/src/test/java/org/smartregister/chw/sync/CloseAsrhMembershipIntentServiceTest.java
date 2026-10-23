package org.smartregister.chw.sync;

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

import java.util.Arrays;
import java.util.Collections;

import timber.log.Timber;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class CloseAsrhMembershipIntentServiceTest {

    @Test
    public void nullMemberListFinishesWithoutClosingMembersOrLoggingAnError() {
        try (MockedStatic<AsrhDao> dao = mockStatic(AsrhDao.class);
                MockedStatic<Timber> logs = mockStatic(Timber.class)) {
            dao.when(AsrhDao::getMembers).thenReturn(null);

            runService();

            dao.verify(() -> AsrhDao.closeAsrhMemberFromRegister(anyString()), never());
            logs.verifyNoInteractions();
        }
    }

    @Test
    public void emptyMemberListFinishesWithoutClosingMembersOrLoggingAnError() {
        try (MockedStatic<AsrhDao> dao = mockStatic(AsrhDao.class);
                MockedStatic<Timber> logs = mockStatic(Timber.class)) {
            dao.when(AsrhDao::getMembers).thenReturn(Collections.emptyList());

            runService();

            dao.verify(() -> AsrhDao.closeAsrhMemberFromRegister(anyString()), never());
            logs.verifyNoInteractions();
        }
    }

    @Test
    public void closesOnlyMembersOlderThanTwentyFour() {
        MemberObject adolescent = member("adolescent", 19);
        MemberObject eligibleBoundary = member("age-24", 24);
        MemberObject olderMember = member("age-25", 25);
        try (MockedStatic<AsrhDao> dao = mockStatic(AsrhDao.class)) {
            dao.when(AsrhDao::getMembers)
                    .thenReturn(Arrays.asList(adolescent, eligibleBoundary, olderMember));

            runService();

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

            runService();

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
            dao.when(AsrhDao::getMembers).thenReturn(Collections.singletonList(olderMember));
            dao.when(() -> AsrhDao.closeAsrhMemberFromRegister("age-25")).thenThrow(failure);

            runService();

            logs.verify(() -> Timber.e(failure));
        }
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
