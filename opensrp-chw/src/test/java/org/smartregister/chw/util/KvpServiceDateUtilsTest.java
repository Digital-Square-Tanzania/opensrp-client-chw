package org.smartregister.chw.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KvpServiceDateUtilsTest {

    @Test
    public void processedVisitDisablesButtonWithoutLocalVisit() {
        Date now = dateAt(2026, Calendar.AUGUST, 13, 15).getTime();

        assertTrue(KvpServiceDateUtils.hasVisitToday(now, null, now));
    }

    @Test
    public void localVisitDisablesButtonBeforeProcessedVisitIsAvailable() {
        Date visit = dateAt(2026, Calendar.AUGUST, 13, 8).getTime();
        Date now = dateAt(2026, Calendar.AUGUST, 13, 18).getTime();

        assertTrue(KvpServiceDateUtils.hasVisitToday(null, visit, now));
    }

    @Test
    public void visitBecomesAvailableOnNextCalendarDay() {
        Date visit = dateAt(2026, Calendar.AUGUST, 13, 23).getTime();
        Date now = dateAt(2026, Calendar.AUGUST, 14, 0).getTime();

        assertFalse(KvpServiceDateUtils.hasVisitToday(visit, visit, now));
    }

    @Test
    public void previousProcessedVisitDoesNotHideButtonWhenLocalVisitIsMissing() {
        Date processed = dateAt(2026, Calendar.AUGUST, 12, 10).getTime();
        Date now = dateAt(2026, Calendar.AUGUST, 13, 10).getTime();

        assertFalse(KvpServiceDateUtils.hasVisitToday(processed, null, now));
    }

    @Test
    public void missingVisitHistoryKeepsButtonAvailable() {
        assertFalse(KvpServiceDateUtils.hasVisitToday(null, null, new Date()));
    }

    private Calendar dateAt(int year, int month, int day, int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, hour, 0, 0);
        return calendar;
    }
}
