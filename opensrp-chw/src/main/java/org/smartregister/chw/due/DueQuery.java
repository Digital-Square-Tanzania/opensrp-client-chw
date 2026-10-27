package org.smartregister.chw.due;

import org.joda.time.LocalDate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** One local evaluation date and the canonical, authorized All Clients cohort. */
public final class DueQuery {
    private final LocalDate date;
    private final Map<String, String> clientNames;

    public DueQuery(LocalDate date, Map<String, String> clientNames) {
        this.date = Objects.requireNonNull(date);
        this.clientNames = Collections.unmodifiableMap(new LinkedHashMap<>(clientNames));
    }

    public LocalDate getDate() { return date; }
    public Map<String, String> getClientNames() { return clientNames; }
}
