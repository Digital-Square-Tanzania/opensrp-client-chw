package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.application.TestChwApplication;
import org.smartregister.chw.core.activity.CoreTbLeprosyProfileActivity;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.malaria.contract.MalariaProfileContract;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.chw.shadows.ContextShadow;
import org.smartregister.chw.tbleprosy.contract.TbLeprosyProfileContract;
import org.smartregister.chw.tbleprosy.dao.TbLeprosyDao;
import org.smartregister.chw.util.NotificationsUtil;
import org.smartregister.view.activity.BaseProfileActivity;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Tests the concrete app activities, including their adapter and resume callbacks. */
@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(application = ProfileSessionLifecycleTest.SessionApplication.class,
        shadows = {ProfileSessionLifecycleTest.SessionContext.class,
                ProfileSessionLifecycleTest.MalariaUi.class,
                ProfileSessionLifecycleTest.TbPresenter.class,
                ProfileSessionLifecycleTest.TbUi.class})
public class ProfileSessionLifecycleTest extends BaseUnitTest {
    private static final String MEMBER_ID = "session-test-member";

    @ParameterizedRobolectricTestRunner.Parameter
    public Class<? extends BaseProfileActivity> activityClass;

    private ActivityController<? extends BaseProfileActivity> controller;
    private MockedStatic<MalariaDao> malariaDao;
    private MockedStatic<TbLeprosyDao> tbDao;
    private MockedStatic<ChwNotificationUtil> notifications;
    private MockedStatic<org.smartregister.util.Utils> utilities;
    private Object presenter;
    private boolean created;

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static Collection<Object[]> activities() {
        return Arrays.asList(new Object[][]{{MalariaProfileActivity.class}, {TbLeprosyProfileActivity.class}});
    }

    @Before
    public void setUp() {
        SessionContext.loggedOut = false;
        TbUi.buttonRefreshes = 0;
        malariaDao = Mockito.mockStatic(MalariaDao.class);
        tbDao = Mockito.mockStatic(TbLeprosyDao.class);
        notifications = Mockito.mockStatic(ChwNotificationUtil.class);
        utilities = Mockito.mockStatic(org.smartregister.util.Utils.class, Mockito.CALLS_REAL_METHODS);
        utilities.when(() -> org.smartregister.util.Utils.startAsyncTask(any(), any())).thenAnswer(call -> null);

        org.smartregister.chw.malaria.domain.MemberObject malaria = new org.smartregister.chw.malaria.domain.MemberObject();
        malaria.setBaseEntityId(MEMBER_ID);
        malaria.setFamilyName("Test family");
        malariaDao.when(() -> MalariaDao.getMember(MEMBER_ID)).thenReturn(malaria);

        org.smartregister.chw.tbleprosy.domain.MemberObject tb = new org.smartregister.chw.tbleprosy.domain.MemberObject();
        tb.setBaseEntityId(MEMBER_ID);
        tb.setFamilyName("Test family");
        tbDao.when(() -> TbLeprosyDao.getMember(MEMBER_ID)).thenReturn(tb);
    }

    @After
    public void tearDown() {
        try {
            if (controller != null && created) {
                controller.pause().stop().destroy();
            }
        } finally {
            if (utilities != null) utilities.close();
            if (notifications != null) notifications.close();
            if (tbDao != null) tbDao.close();
            if (malariaDao != null) malariaDao.close();
        }
    }

    @Test
    public void expiredCreationSkipsAdapterAndNotifications() {
        SessionContext.loggedOut = true;
        create(profileIntent(), null);
        controller.start().resume();
        drainRefreshes();

        assertNull(controller.get().findViewById(R.id.notification_and_referral_recycler_view));
        assertNull(ReflectionHelpers.getField(controller.get(), "memberObject"));
        assertTrue(application().logoutCount > 0);
        notifications.verifyNoInteractions();
    }

    @Test
    public void expiredCreationWithoutPayloadDoesNotReadMember() {
        SessionContext.loggedOut = true;
        create(new Intent(), null);
        controller.start().resume();
        drainRefreshes();

        malariaDao.verify(() -> MalariaDao.getMember(any()), never());
        tbDao.verify(() -> TbLeprosyDao.getMember(any()), never());
        notifications.verifyNoInteractions();
        assertTrue(application().logoutCount > 0);
    }

