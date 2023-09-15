package org.smartregister.chw.fragment;

import static android.view.View.GONE;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONException;
import org.smartregister.chw.activity.TbCommunityFollowupDetailsActivity;
import org.smartregister.chw.activity.TbRegisterActivity;
import org.smartregister.chw.core.fragment.CoreTbCommunityFollowupRegisterFragment;
import org.smartregister.chw.hiv.util.DBConstants;
import org.smartregister.chw.model.TbCommunityFollowupFragmentModel;
import org.smartregister.chw.presenter.TbCommunityFollowupFragmentPresenter;
import org.smartregister.chw.tb.dao.TbDao;
import org.smartregister.chw.tb.domain.TbMemberObject;
import org.smartregister.chw.tb.util.Constants;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import timber.log.Timber;

public class TbFollowupRegisterFragment extends CoreTbCommunityFollowupRegisterFragment {

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
        presenter = new TbCommunityFollowupFragmentPresenter(this, new TbCommunityFollowupFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    public void setupViews(android.view.View view) {
        super.setupViews(view);
        view.findViewById(org.smartregister.chw.core.R.id.due_only_layout).setVisibility(GONE);
    }

    @Override
    protected void openProfile(CommonPersonObjectClient client) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), TbCommunityFollowupDetailsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constants.TbMemberObject.MEMBER_OBJECT, TbDao.getCommunityFollowupMember(client.getColumnmaps().get(DBConstants.Key.BASE_ENTITY_ID)));
            intent.putExtras(bundle);

            getActivity().startActivity(intent);
        }
    }

    @Override
    protected void openFollowUpVisit(@Nullable TbMemberObject tbMemberObject) {
        if (getActivity() != null) {
            try {
                TbRegisterActivity.startTbFormActivity(getActivity(), tbMemberObject.getBaseEntityId(), org.smartregister.chw.util.Constants.JSON_FORM.getTbFollowupVisit(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(getActivity(), org.smartregister.chw.util.Constants.JSON_FORM.getTbFollowupVisit()).toString());
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

}

