package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class LocalDayRegisterRefreshTest {
    private TimeZone previousZone;
    private ClockedRefresh refresh;
    private final AtomicInteger calls = new AtomicInteger();

    @Before
    public void setUp() throws Exception {
        previousZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        refresh = new ClockedRefresh(RuntimeEnvironment.getApplication(), calls::incrementAndGet);
        refresh.current = date("2026-10-28 23:59:59");
    }

    @After
    public void tearDown() {
        refresh.stop();
        TimeZone.setDefault(previousZone);
    }

    @Test
    public void visibleRegisterRefreshesAtLocalMidnightAndStopsWhenPaused() throws Exception {
        refresh.start();
        assertEquals(1, calls.get());
        refresh.current = date("2026-10-29 00:00:00");
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
        assertEquals(2, calls.get());
        refresh.stop();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.DAYS);
        assertEquals(2, calls.get());
        refresh.start();
        assertEquals(3, calls.get());
    }

    @Test
    public void clockDateAndTimezoneBroadcastsRefreshAndRescheduleWithoutDuplicateRegistration() {
        refresh.start();
        refresh.start();
        assertEquals(1, calls.get());
        for (String action : new String[] {Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED}) {
            RuntimeEnvironment.getApplication().sendBroadcast(new Intent(action));
            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }
        assertEquals(4, calls.get());
        refresh.stop();
        RuntimeEnvironment.getApplication().sendBroadcast(new Intent(Intent.ACTION_DATE_CHANGED));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(4, calls.get());
    }

    @Test
    public void nextLocalMidnightUsesCalendarDaysAcrossDaylightSavingChanges() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        assertEquals(TimeUnit.HOURS.toMillis(23),
                LocalDayRegisterRefresh.millisUntilNextDay(date("2026-03-08 00:00:00")));
        assertEquals(TimeUnit.HOURS.toMillis(25),
                LocalDayRegisterRefresh.millisUntilNextDay(date("2026-11-01 00:00:00")));
    }

    private Date date(String value) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(value);
    }

    private static class ClockedRefresh extends LocalDayRegisterRefresh {
        private Date current;

        ClockedRefresh(Context context, Runnable action) {
            super(context, action);
        }

        @Override
        Date now() {
            return current;
        }
    }
}
