package org.smartregister.chw.util;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.telephony.TelephonyManager;

/** Platform calls used by the pinned job and telemetry dependencies; see the compatibility guide. */
public final class LegacyAlarmCompat {
    private LegacyAlarmCompat() {}

    public static PendingIntent getImmutableBroadcast(
            Context context, int requestCode, Intent intent, int flags) {
        // The app supports API 23+, where FLAG_IMMUTABLE is available. A token's
        // mutability and ONE_SHOT flag participate in its identity.
        if ((flags & PendingIntent.FLAG_NO_CREATE) != 0) {
            int lookupFlags = (flags | PendingIntent.FLAG_IMMUTABLE)
                    & ~(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent current = PendingIntent.getBroadcast(context, requestCode, intent, lookupFlags);
            return current != null ? current : findLegacyJobAlarm(context, requestCode, intent, flags);
        }

        // Retire only this explicit job alarm before upstream schedules its immutable
        // replacement. This also lets both old cleanup calls cancel pre-upgrade tokens.
        PendingIntent legacy = findLegacyJobAlarm(context, requestCode, intent, flags);
        if (legacy != null) {
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (manager != null) manager.cancel(legacy);
            legacy.cancel();
        }
        return PendingIntent.getBroadcast(context, requestCode, intent,
                flags | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent getPrivateImmutableBroadcast(
            Context context, int requestCode, Intent intent, int flags) {
        // Telemetry's alarm is consumed by its receiver in this app, not by other packages.
        Intent privateIntent = new Intent(intent).setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, requestCode, privateIntent,
                flags | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent getPrivateMutableBroadcast(
            Context context, int requestCode, Intent intent, int flags) {
        // The location engine must fill in result extras when sending this token.
        // Package scoping retains a private dynamic receiver without discarding those results.
        Intent privateIntent = new Intent(intent).setPackage(context.getPackageName());
        int mutableFlags = flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mutableFlags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, privateIntent, mutableFlags);
    }

    private static PendingIntent findLegacyJobAlarm(
            Context context, int requestCode, Intent intent, int flags) {
        // JobProxy14 supplies an explicit PlatformAlarmReceiver component. Never use
        // this migration lookup for arbitrary implicit broadcasts such as telemetry.
        if (intent.getComponent() == null) return null;
        int lookupFlags = PendingIntent.FLAG_NO_CREATE | (flags & PendingIntent.FLAG_ONE_SHOT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            lookupFlags |= PendingIntent.FLAG_MUTABLE;
        }
        // No UPDATE_CURRENT/CANCEL_CURRENT: this lookup cannot create or mutate a token.
        return PendingIntent.getBroadcast(context, requestCode, intent, lookupFlags);
    }

    public static void cancelIfPresent(AlarmManager manager, PendingIntent pendingIntent) {
        if (pendingIntent != null) {
            manager.cancel(pendingIntent);
        }
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    public static int getOptionalNetworkType(TelephonyManager manager) {
        try {
            // Keep the API 23-compatible upstream getter and its value when access is allowed.
            return manager.getNetworkType();
        } catch (SecurityException denied) {
            // Mapbox already maps this value to "Unknown" for optional cellular metadata.
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    public static Intent registerPrivateReceiver(
            Context context, BroadcastReceiver receiver, IntentFilter filter) {
        return registerPrivateReceiver(context, receiver, filter, Build.VERSION.SDK_INT);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    static Intent registerPrivateReceiver(
            Context context, BroadcastReceiver receiver, IntentFilter filter, int sdk) {
        if (sdk >= Build.VERSION_CODES.TIRAMISU) {
            return context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
        return context.registerReceiver(receiver, filter);
    }
}
