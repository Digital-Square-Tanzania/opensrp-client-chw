package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.AypInSchoolMemberProfileActivity;
import org.smartregister.chw.activity.AypOutSchoolMemberProfileActivity;
import org.smartregister.chw.core.fragment.CoreAypRegisterFragment;
import org.smartregister.chw.model.AypInSchoolRegisterFragmentModel;
import org.smartregister.chw.presenter.AypOutSchoolRegisterPresenter;
import org.smartregister.view.customcontrols.CustomFontTextView;

public class AypOutSchoolRegisterFragment extends CoreAypRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        AypOutSchoolMemberProfileActivity.startProfileActivity(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new AypOutSchoolRegisterPresenter(this, new AypInSchoolRegisterFragmentModel(), null);
    }

    @Override
    public void setupViews(View view) {

        super.setupViews(view);
        CustomFontTextView titleView = view.findViewById(org.smartregister.hivst.R.id.txt_title_label);
        if (titleView != null) {
            titleView.setText(getString(R.string.ayp_out_school_register_title));
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
        }
    }

}

