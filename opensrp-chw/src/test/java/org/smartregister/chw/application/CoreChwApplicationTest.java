package org.smartregister.chw.application;

import static org.mockito.ArgumentMatchers.eq;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.repository.DelegatingRepository;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.reporting.ReportingLibrary;

import java.util.Date;

public class CoreChwApplicationTest extends BaseUnitTest {

    @Test
    public void immunizationLibraryIsInitialisedOnStart() {
        Assert.assertNotNull(ImmunizationLibrary.getInstance());
    }

    @Test
    public void reportingLibraryIsInitialisedOnStart() {
        Assert.assertNotNull(ReportingLibrary.getInstance());
    }

    @Test
    public void motherMentorResolvesTheCurrentRepositoryAfterSignIn() {
        Assert.assertTrue(MotherMentorLibrary.getInstance().getRepository() instanceof DelegatingRepository);
    }

    @Test
    public void testP2PClassifierIsInitialized() {
        ChwApplication application = new ChwApplication();
        ChwApplicationFlv flv = Mockito.spy(new ChwApplicationFlv());
        Mockito.doReturn(true).when(flv).hasForeignData();

        ReflectionHelpers.setField(application, "flavor", flv);
        Assert.assertNotNull(application.getP2PClassifier());
    }

    @Test
    public void testOnVisitEventShouldExecuteChwScheduleTaskExecutor() {
        Visit visit = new Visit();
        visit.setBaseEntityId("12345-abcde");
        visit.setVisitType(CoreConstants.VisitType.DONE.name());
        visit.setDate(new Date());

        ChwScheduleTaskExecutor chwScheduleTaskExecutor = Mockito.mock(ChwScheduleTaskExecutor.class);
        ReflectionHelpers.setField(ChwScheduleTaskExecutor.getInstance(), "scheduleTaskExecutor", chwScheduleTaskExecutor);

        ChwApplication chwApplication = new ChwApplication();
        chwApplication.onVisitEvent(visit);

        Mockito.verify(chwScheduleTaskExecutor, Mockito.times(1)).execute(eq(visit.getBaseEntityId()), eq(visit.getVisitType()), eq(visit.getDate()));
    }
}
