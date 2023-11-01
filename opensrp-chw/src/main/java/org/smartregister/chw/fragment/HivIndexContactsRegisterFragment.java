package org.smartregister.chw.fragment;

import android.view.View;

import org.json.JSONException;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.HivIndexContactProfileActivity;
import org.smartregister.chw.activity.HivIndexContactsContactsRegisterActivity;
import org.smartregister.chw.core.fragment.CoreHivIndexContactsRegisterFragment;
import org.smartregister.chw.hiv.dao.HivIndexDao;
import org.smartregister.chw.model.HivIndexContactsRegisterFragmentModel;
import org.smartregister.chw.presenter.HivIndexContactsContactsRegisterFragmentPresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.Objects;

import timber.log.Timber;

public class HivIndexContactsRegisterFragment extends CoreHivIndexContactsRegisterFragment {
    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = null;
        try {
            viewConfigurationIdentifier = ((HivIndexContactsContactsRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
        presenter = new HivIndexContactsContactsRegisterFragmentPresenter(this, new HivIndexContactsRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    protected void openProfile(CommonPersonObjectClient client) {
        if (getActivity() != null) {
            HivIndexContactProfileActivity.startHivIndexContactProfileActivity(getActivity(), Objects.requireNonNull(HivIndexDao.getMember(client.getCaseId())));
        }
    }

    @Override
    protected void onViewClicked(View view) {
        if (getActivity() == null) {
            return;
        }
        if (view.getTag() instanceof CommonPersonObjectClient && view.getTag(R.id.VIEW_ID) == CLICK_VIEW_NORMAL) {
            openProfile((CommonPersonObjectClient) view.getTag());
        } else if (view.getTag() instanceof CommonPersonObjectClient && view.getTag(R.id.VIEW_ID) == FOLLOW_UP_VISIT) {
            try {
                HivIndexContactProfileActivity.startHivIndexContactFollowupActivity(getActivity(), HivIndexDao.getMember(((CommonPersonObjectClient) view.getTag()).getCaseId()).getBaseEntityId());
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

}


