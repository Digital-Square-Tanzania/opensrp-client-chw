package org.smartregister.chw.fragment;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONException;
import org.smartregister.chw.activity.TbProfileActivity;
import org.smartregister.chw.activity.TbRegisterActivity;
import org.smartregister.chw.core.fragment.CoreTbRegisterFragment;
import org.smartregister.chw.model.TbRegisterFragmentModel;
import org.smartregister.chw.presenter.TbRegisterFragmentPresenter;
import org.smartregister.chw.tb.dao.TbDao;
import org.smartregister.chw.tb.domain.TbMemberObject;
import org.smartregister.chw.util.Constants;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.Objects;

import timber.log.Timber;

public class TbRegisterFragment extends CoreTbRegisterFragment {

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = null;
        try {
            viewConfigurationIdentifier = ((TbRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
        presenter = new TbRegisterFragmentPresenter(this, new TbRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    protected void openProfile(CommonPersonObjectClient client) {
        if (getActivity() != null)
            TbProfileActivity.startTbProfileActivity(getActivity(), Objects.requireNonNull(TbDao.getMember(client.getCaseId())));
    }

    @Override
    protected void openFollowUpVisit(@Nullable TbMemberObject tbMemberObject) {
        if (getActivity() != null) {
            try {
                TbRegisterActivity.startTbFormActivity(getActivity(), tbMemberObject.getBaseEntityId(), Constants.JSON_FORM.getTbFollowupVisit(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(getActivity(), Constants.JSON_FORM.getTbFollowupVisit()).toString());
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }
}