    @Test
    public void authenticatedCreationBindsAdapterAndRefreshesNotifications() {
        create(profileIntent(), null);
        assertAdapterBound();
        TextView title = controller.get().findViewById(R.id.toolbar_title);
        assertEquals(controller.get().getString(R.string.return_to_family_name, "Test family"), title.getText().toString());
        controller.start().resume();
        drainRefreshes();

        notifications.verify(() -> ChwNotificationUtil.retrieveNotifications(anyBoolean(), eq(MEMBER_ID), eq((OnRetrieveNotifications) controller.get())));
        assertTrue(adapter().canOpen);
        if (presenter instanceof TbLeprosyProfileContract.Presenter) {
            verify((TbLeprosyProfileContract.Presenter) presenter).refreshProfileBottom();
        }
    }

    @Test
    public void logoutBeforePendingRefreshSkipsAppWork() {
        create(profileIntent(), null);
        controller.start().resume();
        notifications.clearInvocations();
        Mockito.clearInvocations(presenter);
        tbDao.clearInvocations();
        int buttonRefreshes = TbUi.buttonRefreshes;
        SessionContext.loggedOut = true;
        drainRefreshes();

        assertEquals(buttonRefreshes, TbUi.buttonRefreshes);
        tbDao.verify(TbLeprosyDao::closeTbNegativeClients, never());

        notifications.verifyNoInteractions();
        if (presenter instanceof TbLeprosyProfileContract.Presenter) {
            verify((TbLeprosyProfileContract.Presenter) presenter, never()).refreshProfileBottom();
        }
    }

    @Test
    public void destroyedActivitySkipsPendingAppRefresh() {
        create(profileIntent(), null);
        controller.start().resume();
        notifications.clearInvocations();
        Mockito.clearInvocations(presenter);
        tbDao.clearInvocations();
        controller.pause().stop().destroy();
        created = false;
        drainRefreshes();

        notifications.verifyNoInteractions();
        tbDao.verify(TbLeprosyDao::closeTbNegativeClients, never());
        if (presenter instanceof TbLeprosyProfileContract.Presenter) {
            verify((TbLeprosyProfileContract.Presenter) presenter, never()).refreshProfileBottom();
        }
    }

    @Test
    public void finishingActivitySkipsPendingAppRefresh() {
        create(profileIntent(), null);
        controller.start().resume();
        notifications.clearInvocations();
        Mockito.clearInvocations(presenter);
        tbDao.clearInvocations();
        controller.get().finish();
        drainRefreshes();

        notifications.verifyNoInteractions();
        tbDao.verify(TbLeprosyDao::closeTbNegativeClients, never());
        if (presenter instanceof TbLeprosyProfileContract.Presenter) {
            verify((TbLeprosyProfileContract.Presenter) presenter, never()).refreshProfileBottom();
        }
    }

    @Test
    public void resumeAfterLogoutDoesNotRequestNotifications() {
        create(profileIntent(), null);
        controller.start().resume();
        drainRefreshes();
        controller.pause();
        notifications.clearInvocations();
        SessionContext.loggedOut = true;
        controller.resume();
        drainRefreshes();

        assertTrue(application().logoutCount > 0);
        notifications.verifyNoInteractions();
    }

    @Test
    public void savedStateRecreationBindsNewAdapterToSameMember() {
        create(profileIntent(), null);
        controller.start().resume();
        drainRefreshes();
        Bundle savedState = new Bundle();
        controller.pause().saveInstanceState(savedState).stop().destroy();
        notifications.clearInvocations();
        create(profileIntent(), savedState);
        controller.start().resume();
        drainRefreshes();

        assertAdapterBound();
        notifications.verify(() -> ChwNotificationUtil.retrieveNotifications(anyBoolean(), eq(MEMBER_ID), eq((OnRetrieveNotifications) controller.get())));
    }

    @Test
    public void restoredNotificationClickUsesItsOwnMemberAfterAnotherProfileOpens() {
        create(profileIntent(), null);
        controller.start().resume();
        drainRefreshes();
        Bundle savedState = new Bundle();
        controller.pause().saveInstanceState(savedState).stop().destroy();

        Activity launcher = mock(Activity.class);
        Mockito.when(launcher.getPackageName()).thenReturn(application().getPackageName());
        MalariaProfileActivity.startMalariaActivity(launcher, "different-member");
        create(profileIntent(), savedState);
        RecyclerView recycler = controller.get().findViewById(R.id.notification_and_referral_recycler_view);
        assertSame(adapter(), recycler.getAdapter());
        View row = new View(controller.get());
        try (MockedStatic<NotificationsUtil> clicks = Mockito.mockStatic(NotificationsUtil.class)) {
            controller.get().onClick(row);
            clicks.verify(() -> NotificationsUtil.handleNotificationRowClick(
                    controller.get(), row, adapter(), MEMBER_ID));
        }
    }

