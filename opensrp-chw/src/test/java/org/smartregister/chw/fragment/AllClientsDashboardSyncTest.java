package org.smartregister.chw.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.os.Looper;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
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
import org.robolectric.shadows.ShadowNetworkInfo;
import org.smartregister.chw.R;
import org.smartregister.chw.due.DueServicesChangedEvent;
import org.greenrobot.eventbus.EventBus;
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
import org.smartregister.repository.AllSharedPreferences;

import java.text.DateFormat;
import java.util.Date;

import java.util.concurrent.TimeUnit;

/** Verifies the dashboard sync action also works in a host with no navigation drawer. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class AllClientsDashboardSyncTest {
    private ActivityController<FragmentActivity> controller;
    private RefreshTrackingFragment fragment;
    private SyncStatusBroadcastReceiver receiver;
    private MockedStatic<SyncStatusBroadcastReceiver> syncReceiver;
    private AllSharedPreferences preferences;
    private ConnectivityManager connectivity;
    private static final long LAST_SUCCESS = 1600000000000L;

    @Before
    public void setUp() {
        receiver = mock(SyncStatusBroadcastReceiver.class);
        syncReceiver = mockStatic(SyncStatusBroadcastReceiver.class);
        syncReceiver.when(SyncStatusBroadcastReceiver::getInstance).thenReturn(receiver);
        controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.Theme_AppCompat_NoActionBar);
        controller.create().start().resume().visible();
        PreferenceManager.getDefaultSharedPreferences(controller.get()).edit().clear().commit();
        preferences = new AllSharedPreferences(PreferenceManager.getDefaultSharedPreferences(controller.get()));
        preferences.updateLastCheckTimeStamp(LAST_SUCCESS);
        connectivity = (ConnectivityManager) controller.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        setConnected(true);
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
    public void showsTheExistingSuccessfulSyncTimestampWithoutClaimingServerVersionIsADate() {
        preferences.saveLastSyncDate(LAST_SUCCESS + 1000000000L);
        fragment.onSyncInProgress(FetchStatus.nothingFetched);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(lastSuccessText(LAST_SUCCESS), lastSuccessLabel().getText().toString());
        assertEquals(View.GONE, statusLabel().getVisibility());
    }

    @Test
    public void missingSuccessfulSyncHasAnExplicitEmptyState() {
        preferences.updateLastCheckTimeStamp(0L);
        fragment.onSyncInProgress(FetchStatus.nothingFetched);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(fragment.getString(R.string.all_clients_dashboard_sync_never),
                lastSuccessLabel().getText().toString());
    }

    @Test
    public void offlineStatusRespondsToConnectivityChangesAndKeepsLastSuccessVisible() {
        setConnected(false);
        broadcastConnectivityChange();

        assertEquals(View.VISIBLE, statusLabel().getVisibility());
        assertEquals(fragment.getString(R.string.all_clients_dashboard_sync_offline),
                statusLabel().getText().toString());
        assertEquals(lastSuccessText(LAST_SUCCESS), lastSuccessLabel().getText().toString());
        assertTrue(syncButton().isEnabled());

        setConnected(true);
        broadcastConnectivityChange();
        assertEquals(View.GONE, statusLabel().getVisibility());
    }

    @Test
    public void failureRemainsVisibleAcrossFragmentRecreationAndOffersRetry() {
        fragment.onSyncComplete(FetchStatus.fetchedFailed);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Fragment.SavedState state = controller.get().getSupportFragmentManager()
                .saveFragmentInstanceState(fragment);
        controller.get().getSupportFragmentManager().beginTransaction().remove(fragment).commitNow();
        fragment = new RefreshTrackingFragment();
        fragment.setInitialSavedState(state);
        controller.get().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment).commitNow();

        assertEquals(View.VISIBLE, statusLabel().getVisibility());
        assertEquals(fragment.getString(R.string.all_clients_dashboard_sync_failed),
                statusLabel().getText().toString());
        assertEquals(fragment.getString(R.string.all_clients_dashboard_sync_retry),
                syncButton().getText().toString());
        assertEquals(lastSuccessText(LAST_SUCCESS), lastSuccessLabel().getText().toString());
        assertEquals(LAST_SUCCESS, preferences.fetchLastCheckTimeStamp());
    }

    @Test
    public void successfulCompletionClearsFailureWithoutWaitingForTheTimestampWrite() {
        fragment.onSyncComplete(FetchStatus.fetchedFailed);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        fragment.onSyncComplete(FetchStatus.nothingFetched);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(View.GONE, statusLabel().getVisibility());
        assertEquals(fragment.getString(R.string.all_clients_dashboard_sync),
                syncButton().getText().toString());
        assertFalse(lastSuccessText(LAST_SUCCESS).equals(lastSuccessLabel().getText().toString()));
        // The dashboard never writes or fabricates the canonical sync metadata.
        assertEquals(LAST_SUCCESS, preferences.fetchLastCheckTimeStamp());
    }

    @Test
    public void laterSuccessfulBackgroundSyncClearsTheEarlierErrorOnReturn() {
        fragment.onSyncComplete(FetchStatus.fetchedFailed);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        long laterSuccess = System.currentTimeMillis() + 60000L;
        preferences.updateLastCheckTimeStamp(laterSuccess);
        fragment.onSyncInProgress(FetchStatus.nothingFetched);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(View.GONE, statusLabel().getVisibility());
        assertEquals(lastSuccessText(laterSuccess), lastSuccessLabel().getText().toString());
    }

    @Test
    public void syncListenerIsRemovedWhenDashboardStops() {
        verify(receiver).addSyncStatusListener(fragment);

        controller.get().getSupportFragmentManager().beginTransaction()
                .remove(fragment).commitNow();

        verify(receiver).removeSyncStatusListener(fragment);
    }

    @Test
    public void localDateChangeRefreshesDueServiceCounts() {
        int refreshes = fragment.refreshes;
        controller.get().sendBroadcast(new Intent(Intent.ACTION_DATE_CHANGED));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(refreshes + 1, fragment.refreshes);
    }

    @Test
    public void processedServiceChangesRefreshOnlyWhileDashboardIsStarted() {
        int refreshes = fragment.refreshes;
        EventBus.getDefault().post(new DueServicesChangedEvent());
        EventBus.getDefault().post(new DueServicesChangedEvent());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS);
        assertEquals(refreshes + 1, fragment.refreshes);

        controller.get().getSupportFragmentManager().beginTransaction().remove(fragment).commitNow();
        EventBus.getDefault().post(new DueServicesChangedEvent());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS);
        assertEquals(refreshes + 1, fragment.refreshes);
    }

    private TextView syncButton() {
        return fragment.requireView().findViewById(R.id.dashboard_sync);
    }

    private TextView statusLabel() {
        return fragment.requireView().findViewById(R.id.dashboard_sync_status);
    }

    private TextView lastSuccessLabel() {
        return fragment.requireView().findViewById(R.id.dashboard_sync_last_success);
    }

    private String lastSuccessText(long timestamp) {
        return fragment.getString(R.string.all_clients_dashboard_sync_last_success,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                        fragment.getResources().getConfiguration().locale).format(new Date(timestamp)));
    }

    private void setConnected(boolean connected) {
        Shadows.shadowOf(connectivity).setActiveNetworkInfo(ShadowNetworkInfo.newInstance(
                connected ? NetworkInfo.DetailedState.CONNECTED : NetworkInfo.DetailedState.DISCONNECTED,
                ConnectivityManager.TYPE_WIFI, 0, connected,
                connected ? NetworkInfo.State.CONNECTED : NetworkInfo.State.DISCONNECTED));
    }

    private void broadcastConnectivityChange() {
        controller.get().sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
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
