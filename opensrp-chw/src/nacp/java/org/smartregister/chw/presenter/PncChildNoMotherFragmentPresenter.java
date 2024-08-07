package org.smartregister.chw.presenter;

import org.smartregister.chw.R;
import org.smartregister.chw.anc.contract.BaseAncRegisterFragmentContract;
import org.smartregister.chw.pnc.presenter.BasePncRegisterFragmentPresenter;
import org.smartregister.chw.util.Constants;

public class PncChildNoMotherFragmentPresenter extends BasePncRegisterFragmentPresenter {

    public PncChildNoMotherFragmentPresenter(BaseAncRegisterFragmentContract.View view, BaseAncRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public void processViewConfigurations() {
        super.processViewConfigurations();
        if (config.getSearchBarText() != null && getView() != null) {
            getView().updateSearchBarHint(getView().getContext().getString(R.string.search_name_or_id));
        }
    }


    @Override
    public String getMainTable() {
        return Constants.TableName.CHILD_NO_MOTHER;
    }

    @Override
    public String getDefaultSortQuery() {
        return "";
    }
}
