package org.smartregister.chw.due;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DueServicesSnapshotTest {
    private static final LocalDate TODAY = new LocalDate(2026, 10, 27);

    @Test
    public void sameClientAcrossModulesAppearsOnceWithEveryServiceReason() {
        DueServicesSnapshot snapshot = snapshot(query("client", "Asha"),
                result("HPS", item("HPS", "client", "home", TODAY, DueServiceItem.Status.DUE)),
                result("NCD", item("NCD", "client", "review", TODAY.minusDays(1), DueServiceItem.Status.OVERDUE)));

        assertTrue(snapshot.getClients(DueServiceItem.Status.DUE).isEmpty());
        List<DueClient> overdue = snapshot.getClients(DueServiceItem.Status.OVERDUE);
        assertEquals(1, overdue.size());
        assertEquals("client", overdue.get(0).getClientId());
        assertEquals("Asha", overdue.get(0).getName());
        assertEquals(2, overdue.get(0).getItems().size());
        assertEquals("NCD", overdue.get(0).getItems().get(0).getModuleId());
        assertEquals(DueServiceItem.Status.OVERDUE, overdue.get(0).getStatus());
    }

    @Test
    public void duplicateObligationIsCollapsedWithoutUsingDueDateAsIdentity() {
        DueServiceItem original = item("HPS", "client", "home", TODAY, DueServiceItem.Status.DUE);
        DueServiceItem duplicate = item("HPS", "client", "home", TODAY.plusDays(1), DueServiceItem.Status.DUE);
        DueServicesSnapshot snapshot = snapshot(query("client", "Asha"), result("HPS", duplicate, original, original));
        assertEquals(1, snapshot.getClients(DueServiceItem.Status.DUE).size());
        assertEquals(1, snapshot.getClients(DueServiceItem.Status.DUE).get(0).getItems().size());
        assertSame(original, snapshot.getClients(DueServiceItem.Status.DUE).get(0).getItems().get(0));
    }

    @Test
    public void differentModulesCanUseSameOccurrenceKeyWithoutLosingService() {
        DueServicesSnapshot snapshot = snapshot(query("client", "Asha"),
                result("AYP_IN_SCHOOL", item("AYP_IN_SCHOOL", "client", "review", TODAY, DueServiceItem.Status.DUE)),
                result("AYP_PARENTAL", item("AYP_PARENTAL", "client", "review", TODAY, DueServiceItem.Status.DUE)));
        assertEquals(1, snapshot.getClients(DueServiceItem.Status.DUE).size());
        assertEquals(2, snapshot.getClients(DueServiceItem.Status.DUE).get(0).getItems().size());
        assertEquals(2, snapshot.getResults().size());
    }

    @Test
    public void clientLocalOccurrenceKeysDoNotDropOtherClients() {
        DueServiceItem asha = new DueServiceItem("HPS", "asha", "asha:enrollment", "home",
                "next-visit", TODAY, TODAY.plusDays(1), DueServiceItem.Status.DUE);
        DueServiceItem baraka = new DueServiceItem("HPS", "baraka", "baraka:enrollment", "home",
                "next-visit", TODAY.minusDays(2), TODAY.minusDays(1), DueServiceItem.Status.OVERDUE);
        DueServicesSnapshot snapshot = snapshot(query("asha", "Asha", "baraka", "Baraka"),
                result("HPS", baraka, asha, asha));

        assertEquals(1, snapshot.getClients(DueServiceItem.Status.DUE).size());
        assertEquals("asha", snapshot.getClients(DueServiceItem.Status.DUE).get(0).getClientId());
        assertEquals(1, snapshot.getClients(DueServiceItem.Status.DUE).get(0).getItems().size());
        assertEquals(1, snapshot.getClients(DueServiceItem.Status.OVERDUE).size());
        assertEquals("baraka", snapshot.getClients(DueServiceItem.Status.OVERDUE).get(0).getClientId());
    }

    @Test
    public void currentlyDueServicesKeepModuleStatusRatherThanBeingReclassifiedByDate() {
        DueServicesSnapshot snapshot = snapshot(query("client", "Asha"), result("HPS",
                item("HPS", "client", "home", TODAY.minusDays(2), DueServiceItem.Status.DUE)));
        assertEquals(1, snapshot.getClients(DueServiceItem.Status.DUE).size());
        assertTrue(snapshot.getClients(DueServiceItem.Status.OVERDUE).isEmpty());
        assertEquals(TODAY, snapshot.getDate());
    }

    @Test
    public void clientOrderUsesOldestRelevantDateThenNameAndIsIndependentOfProviderOrder() {
        DueQuery query = query("z", "Zawadi", "b", "Baraka", "a", "Asha");
        ModuleDueResult result = result("HPS",
                item("HPS", "z", "home", TODAY.minusDays(5), DueServiceItem.Status.OVERDUE),
                item("HPS", "b", "home", TODAY.minusDays(1), DueServiceItem.Status.OVERDUE),
                item("HPS", "a", "home", TODAY.minusDays(1), DueServiceItem.Status.OVERDUE));
        DueServicesSnapshot snapshot = snapshot(query, result);
        assertEquals(Arrays.asList("z", "a", "b"), snapshot.getClients(DueServiceItem.Status.OVERDUE)
                .stream().map(DueClient::getClientId).collect(Collectors.toList()));
    }

    @Test
    public void clientOutsideAuthorizedScopeNeverAppearsInSnapshot() {
        DueServicesSnapshot snapshot = snapshot(query("allowed", "Asha"), result("HPS",
                item("HPS", "hidden", "home", TODAY, DueServiceItem.Status.DUE)));
        assertTrue(snapshot.getClients(DueServiceItem.Status.DUE).isEmpty());
    }

    @Test
    public void successWithZeroUnsupportedPartialAndErrorRemainDistinguishable() {
        ModuleDueResult zero = result("HPS");
        ModuleDueResult unsupported = ModuleDueResult.unsupported("AYP_IN_SCHOOL");
        ModuleDueResult partial = new ModuleDueResult("AYP_PARENTAL", ModuleDueResult.Coverage.PARTIAL,
                Collections.emptyList(), Collections.singleton("followup"));
        ModuleDueResult error = ModuleDueResult.error("NCD");
        DueServicesSnapshot snapshot = snapshot(query("client", "Asha"), zero, unsupported, partial, error);
        assertTrue(snapshot.getClients(DueServiceItem.Status.DUE).isEmpty());
        assertEquals(Arrays.asList(ModuleDueResult.Coverage.COMPLETE, ModuleDueResult.Coverage.UNSUPPORTED,
                        ModuleDueResult.Coverage.PARTIAL, ModuleDueResult.Coverage.ERROR),
                snapshot.getResults().stream().map(ModuleDueResult::getCoverage).collect(Collectors.toList()));
    }

    @Test
    public void callerMutationsDoNotChangeTheSnapshotCohortOrCoverage() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("client", "Asha");
        DueQuery query = new DueQuery(TODAY, names);
        names.clear();
        List<DueServiceItem> items = new ArrayList<>();
        items.add(item("HPS", "client", "home", TODAY, DueServiceItem.Status.DUE));
        Set<String> codes = new LinkedHashSet<>(Collections.singleton("home"));
        ModuleDueResult result = new ModuleDueResult("HPS", ModuleDueResult.Coverage.COMPLETE, items, codes);
        items.clear();
        codes.clear();
        List<ModuleDueResult> results = new ArrayList<>(Collections.singletonList(result));
        DueServicesSnapshot snapshot = DueServicesSnapshot.aggregate(query, results);
        results.clear();

        assertEquals(1, query.getClientNames().size());
        assertEquals(1, snapshot.getResults().size());
        assertEquals(Collections.singleton("home"), snapshot.getResults().get(0).getSupportedServiceCodes());
        assertEquals(1, snapshot.getClients(DueServiceItem.Status.DUE).get(0).getItems().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clientListIsImmutable() {
        snapshot(query("client", "Asha")).getClients(DueServiceItem.Status.DUE).clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void serviceListIsImmutable() {
        snapshot(query("client", "Asha"), result("HPS", item("HPS", "client", "home", TODAY,
                DueServiceItem.Status.DUE))).getClients(DueServiceItem.Status.DUE).get(0).getItems().clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failedProviderCannotPublishServicesAsIfSuccessful() {
        new ModuleDueResult("HPS", ModuleDueResult.Coverage.ERROR,
                Collections.singletonList(item("HPS", "client", "home", TODAY, DueServiceItem.Status.DUE)),
                Collections.singleton("home"));
    }

    private DueServicesSnapshot snapshot(DueQuery query, ModuleDueResult... results) {
        return DueServicesSnapshot.aggregate(query, Arrays.asList(results));
    }

    private DueQuery query(String... clientAndName) {
        Map<String, String> names = new LinkedHashMap<>();
        for (int i = 0; i < clientAndName.length; i += 2) names.put(clientAndName[i], clientAndName[i + 1]);
        return new DueQuery(TODAY, names);
    }

    private ModuleDueResult result(String module, DueServiceItem... items) {
        return ModuleDueResult.complete(module, Arrays.asList(items), Collections.singleton("home"));
    }

    private DueServiceItem item(String module, String client, String service,
                                LocalDate due, DueServiceItem.Status status) {
        return new DueServiceItem(module, client, client + ":enrollment", service, client + ":" + service,
                due, due.plusDays(1), status);
    }
}
