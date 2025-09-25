package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.AypParentalMemberProfileActivity;
import org.smartregister.chw.core.fragment.CoreAypRegisterFragment;
import org.smartregister.chw.model.AypParentalRegisterFragmentModel;
import org.smartregister.chw.presenter.AypParentalRegisterPresenter;
import org.smartregister.view.customcontrols.CustomFontTextView;

public class AypParentalRegisterFragment extends CoreAypRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        AypParentalMemberProfileActivity.startProfileActivity(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new AypParentalRegisterPresenter(this, new AypParentalRegisterFragmentModel(), null);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        CustomFontTextView titleView = view.findViewById(org.smartregister.hivst.R.id.txt_title_label);
        if (titleView != null) {
            titleView.setText(getString(R.string.ayp_parental_register_title));
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
        }
    }
}
