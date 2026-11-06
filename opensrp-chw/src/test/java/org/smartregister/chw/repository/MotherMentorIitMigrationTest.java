package org.smartregister.chw.repository;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import android.app.Application;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.mothermentor.repository.MotherMentorIitRepository;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorIitMigrationTest {
    @Test
    public void database68ReplaysIitOnceFromEitherPriorVersion() {
        SQLiteDatabase database = mock(SQLiteDatabase.class);
        assertTrue(BuildConfig.DATABASE_VERSION >= 68);
        for (int oldVersion : new int[]{66, 67}) {
            try (MockedStatic<MotherMentorIitRepository> iit = mockStatic(MotherMentorIitRepository.class)) {
                ChwRepositoryFlv.onUpgrade(null, database, oldVersion, 68);
                ChwRepositoryFlv.onUpgrade(null, database, 68, 68);
                iit.verify(() -> MotherMentorIitRepository.upgrade(database));
                iit.verifyNoMoreInteractions();
            }
        }
    }
}
