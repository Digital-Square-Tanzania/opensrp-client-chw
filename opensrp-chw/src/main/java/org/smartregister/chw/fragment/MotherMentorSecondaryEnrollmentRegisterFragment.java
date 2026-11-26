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
import org.smartregister.chw.provider.MotherMentorIitRegisterProvider;
import org.smartregister.chw.provider.MotherMentorChildEidRegisterProvider;
import org.smartregister.chw.repository.MotherMentorPartnerRegisterPage;
import org.smartregister.chw.mothermentor.dao.MotherMentorChildEidDao;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

import timber.log.Timber;

import java.util.Set;

public class MotherMentorSecondaryEnrollmentRegisterFragment extends MotherMentorRegisterFragment {

    @Override
    protected boolean isMainClientRegister() {
        return false;
    }

    private static final String ARG_TABLE_NAME = "table_name";
    private static final String ARG_FOLLOW_UP_TYPE = "follow_up_type";
    private String sharedSearchText = "";
    private boolean historyMode;
    private long followUpLoaderGeneration;
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
        ((MotherMentorSecondaryEnrollmentRegisterFragmentPresenter) presenter)
                .setHistoryMode(historyMode);
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

    public void setHistoryMode(boolean historyMode) {
        this.historyMode = historyMode;
        if (presenter instanceof MotherMentorSecondaryEnrollmentRegisterFragmentPresenter) {
            ((MotherMentorSecondaryEnrollmentRegisterFragmentPresenter) presenter)
                    .setHistoryMode(historyMode);
            applySharedSearchText();
        }
    }

    public void setSharedSearchText(String searchText) {
        sharedSearchText = searchText == null ? "" : searchText;
        applySharedSearchText();
    }

    private void applySharedSearchText() {
        if (presenter == null || clientAdapter == null) return;
        if (isChildEid()) {
            clientAdapter.setCurrentoffset(0);
            filterandSortExecute(countBundle());
            return;
        }
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
        if (isPartner()) clientAdapter.setCurrentoffset(0);
        filterandSortExecute(countBundle());
    }

    @Override
    public void initializeAdapter(
            Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        MotherMentorFollowUpRegisterProvider provider =
                isChildEid()
                        ? new MotherMentorChildEidRegisterProvider(
                                getActivity(), paginationViewHandler, registerActionHandler)
                        : getFollowUpType() == MotherMentorFollowUpType.MOTHER_IIT
                        ? new MotherMentorIitRegisterProvider(
                                getActivity(), paginationViewHandler, registerActionHandler)
                        : new MotherMentorFollowUpRegisterProvider(
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
            if (isChildEid()) {
                String page = MotherMentorChildEidDao.pageQuery(sharedSearchText, historyMode,
                        clientAdapter.getCurrentlimit(), clientAdapter.getCurrentoffset());
                String count = args != null && args.getBoolean("count_execute")
                        ? MotherMentorChildEidDao.countQuery(sharedSearchText, historyMode) : null;
                return new ChildCursorLoader(getActivity(), commonRepository(),
                        ++followUpLoaderGeneration, page, count);
            }
            if (isPartner()) {
                return new PartnerCursorLoader(getActivity(), commonRepository(),
                        ++followUpLoaderGeneration, partnerCandidates(), historyMode,
                        clientAdapter.getCurrentoffset(), clientAdapter.getCurrentlimit());
            }
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
                    } else builder.addCondition("AND " + filters);
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
        if (isChildEid()) {
            try (Cursor cursor = commonRepository().rawCustomQueryForAdapter(
                    MotherMentorChildEidDao.countQuery(sharedSearchText, historyMode))) {
                if (cursor.moveToFirst()) clientAdapter.setTotalcount(cursor.getInt(0));
            }
            return;
        }
        if (isPartner()) {
            MotherMentorPartnerRegisterPage page = MotherMentorPartnerRegisterPage.read(
                    commonRepository(), partnerCandidates(), historyMode, 0, 0);
            page.rows.close();
            clientAdapter.setTotalcount(page.count);
            return;
        }
        Cursor cursor = null;
        try {
            SmartRegisterQueryBuilder builder =
                    new SmartRegisterQueryBuilder(
                            registerModel.mainSelect(
                                    presenter().getMainTable(), presenter().getMainCondition()));
            if (!TextUtils.isEmpty(filters)) builder.addCondition("AND " + filters);
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

    private boolean isChildEid() {
        return getFollowUpType() == MotherMentorFollowUpType.CHILD_EID;
    }

    private boolean isPartner() {
        return getFollowUpType() == MotherMentorFollowUpType.PARTNER;
    }

    private String partnerCandidates() {
        return registerModel.partnerCandidates(presenter().getMainCondition(), historyMode, filters, Sortqueries);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (isChildEid() || isPartner()) {
            if (!(loader instanceof FollowUpCursorLoader)) return;
            FollowUpCursorLoader followUp = (FollowUpCursorLoader) loader;
            if (followUp.generation != followUpLoaderGeneration) return;
            if (followUp.total != null) clientAdapter.setTotalcount(followUp.total);
        }
        super.onLoadFinished(loader, data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if ((isChildEid() || isPartner()) && (!(loader instanceof FollowUpCursorLoader)
                || ((FollowUpCursorLoader) loader).generation != followUpLoaderGeneration)) return;
        super.onLoaderReset(loader);
    }

    @Override
    public void onDestroyView() {
        if (isChildEid() || isPartner()) followUpLoaderGeneration++;
        super.onDestroyView();
    }

    /** A loader owns its query snapshot and cursor; obsolete callbacks never touch a new view. */
    private abstract static class FollowUpCursorLoader extends CursorLoader {
        final CommonRepository repository;
        final long generation;
        volatile Integer total;

        FollowUpCursorLoader(android.content.Context context, CommonRepository repository, long generation) {
            super(context);
            this.repository = repository;
            this.generation = generation;
        }
    }

    private static final class ChildCursorLoader extends FollowUpCursorLoader {
        private final String pageQuery;
        private final String countQuery;

        ChildCursorLoader(android.content.Context context, CommonRepository repository,
                long generation, String pageQuery, String countQuery) {
            super(context, repository, generation);
            this.pageQuery = pageQuery;
            this.countQuery = countQuery;
        }

        @Override
        public Cursor loadInBackground() {
            if (countQuery != null) {
                try (Cursor cursor = repository.rawCustomQueryForAdapter(countQuery)) {
                    total = cursor.moveToFirst() ? cursor.getInt(0) : 0;
                }
            }
            return repository.rawCustomQueryForAdapter(pageQuery);
        }
    }

    private static final class PartnerCursorLoader extends FollowUpCursorLoader {
        private final String query;
        private final boolean closed;
        private final int offset;
        private final int limit;

        PartnerCursorLoader(android.content.Context context, CommonRepository repository,
                long generation, String query, boolean closed, int offset, int limit) {
            super(context, repository, generation);
            this.query = query;
            this.closed = closed;
            this.offset = offset;
            this.limit = limit;
        }

        @Override
        public Cursor loadInBackground() {
            MotherMentorPartnerRegisterPage page =
                    MotherMentorPartnerRegisterPage.read(repository, query, closed, offset, limit);
            total = page.count;
            return page.rows;
        }
    }
}
