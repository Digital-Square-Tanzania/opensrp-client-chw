package org.smartregister.chw.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.activity.FamilyProfileActivity;
import org.smartregister.chw.activity.MotherMentorProfileActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.model.MotherMentorHouseholdRegisterFragmentModel;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorContactFragment;
import org.smartregister.chw.presenter.MotherMentorHouseholdRegisterFragmentPresenter;
import org.smartregister.chw.provider.MotherMentorHouseholdRegisterProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.util.Set;

import timber.log.Timber;

public class MotherMentorContactRegisterFragment extends BaseMotherMentorContactFragment {
    private View view;
    private final MotherMentorHouseholdRegisterFragmentModel model = new MotherMentorHouseholdRegisterFragmentModel();

    @Override
    public void initializeAdapter(Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        MotherMentorHouseholdRegisterProvider registerProvider = new MotherMentorHouseholdRegisterProvider(getActivity(), paginationViewHandler, registerActionHandler, visibleColumns);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, registerProvider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public void countExecute() {
        Cursor c = null;
        try {
            String query = "select count(*) from " + presenter().getMainTable() +
                    " where " + presenter().getMainCondition();
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
                    String mainSelect = model.mainSelect(presenter().getMainTable(), presenter().getMainCondition());
                    SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);
                    if (StringUtils.isNotBlank(filters)) {
                        sqb.addCondition(filters);
                    }
                    String query = sqb.orderbyCondition(Sortqueries);
                    query = sqb.Endquery(sqb.addlimitandOffset(query, clientAdapter.getCurrentlimit(), clientAdapter.getCurrentoffset()));
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
        presenter = new MotherMentorHouseholdRegisterFragmentPresenter(this, new MotherMentorHouseholdRegisterFragmentModel(), null);
    }

    @Override
    protected void openProfile(String baseEntityId) {
        MotherMentorProfileActivity.startMe(requireActivity(), baseEntityId);
    }

    @Override
    protected void onViewClicked(android.view.View view) {
        if (getActivity() == null || !(view.getTag() instanceof CommonPersonObjectClient)) {
            return;
        }

        CommonPersonObjectClient client = (CommonPersonObjectClient) view.getTag();
        openHouseholdProfile(Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false));
    }

    private void openHouseholdProfile(String familyBaseEntityId) {
        Intent intent = new Intent(requireActivity(), FamilyProfileActivity.class);
        intent.putExtra(Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, familyBaseEntityId);
        startActivity(intent);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
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
    }
}
