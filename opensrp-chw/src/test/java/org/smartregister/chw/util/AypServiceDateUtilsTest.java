package org.smartregister.chw.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AypServiceDateUtilsTest {

    @Test
    public void processedServiceDisablesButtonWithoutLocalVisit() {
        assertTrue(AypServiceDateUtils.hasServiceToday(true, null, new Date()));
    }

    @Test
    public void localVisitDisablesButtonBeforeProcessedEventIsAvailable() {
        Calendar visit = dateAt(2026, Calendar.AUGUST, 4, 8);
        Calendar now = dateAt(2026, Calendar.AUGUST, 4, 18);

        assertTrue(AypServiceDateUtils.hasServiceToday(false, visit.getTime(), now.getTime()));
    }

    @Test
    public void serviceBecomesAvailableOnNextDay() {
        Calendar visit = dateAt(2026, Calendar.AUGUST, 4, 23);
        Calendar now = dateAt(2026, Calendar.AUGUST, 5, 0);

        assertFalse(AypServiceDateUtils.hasServiceToday(false, visit.getTime(), now.getTime()));
    }

    @Test
    public void missingServiceHistoryKeepsButtonAvailable() {
        assertFalse(AypServiceDateUtils.hasServiceToday(false, null, new Date()));
    }

    private Calendar dateAt(int year, int month, int day, int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, hour, 0, 0);
        return calendar;
    }
}
