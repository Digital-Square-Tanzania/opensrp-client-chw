package org.smartregister.chw.due;

import org.joda.time.DateTime;
import org.junit.Test;
import org.smartregister.chw.repository.AllClientsDashboardRepository;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class LandingDueServiceRepositoryTest {
    private final ModuleDueServiceProvider motherMentor = mock(ModuleDueServiceProvider.class);
    private final AllClientsDashboardRepository clients = mock(AllClientsDashboardRepository.class);
    private final long now = DateTime.parse("2026-10-27T12:00:00+03:00").getMillis();

    @Test
    public void disabledOrEmptyMotherMentorDoesNotInvokeProvider() {
        when(motherMentor.getModuleId()).thenReturn(LandingDueServiceRepository.MOTHER_MENTOR);
        LandingDueServiceRepository repository = repository();
        org.mockito.Mockito.clearInvocations(motherMentor);
        assertTrue(repository.load(now, cohort("NCD", 2)).getResults().isEmpty());
        assertTrue(repository.load(now, cohort("Mother Mentor", 0)).getResults().isEmpty());
        verifyNoInteractions(motherMentor);
    }

    @Test
    public void motherMentorReceivesOnlyDashboardCohortAndPreservesUnsupportedCoverage() {
        when(motherMentor.getModuleId()).thenReturn(LandingDueServiceRepository.MOTHER_MENTOR);
        when(motherMentor.load(any())).thenAnswer(call -> {
            DueQuery query = call.getArgument(0);
            assertEquals(Collections.singletonMap("person", "Test Client"), query.getClientNames());
            return ModuleDueResult.unsupported(LandingDueServiceRepository.MOTHER_MENTOR);
        });
        DueServicesSnapshot result = repository().load(now, cohort("Mother Mentor", 1));
        assertEquals(1, result.getResults().size());
        assertEquals(ModuleDueResult.Coverage.UNSUPPORTED, result.getResults().get(0).getCoverage());
        verify(motherMentor).load(any());
    }

    @Test
    public void standaloneRegisterLoadsTheSameDashboardCohort() {
        when(motherMentor.getModuleId()).thenReturn(LandingDueServiceRepository.MOTHER_MENTOR);
        when(clients.load(now)).thenReturn(cohort("NCD", 1));
        assertTrue(repository().load(now).getResults().isEmpty());
        verify(clients).load(now);
    }

    private LandingDueServiceRepository repository() {
        return new LandingDueServiceRepository(clients,
                new ModuleDueProviderRegistry(Collections.singletonList(motherMentor)));
    }

    private AllClientsDashboardRepository.Snapshot cohort(String module, int count) {
        return new AllClientsDashboardRepository.Snapshot(1, 0, 1, 0,
                Arrays.asList(new AllClientsDashboardRepository.ModuleCount(module, count)),
                Collections.emptyList(), Collections.singletonMap("person", "Test Client"));
    }
}
