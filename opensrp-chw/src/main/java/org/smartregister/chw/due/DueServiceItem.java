package org.smartregister.chw.due;

import org.joda.time.LocalDate;

import java.util.Objects;

/** An outstanding service, evaluated by its module's own scheduling rules. */
public final class DueServiceItem {
    public enum Status { DUE, OVERDUE }

    private final String moduleId;
    private final String clientId;
    private final String sourceEntityId;
    private final String serviceCode;
    private final String occurrenceKey;
    private final LocalDate dueDate;
    private final LocalDate overdueFrom;
    private final Status status;

    /** occurrenceKey must identify this client's obligation within the module, even after rescheduling. */
    public DueServiceItem(String moduleId, String clientId, String sourceEntityId,
                          String serviceCode, String occurrenceKey, LocalDate dueDate,
                          LocalDate overdueFrom, Status status) {
        this.moduleId = Objects.requireNonNull(moduleId);
        this.clientId = Objects.requireNonNull(clientId);
        this.sourceEntityId = Objects.requireNonNull(sourceEntityId);
        this.serviceCode = Objects.requireNonNull(serviceCode);
        this.occurrenceKey = Objects.requireNonNull(occurrenceKey);
        this.dueDate = Objects.requireNonNull(dueDate);
        this.overdueFrom = overdueFrom;
        this.status = Objects.requireNonNull(status);
    }

    public String getModuleId() { return moduleId; }
    public String getClientId() { return clientId; }
    public String getSourceEntityId() { return sourceEntityId; }
    public String getServiceCode() { return serviceCode; }
    public String getOccurrenceKey() { return occurrenceKey; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getOverdueFrom() { return overdueFrom; }
    public Status getStatus() { return status; }
}
