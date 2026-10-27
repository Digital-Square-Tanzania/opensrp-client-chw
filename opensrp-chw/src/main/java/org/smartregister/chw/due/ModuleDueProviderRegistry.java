package org.smartregister.chw.due;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Explicit app composition keeps module implementations independent of the dashboard UI. */
public final class ModuleDueProviderRegistry {
    private final Map<String, ModuleDueServiceProvider> providers = new LinkedHashMap<>();

    public ModuleDueProviderRegistry(List<ModuleDueServiceProvider> providers) {
        for (ModuleDueServiceProvider provider : providers) {
            String moduleId = provider.getModuleId();
            if (moduleId == null || this.providers.put(moduleId, provider) != null) {
                throw new IllegalArgumentException("Providers must have unique module IDs");
            }
        }
    }

    public List<ModuleDueResult> load(DueQuery query, Set<String> enabledModuleIds) {
        List<ModuleDueResult> results = new ArrayList<>();
        for (String moduleId : new TreeSet<>(enabledModuleIds)) {
            ModuleDueServiceProvider provider = providers.get(moduleId);
            if (provider == null) {
                results.add(ModuleDueResult.unsupported(moduleId));
                continue;
            }
            try {
                ModuleDueResult result = provider.load(query);
                if (result == null || !moduleId.equals(result.getModuleId())) {
                    throw new IllegalArgumentException("Provider returned a different module");
                }
                List<DueServiceItem> authorized = new ArrayList<>();
                for (DueServiceItem item : result.getItems()) {
                    if (item == null || !moduleId.equals(item.getModuleId())) {
                        throw new IllegalArgumentException("Service belongs to a different module");
                    }
                    if (query.getClientNames().containsKey(item.getClientId())) authorized.add(item);
                }
                results.add(new ModuleDueResult(moduleId, result.getCoverage(), authorized,
                        result.getSupportedServiceCodes()));
            } catch (RuntimeException exception) {
                // A failed module must not remove successful modules or become an empty success.
                results.add(ModuleDueResult.error(moduleId));
            }
        }
        return Collections.unmodifiableList(results);
    }
}
