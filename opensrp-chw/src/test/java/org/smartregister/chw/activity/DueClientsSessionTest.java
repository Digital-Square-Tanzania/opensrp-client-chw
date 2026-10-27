package org.smartregister.chw.activity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.application.TestChwApplication;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Preserves the register's existing secured lifecycle rather than replacing session checks. */
@Config(sdk = 28, application = DueClientsSessionTest.SessionApplication.class)
public class DueClientsSessionTest extends BaseUnitTest {
    private ActivityController<DueClientsActivity> controller;

    @Before public void setUp() {
        // Exercise SecuredActivity against the real session, without a second Context shadow.
        // Session.hasExpired() also requires a non-null in-memory password, as after login.
        CoreLibrary.getInstance().context().session().setPassword("unit-test-session")
                .start(TimeUnit.MINUTES.toMillis(10));
        assertFalse(CoreLibrary.getInstance().context().IsUserLoggedOut());
    }

    @After public void tearDown() {
        if (controller != null) controller.destroy();
    }

    @Test public void expiredSessionRedirectsWithoutCreatingOrLoadingTheRegister() {
        CoreLibrary.getInstance().context().userService().logoutSession();
        assertTrue(CoreLibrary.getInstance().context().IsUserLoggedOut());
        SessionApplication application = (SessionApplication) RuntimeEnvironment.getApplication();
        controller = Robolectric.buildActivity(DueClientsActivity.class).create();
        assertEquals(1, application.logoutCalls);
        assertNull(controller.get().findViewById(R.id.due_register_list));
        controller.start().resume();
        assertEquals(2, application.logoutCalls);
        assertNull(controller.get().findViewById(R.id.due_register_list));
        controller.pause().stop();
    }

    @Test public void logoutEventClosesAnAuthenticatedRegister() {
        controller = Robolectric.buildActivity(DueClientsActivity.class).create();
        assertNotNull(controller.get().findViewById(R.id.due_register_list));
        CoreLibrary.getInstance().context().userService().logoutSession();
        assertTrue(CoreLibrary.getInstance().context().IsUserLoggedOut());
        assertTrue(controller.get().isFinishing());
    }

    public static class SessionApplication extends TestChwApplication {
        int logoutCalls;
        @Override public void logoutCurrentUser() { logoutCalls++; }
    }

}
