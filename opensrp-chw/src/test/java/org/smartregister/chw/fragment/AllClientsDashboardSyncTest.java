package org.smartregister.chw.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.os.Looper;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.smartregister.chw.R;
import org.smartregister.chw.core.job.CoreBasePncCloseJob;
import org.smartregister.chw.core.job.HomeVisitServiceJob;
import org.smartregister.chw.core.job.VaccineRecurringServiceJob;
import org.smartregister.domain.FetchStatus;
import org.smartregister.job.BaseJob;
import org.smartregister.job.ImageUploadServiceJob;
import org.smartregister.job.PullUniqueIdsServiceJob;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.job.SyncTaskServiceJob;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;

/** Verifies the dashboard sync action also works in a host with no navigation drawer. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class AllClientsDashboardSyncTest {
    private ActivityController<FragmentActivity> controller;
    private RefreshTrackingFragment fragment;
    private SyncStatusBroadcastReceiver receiver;
    private MockedStatic<SyncStatusBroadcastReceiver> syncReceiver;

    @Before
    public void setUp() {
        receiver = mock(SyncStatusBroadcastReceiver.class);
        syncReceiver = mockStatic(SyncStatusBroadcastReceiver.class);
        syncReceiver.when(SyncStatusBroadcastReceiver::getInstance).thenReturn(receiver);
        controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.Theme_AppCompat_NoActionBar);
        controller.create().start().resume().visible();
        fragment = new RefreshTrackingFragment();
        controller.get().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment).commitNow();
    }

    @After
    public void tearDown() {
        try {
            if (controller != null) controller.pause().stop().destroy();
        } finally {
            if (syncReceiver != null) syncReceiver.close();
        }
    }

    @Test
    public void syncActionSchedulesTheExistingSevenJobsWithoutADrawer() {
        try (MockedStatic<BaseJob> jobs = mockStatic(BaseJob.class)) {
            syncButton().performClick();

            jobs.verify(() -> BaseJob.scheduleJobImmediately(CoreBasePncCloseJob.TAG));
            jobs.verify(() -> BaseJob.scheduleJobImmediately(HomeVisitServiceJob.TAG));
            jobs.verify(() -> BaseJob.scheduleJobImmediately(VaccineRecurringServiceJob.TAG));
            jobs.verify(() -> BaseJob.scheduleJobImmediately(ImageUploadServiceJob.TAG));
            jobs.verify(() -> BaseJob.scheduleJobImmediately(SyncServiceJob.TAG));
            jobs.verify(() -> BaseJob.scheduleJobImmediately(PullUniqueIdsServiceJob.TAG));
            jobs.verify(() -> BaseJob.scheduleJobImmediately(SyncTaskServiceJob.TAG));
            jobs.verifyNoMoreInteractions();
        }
        assertEquals(controller.get().getString(R.string.action_start_sync),
                ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void receiverStateDisablesSyncWhileRunningAndRestoresTheAction() {
        when(receiver.isSyncing()).thenReturn(true);
        fragment.onSyncStart();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(syncButton().isEnabled());
        assertEquals(fragment.getString(R.string.all_clients_dashboard_syncing),
                syncButton().getText().toString());

        when(receiver.isSyncing()).thenReturn(false);
        fragment.onSyncInProgress(FetchStatus.nothingFetched);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(syncButton().isEnabled());
        assertEquals(fragment.getString(R.string.all_clients_dashboard_sync),
                syncButton().getText().toString());
    }

    @Test
    public void syncFailureRefreshesLocalDataAndReportsFailure() {
        int refreshes = fragment.refreshes;

        fragment.onSyncComplete(FetchStatus.fetchedFailed);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(refreshes + 1, fragment.refreshes);
        assertEquals(fragment.getString(R.string.all_clients_dashboard_sync_failed),
                ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void offlineSyncCompletionReportsFailureInsteadOfSuccess() {
        int refreshes = fragment.refreshes;

        fragment.onSyncComplete(FetchStatus.noConnection);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(refreshes + 1, fragment.refreshes);
        assertEquals(fragment.getString(R.string.all_clients_dashboard_sync_failed),
                ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void syncListenerIsRemovedWhenDashboardStops() {
        verify(receiver).addSyncStatusListener(fragment);

        controller.get().getSupportFragmentManager().beginTransaction()
                .remove(fragment).commitNow();

        verify(receiver).removeSyncStatusListener(fragment);
    }

    private TextView syncButton() {
        return fragment.requireView().findViewById(R.id.dashboard_sync);
    }

    public static class RefreshTrackingFragment extends AllClientsDashboardFragment {
        int refreshes;

        @Override
        public void onViewCreated(View view, Bundle state) {
            // This host intentionally has no drawer; isolate sync from the host toolbar hook.
        }

        @Override
        public void refreshDashboard() {
            refreshes++;
        }
    }
}
