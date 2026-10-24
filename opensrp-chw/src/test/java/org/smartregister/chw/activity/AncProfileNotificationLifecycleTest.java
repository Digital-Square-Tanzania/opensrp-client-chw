package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.activity.BaseAncMemberProfileActivity;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.application.TestChwApplication;
import org.smartregister.chw.core.adapter.NotificationListAdapter;
import org.smartregister.chw.core.listener.OnRetrieveNotifications;
import org.smartregister.chw.core.utils.ChwNotificationUtil;
import org.smartregister.chw.shadows.ContextShadow;

/** Preserves the real secured/Core/app creation order while isolating clinical data loading. */
@Config(
        application = AncProfileNotificationLifecycleTest.SessionApplication.class,
        shadows = {
            AncProfileNotificationLifecycleTest.SessionContext.class,
            AncProfileNotificationLifecycleTest.AncClinicalHooks.class
        })
public class AncProfileNotificationLifecycleTest extends BaseUnitTest {
    private static final String MEMBER_ID = "anc-profile-test-member";
    private ActivityController<AncMemberProfileActivity> controller;
    private MockedStatic<ChwNotificationUtil> notifications;

    @Before
    public void setUp() {
        SessionContext.loggedOut = false;
        AncClinicalHooks.creationCalls = 0;
        notifications = Mockito.mockStatic(ChwNotificationUtil.class);
    }

    @After
    public void tearDown() {
        try {
            if (controller != null) controller.destroy();
        } finally {
            notifications.close();
        }
    }

    @Test
    public void authenticatedCreationBindsNotificationsAfterCoreFindsTheView() {
        create(null);

        assertBound();
        assertEquals(1, AncClinicalHooks.creationCalls);
    }

    @Test
    public void savedStateRecreationBindsANewAdapterToTheNewProfileView() {
        create(null);
        NotificationListAdapter original = adapter();
        Bundle state = new Bundle();
        controller.saveInstanceState(state).destroy();
        controller = null;

        create(state);

        assertBound();
        assertTrue(original != adapter());
        assertEquals(2, AncClinicalHooks.creationCalls);
    }

    @Test
    public void expiredCreationSkipsProfileInflationAndAdapterSetup() {
        SessionContext.loggedOut = true;

        create(null);

        assertNull(controller.get().findViewById(R.id.notification_and_referral_recycler_view));
        assertEquals(0, AncClinicalHooks.creationCalls);
        assertTrue(application().logoutCount > 0);
        notifications.verifyNoInteractions();
    }

    @Test
    public void absentOptionalNotificationViewDoesNotCrashInitialization() {
        create(null);
        AncMemberProfileActivity activity = controller.get();
        activity.setContentView(new FrameLayout(activity));

        activity.initializeNotificationReferralRecyclerView();

        assertNull(activity.findViewById(R.id.notification_and_referral_recycler_view));
    }

    @Test
    public void notificationsStillRefreshForTheCurrentMemberAfterBinding() {
        create(null);
        assertBound();

        controller.get().onResumption();

        notifications.verify(
                () ->
                        ChwNotificationUtil.retrieveNotifications(
                                anyBoolean(), eq(MEMBER_ID), eq((OnRetrieveNotifications) controller.get())));
        assertTrue(adapter().canOpen);
    }

    private void create(Bundle state) {
        controller = Robolectric.buildActivity(AncMemberProfileActivity.class, profileIntent());
        controller.get().setTheme(org.smartregister.family.R.style.FamilyTheme_NoActionBar);
        controller.create(state);
    }

    private Intent profileIntent() {
        return new Intent().putExtra(Constants.ANC_MEMBER_OBJECTS.BASE_ENTITY_ID, MEMBER_ID);
    }

    private NotificationListAdapter adapter() {
        return ReflectionHelpers.getField(controller.get(), "notificationListAdapter");
    }

    private void assertBound() {
        RecyclerView recycler =
                controller.get().findViewById(R.id.notification_and_referral_recycler_view);
        assertNotNull(recycler);
        assertTrue(recycler.getLayoutManager() instanceof LinearLayoutManager);
        assertSame(adapter(), recycler.getAdapter());
        assertSame(controller.get(), ReflectionHelpers.getField(adapter(), "onClickListener"));
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

        @Override
        public boolean hasReferrals() {
            return false;
        }

        @Override
        public boolean hasADDO() {
            return false;
        }
    }

    @Implements(Context.class)
    public static class SessionContext extends ContextShadow {
        private static boolean loggedOut;

        @SuppressWarnings("PMD.MethodNamingConventions")
        @Implementation
        public boolean IsUserLoggedOut() {
            return loggedOut;
        }
    }

    @Implements(BaseAncMemberProfileActivity.class)
    public static class AncClinicalHooks extends ShadowActivity {
        @RealObject private BaseAncMemberProfileActivity activity;
        private static int creationCalls;

        @Implementation
        protected void onCreation() {
            creationCalls++;
            activity.setContentView(R.layout.content_anc_profile);
            MemberObject member = new MemberObject();
            member.setBaseEntityId(MEMBER_ID);
            ReflectionHelpers.setField(activity, "baseEntityID", MEMBER_ID);
            ReflectionHelpers.setField(activity, "memberObject", member);
        }

        @Implementation
        protected void onResumption() {
            // Clinical presenter refresh is outside this view-initialization regression.
        }
    }
}
