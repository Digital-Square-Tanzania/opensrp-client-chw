package org.smartregister.chw.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.Calendar;
import java.util.Date;

/** Requeries date-dependent register rows only while their activity is in the foreground. */
public class LocalDayRegisterRefresh {
    private final Context context;
    private final Runnable refresh;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean started;
    private final Runnable refreshAndSchedule = this::refreshAndSchedule;

    private void refreshAndSchedule() {
        if (!started) return;
        refresh.run();
        handler.removeCallbacks(refreshAndSchedule);
        handler.postDelayed(refreshAndSchedule, millisUntilNextDay(now()));
    }

    private final BroadcastReceiver dateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAndSchedule.run();
        }
    };

    public LocalDayRegisterRefresh(Context context, Runnable refresh) {
        this.context = context;
        this.refresh = refresh;
    }

    public void start() {
        if (started) return;
        started = true;
        IntentFilter filter = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        context.registerReceiver(dateChanged, filter);
        refreshAndSchedule.run();
    }

    public void stop() {
        if (!started) return;
        started = false;
        handler.removeCallbacks(refreshAndSchedule);
        context.unregisterReceiver(dateChanged);
    }

    Date now() {
        return new Date();
    }

    static long millisUntilNextDay(Date now) {
        Calendar next = Calendar.getInstance();
        next.setTime(now);
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        return next.getTimeInMillis() - now.getTime();
    }

    public static void refreshAttachedRegisters(FragmentManager manager) {
        for (Fragment fragment : manager.getFragments()) {
            if (!fragment.isAdded() || fragment.getView() == null) continue;
            if (fragment instanceof BaseRegisterFragment) {
                BaseRegisterFragment register = (BaseRegisterFragment) fragment;
                register.filterandSortExecute(register.countBundle());
            }
            refreshAttachedRegisters(fragment.getChildFragmentManager());
        }
    }
}
