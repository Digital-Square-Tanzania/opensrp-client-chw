package org.smartregister.chw.presenter;

import static org.smartregister.chw.util.Constants.TableName.CBHS_REGISTER;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.hiv.contract.BaseHivRegisterFragmentContract;
import org.smartregister.chw.hiv.presenter.BaseHivRegisterFragmentPresenter;
import org.smartregister.chw.hiv.util.DBConstants;
import org.smartregister.chw.util.Constants;

public class HivRegisterFragmentPresenter extends BaseHivRegisterFragmentPresenter {
    public HivRegisterFragmentPresenter(BaseHivRegisterFragmentContract.View view, BaseHivRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }


    @Override
    @NotNull
    public String getMainCondition() {
        return " " + Constants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.Key.DATE_REMOVED + " is null " +
                "AND " + CBHS_REGISTER + "." + DBConstants.Key.IS_CLOSED + " = '0' ";
    }

    @Override
    @NotNull
    public String getDueFilterCondition() {
        return CBHS_REGISTER + ".base_entity_id in (select base_entity_id from schedule_service where strftime('%Y-%m-%d') BETWEEN due_date and expiry_date and schedule_name = '" + CoreConstants.SCHEDULE_TYPES.HIV_VISIT + "' and ifnull(not_done_date,'') = '' and ifnull(completion_date,'') = '' )  ";
    }

    @Override
    public void processViewConfigurations() {
        super.processViewConfigurations();
        if (getConfig().getSearchBarText() != null && getView() != null) {
            getView().updateSearchBarHint(getView().getContext().getString(R.string.search_name_or_id));
        }
    }

    @NonNull
    @Override
    public String getDefaultSortQuery() {
        return getMainTable() + "." + DBConstants.Key.HIV_REGISTRATION_DATE + " DESC ";
    }

    @Override
    public String getMainTable() {
        return CBHS_REGISTER;
    }
}
