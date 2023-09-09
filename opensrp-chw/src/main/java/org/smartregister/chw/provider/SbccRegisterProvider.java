package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import org.smartregister.chw.util.Constants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.provider.HvlResultsViewProvider;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.Set;

import timber.log.Timber;

public class SbccRegisterProvider extends HvlResultsViewProvider {
    private Set<org.smartregister.configurableviews.model.View> visibleColumns;

    public SbccRegisterProvider(Context context, View.OnClickListener paginationClickListener, View.OnClickListener onClickListener, Set visibleColumns) {
        super(context, paginationClickListener, onClickListener, visibleColumns);
        this.visibleColumns = visibleColumns;
    }

    public void getView(Cursor cursor, SmartRegisterClient smartRegisterClient, RegisterViewHolder registerViewHolder) { ;
        if (visibleColumns.isEmpty()) {
            populatePatientColumn(registerViewHolder);
        }
    }

    private void populatePatientColumn(final RegisterViewHolder viewHolder) {
        try {
            viewHolder.hvlWrapper.setVisibility(View.GONE);
            viewHolder.dueWrapper.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
