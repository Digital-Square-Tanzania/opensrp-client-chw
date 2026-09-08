package org.smartregister.chw.repository;

import android.content.Context;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.smartregister.repository.Repository;
import org.smartregister.util.Session;

import java.util.function.Supplier;

/**
 * A repository reference that resolves its delegate for every database operation.
 *
 * <p>OpenSRP replaces the application's repository after login because SQLCipher captures the
 * database passphrase when a {@link Repository} is constructed. Libraries initialized before
 * login must therefore not retain that initial, passwordless repository.</p>
 */
public class DelegatingRepository extends Repository {

    private final Supplier<Repository> repositorySupplier;

    public DelegatingRepository(Context context, Session session,
                                Supplier<Repository> repositorySupplier) {
        super(context, session);
        this.repositorySupplier = repositorySupplier;
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        return getDelegate().getReadableDatabase();
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return getDelegate().getWritableDatabase();
    }

    @Override
    public boolean canUseThisPassword(String password) {
        return getDelegate().canUseThisPassword(password);
    }

    @Override
    public boolean deleteRepository() {
        return getDelegate().deleteRepository();
    }

    private Repository getDelegate() {
        Repository repository = repositorySupplier.get();
        if (repository == null || repository == this) {
            throw new IllegalStateException("A current repository is not available");
        }
        return repository;
    }
}
