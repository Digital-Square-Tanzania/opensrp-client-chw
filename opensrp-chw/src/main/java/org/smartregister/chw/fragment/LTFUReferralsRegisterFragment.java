package org.smartregister.chw.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.LTFUReferralsDetailsViewActivity;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.fragment.BaseReferralRegisterFragment;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.QueryBuilder;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.presenter.LTFUReferralFragmentPresenter;
import org.smartregister.chw.provider.LTFURegisterProvider;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.configurableviews.model.View;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.domain.Task;
import org.smartregister.family.util.DBConstants;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class LTFUReferralsRegisterFragment extends BaseReferralRegisterFragment {
    public Handler handler = new Handler();
    private LTFUReferralFragmentPresenter referralFragmentPresenter;
    private CommonPersonObjectClient commonPersonObjectClient;

    @Override
    public void initializeAdapter(Set<View> visibleColumns, String tableName) {
        LTFURegisterProvider registerProvider = new LTFURegisterProvider(getActivity(), registerActionHandler, paginationViewHandler);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, registerProvider, context().commonrepository(tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public void setClient(CommonPersonObjectClient commonPersonObjectClient) {
        setCommonPersonObjectClient(commonPersonObjectClient);
    }

    @Override
    protected int getToolBarTitle() {
        return R.string.menu_ltfu;
    }

    @Override
    protected String getMainCondition() {
        return "task.business_status = '" + CoreConstants.BUSINESS_STATUS.REFERRED + "' and ec_family_member.date_removed is null";
    }

    @Override
    public CommonPersonObjectClient getCommonPersonObjectClient() {
        return commonPersonObjectClient;
    }

    @Override
    public void setCommonPersonObjectClient(CommonPersonObjectClient commonPersonObjectClient) {
        this.commonPersonObjectClient = commonPersonObjectClient;
    }

    @Override
    protected boolean isValidFilterForFts(CommonRepository commonRepository) {
        return false;
    }

    @Override
    protected void initializePresenter() {
        referralFragmentPresenter = new LTFUReferralFragmentPresenter(this);
        presenter = referralFragmentPresenter;

    }

    @Override
    protected void onViewClicked(android.view.View view) {
        CommonPersonObjectClient client = (CommonPersonObjectClient) view.getTag();
        referralFragmentPresenter.setBaseEntityId(Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false));
        referralFragmentPresenter.fetchClient();

        Task task = getTask(Utils.getValue(client.getColumnmaps(), "_id", false));
        referralFragmentPresenter.setTasksFocus(task.getFocus());
        goToReferralsDetails(client);

    }

    private Task getTask(String taskId) {
        return ChwApplication.getInstance().getTaskRepository().getTaskByIdentifier(taskId);
    }

    private void goToReferralsDetails(CommonPersonObjectClient client) {
        handler.postDelayed(() -> LTFUReferralsDetailsViewActivity.startLTFUReferralsDetailsViewActivity(getActivity(), client, getTask(Utils.getValue(client.getColumnmaps(), "_id", false)), CoreConstants.REGISTERED_ACTIVITIES.LTFU_REFERRALS_REGISTER_ACTIVITY), 100);
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
