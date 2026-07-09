package org.smartregister.chw.loader;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import androidx.loader.content.CursorLoader;

public class NcdFragmentCursorLoader extends CursorLoader {

    private static final String COUNT = "count_execute";
    private final NcdCursorLoaderFragment fragment;
    private final Bundle args;

    public NcdFragmentCursorLoader(Context context, NcdCursorLoaderFragment fragment, Bundle args) {
        super(context);
        this.fragment = fragment;
        this.args = args;
    }

    @Override
    public Cursor loadInBackground() {
        if (args != null && args.getBoolean(COUNT)) {
            fragment.countExecute();
        }
        String query = fragment.defaultFilterAndSortQuery();
        return fragment.commonRepository().rawCustomQueryForAdapter(query);
    }
}
