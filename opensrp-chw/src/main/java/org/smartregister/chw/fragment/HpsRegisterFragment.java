package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.activity.HpsMemberProfileActivity;
import org.smartregister.chw.core.CoreHpsRegisterFragment;
import org.smartregister.chw.hps.R;
import org.smartregister.chw.hps.presenter.BaseHpsRegisterFragmentPresenter;
import org.smartregister.chw.model.HpsRegisterFragmentModel;
import org.smartregister.view.customcontrols.CustomFontTextView;
import org.smartregister.view.customcontrols.FontVariant;

public class HpsRegisterFragment extends CoreHpsRegisterFragment {

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setVisibility(View.VISIBLE);
            titleView.setText(getString(R.string.hps_client_register));
            titleView.setFontVariant(FontVariant.REGULAR);
        }
    }

    @Override
    protected void openProfile(String baseEntityId) {
        HpsMemberProfileActivity.startMe(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new BaseHpsRegisterFragmentPresenter(this, new HpsRegisterFragmentModel(), null);
    }
}
