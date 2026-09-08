package org.smartregister.chw.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import androidx.loader.content.Loader;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.NcdProfileActivity;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.core.fragment.CoreNcdRegisterFragment;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.QueryBuilder;
import org.smartregister.chw.loader.NcdCursorLoaderFragment;
import org.smartregister.chw.loader.NcdFragmentCursorLoader;
import org.smartregister.chw.model.NcdRegisterAtRiskFragmentModel;
import org.smartregister.chw.presenter.NcdConfirmedRegisterFragmentPresenter;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.text.MessageFormat;
import java.util.List;

import timber.log.Timber;

public class NcdConfirmedRegisterFragment extends CoreNcdRegisterFragment implements NcdCursorLoaderFragment {

    private boolean dueFilterActive = false;

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
            titleView.setText(this.getString(R.string.ncd_confirmed_register_title));
        }
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new NcdConfirmedRegisterFragmentPresenter(this, new NcdRegisterAtRiskFragmentModel(), null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID) {
            return new NcdFragmentCursorLoader(getActivity(), this, args);
        }
        return super.onCreateLoader(id, args);
    }

    @Override
    public void countExecute() {
        super.countExecute();
    }

    @Override
    protected void openProfile(String baseEntityId) {
        NcdProfileActivity.startProfileActivity(getActivity(), baseEntityId, true);
    }

    @Override
    public String defaultFilterAndSortQuery() {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);

        String query = "";
        StringBuilder customFilter = new StringBuilder();
        if (StringUtils.isNotBlank(filters)) {
            customFilter.append(MessageFormat.format(" and ( {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.FIRST_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.LAST_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.MIDDLE_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ) ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.UNIQUE_ID, filters));

        }
        if (dueFilterActive) {
            customFilter.append(MessageFormat.format(" and ( {0}) ", presenter().getDueFilterCondition()));
        }
        try {
            if (isValidFilterForFts(commonRepository())) {

                String myquery = QueryBuilder.getQuery(joinTables, mainCondition, tablename, customFilter.toString(), clientAdapter, Sortqueries);
                List<String> ids = commonRepository().findSearchIds(myquery);
                query = sqb.toStringFts(ids, tablename, CommonRepository.ID_COLUMN,
                        Sortqueries);
                query = sqb.Endquery(query);
            } else {
                sqb.addCondition(customFilter.toString());
                query = sqb.orderbyCondition(Sortqueries);
                query = sqb.Endquery(sqb.addlimitandOffset(query, clientAdapter.getCurrentlimit(), clientAdapter.getCurrentoffset()));

            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return query;
    }
}
