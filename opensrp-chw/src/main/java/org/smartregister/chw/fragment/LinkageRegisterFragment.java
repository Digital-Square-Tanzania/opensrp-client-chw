package org.smartregister.chw.fragment;
import androidx.annotation.NonNull;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.ChwLinkageDetailsViewActivity;
import org.smartregister.chw.model.ReferralRegisterFragmentModel;
import org.smartregister.chw.presenter.LinkageRegisterFragmentPresenter;
import org.smartregister.chw.referral.contract.BaseReferralRegisterFragmentContract;
import org.smartregister.chw.referral.domain.MemberObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.view.activity.BaseRegisterActivity;
import org.smartregister.view.contract.BaseRegisterFragmentContract;
import org.smartregister.view.customcontrols.CustomFontTextView;
import org.smartregister.view.customcontrols.FontVariant;

/**
 * Author issyzac on 07/05/2024
 */
public class LinkageRegisterFragment extends ReferralRegisterFragment {

    @Override
    public void setupViews(@NonNull android.view.View view) {
        super.setupViews(view);

        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
            titleView.setText(R.string.nav_menu_linkage);
            titleView.setFontVariant(FontVariant.REGULAR);
        }
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = ((BaseRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        presenter = (BaseRegisterFragmentContract.Presenter) new LinkageRegisterFragmentPresenter((BaseReferralRegisterFragmentContract.View) this, (BaseReferralRegisterFragmentContract.Model) new ReferralRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    protected void openProfile(CommonPersonObjectClient client) {
        ChwLinkageDetailsViewActivity.startChwLinkageDetailsViewActivity(getActivity(), new MemberObject(client), client);
    }

    @Override
    protected void openFollowUpVisit(CommonPersonObjectClient client) {
        //MalariaFollowUpVisitActivity.startMalariaFollowUpActivity(getActivity(), client.getCaseId());
    }

}
