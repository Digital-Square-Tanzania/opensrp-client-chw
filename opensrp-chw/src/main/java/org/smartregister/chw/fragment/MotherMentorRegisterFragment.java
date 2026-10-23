package org.smartregister.chw.fragment;

import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.MotherMentorProfileActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.model.MotherMentorClientRegisterModel;
import org.smartregister.chw.model.MotherMentorRegisterFragmentModel;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.presenter.MotherMentorRegisterFragmentPresenter;
import org.smartregister.chw.provider.MotherMentorClientRegisterProvider;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;

import timber.log.Timber;

import java.util.Set;

public class MotherMentorRegisterFragment extends BaseMotherMentorRegisterFragment
        implements MotherMentorRegisterFilterDialogFragment.Listener {
    private static final String STATE_SEARCH = "mama_kinara_search";
    private static final String STATE_FILTER = "mama_kinara_filter";
    private final MotherMentorClientRegisterModel clientModel =
            new MotherMentorClientRegisterModel();
    private View view;
    private String searchText = "";
    private MotherMentorRegisterFilterState filterState =
            MotherMentorRegisterFilterState.allClients();
    private long loaderGeneration;

    protected boolean isMainClientRegister() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && isMainClientRegister()) {
            searchText = savedInstanceState.getString(STATE_SEARCH, "");
            filterState =
                    MotherMentorRegisterFilterState.fromBundle(
                            savedInstanceState.getBundle(STATE_FILTER));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isMainClientRegister()) {
            outState.putString(STATE_SEARCH, searchText);
            outState.putBundle(STATE_FILTER, filterState.toBundle());
        }
    }

    @Override
    protected int getLayout() {
        return isMainClientRegister() ? R.layout.fragment_mama_kinara_register : super.getLayout();
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter =
                new MotherMentorRegisterFragmentPresenter(
                        this,
                        isMainClientRegister()
                                ? clientModel
                                : new MotherMentorRegisterFragmentModel(),
                        null);
    }

    @Override
    public void initializeAdapter(
            Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        if (!isMainClientRegister()) {
            super.initializeAdapter(visibleColumns);
            return;
        }
        MotherMentorClientRegisterProvider provider =
                new MotherMentorClientRegisterProvider(
                        getActivity(), paginationViewHandler, registerActionHandler);
        clientAdapter =
                new RecyclerViewPaginatedAdapter(
                        null, provider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    protected void openProfile(String baseEntityId) {
        MotherMentorProfileActivity.startMe(requireActivity(), baseEntityId);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        View filterSortLayout = view.findViewById(org.smartregister.R.id.filter_sort_layout);
        if (filterSortLayout != null) {
            filterSortLayout.setVisibility(View.GONE);
        }
        this.view = view;
        setupNavigationMenu(view);
        if (isMainClientRegister()) setupClientHeader(view);
    }

    private void setupClientHeader(View root) {
        for (int i = clientsView.getItemDecorationCount() - 1; i >= 0; i--) {
            if (clientsView.getItemDecorationAt(i) instanceof DividerItemDecoration) {
                clientsView.removeItemDecorationAt(i);
            }
        }
        root.findViewById(R.id.mama_kinara_filter_button).setOnClickListener(v -> openFilter());
        if (getSearchView() != null) {
            getSearchView().setHint(R.string.mama_kinara_register_search_hint);
            getSearchView().setBackgroundResource(R.drawable.mama_kinara_search_background);
            getSearchView()
                    .setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.mama_kinara_ic_search, 0, 0, 0);
            getSearchView().setTextColor(getResources().getColor(R.color.mama_kinara_text));
            getSearchView()
                    .setHintTextColor(getResources().getColor(R.color.mama_kinara_search_hint));
            getSearchView().setText(searchText);
        }
        root.findViewById(org.smartregister.R.id.search_bar_layout)
                .setBackgroundColor(getResources().getColor(R.color.mama_kinara_blue));
        root.findViewById(org.smartregister.R.id.txt_title_label).setVisibility(View.GONE);
        updateFilterButton();
    }

    private void openFilter() {
        if (getFragmentManager() == null
                || getFragmentManager()
                                .findFragmentByTag(MotherMentorRegisterFilterDialogFragment.TAG)
                        != null) return;
        MotherMentorRegisterFilterDialogFragment dialog =
                MotherMentorRegisterFilterDialogFragment.newInstance(filterState);
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), MotherMentorRegisterFilterDialogFragment.TAG);
    }

    @Override
    public void onFilterSaved(MotherMentorRegisterFilterState state) {
        filterState = state.applied();
        updateFilterButton();
        reloadFirstPage();
    }

    private void updateFilterButton() {
        if (view == null || !isMainClientRegister()) return;
        TextView button = view.findViewById(R.id.mama_kinara_filter_button);
        boolean active = filterState.isActive();
        button.setText(
                active
                        ? R.string.mama_kinara_register_filtered
                        : R.string.mama_kinara_register_filter);
        button.setSelected(active);
        button.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    @Override
    public void filter(
            String filterString,
            String joinTableString,
            String mainConditionString,
            boolean qrCode) {
        if (!isMainClientRegister()) {
            super.filter(filterString, joinTableString, mainConditionString, qrCode);
            return;
        }
        searchText = filterString == null ? "" : filterString;
        filters = searchText;
        if (getSearchCancelView() != null) {
            getSearchCancelView()
                    .setVisibility(TextUtils.isEmpty(searchText) ? View.INVISIBLE : View.VISIBLE);
        }
        reloadFirstPage();
    }

    private void reloadFirstPage() {
        if (clientAdapter == null || !isAdded()) return;
        clientAdapter.setCurrentoffset(0);
        filterandSortExecute(countBundle());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        if (!isMainClientRegister() || id != LOADER_ID) return super.onCreateLoader(id, args);
        String condition = presenter().getMainCondition();
        String search = searchText;
        MotherMentorRegisterFilterState filter = filterState;
        int limit = clientAdapter.getCurrentlimit();
        int offset = clientAdapter.getCurrentoffset();
        String query =
                clientModel.selectClientsPage(
                        condition,
                        search,
                        filter.isEnabled(),
                        filter.isPregnant(),
                        filter.isBreastfeeding(),
                        filter.isUnderTwo(),
                        limit,
                        offset);
        String countQuery =
                args != null && args.getBoolean("count_execute")
                        ? clientModel.countClients(
                                condition,
                                search,
                                filter.isEnabled(),
                                filter.isPregnant(),
                                filter.isBreastfeeding(),
                                filter.isUnderTwo())
                        : null;
        return new ClientLoader(
                getActivity(), commonRepository(), ++loaderGeneration, query, countQuery);
    }

    @Override
    public void countExecute() {
        if (!isMainClientRegister()) {
            super.countExecute();
            return;
        }
        if (clientAdapter == null) return;
        try (Cursor cursor =
                commonRepository()
                        .rawCustomQueryForAdapter(
                                clientModel.countClients(
                                        presenter().getMainCondition(), searchText,
                                        filterState.isEnabled(), filterState.isPregnant(),
                                        filterState.isBreastfeeding(), filterState.isUnderTwo()))) {
            clientAdapter.setTotalcount(cursor.moveToFirst() ? cursor.getInt(0) : 0);
            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (isMainClientRegister()) {
            if (!(loader instanceof ClientLoader)) return;
            ClientLoader clientLoader = (ClientLoader) loader;
            if (clientLoader.generation != loaderGeneration) return;
            if (clientLoader.totalCount != null)
                clientAdapter.setTotalcount(clientLoader.totalCount);
        }
        super.onLoadFinished(loader, cursor);
        if (isMainClientRegister() && view != null) {
            boolean empty = cursor == null || cursor.getCount() == 0;
            view.findViewById(org.smartregister.R.id.empty_view)
                    .setVisibility(empty ? View.VISIBLE : View.GONE);
            clientsView.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (isMainClientRegister()
                && (!(loader instanceof ClientLoader)
                        || ((ClientLoader) loader).generation != loaderGeneration)) return;
        super.onLoaderReset(loader);
    }

    @Override
    public void setTotalPatients() {
        if (!isMainClientRegister()) super.setTotalPatients();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (view != null) {
            setupNavigationMenu(view);
        }
    }

    @Override
    public void onDestroyView() {
        view = null;
        if (isMainClientRegister()) loaderGeneration++;
        super.onDestroyView();
    }

    private void setupNavigationMenu(View view) {
        Toolbar toolbar = view.findViewById(org.smartregister.R.id.register_toolbar);
        if (toolbar == null) {
            return;
        }

        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetStartWithNavigation(0);

        try {
            NavigationMenu.getInstance(getActivity(), null, toolbar);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
    }

    /** A loader owns its immutable query snapshot; only active results update the adapter. */
    private static final class ClientLoader extends CursorLoader {
        private final CommonRepository repository;
        private final long generation;
        private final String query;
        private final String countQuery;
        private volatile Integer totalCount;

        private ClientLoader(
                android.content.Context context,
                CommonRepository repository,
                long generation,
                String query,
                String countQuery) {
            super(context);
            this.repository = repository;
            this.generation = generation;
            this.query = query;
            this.countQuery = countQuery;
        }

        @Override
        public Cursor loadInBackground() {
            if (countQuery != null) {
                try (Cursor count = repository.rawCustomQueryForAdapter(countQuery)) {
                    totalCount = count.moveToFirst() ? count.getInt(0) : 0;
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            return repository.rawCustomQueryForAdapter(query);
        }
    }
}
