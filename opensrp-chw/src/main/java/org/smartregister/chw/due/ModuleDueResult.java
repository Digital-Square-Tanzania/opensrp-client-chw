package org.smartregister.chw.due;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Coverage is explicit: an unsupported or failed module must never appear as a complete zero. */
public final class ModuleDueResult {
    public enum Coverage { COMPLETE, PARTIAL, UNSUPPORTED, ERROR }

    private final String moduleId;
    private final Coverage coverage;
    private final List<DueServiceItem> items;
    private final Set<String> supportedServiceCodes;

    public ModuleDueResult(String moduleId, Coverage coverage, List<DueServiceItem> items,
                           Set<String> supportedServiceCodes) {
        this.moduleId = Objects.requireNonNull(moduleId);
        this.coverage = Objects.requireNonNull(coverage);
        if ((coverage == Coverage.UNSUPPORTED || coverage == Coverage.ERROR) && !items.isEmpty()) {
            throw new IllegalArgumentException("Unsupported or failed results cannot contain services");
        }
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.supportedServiceCodes = Collections.unmodifiableSet(new LinkedHashSet<>(supportedServiceCodes));
    }

    public static ModuleDueResult complete(String moduleId, List<DueServiceItem> items,
                                           Set<String> supportedServiceCodes) {
        return new ModuleDueResult(moduleId, Coverage.COMPLETE, items, supportedServiceCodes);
    }

    public static ModuleDueResult unsupported(String moduleId) {
        return new ModuleDueResult(moduleId, Coverage.UNSUPPORTED,
                Collections.emptyList(), Collections.emptySet());
    }

    public static ModuleDueResult error(String moduleId) {
        return new ModuleDueResult(moduleId, Coverage.ERROR,
                Collections.emptyList(), Collections.emptySet());
    }

    public String getModuleId() { return moduleId; }
    public Coverage getCoverage() { return coverage; }
    public List<DueServiceItem> getItems() { return items; }
    public Set<String> getSupportedServiceCodes() { return supportedServiceCodes; }
}
