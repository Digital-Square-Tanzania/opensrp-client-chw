package org.smartregister.chw.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public final class HarmReductionMatAppointmentStatus {

    private static final DateTimeFormatter STORED_DATE = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormat.forPattern("dd-MM-yyyy");

    public enum Status {
        NONE,
        UPCOMING,
        DUE,
        MISSED
    }

    private HarmReductionMatAppointmentStatus() {
    }

    public static Status classify(String value, LocalDate today) {
        LocalDate appointmentDate = parse(value);
        if (appointmentDate == null || today == null) {
            return Status.NONE;
        }
        if (appointmentDate.isBefore(today)) {
            return Status.MISSED;
        }
        if (appointmentDate.isEqual(today)) {
            return Status.DUE;
        }
        return Status.UPCOMING;
    }

    public static String displayDate(String value) {
        LocalDate appointmentDate = parse(value);
        return appointmentDate == null ? value : DISPLAY_DATE.print(appointmentDate);
    }

    private static LocalDate parse(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return STORED_DATE.parseLocalDate(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
