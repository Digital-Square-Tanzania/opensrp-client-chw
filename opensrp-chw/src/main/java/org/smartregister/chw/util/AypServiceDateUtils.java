package org.smartregister.chw.util;

import java.util.Calendar;
import java.util.Date;

public final class AypServiceDateUtils {

    private AypServiceDateUtils() {
    }

    public static boolean hasServiceToday(boolean processedServiceToday, Date latestVisitDate,
                                          Date currentDate) {
        return processedServiceToday || isSameDay(latestVisitDate, currentDate);
    }

    private static boolean isSameDay(Date firstDate, Date secondDate) {
        if (firstDate == null || secondDate == null) {
            return false;
        }

        Calendar firstCalendar = Calendar.getInstance();
        firstCalendar.setTime(firstDate);
        Calendar secondCalendar = Calendar.getInstance();
        secondCalendar.setTime(secondDate);

        return firstCalendar.get(Calendar.ERA) == secondCalendar.get(Calendar.ERA)
                && firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                && firstCalendar.get(Calendar.DAY_OF_YEAR) == secondCalendar.get(Calendar.DAY_OF_YEAR);
    }
}
