package org.smartregister.chw.application;

import static org.mockito.ArgumentMatchers.eq;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.repository.Repository;

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
    @Config(application = RepositorySwitchingApplication.class)
    public void motherMentorReadsFromTheReplacementRepositoryAfterSignIn() {
        RepositorySwitchingApplication application =
                (RepositorySwitchingApplication) RuntimeEnvironment.getApplication();
        Repository libraryRepository = MotherMentorLibrary.getInstance().getRepository();
        Repository authenticatedRepository = Mockito.mock(Repository.class);
        SQLiteDatabase authenticatedDatabase = Mockito.mock(SQLiteDatabase.class);
        Mockito.when(authenticatedRepository.getReadableDatabase()).thenReturn(authenticatedDatabase);

        application.replacementRepository = authenticatedRepository;

        Assert.assertSame(authenticatedDatabase, libraryRepository.getReadableDatabase());
    }

    public static final class RepositorySwitchingApplication extends TestChwApplication {
        private Repository replacementRepository;
        private MotherMentorLibrary previousLibrary;

        @Override
        public void onCreate() {
            // Robolectric can recreate the Application while retaining library singletons.
            previousLibrary = ReflectionHelpers.getStaticField(MotherMentorLibrary.class, "instance");
            ReflectionHelpers.setStaticField(MotherMentorLibrary.class, "instance", null);
            super.onCreate();
        }

        @Override
        public void onTerminate() {
            try {
                super.onTerminate();
            } finally {
                ReflectionHelpers.setStaticField(MotherMentorLibrary.class, "instance", previousLibrary);
            }
        }

        @Override
        public Repository getRepository() {
            return replacementRepository == null ? super.getRepository() : replacementRepository;
        }
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
