package org.smartregister.chw.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.activity.MotherMentorHouseholdProfileActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.model.MotherMentorHouseholdRegisterFragmentModel;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorContactFragment;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.presenter.MotherMentorHouseholdRegisterFragmentPresenter;
import org.smartregister.chw.provider.MotherMentorHouseholdRegisterProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.util.Utils;

import timber.log.Timber;

import java.util.Set;

public class MotherMentorContactRegisterFragment extends BaseMotherMentorContactFragment {
    private static final String STATE_HISTORY = "household_history";
    private View view;
    private boolean historyMode;
    private MenuItem historyMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyMode = savedInstanceState != null && savedInstanceState.getBoolean(STATE_HISTORY);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean(STATE_HISTORY, historyMode);
    }

    private final MotherMentorHouseholdRegisterFragmentModel model =
            new MotherMentorHouseholdRegisterFragmentModel();

    @Override
    public void initializeAdapter(
            Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        MotherMentorHouseholdRegisterProvider registerProvider =
                new MotherMentorHouseholdRegisterProvider(
                        getActivity(),
                        paginationViewHandler,
                        registerActionHandler,
                        visibleColumns);
        clientAdapter =
                new RecyclerViewPaginatedAdapter(
                        null, registerProvider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public void countExecute() {
        Cursor c = null;
        try {
            String query =
                    "select count(*) from "
                            + presenter().getMainTable()
                            + " where "
                            + presenter().getMainCondition();
            if (StringUtils.isNotBlank(filters)) {
                query = query + " and ( " + filters + " ) ";
            }
            c = commonRepository().rawCustomQueryForAdapter(query);
            if (c.moveToFirst()) {
                clientAdapter.setTotalcount(c.getInt(0));
            }
            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        if (id == LOADER_ID) {
            return new CursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    if (args != null && args.getBoolean("count_execute")) {
                        countExecute();
                    }
                    String mainSelect =
                            model.mainSelect(
                                    presenter().getMainTable(), presenter().getMainCondition());
                    SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);
                    if (StringUtils.isNotBlank(filters)) {
                        sqb.addCondition(filters);
                    }
                    String query = sqb.orderbyCondition(Sortqueries);
                    query =
                            sqb.Endquery(
                                    sqb.addlimitandOffset(
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
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter =
                new MotherMentorHouseholdRegisterFragmentPresenter(
                        this, new MotherMentorHouseholdRegisterFragmentModel(), null);
        ((MotherMentorHouseholdRegisterFragmentPresenter) presenter).setHistoryMode(historyMode);
    }

    @Override
    protected void openProfile(String baseEntityId) {
        MotherMentorHouseholdProfileActivity.startMe(requireActivity(), baseEntityId);
    }

    @Override
    protected void onViewClicked(View view) {
        if (getActivity() == null || !(view.getTag() instanceof CommonPersonObjectClient)) {
            return;
        }

        CommonPersonObjectClient client = (CommonPersonObjectClient) view.getTag();
        openHouseholdProfile(getHouseholdBaseEntityId(client));
    }

    private void openHouseholdProfile(String familyBaseEntityId) {
        MotherMentorHouseholdProfileActivity.startMe(requireActivity(), familyBaseEntityId);
    }

    private String getHouseholdBaseEntityId(CommonPersonObjectClient client) {
        String baseEntityId =
                Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
        if (StringUtils.isNotBlank(baseEntityId)) {
            return baseEntityId;
        }

        baseEntityId =
                Utils.getValue(
                        client.getColumnmaps(), DBConstants.KEY.FAMILY_BASE_ENTITY_ID, false);
        if (StringUtils.isNotBlank(baseEntityId)) {
            return baseEntityId;
        }

        return client.getCaseId();
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
    }

    @Override
    public void onResume() {
        super.onResume();
        if (view != null) {
            setupNavigationMenu(view);
        }
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
        int historyId = org.smartregister.chw.mothermentor.R.id.action_mothermentor_household_history;
        historyMenuItem = toolbar.getMenu().findItem(historyId);
        if (historyMenuItem == null) {
            historyMenuItem = toolbar.getMenu().add(Menu.NONE, historyId, Menu.NONE,
                    org.smartregister.chw.mothermentor.R.string.mothermentor_household_closed);
            historyMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        historyMenuItem.setTitle(historyMode
                ? org.smartregister.chw.mothermentor.R.string.mothermentor_household_active_follow_up
                : org.smartregister.chw.mothermentor.R.string.mothermentor_household_closed);
        historyMenuItem.setOnMenuItemClickListener(item -> {
            setHistoryMode(!historyMode);
            return true;
        });
    }

    void setHistoryMode(boolean historyMode) {
        this.historyMode = historyMode;
        if (presenter instanceof MotherMentorHouseholdRegisterFragmentPresenter) {
            ((MotherMentorHouseholdRegisterFragmentPresenter) presenter).setHistoryMode(historyMode);
        }
        if (historyMenuItem != null) {
            historyMenuItem.setTitle(historyMode
                    ? org.smartregister.chw.mothermentor.R.string.mothermentor_household_active_follow_up
                    : org.smartregister.chw.mothermentor.R.string.mothermentor_household_closed);
        }
        if (clientAdapter != null) {
            Bundle count = new Bundle();
            count.putBoolean("count_execute", true);
            filterandSortExecute(count);
        }
    }
}
