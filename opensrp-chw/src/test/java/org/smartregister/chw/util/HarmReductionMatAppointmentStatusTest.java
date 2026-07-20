package org.smartregister.chw.util;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

public class HarmReductionMatAppointmentStatusTest {

    private static final LocalDate TODAY = new LocalDate(2026, 7, 20);

    @Test
    public void classifiesUpcomingDueAndMissedAppointments() {
        Assert.assertEquals(HarmReductionMatAppointmentStatus.Status.UPCOMING,
                HarmReductionMatAppointmentStatus.classify("2026-07-21", TODAY));
        Assert.assertEquals(HarmReductionMatAppointmentStatus.Status.DUE,
                HarmReductionMatAppointmentStatus.classify("2026-07-20", TODAY));
        Assert.assertEquals(HarmReductionMatAppointmentStatus.Status.MISSED,
                HarmReductionMatAppointmentStatus.classify("2026-07-19", TODAY));
    }

    @Test
    public void invalidOrMissingDatesDoNotShowAppointmentStatus() {
        Assert.assertEquals(HarmReductionMatAppointmentStatus.Status.NONE,
                HarmReductionMatAppointmentStatus.classify("", TODAY));
        Assert.assertEquals(HarmReductionMatAppointmentStatus.Status.NONE,
                HarmReductionMatAppointmentStatus.classify("20-07-2026", TODAY));
    }

    @Test
    public void formatsStoredDateForDisplay() {
        Assert.assertEquals("20-07-2026",
                HarmReductionMatAppointmentStatus.displayDate("2026-07-20"));
    }
}
