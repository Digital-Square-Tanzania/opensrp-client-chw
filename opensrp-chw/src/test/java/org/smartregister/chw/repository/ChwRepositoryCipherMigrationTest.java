package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.database.MatrixCursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteStatement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.CoreLibrary;
import org.smartregister.exception.DatabaseMigrationException;
import org.smartregister.repository.AllSharedPreferences;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class ChwRepositoryCipherMigrationTest {
    private CoreLibrary previousCoreLibrary;
    private AllSharedPreferences preferences;
    private ChwRepository repository;
    private SQLiteDatabase database;
    private SQLiteStatement migration;

    @Before
    public void setUp() {
        previousCoreLibrary = ReflectionHelpers.getStaticField(CoreLibrary.class, "instance");
        CoreLibrary library = mock(CoreLibrary.class);
        org.smartregister.Context context = mock(org.smartregister.Context.class);
        preferences = mock(AllSharedPreferences.class);
        when(library.context()).thenReturn(context);
        when(context.allSharedPreferences()).thenReturn(preferences);
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", library);
        repository = mock(ChwRepository.class, CALLS_REAL_METHODS);
        database = mock(SQLiteDatabase.class);
        migration = mock(SQLiteStatement.class);
        when(database.compileStatement("PRAGMA cipher_migrate")).thenReturn(migration);
        // Optional Mother Mentor enrollment tables are absent in this focused lifecycle fixture.
        when(database.rawQuery(anyString(), isNull()))
                .thenAnswer(call -> new MatrixCursor(new String[] {"name"}));
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", previousCoreLibrary);
    }

    @Test
    public void freshlyCreatedDatabaseSkipsLegacyMigrationAndPersistsCurrentCipherMarker() {
        ReflectionHelpers.setField(repository, "createdWithCurrentCipher", true);
        when(preferences.isMigratedToSqlite4()).thenReturn(false);

        repository.onOpen(database);

        verify(database, never()).compileStatement(anyString());
        verify(preferences).setMigratedToSqlite4();
        verify(database).execSQL("PRAGMA cipher_memory_security = OFF;");
        verify(database).rawExecSQL("PRAGMA journal_mode = TRUNCATE;");
    }

    @Test
    public void existingCurrentCipherMarkerSkipsMigrationWithoutRewritingTheMarker() {
        when(preferences.isMigratedToSqlite4()).thenReturn(true);

        repository.onOpen(database);

        verify(database, never()).compileStatement(anyString());
        verify(preferences, never()).setMigratedToSqlite4();
        verify(database).rawExecSQL("PRAGMA journal_mode = TRUNCATE;");
    }

    @Test
    public void existingDatabaseRetainsLegacyMigrationAndMarksOnlySuccessfulResult() {
        when(preferences.isMigratedToSqlite4()).thenReturn(false);
        when(migration.simpleQueryForLong()).thenReturn(0L);

        repository.onOpen(database);

        verify(database).compileStatement("PRAGMA cipher_migrate");
        verify(migration).simpleQueryForLong();
        verify(migration).close();
        verify(preferences).setMigratedToSqlite4();
        verify(database).rawExecSQL("PRAGMA journal_mode = TRUNCATE;");
    }

    @Test
    public void failedExistingDatabaseMigrationStillThrowsWithoutMarkingItCurrent() {
        when(preferences.isMigratedToSqlite4()).thenReturn(false);
        when(migration.simpleQueryForLong()).thenReturn(21L);

        try {
            repository.onOpen(database);
            fail("A failed legacy migration must not allow the database to open");
        } catch (DatabaseMigrationException exception) {
            assertEquals(
                    "Database migration to SQLiteCipher v4 was not successful",
                    exception.getMessage());
        }

        verify(database).compileStatement("PRAGMA cipher_migrate");
        verify(migration).close();
        verify(preferences, never()).setMigratedToSqlite4();
        verify(database, never()).rawExecSQL(anyString());
    }
}
