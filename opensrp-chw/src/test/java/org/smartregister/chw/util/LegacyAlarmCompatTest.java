package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPendingIntent;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {23, 31}, manifest = Config.NONE)
public class LegacyAlarmCompatTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void jobPendingIntentKeepsIdentityPayloadAndExistingFlags() {
        Intent intent = new Intent("job")
                .setClassName(context, "com.evernote.android.job.v14.PlatformAlarmReceiver")
                .putExtra("jobId", 41);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT;

        PendingIntent pending = LegacyAlarmCompat.getImmutableBroadcast(context, 41, intent, flags);
        ShadowPendingIntent shadow = Shadows.shadowOf(pending);

        assertEquals(41, shadow.getRequestCode());
        assertEquals(flags | PendingIntent.FLAG_IMMUTABLE, shadow.getFlags());
        assertTrue(shadow.isImmutable());
        assertEquals(intent.getComponent(), shadow.getSavedIntent().getComponent());
        assertEquals(41, shadow.getSavedIntent().getIntExtra("jobId", -1));
    }

    @Test
    public void noCreateLookupDoesNotCreateATokenAndFindsAnExistingImmutableToken() {
        Intent intent = new Intent("lookup").setPackage(context.getPackageName());
        assertNull(LegacyAlarmCompat.getImmutableBroadcast(
                context, 72, intent, PendingIntent.FLAG_NO_CREATE));

        PendingIntent created = LegacyAlarmCompat.getImmutableBroadcast(
                context, 72, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        assertSame(created, LegacyAlarmCompat.getImmutableBroadcast(
                context, 72, intent, PendingIntent.FLAG_NO_CREATE));
    }

    @Test
    public void passiveLookupFindsLegacyOneShotWithoutCreatingOrMutatingTokens() {
        Intent original = jobIntent("legacy-lookup").putExtra("payload", "original");
        int legacyFlags = legacyMutabilityFlag() | PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_ONE_SHOT;
        PendingIntent legacy = PendingIntent.getBroadcast(context, 73, original, legacyFlags);
        Intent newer = new Intent(original).putExtra("payload", "must-not-replace");

        PendingIntent lookup = LegacyAlarmCompat.getImmutableBroadcast(context, 73, newer,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_ONE_SHOT
                        | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_CANCEL_CURRENT);

        assertSame(legacy, lookup);
        assertFalse(Shadows.shadowOf(legacy).isCanceled());
        assertEquals("original", Shadows.shadowOf(legacy).getSavedIntent().getStringExtra("payload"));
        assertNull(PendingIntent.getBroadcast(context, 73, original,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE));
    }

    @Test
    public void passiveLookupPrefersImmutableTokenWithoutRetiringLegacyToken() {
        Intent intent = jobIntent("prefer-current");
        PendingIntent legacy = PendingIntent.getBroadcast(
                context, 74, intent, legacyMutabilityFlag());
        PendingIntent immutable = PendingIntent.getBroadcast(
                context, 74, intent, PendingIntent.FLAG_IMMUTABLE);

        assertSame(immutable, LegacyAlarmCompat.getImmutableBroadcast(
                context, 74, intent, PendingIntent.FLAG_NO_CREATE));
        assertFalse(Shadows.shadowOf(legacy).isCanceled());
        assertFalse(Shadows.shadowOf(immutable).isCanceled());
    }

    @Test
    public void replacementRetiresOnlyMatchingLegacyAlarmAndKeepsJobIdentityAndSchedule() {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = jobIntent("replace-legacy");
        PendingIntent oldOneShot = PendingIntent.getBroadcast(context, 75, intent,
                legacyMutabilityFlag() | PendingIntent.FLAG_ONE_SHOT);
        PendingIntent repeating = PendingIntent.getBroadcast(
                context, 75, intent, legacyMutabilityFlag());
        PendingIntent otherJob = PendingIntent.getBroadcast(context, 76, intent,
                legacyMutabilityFlag() | PendingIntent.FLAG_ONE_SHOT);
        long trigger = System.currentTimeMillis() + 60000;
        // ShadowAlarmManager 4.8.2 compares only Intent.filterEquals + requestCode,
        // ignoring ONE_SHOT and mutability. Keep the other-mode token unscheduled;
        // its cancellation state still verifies that migration leaves it untouched.
        manager.set(AlarmManager.RTC, trigger, oldOneShot);
        manager.set(AlarmManager.RTC, trigger + 1000, otherJob);

        PendingIntent replacement = LegacyAlarmCompat.getImmutableBroadcast(context, 75, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        assertTrue(Shadows.shadowOf(oldOneShot).isCanceled());
        assertFalse(Shadows.shadowOf(repeating).isCanceled());
        assertFalse(Shadows.shadowOf(otherJob).isCanceled());
        assertEquals(1, Shadows.shadowOf(manager).getScheduledAlarms().size());
        assertTrue(Shadows.shadowOf(manager).getScheduledAlarms().stream()
                .anyMatch(alarm -> alarm.operation == otherJob && alarm.triggerAtTime == trigger + 1000));
        assertEquals(75, Shadows.shadowOf(replacement).getRequestCode());
        assertEquals(intent.getComponent(), Shadows.shadowOf(replacement).getSavedIntent().getComponent());
        assertTrue(Shadows.shadowOf(replacement).isImmutable());
        // Upstream retains responsibility for the unchanged alarm timing.
        manager.set(AlarmManager.RTC, trigger, replacement);
        assertEquals(2, Shadows.shadowOf(manager).getScheduledAlarms().size());
        assertTrue(Shadows.shadowOf(manager).getScheduledAlarms().stream()
                .anyMatch(alarm -> alarm.operation == replacement && alarm.triggerAtTime == trigger));
        assertTrue(Shadows.shadowOf(manager).getScheduledAlarms().stream()
                .anyMatch(alarm -> alarm.operation == otherJob && alarm.triggerAtTime == trigger + 1000));
    }

    private Intent jobIntent(String action) {
        return new Intent(action).setClassName(context,
                "com.evernote.android.job.v14.PlatformAlarmReceiver");
    }

    private int legacyMutabilityFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
    }

    @Test
    public void telemetryBroadcastIsPrivateWithoutMutatingTheOriginalIntent() {
        Intent original = new Intent("com.mapbox.scheduler_flusher").putExtra("payload", "retained");
        PendingIntent pending = LegacyAlarmCompat.getPrivateImmutableBroadcast(
                context, 0, original, PendingIntent.FLAG_UPDATE_CURRENT);
        ShadowPendingIntent shadow = Shadows.shadowOf(pending);

        assertNull(original.getPackage());
        assertEquals(context.getPackageName(), shadow.getSavedIntent().getPackage());
        assertEquals(original.getAction(), shadow.getSavedIntent().getAction());
        assertEquals("retained", shadow.getSavedIntent().getStringExtra("payload"));
        assertEquals(0, shadow.getRequestCode());
        assertTrue(shadow.isImmutable());
    }

    @Test
    public void privateLocationTokenRetainsMutableFillInResultDelivery() throws Exception {
        String action = "com.mapbox.android.telemetry.location.locationupdatespendingintent.action.LOCATION_UPDATED";
        final String[] received = {null};
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ignored, Intent intent) {
                received[0] = intent.getStringExtra("location-result");
            }
        };
        LegacyAlarmCompat.registerPrivateReceiver(context, receiver, new IntentFilter(action));
        Intent original = new Intent(action);
        PendingIntent pending = LegacyAlarmCompat.getPrivateMutableBroadcast(
                context, 0, original, PendingIntent.FLAG_UPDATE_CURRENT);
        ShadowPendingIntent shadow = Shadows.shadowOf(pending);
        int expectedFlags = PendingIntent.FLAG_UPDATE_CURRENT | legacyMutabilityFlag();
        assertEquals(expectedFlags, shadow.getFlags());
        assertFalse(shadow.isImmutable());
        assertNull(original.getPackage());
        assertEquals(context.getPackageName(), shadow.getSavedIntent().getPackage());

        pending.send(context, 0, new Intent().putExtra("location-result", "delivered"));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals("delivered", received[0]);
        context.unregisterReceiver(receiver);
    }

    @Test
    public void missingLegacyAlarmIsSkipped() {
        AlarmManager manager = mock(AlarmManager.class);
        LegacyAlarmCompat.cancelIfPresent(manager, null);
        verifyNoInteractions(manager);
    }

    @Test
    public void missingFirstAlarmDoesNotPreventCancellingTheSecond() {
        AlarmManager manager = mock(AlarmManager.class);
        PendingIntent present = LegacyAlarmCompat.getImmutableBroadcast(
                context, 90, new Intent("cancel"), PendingIntent.FLAG_UPDATE_CURRENT);

        LegacyAlarmCompat.cancelIfPresent(manager, null);
        LegacyAlarmCompat.cancelIfPresent(manager, present);

        verify(manager).cancel(present);
    }

    @Test
    public void optionalNetworkMetadataKeepsAllowedPlatformValues() {
        TelephonyManager manager = mock(TelephonyManager.class);
        when(manager.getNetworkType()).thenReturn(
                TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_NR);

        assertEquals(TelephonyManager.NETWORK_TYPE_LTE, LegacyAlarmCompat.getOptionalNetworkType(manager));
        assertEquals("Newer values remain available to upstream mapping", TelephonyManager.NETWORK_TYPE_NR,
                LegacyAlarmCompat.getOptionalNetworkType(manager));
        verify(manager, times(2)).getNetworkType();
    }

    @Test
    public void deniedOptionalNetworkMetadataReturnsTheUpstreamUnknownValue() {
        TelephonyManager manager = mock(TelephonyManager.class);
        when(manager.getNetworkType()).thenThrow(new SecurityException("Phone-state access denied"));

        assertEquals(TelephonyManager.NETWORK_TYPE_UNKNOWN, LegacyAlarmCompat.getOptionalNetworkType(manager));
        verify(manager).getNetworkType();
    }

    @Test
    public void unrelatedNetworkMetadataFailuresRemainVisible() {
        TelephonyManager manager = mock(TelephonyManager.class);
        UnsupportedOperationException failure = new UnsupportedOperationException("No telephony radio");
        when(manager.getNetworkType()).thenThrow(failure);

        try {
            LegacyAlarmCompat.getOptionalNetworkType(manager);
            fail("Only denied access to optional metadata should be handled");
        } catch (UnsupportedOperationException actual) {
            assertSame(failure, actual);
        }
        verify(manager).getNetworkType();
    }

    @Test
    public void legacyReceiverRegistrationRetainsTheStickyResult() {
        Context recordingContext = mock(Context.class);
        BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        IntentFilter filter = new IntentFilter("telemetry");
        Intent sticky = new Intent("sticky");
        org.mockito.Mockito.when(recordingContext.registerReceiver(receiver, filter)).thenReturn(sticky);

        assertSame(sticky, LegacyAlarmCompat.registerPrivateReceiver(
                recordingContext, receiver, filter, 23));
        verify(recordingContext).registerReceiver(receiver, filter);
    }

    @Test
    @Config(sdk = 31)
    public void modernReceiverRegistrationUsesNotExportedAndRetainsTheStickyResult() {
        // The three-argument platform overload exists since API 26. Use a recording Context
        // to exercise the API 33 branch while retaining the project's Robolectric version.
        Context recordingContext = mock(Context.class);
        BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        IntentFilter filter = new IntentFilter("telemetry");
        Intent sticky = new Intent("sticky");
        org.mockito.Mockito.when(recordingContext.registerReceiver(
                receiver, filter, Context.RECEIVER_NOT_EXPORTED)).thenReturn(sticky);

        assertSame(sticky, LegacyAlarmCompat.registerPrivateReceiver(
                recordingContext, receiver, filter, 33));
        verify(recordingContext).registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Test
    public void privateTelemetryActionIsDeliveredAndReceiverCanBeUnregistered() throws Exception {
        final boolean[] delivered = {false};
        String action = "com.mapbox.scheduler_flusher";
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ignored, Intent intent) {
                delivered[0] = action.equals(intent.getAction());
            }
        };
        LegacyAlarmCompat.registerPrivateReceiver(context, receiver, new IntentFilter(action));
        PendingIntent pending = LegacyAlarmCompat.getPrivateImmutableBroadcast(
                context, 1, new Intent(action), PendingIntent.FLAG_UPDATE_CURRENT);
        assertNotNull(pending);

        pending.send();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue(delivered[0]);

        context.unregisterReceiver(receiver);
        delivered[0] = false;
        pending.send();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(delivered[0]);
    }
}
