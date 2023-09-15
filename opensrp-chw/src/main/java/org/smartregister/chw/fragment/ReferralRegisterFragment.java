package org.smartregister.chw.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.ChwReferralDetailsViewActivity;
import org.smartregister.chw.activity.MalariaFollowUpVisitActivity;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.QueryBuilder;
import org.smartregister.chw.model.ReferralRegisterFragmentModel;
import org.smartregister.chw.presenter.ReferralRegisterFragmentPresenter;
import org.smartregister.chw.referral.contract.BaseReferralRegisterFragmentContract;
import org.smartregister.chw.referral.domain.MemberObject;
import org.smartregister.chw.referral.fragment.BaseReferralRegisterFragment;
import org.smartregister.chw.referral.provider.ReferralRegisterProvider;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.view.activity.BaseRegisterActivity;
import org.smartregister.view.contract.BaseRegisterFragmentContract;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class ReferralRegisterFragment extends BaseReferralRegisterFragment {

    private static final String DUE_FILTER_TAG = "PRESSED";
    private View view;
    private View dueOnlyLayout;
    private boolean dueFilterActive = false;

    @Override
    public void initializeAdapter(@Nullable Set<? extends org.smartregister.configurableviews.model.View> visibleColumns) {
        ReferralRegisterProvider referralRegisterProvider = new ReferralRegisterProvider(getActivity(), paginationViewHandler, registerActionHandler, visibleColumns);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, referralRegisterProvider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        this.view = view;

        Toolbar toolbar = view.findViewById(org.smartregister.R.id.register_toolbar);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetStartWithNavigation(0);

        NavigationMenu.getInstance(getActivity(), null, toolbar);

        View navbarContainer = view.findViewById(R.id.register_nav_bar_container);
        navbarContainer.setFocusable(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        View searchBarLayout = view.findViewById(R.id.search_bar_layout);
        searchBarLayout.setLayoutParams(params);
        searchBarLayout.setBackgroundResource(R.color.chw_primary);
        searchBarLayout.setPadding(searchBarLayout.getPaddingLeft(), searchBarLayout.getPaddingTop(), searchBarLayout.getPaddingRight(), (int) Utils.convertDpToPixel(10, getActivity()));

        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
        }

        View topLeftLayout = view.findViewById(R.id.top_left_layout);
        topLeftLayout.setVisibility(View.GONE);

        View topRightLayout = view.findViewById(R.id.top_right_layout);
        topRightLayout.setVisibility(View.VISIBLE);

        View sortFilterBarLayout = view.findViewById(R.id.register_sort_filter_bar_layout);
        sortFilterBarLayout.setVisibility(View.GONE);

        View filterSortLayout = view.findViewById(R.id.filter_sort_layout);
        filterSortLayout.setVisibility(View.GONE);

        dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        dueOnlyLayout.setVisibility(View.VISIBLE);
        dueOnlyLayout.setOnClickListener(registerActionHandler);

        if (getSearchView() != null) {
            getSearchView().setBackgroundResource(org.smartregister.family.R.color.white);
            getSearchView().setCompoundDrawablesWithIntrinsicBounds(org.smartregister.family.R.drawable.ic_action_search, 0, 0, 0);
            getSearchView().setTextColor(getResources().getColor(R.color.text_black));
        }
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = ((BaseRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        presenter = (BaseRegisterFragmentContract.Presenter) new ReferralRegisterFragmentPresenter((BaseReferralRegisterFragmentContract.View) this, (BaseReferralRegisterFragmentContract.Model) new ReferralRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    public void setAdvancedSearchFormData(HashMap<String, String> hashMap) {
        //TODO
        //Log.d(TAG, "setAdvancedSearchFormData unimplemented");
    }

    @Override
    protected void onViewClicked(View view) {
        super.onViewClicked(view);

        if (view.getId() == R.id.due_only_layout) {
            toggleFilterSelection(view);
        }
    }

    protected void toggleFilterSelection(View dueOnlyLayout) {
        if (dueOnlyLayout != null) {
            if (dueOnlyLayout.getTag() == null) {
                dueFilterActive = true;
                dueFilter(dueOnlyLayout);
            } else if (dueOnlyLayout.getTag().toString().equals(DUE_FILTER_TAG)) {
                dueFilterActive = false;
                normalFilter(dueOnlyLayout);
            }
        }
    }

    protected String searchText() {
        return (getSearchView() == null) ? "" : getSearchView().getText().toString();
    }

    private void switchViews(View dueOnlyLayout, boolean isPress) {
        TextView dueOnlyTextView = dueOnlyLayout.findViewById(R.id.due_only_text_view);
        if (isPress) {
            dueOnlyTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_due_filter_on, 0);
        } else {
            dueOnlyTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_due_filter_off, 0);

        }
    }

    @Override
    protected void openProfile(CommonPersonObjectClient client) {
        ChwReferralDetailsViewActivity.startChwReferralDetailsViewActivity(getActivity(), new MemberObject(client), client);
    }

    @Override
    protected void openFollowUpVisit(CommonPersonObjectClient client) {
        MalariaFollowUpVisitActivity.startMalariaFollowUpActivity(getActivity(), client.getCaseId());
    }

    @Override
    protected void onResumption() {
        if (dueFilterActive && dueOnlyLayout != null) {
            dueFilter(dueOnlyLayout);
        } else {
            super.onResumption();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = view.findViewById(org.smartregister.R.id.register_toolbar);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetStartWithNavigation(0);
        NavigationMenu.getInstance(getActivity(), null, toolbar);
    }

    @Override
    protected void refreshSyncProgressSpinner() {
        if (syncProgressBar != null) {
            syncProgressBar.setVisibility(View.GONE);
        }
        if (syncButton != null) {
            syncButton.setVisibility(View.GONE);
        }
    }

    private String defaultFilterAndSortQuery() {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);

        String query = "";
        StringBuilder customFilter = new StringBuilder();
        if (StringUtils.isNotBlank(filters)) {
            customFilter.append(MessageFormat.format(" and ( {0}.{1} like ''%{2}%'' ", Constants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.FIRST_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", Constants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.LAST_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", Constants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.MIDDLE_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ) ", Constants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.UNIQUE_ID, filters));

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


    @Override
    public void countExecute() {
        Cursor c = null;
        try {

            String query = "select count(*) from " + presenter().getMainTable() + " inner join " + Constants.TABLE_NAME.FAMILY_MEMBER +
                    " on " + presenter().getMainTable() + ".entity_id = " +
                    Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.BASE_ENTITY_ID +
                    " inner join task on task.for = ec_family_member.base_entity_id" +
                    " where " + presenter().getMainCondition();

            if (StringUtils.isNotBlank(filters)) {
                query = query + " and ( " + filters + " ) ";
            }

            if (dueFilterActive) {
                query = query + " and ( " + presenter().getDueFilterCondition() + " ) ";
            }

            c = commonRepository().rawCustomQueryForAdapter(query);
            c.moveToFirst();
            clientAdapter.setTotalcount(c.getInt(0));
            Timber.v("total count here %s", clientAdapter.getTotalcount());

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
                    // Count query
                    final String COUNT = "count_execute";
                    if (args != null && args.getBoolean(COUNT)) {
                        countExecute();
                    }
                    String query = defaultFilterAndSortQuery();
                    return commonRepository().rawCustomQueryForAdapter(query);
                }
            };
        }
        return super.onCreateLoader(id, args);
    }

    protected void dueFilter(View dueOnlyLayout) {
        filterDue(searchText(), "", presenter().getDueFilterCondition());
        dueOnlyLayout.setTag(DUE_FILTER_TAG);
        switchViews(dueOnlyLayout, true);
    }

    protected void normalFilter(View dueOnlyLayout) {
        filterDue(searchText(), "", presenter().getMainCondition());
        dueOnlyLayout.setTag(null);
        switchViews(dueOnlyLayout, false);
    }

    protected void filterDue(String filterString, String joinTableString, String mainConditionString) {
        filters = filterString;
        joinTable = joinTableString;
        mainCondition = mainConditionString;
        filterandSortExecute(countBundle());
    }

}


