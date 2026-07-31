package org.smartregister.chw.repository;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.repository.Repository;

import java.util.concurrent.atomic.AtomicReference;

public class DelegatingRepositoryTest extends BaseUnitTest {

    @Test
    public void databaseOperationsUseTheCurrentRepository() {
        Repository initialRepository = Mockito.mock(Repository.class);
        Repository authenticatedRepository = Mockito.mock(Repository.class);
        SQLiteDatabase initialDatabase = Mockito.mock(SQLiteDatabase.class);
        SQLiteDatabase authenticatedDatabase = Mockito.mock(SQLiteDatabase.class);
        Mockito.when(initialRepository.getWritableDatabase()).thenReturn(initialDatabase);
        Mockito.when(authenticatedRepository.getWritableDatabase()).thenReturn(authenticatedDatabase);

        AtomicReference<Repository> currentRepository =
                new AtomicReference<>(initialRepository);
        DelegatingRepository delegatingRepository = new DelegatingRepository(
                RuntimeEnvironment.getApplication(),
                org.smartregister.Context.getInstance().session(),
                currentRepository::get);

        Assert.assertSame(initialDatabase, delegatingRepository.getWritableDatabase());

        currentRepository.set(authenticatedRepository);

        Assert.assertSame(authenticatedDatabase, delegatingRepository.getWritableDatabase());
        Mockito.verify(initialRepository).getWritableDatabase();
        Mockito.verify(authenticatedRepository).getWritableDatabase();
    }
}
