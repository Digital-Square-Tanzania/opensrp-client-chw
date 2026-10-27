package org.smartregister.chw.due;

import org.joda.time.LocalDate;
import org.smartregister.chw.repository.AllClientsDashboardRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Reads due services for the dashboard's existing role-scoped, canonical person cohort. */
public class LandingDueServiceRepository {
    public static final String MOTHER_MENTOR = "MOTHER_MENTOR";

    private final AllClientsDashboardRepository clients;
    private final ModuleDueProviderRegistry registry;

    public LandingDueServiceRepository() {
        this(new AllClientsDashboardRepository(), new ModuleDueProviderRegistry(
                Collections.singletonList(new MotherMentorDueServiceProvider())));
    }

    LandingDueServiceRepository(AllClientsDashboardRepository clients, ModuleDueProviderRegistry registry) {
        this.clients = clients;
        this.registry = registry;
    }

    /** Call on a worker thread. The current device timezone determines the local calendar date. */
    public DueServicesSnapshot load(long now) {
        return load(now, clients.load(now));
    }

    public DueServicesSnapshot load(long now, AllClientsDashboardRepository.Snapshot cohort) {
        DueQuery query = new DueQuery(new LocalDate(now), cohort.getClientNames());
        Set<String> enabled = new HashSet<>();
        // Breakdown rows already require an available menu and a non-empty client membership.
        for (AllClientsDashboardRepository.ModuleCount module : cohort.getBreakdown()) {
            if (AllClientsDashboardRepository.MOTHER_MENTOR.equals(module.getRegisterType()) && module.getCount() > 0) enabled.add(MOTHER_MENTOR);
        }
        return DueServicesSnapshot.aggregate(query, registry.load(query, enabled));
    }
}
