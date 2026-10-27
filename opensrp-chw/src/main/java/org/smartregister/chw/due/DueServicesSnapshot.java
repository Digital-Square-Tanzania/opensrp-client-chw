package org.smartregister.chw.due;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The same immutable snapshot supplies card counts and client-register rows. */
public final class DueServicesSnapshot {
    private static final Comparator<DueServiceItem> SERVICE_ORDER = Comparator
            .comparingInt((DueServiceItem item) -> item.getStatus() == DueServiceItem.Status.OVERDUE ? 0 : 1)
            .thenComparing(item -> item.getStatus() == DueServiceItem.Status.OVERDUE
                    && item.getOverdueFrom() != null ? item.getOverdueFrom() : item.getDueDate())
            .thenComparing(DueServiceItem::getDueDate)
            .thenComparing(DueServiceItem::getModuleId)
            .thenComparing(DueServiceItem::getServiceCode)
            .thenComparing(DueServiceItem::getOccurrenceKey);

    private final LocalDate date;
    private final List<ModuleDueResult> results;
    private final Map<DueServiceItem.Status, List<DueClient>> clients;

    private DueServicesSnapshot(DueQuery query, List<ModuleDueResult> results) {
        this.date = query.getDate();
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        Map<List<String>, DueServiceItem> unique = new LinkedHashMap<>();
        for (ModuleDueResult result : results) {
            if (result.getCoverage() != ModuleDueResult.Coverage.COMPLETE
                    && result.getCoverage() != ModuleDueResult.Coverage.PARTIAL) continue;
            for (DueServiceItem item : result.getItems()) {
                if (!query.getClientNames().containsKey(item.getClientId())) continue;
                List<String> key = Arrays.asList(item.getModuleId(), item.getClientId(), item.getOccurrenceKey());
                DueServiceItem previous = unique.get(key);
                if (previous == null || SERVICE_ORDER.compare(item, previous) < 0) unique.put(key, item);
            }
        }
        Map<String, List<DueServiceItem>> byClient = new LinkedHashMap<>();
        for (DueServiceItem item : unique.values()) {
            byClient.computeIfAbsent(item.getClientId(), ignored -> new ArrayList<>()).add(item);
        }
        List<DueClient> people = new ArrayList<>();
        for (Map.Entry<String, List<DueServiceItem>> entry : byClient.entrySet()) {
            entry.getValue().sort(SERVICE_ORDER);
            people.add(new DueClient(entry.getKey(), query.getClientNames().get(entry.getKey()), entry.getValue()));
        }
        people.sort(Comparator.comparingInt((DueClient person) ->
                        person.getStatus() == DueServiceItem.Status.OVERDUE ? 0 : 1)
                .thenComparing(person -> relevantDate(person.getItems().get(0)))
                .thenComparing(DueClient::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DueClient::getClientId));
        clients = new EnumMap<>(DueServiceItem.Status.class);
        for (DueServiceItem.Status status : DueServiceItem.Status.values()) {
            List<DueClient> bucket = new ArrayList<>();
            for (DueClient person : people) if (person.getStatus() == status) bucket.add(person);
            clients.put(status, Collections.unmodifiableList(bucket));
        }
    }

    private static LocalDate relevantDate(DueServiceItem item) {
        return item.getStatus() == DueServiceItem.Status.OVERDUE && item.getOverdueFrom() != null
                ? item.getOverdueFrom() : item.getDueDate();
    }

    public static DueServicesSnapshot aggregate(DueQuery query, List<ModuleDueResult> results) {
        return new DueServicesSnapshot(query, results);
    }

    public LocalDate getDate() { return date; }
    public List<ModuleDueResult> getResults() { return results; }
    public List<DueClient> getClients(DueServiceItem.Status status) { return clients.get(status); }
}
