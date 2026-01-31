package org.smartregister.chw.application;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.koin.core.context.GlobalContext;
import org.mockito.Mockito;
import org.smartregister.chw.R;
import org.smartregister.family.util.AppExecutors;
import org.smartregister.repository.Repository;

import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * Created by keyman on 11/03/2019.
 */

public class TestChwApplication extends ChwApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        setTheme(R.style.Theme_AppCompat);
    }

    @Override
    protected void initializeMapBox() {
        Timber.v("Mute Map Box");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        GlobalContext.stop();
    }

    @Override
    public Repository getRepository() {
        Repository repository = Mockito.mock(Repository.class);
        SQLiteDatabase sqLiteDatabase = Mockito.mock(SQLiteDatabase.class);
        Mockito.when(repository.getReadableDatabase()).thenReturn(sqLiteDatabase);
        Mockito.when(repository.getWritableDatabase()).thenReturn(sqLiteDatabase);
        return repository;
    }

    @Override
    public AppExecutors getAppExecutors() {
        return new AppExecutors(Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
    }


}
