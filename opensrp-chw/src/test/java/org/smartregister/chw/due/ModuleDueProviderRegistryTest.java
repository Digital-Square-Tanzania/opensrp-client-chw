package org.smartregister.chw.due;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModuleDueProviderRegistryTest {
    private final DueQuery query = new DueQuery(new LocalDate(2026, 10, 27),
            Collections.singletonMap("allowed", "Allowed client"));

    @Test
    public void disabledModulesAreNotLoadedOrReported() {
        AtomicInteger calls = new AtomicInteger();
        ModuleDueServiceProvider provider = new ModuleDueServiceProvider() {
            @Override public String getModuleId() { return "HPS"; }
            @Override public ModuleDueResult load(DueQuery query) {
                calls.incrementAndGet();
                throw new AssertionError("Disabled module was queried");
            }
        };
        List<ModuleDueResult> result = new ModuleDueProviderRegistry(Collections.singletonList(provider))
                .load(query, Collections.emptySet());
        assertTrue(result.isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    public void missingProviderIsUnsupportedRatherThanCompleteZero() {
        List<ModuleDueResult> result = new ModuleDueProviderRegistry(Collections.emptyList())
                .load(query, Collections.singleton("AYP_IN_SCHOOL"));
        assertEquals("AYP_IN_SCHOOL", result.get(0).getModuleId());
        assertEquals(ModuleDueResult.Coverage.UNSUPPORTED, result.get(0).getCoverage());
        assertTrue(result.get(0).getItems().isEmpty());
    }

    @Test
    public void providerFailureDoesNotRemoveSuccessfulModule() {
        ModuleDueServiceProvider failing = new ModuleDueServiceProvider() {
            @Override public String getModuleId() { return "NCD"; }
            @Override public ModuleDueResult load(DueQuery query) { throw new IllegalStateException("unavailable"); }
        };
        ModuleDueResult hps = complete("HPS", item("HPS", "allowed"));
        List<ModuleDueResult> result = new ModuleDueProviderRegistry(Arrays.asList(failing, provider("HPS", hps)))
                .load(query, new HashSet<>(Arrays.asList("NCD", "HPS")));
        assertEquals("HPS", result.get(0).getModuleId());
        assertEquals(ModuleDueResult.Coverage.COMPLETE, result.get(0).getCoverage());
        assertEquals(1, result.get(0).getItems().size());
        assertEquals(ModuleDueResult.Coverage.ERROR, result.get(1).getCoverage());
    }

    @Test
    public void outOfCohortClientsAreRemovedAtTheRegistryBoundary() {
        ModuleDueResult loaded = complete("HPS", item("HPS", "allowed"), item("HPS", "hidden"));
        List<ModuleDueResult> result = registry(provider("HPS", loaded)).load(query, Collections.singleton("HPS"));
        assertEquals(1, result.get(0).getItems().size());
        assertEquals("allowed", result.get(0).getItems().get(0).getClientId());
    }

    @Test
    public void crossModuleResultIsReportedAsFailure() {
        List<ModuleDueResult> result = registry(provider("HPS", complete("NCD", item("NCD", "allowed"))))
                .load(query, Collections.singleton("HPS"));
        assertEquals("HPS", result.get(0).getModuleId());
        assertEquals(ModuleDueResult.Coverage.ERROR, result.get(0).getCoverage());
    }

    @Test
    public void crossModuleServiceCannotEscapeThroughCorrectResultLabel() {
        List<ModuleDueResult> result = registry(provider("HPS", complete("HPS", item("NCD", "allowed"))))
                .load(query, Collections.singleton("HPS"));
        assertEquals(ModuleDueResult.Coverage.ERROR, result.get(0).getCoverage());
        assertTrue(result.get(0).getItems().isEmpty());
    }

    @Test
    public void nullProviderResultIsAnError() {
        List<ModuleDueResult> result = registry(provider("HPS", null)).load(query, Collections.singleton("HPS"));
        assertEquals(ModuleDueResult.Coverage.ERROR, result.get(0).getCoverage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateModuleRegistrationFailsInsteadOfSilentlyReplacingProvider() {
        new ModuleDueProviderRegistry(Arrays.asList(provider("HPS", null), provider("HPS", null)));
    }

    @Test
    public void partialCoverageIsPreservedEvenWhenNoServicesAreDue() {
        ModuleDueResult partial = new ModuleDueResult("HPS", ModuleDueResult.Coverage.PARTIAL,
                Collections.emptyList(), Collections.singleton("followup"));
        ModuleDueResult result = registry(provider("HPS", partial)).load(query, Collections.singleton("HPS")).get(0);
        assertEquals(ModuleDueResult.Coverage.PARTIAL, result.getCoverage());
        assertEquals(Collections.singleton("followup"), result.getSupportedServiceCodes());
    }

    private ModuleDueProviderRegistry registry(ModuleDueServiceProvider provider) {
        return new ModuleDueProviderRegistry(Collections.singletonList(provider));
    }

    private ModuleDueServiceProvider provider(String id, ModuleDueResult result) {
        return new ModuleDueServiceProvider() {
            @Override public String getModuleId() { return id; }
            @Override public ModuleDueResult load(DueQuery query) { return result; }
        };
    }

    private ModuleDueResult complete(String module, DueServiceItem... items) {
        return ModuleDueResult.complete(module, Arrays.asList(items), Collections.singleton("followup"));
    }

    private DueServiceItem item(String module, String client) {
        return new DueServiceItem(module, client, client, "followup", client + ":followup",
                query.getDate(), query.getDate().plusDays(1), DueServiceItem.Status.DUE);
    }
}
