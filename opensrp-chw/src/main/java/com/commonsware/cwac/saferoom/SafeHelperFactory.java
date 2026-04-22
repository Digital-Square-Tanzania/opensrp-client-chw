package com.commonsware.cwac.saferoom;

import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

/**
 * Minimal fallback implementation that delegates to the stock framework helper.
 */
public class SafeHelperFactory implements SupportSQLiteOpenHelper.Factory {
    private final SupportSQLiteOpenHelper.Factory delegate;

    private SafeHelperFactory(SupportSQLiteOpenHelper.Factory delegate) {
        this.delegate = delegate;
    }

    public SafeHelperFactory() {
        this(new FrameworkSQLiteOpenHelperFactory());
    }

    public static SafeHelperFactory fromUser(Editable ignored) {
        return new SafeHelperFactory();
    }

    @NonNull
    @Override
    public SupportSQLiteOpenHelper create(@NonNull SupportSQLiteOpenHelper.Configuration configuration) {
        return delegate.create(configuration);
    }
}
