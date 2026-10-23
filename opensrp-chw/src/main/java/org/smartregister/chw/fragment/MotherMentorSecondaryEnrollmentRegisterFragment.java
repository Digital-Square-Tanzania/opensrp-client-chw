package org.smartregister.chw.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.smartregister.chw.activity.MotherMentorProfileActivity;
import org.smartregister.chw.model.MotherMentorRegisterFragmentModel;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.presenter.MotherMentorSecondaryEnrollmentRegisterFragmentPresenter;
import org.smartregister.chw.provider.MotherMentorFollowUpRegisterProvider;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

import timber.log.Timber;

import java.util.Set;

public class MotherMentorSecondaryEnrollmentRegisterFragment extends MotherMentorRegisterFragment {

    private static final String ARG_TABLE_NAME = "table_name";
    private static final String ARG_FOLLOW_UP_TYPE = "follow_up_type";
    private String sharedSearchText = "";
    private final MotherMentorRegisterFragmentModel registerModel =
            new MotherMentorRegisterFragmentModel();

    public static MotherMentorSecondaryEnrollmentRegisterFragment newInstance(
            MotherMentorFollowUpType followUpType) {
        MotherMentorSecondaryEnrollmentRegisterFragment fragment =
                new MotherMentorSecondaryEnrollmentRegisterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE_NAME, followUpType.getEnrollmentTable());
        args.putString(ARG_FOLLOW_UP_TYPE, followUpType.getValue());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter =
                new MotherMentorSecondaryEnrollmentRegisterFragmentPresenter(
                        this, new MotherMentorRegisterFragmentModel(), null, getTableName());
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        hideRegisterHeader(view);
        applySharedSearchText();
    }

    @Override
    protected void openProfile(String baseEntityId) {
        MotherMentorProfileActivity.startFollowUpProfile(
                requireActivity(), baseEntityId, getFollowUpType());
    }

    public void setSharedSearchText(String searchText) {
        sharedSearchText = searchText == null ? "" : searchText;
        applySharedSearchText();
    }

    private void applySharedSearchText() {
        if (presenter == null || clientAdapter == null) return;
        String term = sharedSearchText.trim().replace("'", "''");
        filters =
                TextUtils.isEmpty(term)
                        ? ""
                        : "(ec_family_member."
                                + DBConstants.KEY.FIRST_NAME
                                + " LIKE '%"
                                + term
                                + "%' OR ec_family_member."
                                + DBConstants.KEY.MIDDLE_NAME
                                + " LIKE '%"
                                + term
                                + "%' OR ec_family_member."
                                + DBConstants.KEY.LAST_NAME
                                + " LIKE '%"
                                + term
                                + "%' OR ec_family_member."
                                + DBConstants.KEY.UNIQUE_ID
                                + " LIKE '%"
                                + term
                                + "%' OR ec_family."
                                + DBConstants.KEY.VILLAGE_TOWN
                                + " LIKE '%"
                                + term
                                + "%')";
        filterandSortExecute(countBundle());
    }

    @Override
    public void initializeAdapter(
            Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        MotherMentorFollowUpRegisterProvider provider =
                new MotherMentorFollowUpRegisterProvider(
                        getActivity(), paginationViewHandler, registerActionHandler);
        clientAdapter =
                new RecyclerViewPaginatedAdapter(
                        null, provider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        if (id == LOADER_ID) {
            return new CursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    if (args != null && args.getBoolean("count_execute")) countExecute();
                    SmartRegisterQueryBuilder builder =
                            new SmartRegisterQueryBuilder(
                                    registerModel.mainSelect(
                                            presenter().getMainTable(),
                                            presenter().getMainCondition()));
                    if (TextUtils.isEmpty(filters)) {
                        /* no search */
                    } else builder.addCondition(filters);
                    String query = builder.orderbyCondition(Sortqueries);
                    query =
                            builder.Endquery(
                                    builder.addlimitandOffset(
                                            query,
                                            clientAdapter.getCurrentlimit(),
                                            clientAdapter.getCurrentoffset()));
                    return commonRepository().rawCustomQueryForAdapter(query);
                }
            };
        }
        return super.onCreateLoader(id, args);
    }

    @Override
    public void countExecute() {
        Cursor cursor = null;
        try {
            SmartRegisterQueryBuilder builder =
                    new SmartRegisterQueryBuilder(
                            registerModel.mainSelect(
                                    presenter().getMainTable(), presenter().getMainCondition()));
            if (!TextUtils.isEmpty(filters)) builder.addCondition(filters);
            cursor =
                    commonRepository()
                            .rawCustomQueryForAdapter(
                                    "SELECT count(*) FROM ("
                                            + builder.toString()
                                            + ") follow_up_clients");
            if (cursor.moveToFirst()) clientAdapter.setTotalcount(cursor.getInt(0));
            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void hideRegisterHeader(View view) {
        setGone(view.findViewById(org.smartregister.R.id.toolbar_parent_layout));
        setGone(view.findViewById(org.smartregister.R.id.search_bar_layout));
        setGone(view.findViewById(org.smartregister.R.id.register_sort_filter_bar_layout));
        setGone(view.findViewById(org.smartregister.R.id.clients_header_layout));
    }

    private void setGone(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private String getTableName() {
        Bundle arguments = getArguments();
        return arguments == null ? null : arguments.getString(ARG_TABLE_NAME);
    }

    private MotherMentorFollowUpType getFollowUpType() {
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_FOLLOW_UP_TYPE)) {
            return MotherMentorFollowUpType.fromValue(arguments.getString(ARG_FOLLOW_UP_TYPE));
        }
        return MotherMentorFollowUpType.fromEnrollmentTable(getTableName());
    }
}
