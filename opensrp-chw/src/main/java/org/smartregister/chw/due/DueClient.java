package org.smartregister.chw.due;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One person in the worklist, retaining every qualifying service across modules. */
public final class DueClient {
    private final String clientId;
    private final String name;
    private final List<DueServiceItem> items;
    private final DueServiceItem.Status status;

    DueClient(String clientId, String name, List<DueServiceItem> items) {
        this.clientId = clientId;
        this.name = name == null ? "" : name;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.status = items.stream().anyMatch(item -> item.getStatus() == DueServiceItem.Status.OVERDUE)
                ? DueServiceItem.Status.OVERDUE : DueServiceItem.Status.DUE;
    }

    public String getClientId() { return clientId; }
    public String getName() { return name; }
    public List<DueServiceItem> getItems() { return items; }
    public DueServiceItem.Status getStatus() { return status; }
}