    @Test
    public void savedStateRecreationAfterLogoutSkipsAdapterSetup() {
        create(profileIntent(), null);
        controller.start().resume();
        drainRefreshes();
        Bundle savedState = new Bundle();
        controller.pause().saveInstanceState(savedState).stop().destroy();
        notifications.clearInvocations();
        SessionContext.loggedOut = true;
        create(profileIntent(), savedState);
        controller.start().resume();
        drainRefreshes();

        assertNull(controller.get().findViewById(R.id.notification_and_referral_recycler_view));
        notifications.verifyNoInteractions();
    }

    private void create(Intent intent, Bundle savedState) {
        created = false;
        controller = Robolectric.buildActivity(activityClass, intent);
        BaseProfileActivity activity = controller.get();
        presenter = activityClass == MalariaProfileActivity.class
                ? mock(MalariaProfileContract.Presenter.class) : mock(TbLeprosyProfileContract.Presenter.class);
        ReflectionHelpers.setField(activity, "profilePresenter", presenter);
        activity.setTheme(org.smartregister.family.R.style.FamilyTheme_NoActionBar);
        controller.create(savedState);
        created = true;
    }

    private Intent profileIntent() {
        return new Intent().putExtra("BASE_ENTITY_ID", MEMBER_ID)
                .putExtra(CoreConstants.INTENT_KEY.TOOLBAR_TITLE, R.string.return_to_family_name);
    }

    private NotificationListAdapter adapter() {
        return ReflectionHelpers.getField(controller.get(), "notificationListAdapter");
    }

    private void assertAdapterBound() {
        RecyclerView recycler = controller.get().findViewById(R.id.notification_and_referral_recycler_view);
        assertNotNull(recycler);
        assertSame(adapter(), recycler.getAdapter());
        assertSame(controller.get(), ReflectionHelpers.getField(adapter(), "onClickListener"));
    }

    private void drainRefreshes() {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
    }

    private SessionApplication application() {
        return (SessionApplication) RuntimeEnvironment.getApplication();
    }

    public static class SessionApplication extends TestChwApplication {
        private int logoutCount;

        @Override
        public void logoutCurrentUser() {
            logoutCount++;
        }
    }

    @Implements(Context.class)
    public static class SessionContext extends ContextShadow {
        private static boolean loggedOut;

        // Preserve the exact legacy API name required by this Robolectric shadow.
        @SuppressWarnings("PMD.MethodNamingConventions")
        @Implementation
        public boolean IsUserLoggedOut() {
            return loggedOut;
        }
    }

    // Keep real lifecycle, member lookup, layout inflation and adapter setup; isolate business hooks.
    @Implements(MalariaProfileActivity.class)
    public static class MalariaUi extends ShadowActivity {
        @Implementation
        protected void initializePresenter() {
            // The test injects a presenter before running the real lifecycle.
        }

        @Implementation
        public void initializeFloatingMenu() {
            // Clinical menu services are outside this lifecycle regression.
        }

        @Implementation
        public void recordAnc(org.smartregister.chw.malaria.domain.MemberObject member) {
            // ANC services are outside this lifecycle regression.
        }

        @Implementation
        public void recordPnc(org.smartregister.chw.malaria.domain.MemberObject member) {
            // PNC services are outside this lifecycle regression.
        }
    }

    @Implements(CoreTbLeprosyProfileActivity.class)
    public static class TbPresenter extends ShadowActivity {
        @Implementation
        protected void initializePresenter() {
            // The test injects a presenter before running the real lifecycle.
        }
    }

    @Implements(TbLeprosyProfileActivity.class)
    public static class TbUi extends TbPresenter {
        private static int buttonRefreshes;

        @Implementation
        public void initializeFloatingMenu() {
            // Clinical menu services are outside this lifecycle regression.
        }

        @Implementation
        protected void setupButtons() {
            buttonRefreshes++;
        }
    }
}
