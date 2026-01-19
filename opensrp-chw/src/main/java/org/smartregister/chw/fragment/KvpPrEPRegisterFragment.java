package org.smartregister.chw.fragment;

import static org.smartregister.chw.util.Constants.ENABLE_DATE_RANGE_FILTER;
import static org.smartregister.chw.util.Constants.ENABLE_HIV_STATUS_FILTER;
import static org.smartregister.chw.util.Constants.ENABLE_PREP_STATUS_FILTER;
import static org.smartregister.chw.util.Constants.FILTERS_ENABLED;
import static org.smartregister.chw.util.Constants.FILTER_APPOINTMENT_DATE;
import static org.smartregister.chw.util.Constants.FILTER_APPOINTMENT_DATE_RANGE_END_DATE;
import static org.smartregister.chw.util.Constants.FILTER_APPOINTMENT_DATE_RANGE_START_DATE;
import static org.smartregister.chw.util.Constants.REQUEST_FILTERS;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.tabs.TabLayout;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.KvpPrEPProfileActivity;
import org.smartregister.chw.activity.RegisterFilterActivity;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.core.fragment.CoreKvpRegisterFragment;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.QueryBuilder;
import org.smartregister.chw.model.KvpPrEPRegisterFragmentModel;
import org.smartregister.chw.presenter.KvpPrEPRegisterFragmentPresenter;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

import java.text.MessageFormat;
import java.util.List;

import timber.log.Timber;

