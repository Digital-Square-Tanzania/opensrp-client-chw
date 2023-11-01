package org.smartregister.chw.fragment;

import static org.smartregister.chw.core.utils.CoreConstants.DB_CONSTANTS.NOTIFICATION_ID;
import static org.smartregister.chw.core.utils.CoreConstants.DB_CONSTANTS.NOTIFICATION_TYPE;

import android.view.View;

import org.smartregister.chw.core.fragment.BaseChwNotificationFragment;
import org.smartregister.chw.presenter.UpdatesFragmentPresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.Utils;

import java.util.HashMap;

public class UpdatesRegisterFragment extends BaseChwNotificationFragment {

    @Override
    protected void startRegistration() {
        // Overridden not required
    }

    @Override
    protected void initializePresenter() {
        presenter = new UpdatesFragmentPresenter(this);
    }

    @Override
    public void setUniqueID(String qrCode) {
        // Overridden not required
    }

    @Override
    public void setAdvancedSearchFormData(HashMap<String, String> advancedSearchFormData) {
        // Overridden not required
    }

    @Override
    protected void onViewClicked(View view) {
        CommonPersonObjectClient client = (CommonPersonObjectClient) view.getTag();
        String notificationType = Utils.getValue(client.getColumnmaps(), NOTIFICATION_TYPE, true);
        String notificationId = Utils.getValue(client.getColumnmaps(), NOTIFICATION_ID, false);
        getFragmentPresenter().displayDetailsActivity(client, notificationId, notificationType);
    }

    @Override
    public void showNotFoundPopup(String opensrpId) {
        // Overridden not required
    }

    private UpdatesFragmentPresenter getFragmentPresenter() {
        return (UpdatesFragmentPresenter) presenter;
    }
}