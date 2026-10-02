package org.smartregister.chw.activity;

import android.app.Activity;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.application.TestChwApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Regression coverage for Play vitals crashes when a profile is restored after logout. */
@Config(sdk = 28, application = ProfileSessionExpiryTest.LoggedOutApplication.class)
public class ProfileSessionExpiryTest extends BaseUnitTest {

    @Test
    public void allClientsProfileRedirectsWithoutUsingUninitializedViews() {
        assertLoggedOutProfile(AllClientsMemberProfileActivity.class);
    }

    @Test
    public void familyMemberProfileRedirectsWithoutUsingUninitializedViews() {
        assertLoggedOutProfile(FamilyOtherMemberProfileActivity.class);
    }

    @Test
    public void aypOutSchoolProfileRedirectsWithoutUsingUninitializedViews() {
        assertLoggedOutProfile(AypOutSchoolMemberProfileActivity.class);
    }

    @Test
    public void ancProfileRedirectsWithoutUsingUninitializedMember() {
        assertLoggedOutProfile(AncMemberProfileActivity.class);
    }

    @Test
    public void prepProfileRedirectsWithoutUsingUninitializedViews() {
        assertLoggedOutProfile(KvpPrEPProfileActivity.class);
    }

    @Test
    public void childProfileRedirectsWithoutUsingUninitializedMember() {
        assertLoggedOutProfile(ChildProfileActivity.class);
    }

    private <T extends Activity> void assertLoggedOutProfile(Class<T> activityClass) {
        LoggedOutApplication application = (LoggedOutApplication) RuntimeEnvironment.getApplication();
        CoreLibrary.getInstance().context().userService().logoutSession();
        assertTrue(CoreLibrary.getInstance().context().IsUserLoggedOut());
        application.logoutCalls = 0;
        ActivityController<T> controller = Robolectric.buildActivity(activityClass).create();
        assertEquals(1, application.logoutCalls);
        assertNull(controller.get().findViewById(org.smartregister.chw.R.id.notification_and_referral_recycler_view));
        // Android may still deliver resume while the login redirect is being processed.
        controller.start().resume();
        assertEquals(2, application.logoutCalls);
        controller.pause().stop().destroy();
    }

    public static class LoggedOutApplication extends TestChwApplication {
        int logoutCalls;

        @Override
        public void logoutCurrentUser() {
            // Keep the redirect deterministic without starting another activity in this test.
            logoutCalls++;
        }
    }
}