public class KvpPrEPRegisterFragment extends CoreKvpRegisterFragment implements View.OnClickListener {
    String customGroupFilter;
    private String appointmentDate;
    private String appointmentStartDate;
    private String appointmentEndDate;
    private boolean filterEnabled = false;
    private TextView filterSortTextView;

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new KvpPrEPRegisterFragmentPresenter(this, new KvpPrEPRegisterFragmentModel(), null);
    }

    @Override
    protected int getTitleString() {
        return R.string.menu_kvp;
    }

    @Override
    protected void openProfile(String baseEntityId) {
        KvpPrEPProfileActivity.startProfileActivity(requireActivity(), baseEntityId);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);

        View sortFilterBarLayout = view.findViewById(org.smartregister.chw.core.R.id.register_sort_filter_bar_layout);
        sortFilterBarLayout.setVisibility(View.GONE);

        View dueOnlyLayout = view.findViewById(org.smartregister.chw.core.R.id.due_only_layout);
        dueOnlyLayout.setVisibility(View.GONE);

        View filterSortLayout = view.findViewById(org.smartregister.chw.core.R.id.filter_sort_layout);
        filterSortTextView = view.findViewById(org.smartregister.chw.core.R.id.filter_text_view);
        filterSortTextView.setText(R.string.filter);

        filterSortLayout.setVisibility(View.VISIBLE);
        filterSortLayout.setOnClickListener(this);

        setUpTabLayout(view, true);

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.filter_sort_layout || view.getId() == org.smartregister.chw.core.R.id.filter_sort_layout) {
            Intent intent = new Intent(getContext(), RegisterFilterActivity.class);
            intent.putExtra(FILTERS_ENABLED, filterEnabled);
            intent.putExtra(FILTER_APPOINTMENT_DATE, appointmentDate);
            intent.putExtra(FILTER_APPOINTMENT_DATE_RANGE_START_DATE, appointmentStartDate);
            intent.putExtra(FILTER_APPOINTMENT_DATE_RANGE_END_DATE, appointmentEndDate);
            intent.putExtra(ENABLE_HIV_STATUS_FILTER, false);
            intent.putExtra(ENABLE_PREP_STATUS_FILTER, false);
            intent.putExtra(ENABLE_DATE_RANGE_FILTER, true);
            ((Activity) getContext()).startActivityForResult(intent, REQUEST_FILTERS);
        }

    }

    public void onFiltersUpdated(int requestCode, @Nullable Intent data) {
        if (requestCode == REQUEST_FILTERS) {
            if (data != null) {
                filterEnabled = data.getBooleanExtra(FILTERS_ENABLED, false);
                if (filterEnabled) {
                    setTextViewDrawableColor(filterSortTextView, R.color.chw_accent);
                    filterSortTextView.setText(R.string.filter_applied);
                    appointmentDate = data.getStringExtra(FILTER_APPOINTMENT_DATE);
                    appointmentStartDate = data.getStringExtra(FILTER_APPOINTMENT_DATE_RANGE_START_DATE);
                    appointmentEndDate = data.getStringExtra(FILTER_APPOINTMENT_DATE_RANGE_END_DATE);
                    filter(prepClientsSearchText(), "", ((KvpPrEPRegisterFragmentPresenter) presenter()).getDueFilterCondition(appointmentStartDate, appointmentEndDate, getContext()), false);
                } else {
                    setTextViewDrawableColor(filterSortTextView, R.color.grey);
                    filterSortTextView.setText(R.string.filter);
                }
            }
        }
    }

    private String prepClientsSearchText() {
        String searchTextInput;
        if (this.getSearchView() == null) {
            searchTextInput = "";
        } else {
            searchTextInput = this.getSearchView().getText().toString();
        }
        return searchTextInput;
    }

    private void setTextViewDrawableColor(TextView textView, int color) {
        for (Drawable drawable : textView.getCompoundDrawablesRelative()) {
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(textView.getContext(), color), PorterDuff.Mode.SRC_IN));
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        if (id == LOADER_ID) {
            return new CursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
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

    private String defaultFilterAndSortQuery() {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);

        String query = "";
        StringBuilder customFilter = new StringBuilder();
        if (StringUtils.isNotBlank(filters)) {
            customFilter.append(MessageFormat.format(" and ( {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.FIRST_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.LAST_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.MIDDLE_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ) ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.UNIQUE_ID, filters));
        }
        if (filterEnabled) {
            customFilter.append(((KvpPrEPRegisterFragmentPresenter) presenter()).getDueFilterCondition(appointmentStartDate, appointmentEndDate, getContext()));
        }
        if (StringUtils.isNotBlank(customGroupFilter)) {
            customFilter.append(MessageFormat.format((" and ( {0} ) "), customGroupFilter));
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
    protected int getLayout() {
        return R.layout.fragment_prep_register;
    }

    protected void setUpTabLayout(View view, boolean enabled) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        if (enabled) {
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case 0:
                            customGroupFilter = "";
                            filterandSortExecute();
                            break;
                        case 1:
                            customGroupFilter = getDue();
                            filterandSortExecute();
                            break;
                        case 2:
                            customGroupFilter = getTomorrowVisitsDue();
                            filterandSortExecute();
                            break;
                        case 3:
                            customGroupFilter = getOverDue();
                            filterandSortExecute();
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }
    }

    protected String getDue() {
        return "CASE\n" +
                "    WHEN next_visit_date is not null\n" +
                "        THEN date(substr(next_visit_date, 7, 4) || '-' || substr(next_visit_date, 4, 2) || '-' || substr(next_visit_date, 1, 2)) = date('now')\n" +
                "    ELSE\n" +
                "        date(" +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 1, 4) || " +
                "               '-' ||  " +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 6, 2) || " +
                "               '-' ||  " +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 9, 2) " +
                "       )  = date('now')  AND visit_entity_id IS NOT NULL" +
                "    END";
    }

    protected String getTomorrowVisitsDue() {
        return "CASE\n" +
                "    WHEN next_visit_date is not null\n" +
                "        THEN date(substr(next_visit_date, 7, 4) || '-' || substr(next_visit_date, 4, 2) || '-' || substr(next_visit_date, 1, 2)) = date('now','+1 day') " +
                "    ELSE\n" +
                "        date(" +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 1, 4) || " +
                "               '-' ||  " +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 6, 2) || " +
                "               '-' ||  " +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 9, 2) " +
                "       )   = date('now','+1 day')  AND visit_entity_id IS NOT NULL " +
                "    END";
    }

    protected String getOverDue() {
        return "CASE\n" +
                " WHEN next_visit_date is not null\n" +
                "       THEN date(substr(next_visit_date, 7, 4) || '-' || substr(next_visit_date, 4, 2) || '-' || substr(next_visit_date, 1, 2)) < date('now')\n" +
                "   ELSE\n" +
                "        date(" +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 1, 4) || " +
                "               '-' ||  " +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 6, 2) || " +
                "               '-' ||  " +
                "               substr(strftime('%Y-%m-%d', datetime(ec_kvp_prep_register.last_interacted_with / 1000, 'unixepoch', 'localtime')), 9, 2) " +
                "       )  < date('now') AND visit_entity_id IS NOT NULL" +
                "   END";
    }
}
