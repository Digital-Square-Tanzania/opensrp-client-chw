package org.smartregister.chw.fragment;

import android.view.View;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.HivProfileActivity;
import org.smartregister.chw.activity.HivRegisterActivity;
import org.smartregister.chw.core.fragment.CoreHivRegisterFragment;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.model.HivRegisterFragmentModel;
import org.smartregister.chw.presenter.HivRegisterFragmentPresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.util.Objects;

import timber.log.Timber;

public class HivRegisterFragment extends CoreHivRegisterFragment {
    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        ((CustomFontTextView) view.findViewById(R.id.txt_title_label)).setText(getString(R.string.hiv_clients));
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = null;
        try {
            viewConfigurationIdentifier = ((HivRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
        presenter = new HivRegisterFragmentPresenter(this, new HivRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    protected void openProfile(CommonPersonObjectClient client) {
        if (getActivity() != null)
            HivProfileActivity.startHivProfileActivity(getActivity(), Objects.requireNonNull(HivDao.getMember(client.getCaseId())));
    }


    @Override
    protected void openFollowUpVisit(@Nullable HivMemberObject hivMemberObject) {
        if (getActivity() != null) {
            try {
                HivProfileActivity.startHivFollowupActivity(getActivity(), hivMemberObject.getBaseEntityId());
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

    }
}


