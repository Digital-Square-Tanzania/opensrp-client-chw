package org.smartregister.chw.repository;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.app.Application;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.mothermentor.repository.MotherMentorHouseholdClosureRepository;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorHouseholdClosureMigrationTest {
    @Test
    public void version62RunsTheLibraryMigrationOnceOnlyWhenCrossingItsVersion() {
        SQLiteDatabase database = mock(SQLiteDatabase.class);
        try (MockedStatic<MotherMentorHouseholdClosureRepository> library =
                     Mockito.mockStatic(MotherMentorHouseholdClosureRepository.class)) {
            ChwRepositoryFlv.onUpgrade(null, database, 61, 62);
            library.verify(() -> MotherMentorHouseholdClosureRepository.upgrade(database));
            library.clearInvocations();
            ChwRepositoryFlv.onUpgrade(null, database, 62, 62);
            library.verifyNoInteractions();
        }
        assertTrue(BuildConfig.DATABASE_VERSION >= 62);
    }
}
