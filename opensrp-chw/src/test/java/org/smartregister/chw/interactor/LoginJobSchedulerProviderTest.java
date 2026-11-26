package org.smartregister.chw.interactor;

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
import org.smartregister.CoreLibrary;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.job.CloseAsrhMemberServiceJob;
import org.smartregister.job.BaseJob;
import org.smartregister.job.SyncServiceJob;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class LoginJobSchedulerProviderTest {

    @Test
    public void disabledAsrhDoesNotScheduleItsMembershipClosureJob() {
        assertAsrhScheduling(false);
    }

    @Test
    public void enabledAsrhStillSchedulesItsMembershipClosureJob() {
        assertAsrhScheduling(true);
    }

    private void assertAsrhScheduling(boolean asrhEnabled) {
        ChwApplication.Flavor flavor = mock(ChwApplication.Flavor.class);
        when(flavor.hasAsrh()).thenReturn(asrhEnabled);
        CoreLibrary coreLibrary = mock(CoreLibrary.class);
        try (MockedStatic<ChwApplication> application = mockStatic(ChwApplication.class);
                MockedStatic<CoreLibrary> core = mockStatic(CoreLibrary.class);
                MockedStatic<BaseJob> jobs = mockStatic(BaseJob.class)) {
            application.when(ChwApplication::getApplicationFlavor).thenReturn(flavor);
            core.when(CoreLibrary::getInstance).thenReturn(coreLibrary);

            new LoginJobSchedulerProvider().scheduleJobsImmediately();

            jobs.verify(() -> BaseJob.scheduleJobImmediately(SyncServiceJob.TAG));
            if (asrhEnabled) {
                jobs.verify(() -> BaseJob.scheduleJobImmediately(CloseAsrhMemberServiceJob.TAG));
            } else {
                jobs.verify(
                        () -> BaseJob.scheduleJobImmediately(CloseAsrhMemberServiceJob.TAG),
                        never());
            }
        }
    }
}
